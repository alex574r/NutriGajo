package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.Carrera
import com.example.nutrigajo.domain.model.PlanCaminata
import com.example.nutrigajo.domain.model.PuntoRuta
import com.example.nutrigajo.domain.model.Ruta
import com.example.nutrigajo.domain.model.TipoActividad
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToLong

// El plan de caminata que sigue el paciente y el día (milisegundos desde 1970) en que lo eligió.
data class PlanElegido(val plan: PlanCaminata, val inicio: Long)

// El modo carrera es personal: lo que se guarda vive bajo el propio usuario (usuarios/{uid}/carreras/{id} y
// usuarios/{uid}/modoCarrera/plan) y solo él lo lee. La ruta se guarda como una lista plana [lat, lng, lat, lng…] para no pasar
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
        ).await()
    }

    suspend fun eliminar(id: String) {
        usuario().collection(FirestorePaths.CARRERAS).document(id).delete().await()
    }

    suspend fun plan(): PlanElegido? {
        val doc = usuario().collection(FirestorePaths.MODO_CARRERA).document(DOC_PLAN).get().await()
        val plan = PlanCaminata.entries.firstOrNull { it.name == doc.getString("plan") } ?: return null
        return PlanElegido(plan, doc.getLong("inicio") ?: return null)
    }

    suspend fun elegirPlan(plan: PlanCaminata, inicio: Long) {
        usuario().collection(FirestorePaths.MODO_CARRERA).document(DOC_PLAN).set(mapOf("plan" to plan.name, "inicio" to inicio)).await()
    }

    suspend fun quitarPlan() {
        usuario().collection(FirestorePaths.MODO_CARRERA).document(DOC_PLAN).delete().await()
    }

    // seis decimales son ~10 cm: de sobra para una ruta y la mitad de bytes
    private fun redondear(grados: Double) = (grados * 1e6).roundToLong() / 1e6

    private companion object {
        const val DOC_PLAN = "plan"
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
