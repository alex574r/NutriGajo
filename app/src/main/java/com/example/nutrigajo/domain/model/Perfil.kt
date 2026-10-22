package com.example.nutrigajo.domain.model

// Datos de la cuenta que la interfaz necesita; el documento completo vive en usuarios/{uid}.
data class Perfil(
    val uid: String,
    val nombre: String,
    val correo: String,
    val rol: Rol,
)
