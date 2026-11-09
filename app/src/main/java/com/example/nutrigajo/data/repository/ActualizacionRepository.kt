package com.example.nutrigajo.data.repository

import com.example.nutrigajo.BuildConfig
import com.example.nutrigajo.domain.Actualizacion
import com.example.nutrigajo.domain.actualizacionDeJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

// Lo más que se lee del manifiesto: pesa unos cientos de bytes, lo que pase de aquí no es un manifiesto.
private const val MAX_MANIFIESTO = 64 * 1024

// Busca la última versión publicada y la descarga del Worker de Cloudflare (backend/: GET /app/ultima y GET /app/apk/{versionCode}, públicas).
// La app nunca descarga de una dirección que venga en el manifiesto: arma la suya con el número, así un manifiesto no la manda a otro sitio.
class ActualizacionRepository(private val base: String = BuildConfig.WORKER_URL) {
    // La última versión publicada; null si aún no hay ninguna o si el manifiesto no es válido. Sin red (o el Worker con otro error) lanza
    // IOException: quien busca a mano lo dice, la búsqueda automática se queda callada.
    suspend fun consultar(): Actualizacion? = withContext(Dispatchers.IO) {
        val conexion = abrir("$base/app/ultima")
        try {
            when (conexion.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    conexion.inputStream.use { leerAcotado(it, MAX_MANIFIESTO) }?.let(::actualizacionDeJson)
                }
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw IOException("El Worker respondió ${conexion.responseCode}")
            }
        } finally {
            conexion.disconnect()
        }
    }

    // Descarga el APK a un archivo parcial y solo lo deja en `destino` si coincide con lo publicado (tamaño y SHA-256); si no, lo borra y
    // lanza IOException. Se corta si se cancela la corrutina. `onAvance` recibe los bytes que lleva.
    suspend fun descargar(actualizacion: Actualizacion, destino: File, onAvance: (Long) -> Unit) = withContext(Dispatchers.IO) {
        destino.parentFile?.mkdirs()
        val parcial = File(destino.path + ".part")
        val conexion = abrir("$base/app/apk/${actualizacion.versionCode}")
        try {
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) throw IOException("El Worker respondió ${conexion.responseCode}")
            parcial.outputStream().use { salida ->
                conexion.inputStream.use { entrada ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val leidos = entrada.read(buffer)
                        if (leidos < 0) break
                        ensureActive()
                        total += leidos
                        // más de lo publicado: no es ese archivo, no se sigue llenando el teléfono
                        if (total > actualizacion.bytes) throw IOException("El archivo es más grande de lo publicado")
                        salida.write(buffer, 0, leidos)
                        onAvance(total)
                    }
                }
            }
            if (!archivoCoincide(parcial, actualizacion.bytes, actualizacion.sha256)) throw IOException("El archivo no coincide con lo publicado")
            destino.delete()
            if (!parcial.renameTo(destino)) throw IOException("No se pudo guardar el archivo")
        } catch (e: Exception) {
            parcial.delete()
            throw e
        } finally {
            conexion.disconnect()
        }
    }

    private fun abrir(direccion: String) = (URL(direccion).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 20_000
    }
}

// Lee la respuesta como texto sin pasar de `maximo` bytes: null si es más larga (no es un manifiesto). Sin InputStream.readNBytes, que no existe
// en Android 10 a 12 (minSdk 29).
internal fun leerAcotado(entrada: InputStream, maximo: Int): String? {
    val acumulado = ByteArrayOutputStream()
    val trozo = ByteArray(4096)
    while (true) {
        val leidos = entrada.read(trozo, 0, minOf(trozo.size, maximo + 1 - acumulado.size()))
        if (leidos < 0) break
        acumulado.write(trozo, 0, leidos)
        if (acumulado.size() > maximo) return null
    }
    // String(bytes, charset) y no ByteArrayOutputStream.toString(Charset), que es de la API 33
    return String(acumulado.toByteArray(), Charsets.UTF_8)
}

// El archivo existe y tiene exactamente el tamaño y el SHA-256 (hexadecimal en minúsculas) publicados.
internal fun archivoCoincide(archivo: File, bytes: Long, sha256: String): Boolean {
    if (!archivo.isFile || archivo.length() != bytes) return false
    val resumen = MessageDigest.getInstance("SHA-256")
    archivo.inputStream().use { entrada ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val leidos = entrada.read(buffer)
            if (leidos < 0) break
            resumen.update(buffer, 0, leidos)
        }
    }
    return resumen.digest().joinToString("") { "%02x".format(it) } == sha256
}
