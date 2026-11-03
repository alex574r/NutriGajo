package com.example.nutrigajo.data.repository

import android.graphics.Bitmap
import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.EstadoPlatillo
import com.example.nutrigajo.domain.model.EtiquetaPlatillo
import com.example.nutrigajo.domain.model.MomentoComida
import com.example.nutrigajo.domain.model.Platillo
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.domain.model.limpio
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.io.File

// El banco global de platillos (platillos/{id}). Las reglas de Firestore son las que mandan: solo un nutriólogo o un entrenador
// verificado crea y edita lo suyo, y solo el ADMIN publica o rechaza. Las fotos viven en R2 detrás del Worker, bajo el uid del autor.
// ponytail: listas completas sin paginar; paginar o indexar por momento cuando el banco pase de unos cientos.
class PlatillosRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val fotos: FotosBanco = FotosBanco(auth),
) {
    private val uid: String get() = auth.currentUser?.uid ?: error("Sin sesión")

    private fun coleccion() = db.collection(FirestorePaths.PLATILLOS)

    // ---- lectura (se ordena aquí para no pedir índices compuestos)

    suspend fun mios(): List<Platillo> =
        leer(coleccion().whereEqualTo("autorId", uid)).sortedByDescending { it.actualizadoEn }

    suspend fun publicados(): List<Platillo> =
        leer(coleccion().whereEqualTo("estado", EstadoPlatillo.PUBLICADO.name)).sortedByDescending { it.publicadoEn ?: 0L }

    // De la más antigua a la más reciente, como la cola de verificaciones.
    suspend fun enRevision(): List<Platillo> =
        leer(coleccion().whereEqualTo("estado", EstadoPlatillo.EN_REVISION.name)).sortedBy { it.actualizadoEn }

    private suspend fun leer(consulta: Query): List<Platillo> =
        consulta.get().await().documents.mapNotNull { d -> d.data?.let { platilloDeMapa(d.id, it) } }

    // ---- autor

    // Guarda como borrador o lo manda a revisión; si hay foto nueva la sube antes y apunta su versión. Devuelve el guardado.
    suspend fun guardar(platillo: Platillo, autorNombre: String, autorRol: Rol, enviarARevision: Boolean, foto: ByteArray?, cache: File): Platillo {
        val referencia = if (platillo.id.isEmpty()) coleccion().document() else coleccion().document(platillo.id)
        val version = foto?.let { fotos.subir(RUTA_FOTOS, referencia.id, it, cache) } ?: platillo.fotoVersion
        val ahora = System.currentTimeMillis()
        val nuevo = platillo.limpio().copy(
            id = referencia.id,
            autorId = uid,
            autorNombre = autorNombre,
            autorRol = autorRol,
            fotoVersion = version,
            estado = if (enviarARevision) EstadoPlatillo.EN_REVISION else EstadoPlatillo.BORRADOR,
            motivoRechazo = null,
            creadoEn = if (platillo.creadoEn == 0L) ahora else platillo.creadoEn,
            actualizadoEn = ahora,
        )
        if (platillo.id.isEmpty()) {
            referencia.set(nuevo.aMapa()).await()
        } else {
            // update y no set: conserva lo que puso el ADMIN al rechazarlo (revisor y fecha), que las reglas piden intacto
            referencia.update(nuevo.aMapa() + ("motivoRechazo" to FieldValue.delete())).await()
        }
        return nuevo
    }

    // El autor retira lo publicado o saca de revisión lo suyo para seguir editándolo.
    suspend fun retirar(id: String) = cambiarEstado(id, EstadoPlatillo.RETIRADO)

    suspend fun sacarDeRevision(id: String) = cambiarEstado(id, EstadoPlatillo.BORRADOR)

    private suspend fun cambiarEstado(id: String, estado: EstadoPlatillo) {
        coleccion().document(id).update(mapOf("estado" to estado.name, "actualizadoEn" to System.currentTimeMillis())).await()
    }

    // ponytail: la foto de R2 se queda; el Worker aún no tiene ruta para borrarla.
    suspend fun eliminar(id: String) {
        coleccion().document(id).delete().await()
    }

    // ---- ADMIN

    suspend fun publicar(id: String) {
        val ahora = System.currentTimeMillis()
        coleccion().document(id).update(
            mapOf("estado" to EstadoPlatillo.PUBLICADO.name, "publicadoEn" to ahora, "revisadoPor" to uid, "revisadoEn" to ahora, "actualizadoEn" to ahora),
        ).await()
    }

    suspend fun rechazar(id: String, motivo: String) {
        val ahora = System.currentTimeMillis()
        coleccion().document(id).update(
            mapOf(
                "estado" to EstadoPlatillo.RECHAZADO.name,
                "motivoRechazo" to motivo.trim(),
                "revisadoPor" to uid,
                "revisadoEn" to ahora,
                "actualizadoEn" to ahora,
            ),
        ).await()
    }

    // ---- foto

    // La foto de ese platillo en su versión: de la caché o descargada del Worker; null si no tiene.
    suspend fun foto(platillo: Platillo, cache: File): Bitmap? =
        platillo.fotoVersion?.let { fotos.bajar(RUTA_FOTOS, platillo.autorId, platillo.id, it, cache) }

    private companion object {
        const val RUTA_FOTOS = "platillo"
    }
}

// Los campos que escribe el autor; el estado, la publicación y la revisión los fija quien los cambia.
internal fun Platillo.aMapa(): Map<String, Any> = buildMap {
    put("autorId", autorId)
    put("autorNombre", autorNombre)
    put("autorRol", autorRol.name)
    put("nombre", nombre)
    put("resumen", resumen)
    put("momento", momento.name)
    put("proteinaG", proteinaG)
    put("grasasG", grasasG)
    put("carbosG", carbosG)
    put("kcal", kcal)
    put("minutos", minutos)
    put("ingredientes", ingredientes)
    put("pasos", pasos)
    put("etiquetas", etiquetas.map { it.name })
    fotoVersion?.let { put("fotoVersion", it) }
    put("estado", estado.name)
    put("creadoEn", creadoEn)
    put("actualizadoEn", actualizadoEn)
}

// null si falta lo básico o el estado no se reconoce (un documento de una versión futura de la app).
internal fun platilloDeMapa(id: String, d: Map<String, Any?>): Platillo? {
    val estado = enumDe<EstadoPlatillo>(d["estado"]) ?: return null
    val autorId = d["autorId"] as? String ?: return null
    return Platillo(
        id = id,
        autorId = autorId,
        autorNombre = d["autorNombre"] as? String ?: "",
        // solo un nutriólogo o un entrenador crea platillos: cualquier otro valor se lee como nutriólogo
        autorRol = listOf(Rol.NUTRIOLOGO, Rol.ENTRENADOR).firstOrNull { it.name == d["autorRol"] } ?: Rol.NUTRIOLOGO,
        nombre = d["nombre"] as? String ?: "",
        resumen = d["resumen"] as? String ?: "",
        momento = enumDe<MomentoComida>(d["momento"]) ?: MomentoComida.COMIDA,
        proteinaG = (d["proteinaG"] as? Number)?.toInt() ?: 0,
        grasasG = (d["grasasG"] as? Number)?.toInt() ?: 0,
        carbosG = (d["carbosG"] as? Number)?.toInt() ?: 0,
        kcal = (d["kcal"] as? Number)?.toInt() ?: 0,
        minutos = (d["minutos"] as? Number)?.toInt() ?: 15,
        ingredientes = (d["ingredientes"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
        pasos = (d["pasos"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
        etiquetas = (d["etiquetas"] as? List<*>)?.mapNotNull { enumDe<EtiquetaPlatillo>(it) }.orEmpty(),
        fotoVersion = (d["fotoVersion"] as? Number)?.toLong(),
        estado = estado,
        motivoRechazo = d["motivoRechazo"] as? String,
        creadoEn = (d["creadoEn"] as? Number)?.toLong() ?: 0L,
        actualizadoEn = (d["actualizadoEn"] as? Number)?.toLong() ?: 0L,
        publicadoEn = (d["publicadoEn"] as? Number)?.toLong(),
    )
}
