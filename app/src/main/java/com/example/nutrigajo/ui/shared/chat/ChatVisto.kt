package com.example.nutrigajo.ui.shared.chat

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.UltimoMensaje

// Hasta qué mensaje ha visto esta cuenta de cada chat (la hora del más nuevo que tuvo a la vista). Vive en este teléfono, como lo de
// las notificaciones de planes: basta para el punto de «sin leer».
// ponytail: SharedPreferences; si se quiere el mismo estado en varios teléfonos, pasa a Firestore.
internal fun chatVistoHasta(contexto: Context, uid: String, pacienteId: String): Long =
    contexto.prefs().getLong(clave(uid, pacienteId), 0L)

internal fun marcarChatVisto(contexto: Context, uid: String, pacienteId: String, hasta: Long) {
    if (hasta > chatVistoHasta(contexto, uid, pacienteId)) contexto.prefs().edit().putLong(clave(uid, pacienteId), hasta).apply()
}

// ¿Hay un mensaje de la otra persona que `uid` aún no ha visto?
internal fun UltimoMensaje?.sinLeer(uid: String, visto: Long): Boolean = this != null && autorId != uid && enviadoEn > visto

// La vista previa de la conversación para una tarjeta: el último mensaje (con «Tú:» si es mío, «Foto» si no lleva texto) o `vacio`.
@Composable
internal fun vistaPreviaChat(ultimo: UltimoMensaje?, uid: String, vacio: String): String {
    if (ultimo == null) return vacio
    val cuerpo = ultimo.texto.ifBlank { stringResource(R.string.chat_vista_foto) }
    return if (ultimo.autorId == uid) stringResource(R.string.chat_vista_yo, cuerpo) else cuerpo
}

private fun clave(uid: String, pacienteId: String) = "chat_visto_${uid}_$pacienteId"

private fun Context.prefs() = getSharedPreferences("nutrigajo", Context.MODE_PRIVATE)
