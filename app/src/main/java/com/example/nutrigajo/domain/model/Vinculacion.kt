package com.example.nutrigajo.domain.model

// Código con el que un nutriólogo invita a un paciente; se escribe con guion (XXXX-XXXX) pero se guarda sin él.
data class CodigoVinculacion(val codigo: String, val expiraEn: Long) {
    val formateado: String get() = codigo.chunked(4).joinToString("-")
}

// Fila de la lista de pacientes de un nutriólogo.
// `usuarioId` es la cuenta del paciente: con él se encuentra su foto de perfil.
data class PacienteResumen(val id: String, val usuarioId: String, val nombre: String, val correo: String, val fechaAlta: Long)

// Lo que el paciente necesita saber de su nutriólogo.
data class Vinculo(val pacienteId: String, val nutriologoNombre: String)
