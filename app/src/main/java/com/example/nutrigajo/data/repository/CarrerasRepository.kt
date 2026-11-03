package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.Carrera
import com.example.nutrigajo.domain.model.PuntoRuta
import com.example.nutrigajo.domain.model.Ruta
import com.example.nutrigajo.domain.model.TipoActividad
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToLong

// El modo carrera es personal: lo que se guarda vive bajo el propio usuario (usuarios/{uid}/carreras/{id}) y solo él lo lee. La ruta se guarda como una lista plana [lat, lng, lat, lng…] para no pasar
// de un documento con miles de mapas.
class CarrerasRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun usuario() = db.collection(FirestorePaths.USUARIOS).document(auth.currentUser?.uid ?: error("Sin sesión"))

    // Lo más reciente primero, hasta `limite`.
    suspend fun listar(limite: Long = 50): List<Carrera> =
        usuario().collection(FirestorePaths.CARRERAS).orderBy("inicio", Query.Direction.DESCENDING).limit(limite).get().await()
            .documents.mapNotNull { it.aCarrera() }

    suspend fun guardar(carrera: Carrera) {
        val ruta = Ruta.aligerar(carrera.ruta).flatMap { listOf(redondear(it.lat), redondear(it.lng)) }
        usuario().collection(FirestorePaths.CARRERAS).add(
            mapOf(
                "tipo" to carrera.tipo.name,
                "inicio" to carrera.inicio,
                "duracionSeg" to carrera.duracionSeg,
                "distanciaM" to carrera.distanciaM,
                "kcal" to carrera.kcal,
                "ruta" to ruta,
            ),
        ).esperarUnPoco()
    }

    suspend fun eliminar(id: String) {
        usuario().collection(FirestorePaths.CARRERAS).document(id).delete().esperarUnPoco()
    }

    // Firestore solo confirma una escritura cuando el servidor la recibe. Sin conexión el cambio ya está en la caché del teléfono y
    // sale al volver la red, así que no se espera más que unos segundos: con red, la confirmación (o el rechazo de las reglas) llega
    // mucho antes. Se corre al aire libre, donde muchas veces no hay datos.
    // ponytail: si el servidor rechaza la escritura después de esa espera (reglas), el cambio se revierte en la caché sin avisar.
    private suspend fun Task<*>.esperarUnPoco() {
        withTimeoutOrNull(ESPERA_ESCRITURA_MS) { await() }
    }

    // seis decimales son ~10 cm: de sobra para una ruta y la mitad de bytes
    private fun redondear(grados: Double) = (grados * 1e6).roundToLong() / 1e6

    private companion object {
        const val ESPERA_ESCRITURA_MS = 4_000L
    }
}

internal fun DocumentSnapshot.aCarrera(): Carrera? {
    val tipo = TipoActividad.entries.firstOrNull { it.name == getString("tipo") } ?: return null
    val plana = (get("ruta") as? List<*>)?.mapNotNull { (it as? Number)?.toDouble() } ?: emptyList()
    return Carrera(
        id = id,
        tipo = tipo,
        inicio = getLong("inicio") ?: return null,
        duracionSeg = getLong("duracionSeg")?.toInt() ?: return null,
        distanciaM = getLong("distanciaM")?.toInt() ?: return null,
        kcal = getLong("kcal")?.toInt() ?: return null,
        ruta = plana.chunked(2).filter { it.size == 2 }.map { PuntoRuta(it[0], it[1]) },
    )
}
