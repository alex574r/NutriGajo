package com.example.nutrigajo.data.repository

import com.example.nutrigajo.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// Pide al Worker de Cloudflare (backend/) que avise por FCM al paciente de que su cita quedó confirmada. La app solo manda el
// id de la cita con la sesión del nutriólogo: el Worker comprueba en Firestore que es de ese nutriólogo y está confirmada,
// y saca de ahí el dispositivo del paciente.
class AvisoCitaRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val base: String = BuildConfig.WORKER_URL,
) {
    suspend fun confirmada(citaId: String) = withContext(Dispatchers.IO) {
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: error("Sin sesión")
        val conexion = (URL("$base/citas/notificar").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            conexion.outputStream.use { it.write(JSONObject().put("citaId", citaId).toString().toByteArray()) }
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) throw IOException("El Worker respondió ${conexion.responseCode}")
        } finally {
            conexion.disconnect()
        }
    }
}
