package com.example.nutrigajo.domain.model

import java.text.Normalizer
import kotlin.math.abs

// No renombrar valores: Firestore guarda el nombre del enum como texto.
enum class EtiquetaPlatillo { SIN_GLUTEN, SIN_LACTOSA, VEGETARIANO, VEGANO, ALTO_PROTEINA, BAJO_CARBOHIDRATOS }

// BORRADOR lo ve solo su autor; EN_REVISION espera al ADMIN; PUBLICADO está en el banco global; RECHAZADO vuelve a su autor con el
// motivo; RETIRADO lo quitó su autor o el ADMIN del banco.
enum class EstadoPlatillo { BORRADOR, EN_REVISION, PUBLICADO, RECHAZADO, RETIRADO }

// Un platillo del banco global que crea un nutriólogo o un entrenador verificado (platillos/{id}). `autorRol` dice cuál de los dos
// es, para mostrar «Receta de nutriólogo» o «Receta de entrenador».
data class Platillo(
    val id: String = "",
    val autorId: String = "",
    val autorNombre: String = "",
    val autorRol: Rol = Rol.NUTRIOLOGO,
    val nombre: String = "",
    val resumen: String = "",
    val momento: MomentoComida = MomentoComida.COMIDA,
    val proteinaG: Int = 0,
    val grasasG: Int = 0,
    val carbosG: Int = 0,
    val kcal: Int = 0,
    // tiempo de preparación
    val minutos: Int = 15,
    // un ingrediente por elemento
    val ingredientes: List<String> = emptyList(),
    // «cómo prepararlo», un paso por elemento
    val pasos: List<String> = emptyList(),
    val etiquetas: List<EtiquetaPlatillo> = emptyList(),
    // la versión de la foto en R2 (la hora de su subida); null si aún no tiene
    val fotoVersion: Long? = null,
    val estado: EstadoPlatillo = EstadoPlatillo.BORRADOR,
    // solo con RECHAZADO
    val motivoRechazo: String? = null,
    val creadoEn: Long = 0,
    val actualizadoEn: Long = 0,
    val publicadoEn: Long? = null,
) {
    companion object {
        // los topes de las reglas de Firestore (firebase/firestore.rules, platilloValido)
        const val MIN_NOMBRE = 3
        const val MAX_NOMBRE = 80
        const val MIN_RESUMEN = 10
        const val MAX_RESUMEN = 300
        const val MAX_INGREDIENTES = 20
        const val MAX_INGREDIENTE = 80
        const val MAX_PASOS = 12
        const val MAX_PASO = 300
        const val MAX_MACRO_G = 500
        const val MAX_KCAL = 3000
        const val MIN_MINUTOS = 1
        const val MAX_MINUTOS = 240
        const val MAX_ETIQUETAS = 4
        const val MAX_MOTIVO = 200
    }
}

enum class ProblemaPlatillo { NOMBRE, RESUMEN, INGREDIENTES, PASOS, MACROS, KCAL, MINUTOS, ETIQUETAS, FOTO }

// Lo primero que impide mandar el platillo a revisión, en el orden del enum; null si está listo. Guardar un borrador es más laxo: solo
// pide el nombre.
fun Platillo.problemaParaRevision(): ProblemaPlatillo? = when {
    nombre.trim().length !in Platillo.MIN_NOMBRE..Platillo.MAX_NOMBRE -> ProblemaPlatillo.NOMBRE
    resumen.trim().length !in Platillo.MIN_RESUMEN..Platillo.MAX_RESUMEN -> ProblemaPlatillo.RESUMEN
    ingredientes.isEmpty() || ingredientes.size > Platillo.MAX_INGREDIENTES ||
        ingredientes.any { it.isBlank() || it.trim().length > Platillo.MAX_INGREDIENTE } -> ProblemaPlatillo.INGREDIENTES
    pasos.isEmpty() || pasos.size > Platillo.MAX_PASOS || pasos.any { it.isBlank() || it.trim().length > Platillo.MAX_PASO } -> ProblemaPlatillo.PASOS
    listOf(proteinaG, grasasG, carbosG).any { it !in 0..Platillo.MAX_MACRO_G } -> ProblemaPlatillo.MACROS
    kcal !in 0..Platillo.MAX_KCAL -> ProblemaPlatillo.KCAL
    minutos !in Platillo.MIN_MINUTOS..Platillo.MAX_MINUTOS -> ProblemaPlatillo.MINUTOS
    etiquetas.size > Platillo.MAX_ETIQUETAS -> ProblemaPlatillo.ETIQUETAS
    fotoVersion == null -> ProblemaPlatillo.FOTO
    else -> null
}

// Un borrador solo necesita el nombre; el resto puede estar a medias (las reglas lo aceptan).
fun Platillo.puedeGuardarseComoBorrador(): Boolean = nombre.trim().length in Platillo.MIN_NOMBRE..Platillo.MAX_NOMBRE

// Lo que se guarda: textos sin espacios de más, solo los ingredientes y pasos que dicen algo y cada etiqueta una vez.
fun Platillo.limpio(): Platillo = copy(
    nombre = nombre.trim(),
    resumen = resumen.trim(),
    ingredientes = ingredientes.map { it.trim() }.filter { it.isNotEmpty() },
    pasos = pasos.map { it.trim() }.filter { it.isNotEmpty() },
    etiquetas = etiquetas.distinct(),
)

// Una copia para editar en limpio: borrador sin foto (la foto vive bajo el id del original), sin motivo de rechazo ni fechas.
fun Platillo.duplicado(): Platillo = copy(
    id = "",
    estado = EstadoPlatillo.BORRADOR,
    fotoVersion = null,
    motivoRechazo = null,
    creadoEn = 0,
    actualizadoEn = 0,
    publicadoEn = null,
)

// Las kcal que salen de los macros: 4 por gramo de proteína o de carbohidratos y 9 por gramo de grasa.
fun kcalSugeridas(proteinaG: Int, grasasG: Int, carbosG: Int): Int = 4 * proteinaG + 4 * carbosG + 9 * grasasG

// Las kcal escritas difieren de las de los macros en más del 15 %. Sin macros (un té, por ejemplo) no hay con qué comparar.
fun Platillo.kcalNoCuadran(): Boolean {
    if (proteinaG + grasasG + carbosG == 0) return false
    val sugeridas = kcalSugeridas(proteinaG, grasasG, carbosG)
    return abs(kcal - sugeridas) > sugeridas * 0.15
}

// Lo que se busca en la lista de Explorar: el texto en el nombre, el resumen o los ingredientes (sin distinguir mayúsculas ni
// acentos) y los filtros que haya.
fun Platillo.coincide(busqueda: String, momento: MomentoComida?, etiqueta: EtiquetaPlatillo?): Boolean {
    if (momento != null && this.momento != momento) return false
    if (etiqueta != null && etiqueta !in etiquetas) return false
    val q = busqueda.trim().sinAcentos()
    return q.isEmpty() || (nombre + " " + resumen + " " + ingredientes.joinToString(" ")).sinAcentos().contains(q)
}

// Lo que se escribe en el menú de un plan al elegir este platillo: «Nombre: ingredientes · N kcal». Cabe en el tope de un menú: si no
// cabe se acortan los ingredientes con «…» y las kcal se conservan.
fun Platillo.textoParaPlan(): String {
    val cola = if (kcal > 0) " · $kcal kcal" else ""
    val cabeza = if (ingredientes.isEmpty()) nombre else "$nombre: ${ingredientes.joinToString(", ")}"
    val sitio = PlanContenido.MAX_MENU - cola.length
    return (if (cabeza.length <= sitio) cabeza else cabeza.take(sitio - 1).trimEnd() + "…") + cola
}

// El platillo como lo entrega el selector de un plan: una comida del banco con su momento y el texto que se escribe en el menú.
fun Platillo.comoItemDelBanco(): ItemBanco =
    ItemBanco(tipo = TipoBanco.COMIDA, nombre = nombre.take(ItemBanco.MAX_NOMBRE), texto = textoParaPlan(), momento = momento)

// Lo que ve un paciente: se esconden los platillos que nombran una de sus alergias (en el nombre o en un ingrediente, sin importar
// mayúsculas, acentos ni singular o plural) y, según su dieta, se prefieren los que llevan la etiqueta que le toca; el orden de la lista
// no cambia más. Mientras no se sepan sus alergias (`expedienteListo` en false: el expediente aún carga o no se pudo leer) no se muestra
// ninguno, porque sin ellas no se puede cuidar nada.
// ponytail: coincidencia de texto, no una garantía médica; una alergia mal escrita o un ingrediente con otro nombre no se detecta.
fun List<Platillo>.paraPaciente(alergias: List<String>, dieta: Dieta?, expedienteListo: Boolean = true): List<Platillo> {
    if (!expedienteListo) return emptyList()
    val terminos = alergias.map { terminosDe(it) }.filter { it.isNotEmpty() }
    val visibles = filter { p ->
        val texto = (p.nombre + " " + p.ingredientes.joinToString(" ")).sinAcentos()
        val formas = texto.split(NO_LETRAS).filter { it.isNotEmpty() }.map { variantes(it) }
        terminos.none { alergia -> alergia.any { nombra(texto, formas, it) } }
    }
    val preferidas = when (dieta) {
        Dieta.PLANTAS -> setOf(EtiquetaPlatillo.VEGETARIANO, EtiquetaPlatillo.VEGANO)
        Dieta.BAJA_CARBOHIDRATOS -> setOf(EtiquetaPlatillo.BAJO_CARBOHIDRATOS)
        else -> emptySet()
    }
    // sortedBy es estable: dentro de cada grupo se conserva el orden de entrada
    return if (preferidas.isEmpty()) visibles else visibles.sortedBy { p -> if (p.etiquetas.any { it in preferidas }) 0 else 1 }
}

private val MARCAS = Regex("\\p{Mn}+")
private val NO_LETRAS = Regex("[^a-z0-9ñ]+")

private val LACTEOS = listOf("lactosa", "lacteo", "leche", "queso", "yogur", "yogurt", "crema", "mantequilla", "requeson", "suero", "nata", "helado")
private val FRUTOS_SECOS = listOf("nuez", "almendra", "avellana", "pistache", "pistacho", "anacardo", "castana", "macadamia", "pecana", "pinon", "cacahuate", "mani")
private val MARISCOS = listOf("marisco", "camaron", "langosta", "langostino", "cangrejo", "jaiba", "ostion", "ostra", "almeja", "mejillon", "pulpo", "calamar", "vieira")

// Lo que lleva cada alergia que ofrece el expediente (alergias_comunes) sin llamarse igual: «Lactosa» está en la leche, el queso o el
// yogur. Las demás alergias, las que escribe el paciente, solo buscan lo que escribió.
private val ALERGENOS: Map<String, List<String>> = mapOf(
    "lactosa" to LACTEOS,
    "lacteo" to LACTEOS,
    "gluten" to listOf("gluten", "trigo", "harina", "pan", "pasta", "cebada", "centeno", "espelta", "avena", "galleta", "couscous", "seitan"),
    "frutos secos" to FRUTOS_SECOS,
    "fruto seco" to FRUTOS_SECOS,
    "cacahuate" to listOf("cacahuate", "mani"),
    "marisco" to MARISCOS,
    "huevo" to listOf("huevo", "clara", "yema", "mayonesa", "merengue"),
    "soya" to listOf("soya", "soja", "tofu", "tempeh", "edamame", "miso"),
)

// Cómo se escribe una palabra en singular o en plural: ella misma y sin la «s», sin la «es» o con «z» en lugar de «ces» (nuez, nueces).
// Dos palabras son la misma si comparten alguna forma.
private fun variantes(palabra: String): Set<String> = buildSet {
    add(palabra)
    if (palabra.endsWith("s")) add(palabra.dropLast(1))
    if (palabra.endsWith("es")) add(palabra.dropLast(2))
    if (palabra.endsWith("ces")) add(palabra.dropLast(3) + "z")
}

// Lo que se busca por una alergia: lo que se escribió y, si es de las que ofrece el expediente, lo que la lleva.
private fun terminosDe(alergia: String): List<String> {
    val escrita = alergia.sinAcentos().trim()
    if (escrita.isEmpty()) return emptyList()
    val clave = ALERGENOS.keys.firstOrNull { it in variantes(escrita) }
    return listOf(escrita) + clave?.let { ALERGENOS.getValue(it) }.orEmpty()
}

// Un término de una palabra se compara con cada palabra del texto (así «pan» no es «panela»); uno de varias, como frase.
private fun nombra(texto: String, formas: List<Set<String>>, termino: String): Boolean =
    if (' ' in termino) termino in texto else variantes(termino).let { v -> formas.any { it.any(v::contains) } }

private fun String.sinAcentos(): String = Normalizer.normalize(this, Normalizer.Form.NFD).replace(MARCAS, "").lowercase()
