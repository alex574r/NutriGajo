package com.example.nutrigajo.data.repository

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import com.example.nutrigajo.BuildConfig
import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val CALIDAD_JPEG = 85

// Las dos fotos del perfil: cada una tiene su ruta en el Worker, su versión en `usuarios/{uid}` y su tamaño.
// La cuadrada se recorta al centro; la otra solo se reduce.
enum class FotoPerfil(val ruta: String, val campo: String, val ladoMaximo: Int, val cuadrada: Boolean) {
    PORTADA("portada", "portadaVersion", 1600, false),
    AVATAR("avatar", "avatarVersion", 512, true),
}

// El archivo vive en R2 detrás del Worker de Cloudflare (backend/) y en Firestore solo queda su versión.
// Cada versión se guarda en la caché del teléfono.
// ponytail: HttpURLConnection y un archivo por versión; Coil/OkHttp cuando haya más imágenes remotas.
class FotoPerfilRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val base: String = BuildConfig.WORKER_URL,
) {
    // Reduce la foto elegida a su tamaño (recortándola al centro si es cuadrada) y la pasa a JPEG: el Worker no acepta más de 3 MB.
    suspend fun preparar(resolver: ContentResolver, uri: Uri, foto: FotoPerfil): ByteArray = preparar(resolver, uri, foto.ladoMaximo, foto.cuadrada)

    // Lo mismo para cualquier otra imagen que sube la app (las del chat): `ladoMaximo` es el lado más largo, o el más corto si es cuadrada.
    suspend fun preparar(resolver: ContentResolver, uri: Uri, ladoMaximo: Int, cuadrada: Boolean): ByteArray = withContext(Dispatchers.IO) {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val ancho = info.size.width
            val alto = info.size.height
            if (cuadrada) {
                val escala = min(1f, ladoMaximo.toFloat() / min(ancho, alto))
                val destinoAncho = (ancho * escala).roundToInt()
                val destinoAlto = (alto * escala).roundToInt()
                decoder.setTargetSize(destinoAncho, destinoAlto)
                val lado = min(destinoAncho, destinoAlto)
                val izquierda = (destinoAncho - lado) / 2
                val arriba = (destinoAlto - lado) / 2
                decoder.setCrop(Rect(izquierda, arriba, izquierda + lado, arriba + lado))
            } else {
                val escala = ladoMaximo.toFloat() / max(ancho, alto)
                if (escala < 1f) decoder.setTargetSize((ancho * escala).roundToInt(), (alto * escala).roundToInt())
            }
        }
        ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, it) }.toByteArray()
    }

    // Sube la imagen con el ID token de la sesión, apunta la versión en el perfil, la deja en la caché y devuelve la versión.
    suspend fun subir(foto: FotoPerfil, cache: File, jpeg: ByteArray): Long = withContext(Dispatchers.IO) {
        val usuario = auth.currentUser ?: error("Sin sesión")
        val token = usuario.getIdToken(false).await().token ?: error("Sin token")
        val conexion = (URL("$base/${foto.ruta}").openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 20_000
            setFixedLengthStreamingMode(jpeg.size)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "image/jpeg")
        }
        try {
            conexion.outputStream.use { it.write(jpeg) }
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) throw IOException("El Worker respondió ${conexion.responseCode}")
            val version = JSONObject(conexion.inputStream.bufferedReader().readText()).getLong("version")
            db.collection(FirestorePaths.USUARIOS).document(usuario.uid)
                .update(mapOf(foto.campo to version, "ultimaModificacion" to System.currentTimeMillis()))
                .await()
            guardar(foto, cache, usuario.uid, version) { it.write(jpeg) }
            version
        } finally {
            conexion.disconnect()
        }
    }

    // La foto de esa versión: de la caché o descargada del Worker.
    suspend fun cargar(foto: FotoPerfil, cache: File, uid: String, version: Long): Bitmap = withContext(Dispatchers.IO) {
        if (!archivoDe(foto, cache, uid, version).exists()) {
            val conexion = (URL("$base/${foto.ruta}/$uid?v=$version").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 20_000
            }
            try {
                if (conexion.responseCode != HttpURLConnection.HTTP_OK) throw IOException("El Worker respondió ${conexion.responseCode}")
                guardar(foto, cache, uid, version) { salida -> conexion.inputStream.use { it.copyTo(salida) } }
            } finally {
                conexion.disconnect()
            }
        }
        BitmapFactory.decodeFile(archivoDe(foto, cache, uid, version).path) ?: throw IOException("La foto guardada no es una imagen")
    }

    // La foto de perfil de otra persona (el nutriólogo viendo a sus pacientes). No conoce su versión, así que pregunta al
    // Worker con el ETag de la copia que tiene: si no cambió responde 304 y se usa esa; null si no tiene foto.
    suspend fun avatarAjeno(cache: File, uid: String): Bitmap? = withContext(Dispatchers.IO) {
        val prefijo = "ajeno-$uid-"
        val copia = cache.listFiles { f -> f.name.startsWith(prefijo) && f.extension == "jpg" }?.firstOrNull()
        val etag = copia?.name?.removePrefix(prefijo)?.removeSuffix(".jpg")
        val conexion = (URL("$base/${FotoPerfil.AVATAR.ruta}/$uid").openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
            if (etag != null) setRequestProperty("If-None-Match", "\"$etag\"")
        }
        try {
            when (conexion.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> copia?.let { BitmapFactory.decodeFile(it.path) }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    copia?.delete()
                    null
                }
                HttpURLConnection.HTTP_OK -> {
                    // el ETag de R2 es hexadecimal entre comillas: sirve tal cual como parte del nombre del archivo
                    val nuevo = conexion.getHeaderField("ETag")?.trim('"')?.takeIf { Regex("[0-9a-f-]+").matches(it) } ?: return@withContext null
                    val destino = File(cache, "$prefijo$nuevo.jpg")
                    val parcial = File(cache, "${destino.name}.part")
                    parcial.outputStream().use { salida -> conexion.inputStream.use { it.copyTo(salida) } }
                    parcial.renameTo(destino)
                    cache.listFiles { f -> f.name.startsWith(prefijo) && f != destino }?.forEach { it.delete() }
                    BitmapFactory.decodeFile(destino.path)
                }
                else -> throw IOException("El Worker respondió ${conexion.responseCode}")
            }
        } finally {
            conexion.disconnect()
        }
    }

    private fun archivoDe(foto: FotoPerfil, cache: File, uid: String, version: Long) = File(cache, "${foto.ruta}-$uid-$version.jpg")

    // Escribe la versión en un archivo parcial y lo renombra al terminar; después quita las versiones anteriores.
    private fun guardar(foto: FotoPerfil, cache: File, uid: String, version: Long, escribir: (OutputStream) -> Unit) {
        val destino = archivoDe(foto, cache, uid, version)
        val parcial = File(cache, "${destino.name}.part")
        parcial.outputStream().use(escribir)
        parcial.renameTo(destino)
        cache.listFiles { f -> f.name.startsWith("${foto.ruta}-$uid-") && f != destino }?.forEach { it.delete() }
    }
}
