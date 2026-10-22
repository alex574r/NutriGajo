package com.example.nutrigajo.domain.model

// No renombrar valores: Firestore guarda el nombre del enum como texto.

enum class Objetivo { BAJAR_PESO, GANAR_MUSCULO, MEJORAR_ALIMENTACION, MEJORAR_RESISTENCIA, CONDICION_SALUD }

enum class Dieta { PLANTAS, BAJA_CARBOHIDRATOS, ESPECIALIZADA, TRADICIONAL }

enum class CalidadSueno { EXCELENTE, BUENA, NORMAL, MALA, INSOMNIO }

enum class EjercicioPreferido { CORRER, CAMINAR, SENDERISMO, PATINAR, CICLISMO, PESAS, CARDIO, YOGA, OTRO }

// Datos y estilo de vida del paciente (vive en pacientes/{id}.expediente). Todo es opcional: lo puede llenar
// el paciente con el asistente, el nutriólogo en consulta, o los dos; gana la última edición.
data class Expediente(
    val objetivo: Objetivo? = null,
    val sexo: Sexo? = null,
    // texto ISO yyyy-MM-dd; la edad se calcula, no se guarda
    val fechaNacimiento: String? = null,
    val pesoKg: Double? = null,
    val estaturaCm: Double? = null,
    // 1 (sedentario) a 5 (atleta)
    val nivelActividad: Int? = null,
    val condiciones: List<String> = emptyList(),
    val alergias: List<String> = emptyList(),
    val dieta: Dieta? = null,
    // días de ejercicio por semana, 0 a 7
    val diasEjercicio: Int? = null,
    val ejercicios: List<EjercicioPreferido> = emptyList(),
    val suplementos: List<String> = emptyList(),
    val caloriasObjetivo: Int? = null,
    val calidadSueno: CalidadSueno? = null,
    // el asistente guarda cada paso: con completo = false se retoma en `paso`
    val paso: Int = 0,
    val completo: Boolean = false,
    val actualizadoPor: Rol? = null,
    val actualizadoEn: Long = 0L,
) {
    // Hay al menos una respuesta (el asistente puede haberse omitido entero).
    val vacio: Boolean
        get() = this.copy(paso = 0, completo = false, actualizadoPor = null, actualizadoEn = 0L) == Expediente()
}
