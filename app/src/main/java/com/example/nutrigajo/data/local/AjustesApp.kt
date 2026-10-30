package com.example.nutrigajo.data.local

import android.content.Context
import androidx.compose.runtime.mutableStateOf

// Las preferencias de este teléfono (no de la cuenta): el modo oscuro y el desbloqueo con huella o rostro. Cada una vive en
// SharedPreferences y en un estado de Compose para que la pantalla reaccione al cambiarla.
// ponytail: singleton con estado; pasa a DataStore con un ViewModel cuando haya más preferencias locales.
object AjustesApp {
    private const val ARCHIVO = "ajustes_app"
    private const val CLAVE_OSCURO = "modo_oscuro"
    private const val CLAVE_BLOQUEO = "bloqueo_biometrico"

    val oscuro = mutableStateOf(false)
    val bloqueo = mutableStateOf(false)

    // la app está tapada por la pantalla de desbloqueo; vive aquí (y no en la actividad) para que girar el teléfono no la quite
    val bloqueada = mutableStateOf(false)

    private fun prefs(contexto: Context) = contexto.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    // una vez, al abrir la actividad y antes de dibujar
    fun iniciar(contexto: Context) {
        val p = prefs(contexto)
        oscuro.value = p.getBoolean(CLAVE_OSCURO, false)
        bloqueo.value = p.getBoolean(CLAVE_BLOQUEO, false)
    }

    fun cambiarOscuro(contexto: Context, valor: Boolean) {
        prefs(contexto).edit().putBoolean(CLAVE_OSCURO, valor).apply()
        oscuro.value = valor
    }

    fun cambiarBloqueo(contexto: Context, valor: Boolean) {
        prefs(contexto).edit().putBoolean(CLAVE_BLOQUEO, valor).apply()
        bloqueo.value = valor
    }
}
