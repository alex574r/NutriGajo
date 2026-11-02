package com.example.nutrigajo.domain.model

enum class EstadoVerificacion { PENDIENTE, APROBADA, RECHAZADA }

// Solicitud de un nutriólogo o de un entrenador tal como la ve su dueño; vive en verificaciones/{uid}.
// El nutriólogo manda su cédula; el entrenador, su credencial en texto (certificación, institución y folio).
data class Verificacion(
    val estado: EstadoVerificacion,
    val cedula: String,
    val nombreCedula: String,
    // solo con RECHAZADA
    val motivo: String?,
    val credencial: String = "",
)

// Solicitud pendiente tal como la ve el ADMIN. `uid` es el de quien la envió.
data class SolicitudVerificacion(
    val uid: String,
    val nombreCuenta: String,
    val correo: String,
    val nombreCedula: String,
    val cedula: String,
    val enviadoEn: Long,
    val credencial: String = "",
) {
    val esEntrenador: Boolean get() = credencial.isNotEmpty()
}
