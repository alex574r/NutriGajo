package com.example.nutrigajo.data.local

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// Huella o rostro y, desde Android 11, también el PIN o patrón del teléfono como respaldo (antes de eso el sistema no deja
// combinarlos).
object Biometria {
    private val AUTENTICADORES = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_WEAK or DEVICE_CREDENTIAL else BIOMETRIC_WEAK

    // el teléfono tiene con qué desbloquear: una huella o rostro registrados o, en su defecto, un PIN
    fun disponible(contexto: Context): Boolean =
        BiometricManager.from(contexto).canAuthenticate(AUTENTICADORES) == BiometricManager.BIOMETRIC_SUCCESS

    // Muestra la ventana del sistema; `alFallar` cubre cancelar, demasiados intentos y cualquier otro error.
    fun pedir(actividad: FragmentActivity, titulo: String, subtitulo: String, alExito: () -> Unit, alFallar: () -> Unit) {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setSubtitle(subtitulo)
            .setAllowedAuthenticators(AUTENTICADORES)
            .build()
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = alExito()

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = alFallar()
        }
        BiometricPrompt(actividad, ContextCompat.getMainExecutor(actividad), callback).authenticate(info)
    }
}

// La actividad detrás de un contexto de Compose (que puede venir envuelto).
fun Context.actividad(): FragmentActivity? {
    var contexto = this
    while (contexto is ContextWrapper) {
        if (contexto is FragmentActivity) return contexto
        contexto = contexto.baseContext
    }
    return null
}
