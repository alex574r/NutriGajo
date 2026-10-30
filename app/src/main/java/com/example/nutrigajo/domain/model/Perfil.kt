package com.example.nutrigajo.domain.model

// Datos de la cuenta que la interfaz necesita; el documento completo vive en usuarios/{uid}.
data class Perfil(
    val uid: String,
    val nombre: String,
    val correo: String,
    val rol: Rol,
    // milisegundos del alta; null si el documento no lo trae
    val fechaAlta: Long? = null,
    // versión de la portada en R2 (la hora de su subida); null si no ha puesto una
    val portadaVersion: Long? = null,
    // versión de la foto de perfil en R2; null si no ha puesto una
    val avatarVersion: Long? = null,
    // `nombre` es estos dos juntos; las cuentas más viejas solo lo traen junto y aquí quedan vacíos
    val nombres: String = "",
    val apellidos: String = "",
)
