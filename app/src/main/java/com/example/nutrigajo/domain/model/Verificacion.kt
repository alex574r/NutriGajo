package com.example.nutrigajo.domain.model

enum class EstadoVerificacion { PENDIENTE, APROBADA, RECHAZADA }

// Solicitud de un nutriólogo tal como él la ve; vive en verificaciones/{uid}.
data class Verificacion(
    val estado: EstadoVerificacion,
    val cedula: String,
    val nombreCedula: String,
    // solo con RECHAZADA
    val motivo: String?,
)

// Solicitud pendiente tal como la ve el ADMIN.
data class SolicitudVerificacion(
    val nutriologoId: String,
    val nombreCuenta: String,
    val correo: String,
    val nombreCedula: String,
    val cedula: String,
    val enviadoEn: Long,
)
