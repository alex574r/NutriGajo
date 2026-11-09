package com.example.nutrigajo.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.nutrigajo.BuildConfig
import com.example.nutrigajo.domain.model.EntradaCatalogo
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.debeActualizarIndice
import com.example.nutrigajo.domain.model.fichaAEjercicio
import com.example.nutrigajo.domain.model.parsearIndice
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private val ID_CATALOGO = Regex("[0-9]{4,5}")

// Solo las ids que el importador genera: así una id rara nunca llega a la dirección ni al nombre de un archivo de la caché.
internal fun esIdCatalogo(id: String): Boolean = ID_CATALOGO.matches(id)

// El archivo de la caché de la ficha, la foto o el clip de un ejercicio del catálogo.
internal fun nombreArchivoCatalogo(tipo: String, id: String): String =
    "catalogo-$tipo-$id." + when (tipo) {
        "foto" -> "webp"
        "clip" -> "mp4"
        else -> "json"
    }

// El catálogo de ejercicios en R2, detrás del Worker (GET /catalogo/*, solo con sesión). El índice se baja una vez por versión y queda en
// disco; la ficha, la foto y el clip de un ejercicio se bajan solo cuando se abre. Nada aquí lanza: sin red o sin sesión se devuelve lo que
// haya en la caché, o null.
class CatalogoRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val base: String = BuildConfig.WORKER_URL,
    private val directorio: File,
) {
    private val carpeta get() = File(directorio, "catalogo").apply { mkdirs() }

    // El índice de todo el catálogo: el guardado si ya es la última versión; si no, el nuevo. Sin red devuelve el guardado, o nada.
    suspend fun indice(): List<EntradaCatalogo> = withContext(Dispatchers.IO) {
        val versionGuardada = File(carpeta, "version.txt").takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull()
        runCatching {
            val remota = JSONObject(descargar("version.json").decodeToString()).getInt("version")
            if (debeActualizarIndice(versionGuardada, remota)) {
                val texto = descargar("indice-$remota.json").decodeToString()
                // un índice que no se lee no reemplaza al bueno
                if (parsearIndice(texto).isNotEmpty()) {
                    escribir(File(carpeta, "indice.json")) { it.write(texto.toByteArray()) }
                    escribir(File(carpeta, "version.txt")) { it.write(remota.toString().toByteArray()) }
                    directorio.listFiles { f -> f.name.startsWith("catalogo-ficha-") }?.forEach { it.delete() }
                }
            }
        }.onFailure { if (it is CancellationException) throw it }
        File(carpeta, "indice.json").takeIf { it.exists() }?.let { parsearIndice(it.readText()) }.orEmpty()
    }

    suspend fun ficha(id: String): Ejercicio? = withContext(Dispatchers.IO) {
        if (!esIdCatalogo(id)) return@withContext null
        val archivo = File(directorio, nombreArchivoCatalogo("ficha", id))
        if (!archivo.exists()) {
            val bytes = intentarDescargar("ficha/$id.json") ?: return@withContext null
            escribir(archivo) { it.write(bytes) }
        }
        fichaAEjercicio(archivo.readText())
    }

    suspend fun foto(id: String): Bitmap? = withContext(Dispatchers.IO) {
        if (!esIdCatalogo(id)) return@withContext null
        decodificadas[id]?.let { return@withContext it }
        val archivo = File(directorio, nombreArchivoCatalogo("foto", id))
        if (!archivo.exists()) {
            val bytes = intentarDescargar("foto/$id.webp") ?: return@withContext null
            escribir(archivo) { it.write(bytes) }
        }
        BitmapFactory.decodeFile(archivo.path)?.also {
            it.prepareToDraw()
            decodificadas[id] = it
        }
    }

    // El archivo MP4 del clip, para que el reproductor lo lea; null si no se pudo bajar.
    suspend fun clip(id: String): File? = withContext(Dispatchers.IO) {
        if (!esIdCatalogo(id)) return@withContext null
        val archivo = File(directorio, nombreArchivoCatalogo("clip", id))
        if (!archivo.exists()) {
            val bytes = intentarDescargar("clip/$id.mp4") ?: return@withContext null
            escribir(archivo) { it.write(bytes) }
        }
        archivo
    }

    private suspend fun intentarDescargar(ruta: String): ByteArray? = try {
        descargar(ruta)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    // GET /catalogo/{ruta} con el ID token de la sesión; lanza si no hay sesión o el Worker no responde 200.
    private suspend fun descargar(ruta: String): ByteArray {
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: error("Sin sesión")
        val conexion = (URL("$base/catalogo/$ruta").openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) error("El Worker respondió ${conexion.responseCode}")
            return conexion.inputStream.use { it.readBytes() }
        } finally {
            conexion.disconnect()
        }
    }

    // Escribe en un archivo parcial y lo renombra al terminar: un corte a medias no deja un archivo roto en la caché.
    private fun escribir(destino: File, contenido: (java.io.OutputStream) -> Unit) {
        val parcial = File(destino.parentFile, "${destino.name}.part")
        parcial.outputStream().use(contenido)
        parcial.renameTo(destino)
    }

    private companion object {
        // Fotos ya decodificadas, compartidas por todos los repositorios: un octavo del heap, las menos usadas salen solas.
        val decodificadas = CacheLru<Bitmap>(Runtime.getRuntime().maxMemory() / 8) { it.allocationByteCount.toLong() }
    }
}
