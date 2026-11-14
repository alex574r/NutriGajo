package com.example.nutrigajo.domain.model

import org.json.JSONArray
import org.json.JSONObject

// El catálogo de ejercicios que vive en R2 (backend/scripts/importar-catalogo.mjs lo arma y subir-catalogo.mjs lo sube): la app baja un índice
// ligero una vez, busca en él en el teléfono y trae la ficha, la foto y el clip de un ejercicio solo al abrirlo. Un ejercicio del catálogo entra
// a la app como `Ejercicio` con `fuente = CATALOGO`.

// Quien «firma» los ejercicios del catálogo, para las pantallas que muestran al autor.
const val AUTOR_CATALOGO = "catalogo"
const val NOMBRE_AUTOR_CATALOGO = "Catálogo Nutri Gajo"

// Lo que trae el índice de cada ejercicio: lo justo para listarlo y buscarlo.
data class EntradaCatalogo(val id: String, val titulo: String, val zona: ZonaCuerpo, val tipo: TipoEjercicio, val equipo: EquipoEjercicio)

// El índice (`indice-{n}.json`): una lista de filas [id, título, zona, tipo, equipo]. Una fila mal formada, con un valor que esta versión de la
// app no conoce o sin título se salta; un índice ilegible es una lista vacía.
fun parsearIndice(json: String): List<EntradaCatalogo> {
    val filas = try {
        JSONArray(json)
    } catch (_: Exception) {
        return emptyList()
    }
    return (0 until filas.length()).mapNotNull { i -> filas.optJSONArray(i)?.let(::entradaDe) }
}

private fun entradaDe(fila: JSONArray): EntradaCatalogo? {
    if (fila.length() != 5) return null
    val id = fila.optString(0)
    val titulo = fila.optString(1)
    if (id.isBlank() || titulo.isBlank()) return null
    return EntradaCatalogo(
        id = id,
        titulo = titulo,
        zona = ZonaCuerpo.entries.firstOrNull { it.name == fila.optString(2) } ?: return null,
        tipo = TipoEjercicio.entries.firstOrNull { it.name == fila.optString(3) } ?: return null,
        equipo = EquipoEjercicio.entries.firstOrNull { it.name == fila.optString(4) } ?: return null,
    )
}

private const val DURACION_CATALOGO_MIN = 5

// Calorías por minuto por tipo: el catálogo no trae calorías, así que se estiman (lo mismo que hace el importador).
private fun kcalPorMinuto(tipo: TipoEjercicio): Int = when (tipo) {
    TipoEjercicio.FUERZA -> 5
    TipoEjercicio.CARDIO -> 9
    TipoEjercicio.FLEXIBILIDAD, TipoEjercicio.YOGA -> 3
}

// La entrada como ejercicio, sin descripción ni pasos (esos vienen en la ficha). El catálogo no trae nivel: todos son PRINCIPIANTE y la
// duración y las kcal son las de por defecto (5 min).
fun EntradaCatalogo.aEjercicio(): Ejercicio = Ejercicio(
    id = id,
    autorId = AUTOR_CATALOGO,
    autorNombre = NOMBRE_AUTOR_CATALOGO,
    titulo = titulo,
    zona = zona,
    tipo = tipo,
    equipo = equipo,
    nivel = NivelEjercicio.PRINCIPIANTE,
    duracionMin = DURACION_CATALOGO_MIN,
    kcal = DURACION_CATALOGO_MIN * kcalPorMinuto(tipo),
    estado = EstadoEjercicio.PUBLICADO,
    fuente = FuenteEjercicio.CATALOGO,
)

// La ficha completa (`ficha/{id}.json`) como ejercicio; null si no trae id o título o no se puede leer. Lo que falte (zona, tipo, equipo…) toma
// el valor de por defecto de la entrada.
fun fichaAEjercicio(json: String): Ejercicio? {
    val f = try {
        JSONObject(json)
    } catch (_: Exception) {
        return null
    }
    val id = f.optString("id")
    val titulo = f.optString("titulo")
    if (id.isBlank() || titulo.isBlank()) return null
    val base = EntradaCatalogo(
        id = id,
        titulo = titulo,
        zona = ZonaCuerpo.entries.firstOrNull { it.name == f.optString("zona") } ?: ZonaCuerpo.CUERPO_COMPLETO,
        tipo = TipoEjercicio.entries.firstOrNull { it.name == f.optString("tipo") } ?: TipoEjercicio.FUERZA,
        equipo = EquipoEjercicio.entries.firstOrNull { it.name == f.optString("equipo") } ?: EquipoEjercicio.MAQUINA,
    ).aEjercicio()
    val pasos = f.optJSONArray("pasos")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() } }.orEmpty()
    return base.copy(
        descripcion = f.optString("descripcion"),
        pasos = pasos,
        duracionMin = f.optInt("duracionMin", base.duracionMin).coerceAtLeast(1),
        kcal = f.optInt("kcal", base.kcal).coerceAtLeast(0),
    )
}

// Las entradas cuyo título contiene todas las palabras buscadas (sin acentos ni mayúsculas, en cualquier orden: «press banca» encuentra «Press de
// banca con barra») y cumplen los filtros; sin búsqueda, las primeras. Hasta `limite`.
fun List<EntradaCatalogo>.buscar(busqueda: String, zona: ZonaCuerpo?, tipo: TipoEjercicio?, limite: Int = 60): List<EntradaCatalogo> {
    val palabras = busqueda.sinAcentos().split(' ').filter { it.isNotBlank() }
    return asSequence()
        .filter { (zona == null || it.zona == zona) && (tipo == null || it.tipo == tipo) }
        .filter { entrada -> entrada.titulo.sinAcentos().let { titulo -> palabras.all { titulo.contains(it) } } }
        .take(limite)
        .toList()
}

// Lo que muestra una lista de ejercicios con la búsqueda y los filtros de Explorar, ya como ejercicios. El catálogo no trae niveles (todo es de
// PRINCIPIANTE): pedir otro nivel no deja ninguno. Sin `limite` salen todos; la lista es perezosa y las fotos se bajan solo de las filas a la vista.
fun List<EntradaCatalogo>.ejerciciosFiltrados(
    busqueda: String,
    zona: ZonaCuerpo?,
    tipo: TipoEjercicio?,
    nivel: NivelEjercicio?,
    limite: Int = Int.MAX_VALUE,
): List<Ejercicio> =
    if (nivel != null && nivel != NivelEjercicio.PRINCIPIANTE) emptyList()
    else buscar(busqueda, zona, tipo, limite).map { it.aEjercicio() }

// El ejercicio que se abrió por su id: uno del banco de los entrenadores tal cual; uno del catálogo con su ficha si ya se bajó (trae los pasos) o,
// mientras baja, con lo que dice el índice (sin pasos), para no dejar la pantalla en blanco. Null si la id no existe en ninguna parte.
fun ejercicioAbierto(id: String?, banco: List<Ejercicio>, fichas: Map<String, Ejercicio>, indice: List<EntradaCatalogo>): Ejercicio? {
    if (id == null) return null
    return banco.firstOrNull { it.id == id } ?: fichas[id] ?: indice.firstOrNull { it.id == id }?.aEjercicio()
}

// El índice local solo se vuelve a bajar si el del servidor es de una versión más nueva (o no hay ninguno guardado).
fun debeActualizarIndice(versionLocal: Int?, versionRemota: Int): Boolean = versionLocal == null || versionRemota > versionLocal
