package com.example.nutrigajo.domain.model

// No renombrar valores: Firestore guarda el nombre del enum como texto.
enum class ZonaCuerpo { PECHO, ESPALDA, PIERNAS, BRAZOS, HOMBROS, ABDOMEN, CUERPO_COMPLETO }

enum class TipoEjercicio { FUERZA, CARDIO, FLEXIBILIDAD, YOGA }

enum class NivelEjercicio { PRINCIPIANTE, INTERMEDIO, AVANZADO }

enum class EquipoEjercicio { SIN_EQUIPO, MANCUERNAS, BANDAS, BARRA, MAQUINA }

// BORRADOR lo ve solo su autor; EN_REVISION espera al ADMIN; PUBLICADO está en el banco global; RECHAZADO vuelve a su autor con el
// motivo; RETIRADO lo quitó su autor o el ADMIN del banco.
enum class EstadoEjercicio { BORRADOR, EN_REVISION, PUBLICADO, RECHAZADO, RETIRADO }

// Un ejercicio del banco global que crea un entrenador (ejercicios/{id}). Fase 1: foto y texto; el video llega después.
data class Ejercicio(
    val id: String = "",
    val autorId: String = "",
    val autorNombre: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val zona: ZonaCuerpo = ZonaCuerpo.CUERPO_COMPLETO,
    val tipo: TipoEjercicio = TipoEjercicio.FUERZA,
    val nivel: NivelEjercicio = NivelEjercicio.PRINCIPIANTE,
    val equipo: EquipoEjercicio = EquipoEjercicio.SIN_EQUIPO,
    val duracionMin: Int = 10,
    // «cómo hacerlo», un paso por elemento
    val pasos: List<String> = emptyList(),
    val consejos: String = "",
    val contraindicaciones: String = "",
    // calorías aproximadas de la sesión completa
    val kcal: Int = 0,
    // la versión de la foto en R2 (la hora de su subida); null si aún no tiene
    val fotoVersion: Long? = null,
    val estado: EstadoEjercicio = EstadoEjercicio.BORRADOR,
    // solo con RECHAZADO
    val motivoRechazo: String? = null,
    val creadoEn: Long = 0,
    val actualizadoEn: Long = 0,
    val publicadoEn: Long? = null,
) {
    companion object {
        // los topes de las reglas de Firestore (firebase/firestore.rules, ejercicioValido)
        const val MIN_TITULO = 3
        const val MAX_TITULO = 80
        const val MIN_DESCRIPCION = 10
        const val MAX_DESCRIPCION = 600
        const val MAX_PASOS = 10
        const val MAX_PASO = 200
        const val MAX_TEXTO = 400
        const val MIN_DURACION = 1
        const val MAX_DURACION = 180
        const val MAX_KCAL = 2000
        const val MAX_MOTIVO = 200
    }
}

enum class ProblemaEjercicio { TITULO, DESCRIPCION, PASOS, DURACION, KCAL, TEXTO_LARGO, FOTO }

// Lo primero que impide mandar el ejercicio a revisión; null si está listo. Guardar un borrador es más laxo: solo pide el título.
fun Ejercicio.problemaParaRevision(): ProblemaEjercicio? = when {
    titulo.trim().length !in Ejercicio.MIN_TITULO..Ejercicio.MAX_TITULO -> ProblemaEjercicio.TITULO
    descripcion.trim().length !in Ejercicio.MIN_DESCRIPCION..Ejercicio.MAX_DESCRIPCION -> ProblemaEjercicio.DESCRIPCION
    pasos.isEmpty() || pasos.size > Ejercicio.MAX_PASOS || pasos.any { it.isBlank() || it.trim().length > Ejercicio.MAX_PASO } -> ProblemaEjercicio.PASOS
    duracionMin !in Ejercicio.MIN_DURACION..Ejercicio.MAX_DURACION -> ProblemaEjercicio.DURACION
    kcal !in 0..Ejercicio.MAX_KCAL -> ProblemaEjercicio.KCAL
    consejos.trim().length > Ejercicio.MAX_TEXTO || contraindicaciones.trim().length > Ejercicio.MAX_TEXTO -> ProblemaEjercicio.TEXTO_LARGO
    fotoVersion == null -> ProblemaEjercicio.FOTO
    else -> null
}

// Un borrador solo necesita título; el resto puede estar a medias (las reglas lo aceptan).
fun Ejercicio.puedeGuardarseComoBorrador(): Boolean = titulo.trim().length in Ejercicio.MIN_TITULO..Ejercicio.MAX_TITULO

// Lo que se guarda: textos sin espacios de más y solo los pasos que dicen algo.
fun Ejercicio.limpio(): Ejercicio = copy(
    titulo = titulo.trim(),
    descripcion = descripcion.trim(),
    pasos = pasos.map { it.trim() }.filter { it.isNotEmpty() },
    consejos = consejos.trim(),
    contraindicaciones = contraindicaciones.trim(),
)

// Una copia para editar en limpio: borrador sin foto (la foto vive bajo el id del original), sin motivo de rechazo ni fechas.
fun Ejercicio.duplicado(): Ejercicio = copy(
    id = "",
    estado = EstadoEjercicio.BORRADOR,
    fotoVersion = null,
    motivoRechazo = null,
    creadoEn = 0,
    actualizadoEn = 0,
    publicadoEn = null,
)

// Lo que se busca en la lista de Explorar: el texto en el título, la descripción o la zona, y los filtros que haya.
fun Ejercicio.coincide(
    busqueda: String,
    zona: ZonaCuerpo?,
    tipo: TipoEjercicio?,
    nivel: NivelEjercicio?,
): Boolean {
    if (zona != null && this.zona != zona) return false
    if (tipo != null && this.tipo != tipo) return false
    if (nivel != null && this.nivel != nivel) return false
    val q = busqueda.trim()
    return q.isEmpty() || titulo.contains(q, ignoreCase = true) || descripcion.contains(q, ignoreCase = true)
}
