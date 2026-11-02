package com.example.nutrigajo.domain.model

// Código de 4 dígitos con el que un nutriólogo invita a un paciente; si algún día es más largo, se muestra en grupos de 4 con guion.
data class CodigoVinculacion(val codigo: String, val expiraEn: Long) {
    val formateado: String get() = codigo.chunked(4).joinToString("-")
}

// Fila de la lista de pacientes de un nutriólogo.
// `usuarioId` es la cuenta del paciente: con él se encuentra su foto de perfil.
// `ultimoMensaje` es el último del chat con él, para el punto de «sin leer»; `metas` son sus metas diarias.
data class PacienteResumen(
    val id: String,
    val usuarioId: String,
    val nombre: String,
    val correo: String,
    val fechaAlta: Long,
    val ultimoMensaje: UltimoMensaje? = null,
    val metas: MetasDiarias = MetasDiarias(),
)

// Lo que el paciente necesita saber de su nutriólogo; `ultimoMensaje` es el último del chat con él y `metas` sus metas diarias.
// `nutriologoId` es la cuenta del nutriólogo: con él se encuentra su foto de perfil.
data class Vinculo(
    val pacienteId: String,
    val nutriologoNombre: String,
    val ultimoMensaje: UltimoMensaje? = null,
    val metas: MetasDiarias = MetasDiarias(),
    val nutriologoId: String = "",
)
