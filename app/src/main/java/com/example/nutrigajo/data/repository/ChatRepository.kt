package com.example.nutrigajo.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.nutrigajo.BuildConfig
import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.Mensaje
import com.example.nutrigajo.domain.model.UltimoMensaje
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val LIMITE_MENSAJES = 100L
private const val LARGO_RESUMEN = 120

// Hasta cuántos pendientes se cuentan; la tarjeta del inicio pinta «9+» al llegar a este tope.
const val TOPE_PENDIENTES = 10

// El chat de un paciente con su nutriólogo: los mensajes viven en pacientes/{id}/mensajes (los dos los leen y los escriben) y el
// último queda resumido en pacientes/{id}.ultimoMensaje. Las fotos van a R2 detrás del Worker de Cloudflare (backend/), que también
// avisa por push a la otra persona. Cada foto se guarda en la caché del teléfono.
// ponytail: se escuchan los últimos 100 mensajes en tiempo real; si algún día hay más, se pagina hacia atrás. HttpURLConnection
// como en PlanesRepository.
class ChatRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val base: String = BuildConfig.WORKER_URL,
) {
    // la cuenta que escribe; vacío si no hay sesión
    val uid: String get() = auth.currentUser?.uid.orEmpty()

    private fun paciente(pacienteId: String) = db.collection(FirestorePaths.PACIENTES).document(pacienteId)

    private fun coleccion(pacienteId: String) = paciente(pacienteId).collection(FirestorePaths.MENSAJES)

    // Los últimos mensajes de la conversación, del más nuevo al más viejo, y cada vez que cambian. Un mensaje recién enviado aparece
    // al instante con `pendiente`, antes de que el servidor lo confirme.
    fun mensajes(pacienteId: String): Flow<List<Mensaje>> = callbackFlow {
        val escucha = coleccion(pacienteId)
            .orderBy("enviadoEn", Query.Direction.DESCENDING)
            .limit(LIMITE_MENSAJES)
            .addSnapshotListener(MetadataChanges.INCLUDE) { instantanea, error ->
                if (error != null) close(error) else if (instantanea != null) trySend(instantanea.documents.mapNotNull { it.aMensaje() })
            }
        awaitClose { escucha.remove() }
    }

    // Cuántos mensajes de la otra persona llegaron después de `desde`, hasta TOPE_PENDIENTES: así no se lee toda la conversación
    // solo para pintar un número en la tarjeta.
    suspend fun pendientes(pacienteId: String, desde: Long): Int {
        val yo = uid
        return coleccion(pacienteId)
            .whereGreaterThan("enviadoEn", desde)
            .orderBy("enviadoEn", Query.Direction.DESCENDING)
            .limit(TOPE_PENDIENTES.toLong())
            .get().await()
            .documents.count { it.getString("autorId") != yo }
    }

    // Escribe el mensaje y el resumen del paciente en un solo lote y devuelve el id del mensaje. Con foto, la sube antes al Worker y
    // la deja en la caché para que se vea sin descargarla. Sin conexión el lote queda pendiente y sale al volver; el mensaje ya se ve.
    suspend fun enviar(pacienteId: String, texto: String, jpeg: ByteArray?, cache: File): String {
        val yo = auth.currentUser?.uid ?: error("Sin sesión")
        val ref = coleccion(pacienteId).document()
        if (jpeg != null) {
            subirFoto(pacienteId, ref.id, jpeg)
            withContext(Dispatchers.IO) { archivoDe(cache, ref.id).writeBytes(jpeg) }
        }
        val ahora = System.currentTimeMillis()
        db.runBatch { lote ->
            lote.set(
                ref,
                buildMap<String, Any> {
                    put("autorId", yo)
                    put("texto", texto)
                    put("enviadoEn", ahora)
                    if (jpeg != null) put("foto", true)
                },
            )
            lote.update(
                paciente(pacienteId),
                "ultimoMensaje",
                mapOf("autorId" to yo, "enviadoEn" to ahora, "texto" to texto.take(LARGO_RESUMEN), "foto" to (jpeg != null)),
            )
        }.await()
        return ref.id
    }

    // Pide al Worker avisar a la otra persona del chat; ya con el mensaje confirmado en Firestore.
    suspend fun avisar(pacienteId: String, mensajeId: String) = withContext(Dispatchers.IO) {
        val conexion = abrir("$base/mensajes/notificar", "POST").apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            conexion.outputStream.use { it.write(JSONObject().put("pacienteId", pacienteId).put("mensajeId", mensajeId).toString().toByteArray()) }
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) throw RechazoDelWorker(conexion.responseCode)
        } finally {
            conexion.disconnect()
        }
    }

    // La foto de ese mensaje: de la caché del teléfono o descargada del Worker con la sesión de quien la pide.
    suspend fun foto(pacienteId: String, mensajeId: String, cache: File): Bitmap = withContext(Dispatchers.IO) {
        val destino = archivoDe(cache, mensajeId)
        if (!destino.exists()) {
            val conexion = abrir("$base/chat/$pacienteId/$mensajeId", "GET")
            try {
                if (conexion.responseCode != HttpURLConnection.HTTP_OK) throw RechazoDelWorker(conexion.responseCode)
                val parcial = File(cache, "${destino.name}.part")
                parcial.outputStream().use { salida -> conexion.inputStream.use { it.copyTo(salida) } }
                parcial.renameTo(destino)
            } finally {
                conexion.disconnect()
            }
        }
        BitmapFactory.decodeFile(destino.path) ?: throw IOException("La foto guardada no es una imagen")
    }

    private suspend fun subirFoto(pacienteId: String, mensajeId: String, jpeg: ByteArray) = withContext(Dispatchers.IO) {
        val conexion = abrir("$base/chat/$pacienteId/$mensajeId", "PUT").apply {
            doOutput = true
            setFixedLengthStreamingMode(jpeg.size)
            setRequestProperty("Content-Type", "image/jpeg")
        }
        try {
            conexion.outputStream.use { it.write(jpeg) }
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) throw RechazoDelWorker(conexion.responseCode)
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

    private fun archivoDe(cache: File, mensajeId: String) = File(cache, "chat-$mensajeId.jpg")

    private fun DocumentSnapshot.aMensaje(): Mensaje? {
        return Mensaje(
            id = id,
            autorId = getString("autorId") ?: return null,
            texto = getString("texto").orEmpty(),
            foto = getBoolean("foto") == true,
            enviadoEn = getLong("enviadoEn") ?: return null,
            pendiente = metadata.hasPendingWrites(),
        )
    }
}

// El resumen del último mensaje que guarda el registro del paciente; null si aún no hay conversación.
internal fun DocumentSnapshot.aUltimoMensaje(): UltimoMensaje? {
    val resumen = get("ultimoMensaje") as? Map<*, *> ?: return null
    return UltimoMensaje(
        autorId = resumen["autorId"] as? String ?: return null,
        enviadoEn = (resumen["enviadoEn"] as? Number)?.toLong() ?: return null,
        texto = resumen["texto"] as? String ?: "",
        foto = resumen["foto"] as? Boolean == true,
    )
}
