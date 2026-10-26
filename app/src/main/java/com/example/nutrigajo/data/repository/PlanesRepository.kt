package com.example.nutrigajo.data.repository

import com.example.nutrigajo.BuildConfig
import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.PlanContenido
import com.example.nutrigajo.domain.model.PlanEnviado
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// Planes de alimentación y ejercicio que el nutriólogo envía a un paciente. Cada plan son dos cosas: su PDF, que vive en R2
// detrás del Worker de Cloudflare (backend/) y solo se sirve a el nutriólogo y al paciente, y su documento en
// pacientes/{id}/planes/{planId}, con el contenido para partir de él en el siguiente. Un plan enviado no cambia.
// ponytail: HttpURLConnection como en FotoPerfilRepository; el PDF se guarda entero en la caché del teléfono, uno por plan.
class PlanesRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val base: String = BuildConfig.WORKER_URL,
) {
    private fun planes(pacienteId: String) =
        db.collection(FirestorePaths.PACIENTES).document(pacienteId).collection(FirestorePaths.PLANES)

    // El último plan enviado a ese paciente, o null si aún no tiene.
    suspend fun ultimo(pacienteId: String): PlanEnviado? =
        planes(pacienteId).orderBy("enviadoEn", Query.Direction.DESCENDING).limit(1).get().await().documents.firstOrNull()?.aPlan()

    // Sube el PDF, guarda el plan y pide al Worker avisar al paciente. Si solo falla el aviso el plan ya está enviado:
    // devuelve false y el paciente lo verá al abrir la app.
    suspend fun enviar(pacienteId: String, nutriologoNombre: String, contenido: PlanContenido, pdf: File): Boolean {
        val uid = auth.currentUser?.uid ?: error("Sin sesión")
        val ref = planes(pacienteId).document()
        subirPdf(pacienteId, ref.id, pdf)
        ref.set(
            mapOf(
                "enviadoPor" to uid,
                "nutriologoNombre" to nutriologoNombre,
                "enviadoEn" to System.currentTimeMillis(),
                "titulo" to TITULO,
                "contenido" to contenido.aMapa(),
            ),
        ).await()
        return try {
            avisar(pacienteId, ref.id)
        } catch (e: IOException) {
            false
        }
    }

    // El PDF de ese plan: de la caché del teléfono o descargado del Worker con la sesión de quien lo pide.
    suspend fun pdf(pacienteId: String, planId: String, cache: File): File = withContext(Dispatchers.IO) {
        val destino = File(cache, "plan-$planId.pdf")
        if (!destino.exists()) {
            val conexion = abrir("$base/plan/$pacienteId/$planId", "GET")
            try {
                if (conexion.responseCode != HttpURLConnection.HTTP_OK) throw IOException("El Worker respondió ${conexion.responseCode}")
                val parcial = File(cache, "${destino.name}.part")
                parcial.outputStream().use { salida -> conexion.inputStream.use { it.copyTo(salida) } }
                parcial.renameTo(destino)
            } finally {
                conexion.disconnect()
            }
        }
        destino
    }

    private suspend fun subirPdf(pacienteId: String, planId: String, pdf: File) = withContext(Dispatchers.IO) {
        val conexion = abrir("$base/plan/$pacienteId/$planId", "PUT").apply {
            doOutput = true
            setFixedLengthStreamingMode(pdf.length())
            setRequestProperty("Content-Type", "application/pdf")
        }
        try {
            pdf.inputStream().use { entrada -> conexion.outputStream.use { entrada.copyTo(it) } }
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) throw IOException("El Worker respondió ${conexion.responseCode}")
        } finally {
            conexion.disconnect()
        }
    }

    private suspend fun avisar(pacienteId: String, planId: String): Boolean = withContext(Dispatchers.IO) {
        val conexion = abrir("$base/planes/notificar", "POST").apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            conexion.outputStream.use { it.write(JSONObject().put("pacienteId", pacienteId).put("planId", planId).toString().toByteArray()) }
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) throw IOException("El Worker respondió ${conexion.responseCode}")
            JSONObject(conexion.inputStream.bufferedReader().readText()).optBoolean("enviado")
        } finally {
            conexion.disconnect()
        }
    }

    // Conexión al Worker con el ID token de la sesión.
    private suspend fun abrir(direccion: String, metodo: String): HttpURLConnection {
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: error("Sin sesión")
        return (URL(direccion).openConnection() as HttpURLConnection).apply {
            requestMethod = metodo
            connectTimeout = 10_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $token")
        }
    }

    private fun DocumentSnapshot.aPlan(): PlanEnviado? {
        return PlanEnviado(
            id = id,
            enviadoEn = getLong("enviadoEn") ?: return null,
            nutriologoNombre = getString("nutriologoNombre").orEmpty(),
            contenido = (get("contenido") as? Map<*, *>)?.aPlanContenido() ?: PlanContenido(),
        )
    }

    private companion object {
        const val TITULO = "Plan de alimentación y ejercicio"
    }
}
