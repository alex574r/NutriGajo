package com.example.nutrigajo.ui.paciente.citas

import android.content.Context

// La última cita próxima que el paciente ya vio al abrir «Mis citas», guardada en el teléfono (no en Firestore): basta para quitar el
// «Nueva» de la fila del inicio. Con otra cita nueva vuelve a salir.
// ponytail: SharedPreferences como lo visto de los planes; si se quiere el mismo estado en varios teléfonos, pasa a Firestore.
internal fun citaVistaId(contexto: Context, uid: String): String? = contexto.prefs().getString("cita_vista_$uid", null)

internal fun marcarCitaVista(contexto: Context, uid: String, citaId: String) = contexto.prefs().edit().putString("cita_vista_$uid", citaId).apply()

private fun Context.prefs() = getSharedPreferences("nutrigajo", Context.MODE_PRIVATE)
