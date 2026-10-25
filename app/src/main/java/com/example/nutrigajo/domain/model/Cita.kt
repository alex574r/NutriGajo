package com.example.nutrigajo.domain.model

// No renombrar valores: Firestore guarda el nombre del enum como texto.
enum class EstadoCita { PENDIENTE, CONFIRMADA, RECHAZADA, CANCELADA }

// Cita que un paciente pide a un nutriólogo del directorio; vive en citas/{id}.
data class Cita(
    val id: String,
    val nutriologoId: String,
    val nutriologoNombre: String,
    // uid de la cuenta del paciente: puede pedir la cita antes de vincularse, por eso no es un pacientes/{id}
    val usuarioId: String,
    val pacienteNombre: String,
    // milisegundos de la hora de inicio
    val inicio: Long,
    val estado: EstadoCita,
) {
    // sigue por atender: todavía no llega su hora y no se rechazó ni se canceló
    fun vigente(ahora: Long): Boolean =
        inicio > ahora && (estado == EstadoCita.PENDIENTE || estado == EstadoCita.CONFIRMADA)
}
