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

// Pide al Worker de Cloudflare (backend/) que avise por FCM a la otra persona de que se cerró su vínculo. La app solo manda el id del
// registro con la sesión de quien lo cerró: el Worker comprueba en Firestore que fue esa persona y saca de ahí a quién avisar y su dispositivo.
class AvisoVinculoRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val base: String = BuildConfig.WORKER_URL,
) {
    // true si el aviso salió; false si no había a dónde mandarlo (sin dispositivo, dispositivo caducado o ya avisado).
    suspend fun avisar(pacienteId: String): Boolean = withContext(Dispatchers.IO) {
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: error("Sin sesión")
        val conexion = (URL("$base/vinculos/notificar").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            conexion.outputStream.use { it.write(JSONObject().put("pacienteId", pacienteId).toString().toByteArray()) }
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) throw IOException("El Worker respondió ${conexion.responseCode}")
            JSONObject(conexion.inputStream.bufferedReader().readText()).optBoolean("enviado", false)
        } finally {
            conexion.disconnect()
        }
    }
}
