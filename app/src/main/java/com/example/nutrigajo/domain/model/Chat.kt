package com.example.nutrigajo.domain.model

// Un mensaje del chat entre el nutriólogo y su paciente. `foto`: lleva una imagen (el archivo vive en R2, detrás del Worker).
// `pendiente`: ya se ve en este teléfono, pero Firestore aún no lo confirma (recién enviado o sin conexión).
data class Mensaje(
    val id: String,
    val autorId: String,
    val texto: String,
    val foto: Boolean,
    val enviadoEn: Long,
    val pendiente: Boolean = false,
)

// El último mensaje de una conversación, guardado en el registro del paciente: basta para el punto de «sin leer» y la vista previa
// sin tener que leer el chat.
data class UltimoMensaje(val autorId: String, val enviadoEn: Long, val texto: String, val foto: Boolean)
