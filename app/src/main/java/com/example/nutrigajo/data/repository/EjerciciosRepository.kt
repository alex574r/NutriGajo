package com.example.nutrigajo.data.repository

import android.graphics.Bitmap
import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.EquipoEjercicio
import com.example.nutrigajo.domain.model.EstadoEjercicio
import com.example.nutrigajo.domain.model.NivelEjercicio
import com.example.nutrigajo.domain.model.TipoEjercicio
import com.example.nutrigajo.domain.model.ZonaCuerpo
import com.example.nutrigajo.domain.model.limpio
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.io.File

// El banco global de ejercicios (ejercicios/{id}). Las reglas de Firestore son las que mandan: solo un entrenador verificado crea y
// edita lo suyo, y solo el ADMIN publica o rechaza. Las fotos viven en R2 detrás del Worker, bajo el uid del autor.
// ponytail: listas completas sin paginar; paginar o indexar por zona cuando el banco pase de unos cientos.
class EjerciciosRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val fotos: FotosBanco = FotosBanco(auth),
) {
    private val uid: String get() = auth.currentUser?.uid ?: error("Sin sesión")

    private fun coleccion() = db.collection(FirestorePaths.EJERCICIOS)

    // ---- lectura (se ordena aquí para no pedir índices compuestos)

    suspend fun mios(): List<Ejercicio> =
        leer(coleccion().whereEqualTo("autorId", uid)).sortedByDescending { it.actualizadoEn }

    suspend fun publicados(): List<Ejercicio> =
        leer(coleccion().whereEqualTo("estado", EstadoEjercicio.PUBLICADO.name)).sortedByDescending { it.publicadoEn ?: 0L }

    // De la más antigua a la más reciente, como la cola de verificaciones.
    suspend fun enRevision(): List<Ejercicio> =
        leer(coleccion().whereEqualTo("estado", EstadoEjercicio.EN_REVISION.name)).sortedBy { it.actualizadoEn }

    private suspend fun leer(consulta: Query): List<Ejercicio> =
        consulta.get().await().documents.mapNotNull { d -> d.data?.let { ejercicioDeMapa(d.id, it) } }

    // ---- entrenador

    // Guarda como borrador o lo manda a revisión; si hay foto nueva la sube antes y apunta su versión. Devuelve el guardado.
    suspend fun guardar(ejercicio: Ejercicio, autorNombre: String, enviarARevision: Boolean, foto: ByteArray?, cache: File): Ejercicio {
        val referencia = if (ejercicio.id.isEmpty()) coleccion().document() else coleccion().document(ejercicio.id)
        val version = foto?.let { fotos.subir(RUTA_FOTOS, referencia.id, it, cache) } ?: ejercicio.fotoVersion
        val ahora = System.currentTimeMillis()
        val nuevo = ejercicio.limpio().copy(
            id = referencia.id,
            autorId = uid,
            autorNombre = autorNombre,
            fotoVersion = version,
            estado = if (enviarARevision) EstadoEjercicio.EN_REVISION else EstadoEjercicio.BORRADOR,
            motivoRechazo = null,
            creadoEn = if (ejercicio.creadoEn == 0L) ahora else ejercicio.creadoEn,
            actualizadoEn = ahora,
        )
        if (ejercicio.id.isEmpty()) {
            referencia.set(nuevo.aMapa()).await()
        } else {
            // update y no set: conserva lo que puso el ADMIN al rechazarlo (revisor y fecha), que las reglas piden intacto
            referencia.update(nuevo.aMapa() + ("motivoRechazo" to FieldValue.delete())).await()
        }
        return nuevo
    }

    // El autor retira lo publicado o saca de revisión lo suyo para seguir editándolo.
    suspend fun retirar(id: String) = cambiarEstado(id, EstadoEjercicio.RETIRADO)

    suspend fun sacarDeRevision(id: String) = cambiarEstado(id, EstadoEjercicio.BORRADOR)

    private suspend fun cambiarEstado(id: String, estado: EstadoEjercicio) {
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
            mapOf("estado" to EstadoEjercicio.PUBLICADO.name, "publicadoEn" to ahora, "revisadoPor" to uid, "revisadoEn" to ahora, "actualizadoEn" to ahora),
        ).await()
    }

    suspend fun rechazar(id: String, motivo: String) {
        val ahora = System.currentTimeMillis()
        coleccion().document(id).update(
            mapOf(
                "estado" to EstadoEjercicio.RECHAZADO.name,
                "motivoRechazo" to motivo.trim(),
                "revisadoPor" to uid,
                "revisadoEn" to ahora,
                "actualizadoEn" to ahora,
            ),
        ).await()
    }

    // ---- foto

    // La foto de ese ejercicio en su versión: de la caché o descargada del Worker; null si no tiene.
    suspend fun foto(ejercicio: Ejercicio, cache: File): Bitmap? =
        ejercicio.fotoVersion?.let { fotos.bajar(RUTA_FOTOS, ejercicio.autorId, ejercicio.id, it, cache) }

    private companion object {
        const val RUTA_FOTOS = "ejercicio"
    }
}

// Los campos que escribe el autor; el estado, la publicación y la revisión los fija quien los cambia.
internal fun Ejercicio.aMapa(): Map<String, Any> = buildMap {
    put("autorId", autorId)
    put("autorNombre", autorNombre)
    put("titulo", titulo)
    put("descripcion", descripcion)
    put("zona", zona.name)
    put("tipo", tipo.name)
    put("nivel", nivel.name)
    put("equipo", equipo.name)
    put("duracionMin", duracionMin)
    put("pasos", pasos)
    put("consejos", consejos)
    put("contraindicaciones", contraindicaciones)
    put("kcal", kcal)
    fotoVersion?.let { put("fotoVersion", it) }
    put("estado", estado.name)
    put("creadoEn", creadoEn)
    put("actualizadoEn", actualizadoEn)
}

// null si falta lo básico o el estado no se reconoce (un documento de una versión futura de la app).
internal fun ejercicioDeMapa(id: String, d: Map<String, Any?>): Ejercicio? {
    val estado = enumDe<EstadoEjercicio>(d["estado"]) ?: return null
    val autorId = d["autorId"] as? String ?: return null
    return Ejercicio(
        id = id,
        autorId = autorId,
        autorNombre = d["autorNombre"] as? String ?: "",
        titulo = d["titulo"] as? String ?: "",
        descripcion = d["descripcion"] as? String ?: "",
        zona = enumDe<ZonaCuerpo>(d["zona"]) ?: ZonaCuerpo.CUERPO_COMPLETO,
        tipo = enumDe<TipoEjercicio>(d["tipo"]) ?: TipoEjercicio.FUERZA,
        nivel = enumDe<NivelEjercicio>(d["nivel"]) ?: NivelEjercicio.PRINCIPIANTE,
        equipo = enumDe<EquipoEjercicio>(d["equipo"]) ?: EquipoEjercicio.SIN_EQUIPO,
        duracionMin = (d["duracionMin"] as? Number)?.toInt() ?: 10,
        pasos = (d["pasos"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
        consejos = d["consejos"] as? String ?: "",
        contraindicaciones = d["contraindicaciones"] as? String ?: "",
        kcal = (d["kcal"] as? Number)?.toInt() ?: 0,
        fotoVersion = (d["fotoVersion"] as? Number)?.toLong(),
        estado = estado,
        motivoRechazo = d["motivoRechazo"] as? String,
        creadoEn = (d["creadoEn"] as? Number)?.toLong() ?: 0L,
        actualizadoEn = (d["actualizadoEn"] as? Number)?.toLong() ?: 0L,
        publicadoEn = (d["publicadoEn"] as? Number)?.toLong(),
    )
}

internal inline fun <reified E : Enum<E>> enumDe(valor: Any?): E? =
    (valor as? String)?.let { nombre -> enumValues<E>().firstOrNull { it.name == nombre } }
