package com.example.nutrigajo.domain.model

// Las metas diarias de un paciente: agua, pasos, calorías y minutos de movimiento. Las fija su nutriólogo y el propio paciente
// puede ajustarlas; la última escritura manda y `por` dice de quién fue (null mientras sean las de siempre, sin nada guardado).
// Los topes son los de las reglas de Firestore (metasValidas): si se cambian aquí, se cambian allá.
data class MetasDiarias(
    val aguaMl: Int = 2000,
    val pasos: Int = 8000,
    val kcal: Int = 1750,
    val minutos: Int = 30,
    val por: Rol? = null,
) {
    companion object {
        val AGUA_ML = 250..6000
        val PASOS = 1000..50000
        val KCAL = 200..5000
        val MINUTOS = 5..300

        // lo que sube o baja un paso del contador de cada una
        const val PASO_AGUA_ML = 250
        const val PASO_PASOS = 500
        const val PASO_KCAL = 50
        const val PASO_MINUTOS = 5
    }
}
