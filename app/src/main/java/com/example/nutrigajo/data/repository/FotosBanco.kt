package com.example.nutrigajo.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.nutrigajo.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

// El archivo del clip de un ejercicio en la caché. Su prefijo («ejercicio-clip-») no coincide con el de las fotos («ejercicio-{autor}-{id}-»),
// así que limpiar las versiones viejas de una foto nunca borra el clip.
internal fun nombreArchivoClip(autor: String, id: String, version: Long) = "ejercicio-clip-$autor-$id-$version.mp4"

// Las fotos de los bancos globales (ejercicios y platillos) en R2, detrás del Worker. `ruta` es el segmento de la dirección del Worker
// ("ejercicio" o "platillo") y también el prefijo de los archivos de la caché; cada foto vive bajo el uid de su autor.
class FotosBanco(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val base: String = BuildConfig.WORKER_URL,
) {
    // Sube el JPEG con el ID token de la sesión y devuelve su versión; la deja en la caché para no bajarla otra vez.
    suspend fun subir(ruta: String, id: String, jpeg: ByteArray, cache: File): Long = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: error("Sin sesión")
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: error("Sin token")
        val conexion = (URL("$base/$ruta/$id").openConnection() as HttpURLConnection).apply {
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
            guardarEnCache(cache, ruta, uid, id, version) { it.write(jpeg) }
            version
        } finally {
            conexion.disconnect()
        }
    }

    // La foto de ese elemento en su versión: de la caché o descargada del Worker.
    suspend fun bajar(ruta: String, autorId: String, id: String, version: Long, cache: File): Bitmap? = withContext(Dispatchers.IO) {
        decodificadas[clave(ruta, autorId, id, version)]?.let { return@withContext it }
        val archivo = archivoDe(cache, ruta, autorId, id, version)
        if (!archivo.exists()) {
            val conexion = (URL("$base/$ruta/$autorId/$id?v=$version").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 20_000
            }
            try {
                if (conexion.responseCode != HttpURLConnection.HTTP_OK) throw IOException("El Worker respondió ${conexion.responseCode}")
                guardarEnCache(cache, ruta, autorId, id, version) { salida -> conexion.inputStream.use { it.copyTo(salida) } }
            } finally {
                conexion.disconnect()
            }
        }
        BitmapFactory.decodeFile(archivo.path)?.also {
            // la textura se sube ahora, en este hilo, y no en el cuadro en que la foto aparece al hacer scroll
            it.prepareToDraw()
            decodificadas[clave(ruta, autorId, id, version)] = it
        }
    }

    // Sube el clip (MP4) de un ejercicio con el ID token de la sesión y devuelve su versión; lo deja en la caché para no bajarlo otra vez.
    suspend fun subirClip(id: String, mp4: ByteArray, cache: File): Long = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: error("Sin sesión")
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: error("Sin token")
        val conexion = (URL("$base/ejercicio/$id/clip").openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 30_000
            setFixedLengthStreamingMode(mp4.size)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "video/mp4")
        }
        try {
            conexion.outputStream.use { it.write(mp4) }
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) throw IOException("El Worker respondió ${conexion.responseCode}")
            val version = JSONObject(conexion.inputStream.bufferedReader().readText()).getLong("version")
            guardarClipEnCache(cache, uid, id, version) { it.write(mp4) }
            version
        } finally {
            conexion.disconnect()
        }
    }

    // El clip de ese ejercicio en su versión: de la caché o descargado del Worker; null si el Worker no lo tiene o no hay red.
    suspend fun bajarClip(autorId: String, id: String, version: Long, cache: File): File? = withContext(Dispatchers.IO) {
        val archivo = File(cache, nombreArchivoClip(autorId, id, version))
        if (archivo.exists()) return@withContext archivo
        val conexion = (URL("$base/ejercicio/$autorId/$id/clip?v=$version").openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
        }
        try {
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            guardarClipEnCache(cache, autorId, id, version) { salida -> conexion.inputStream.use { it.copyTo(salida) } }
            archivo
        } finally {
            conexion.disconnect()
        }
    }

    // Escribe en un archivo parcial y lo renombra al terminar; después quita las versiones anteriores de ese clip.
    private fun guardarClipEnCache(cache: File, autor: String, id: String, version: Long, escribir: (OutputStream) -> Unit) {
        val destino = File(cache, nombreArchivoClip(autor, id, version))
        val parcial = File(cache, "${destino.name}.part")
        parcial.outputStream().use(escribir)
        parcial.renameTo(destino)
        cache.listFiles { f -> f.name.startsWith("ejercicio-clip-$autor-$id-") && f != destino }?.forEach { it.delete() }
    }

    private companion object {
        // Fotos ya decodificadas, compartidas por todos los repositorios: volver a una lista no las decodifica otra vez. Un octavo del heap;
        // la versión va en la clave, así que una foto cambiada no se confunde con la anterior (que sale sola por LRU).
        val decodificadas = CacheLru<Bitmap>(Runtime.getRuntime().maxMemory() / 8) { it.allocationByteCount.toLong() }

        fun clave(ruta: String, autor: String, id: String, version: Long) = "$ruta/$autor/$id/$version"
    }

    private fun archivoDe(cache: File, ruta: String, autor: String, id: String, version: Long) = File(cache, "$ruta-$autor-$id-$version.jpg")

    // Escribe en un archivo parcial y lo renombra al terminar; después quita las versiones anteriores de esa foto.
    private fun guardarEnCache(cache: File, ruta: String, autor: String, id: String, version: Long, escribir: (OutputStream) -> Unit) {
        val destino = archivoDe(cache, ruta, autor, id, version)
        val parcial = File(cache, "${destino.name}.part")
        parcial.outputStream().use(escribir)
        parcial.renameTo(destino)
        cache.listFiles { f -> f.name.startsWith("$ruta-$autor-$id-") && f != destino }?.forEach { it.delete() }
    }
}
