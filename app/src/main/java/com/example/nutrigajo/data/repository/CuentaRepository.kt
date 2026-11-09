package com.example.nutrigajo.data.repository

import com.example.nutrigajo.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// La confirmación de identidad (el enlace del correo) tiene más de cinco minutos: hay que volver a confirmar antes de borrar la cuenta.
class ConfirmacionVencidaException : IOException("La confirmación de identidad venció")

// Qué conteste el Worker a «borrar mi cuenta» se vuelve: nada si salió bien, «confirma otra vez» con un 401, o un error para reintentar.
internal fun excepcionDeRespuesta(codigo: Int): IOException? = when (codigo) {
    HttpURLConnection.HTTP_OK -> null
    HttpURLConnection.HTTP_UNAUTHORIZED -> ConfirmacionVencidaException()
    else -> IOException("El Worker respondió $codigo")
}

// Pide al Worker de Cloudflare (backend/) borrar la cuenta de quien tiene la sesión abierta: cierra sus vínculos (el expediente, los planes y
// el chat se conservan, solo para leer) y borra lo demás, el perfil y la cuenta de Firebase. La sesión que se manda tiene que haberse confirmado
// hace poco con el enlace del correo; el Worker lo comprueba y, si no, contesta 401.
class CuentaRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val base: String = BuildConfig.WORKER_URL,
) {
    suspend fun eliminar() = withContext(Dispatchers.IO) {
        // el token se pide de nuevo: el que se guardó antes de volver a confirmar la identidad todavía trae la hora vieja
        val token = auth.currentUser?.getIdToken(true)?.await()?.token ?: error("Sin sesión")
        val conexion = (URL("$base/cuenta/eliminar").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            // borrar tarda: son varias consultas y borrados en el servidor
            readTimeout = 60_000
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            excepcionDeRespuesta(conexion.responseCode)?.let { throw it }
        } finally {
            conexion.disconnect()
        }
    }
}
