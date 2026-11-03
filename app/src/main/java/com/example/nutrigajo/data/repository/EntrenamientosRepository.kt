package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.EnfoqueDia
import com.example.nutrigajo.domain.model.Entrenamiento
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

// El historial de «Entrenar ahora» es personal: vive bajo el propio usuario (usuarios/{uid}/entrenamientos/{id}) y solo él lo lee y lo
// crea. Uno guardado no se edita.
class EntrenamientosRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun coleccion() =
        db.collection(FirestorePaths.USUARIOS).document(auth.currentUser?.uid ?: error("Sin sesión")).collection(FirestorePaths.ENTRENAMIENTOS)

    // Lo más reciente primero, hasta `limite`.
    suspend fun listar(limite: Long = 50): List<Entrenamiento> =
        coleccion().orderBy("fecha", Query.Direction.DESCENDING).limit(limite).get().await()
            .documents.mapNotNull { d -> d.data?.let { entrenamientoDeMapa(d.id, it) } }

    suspend fun guardar(entrenamiento: Entrenamiento) {
        coleccion().add(entrenamiento.aMapa()).esperarUnPoco()
    }

    // Igual que el modo carrera: sin conexión la escritura ya está en la caché del teléfono y sale sola al volver la red, así que no se
    // espera más que unos segundos.
    // ponytail: si el servidor rechaza la escritura después de esa espera (reglas), el cambio se revierte en la caché sin avisar.
    private suspend fun Task<*>.esperarUnPoco() {
        withTimeoutOrNull(ESPERA_ESCRITURA_MS) { await() }
    }

    private companion object {
        const val ESPERA_ESCRITURA_MS = 4_000L
    }
}

internal fun Entrenamiento.aMapa(): Map<String, Any> = mapOf(
    "fecha" to fecha,
    "enfoque" to enfoque.name,
    "minutos" to minutos.coerceIn(1, Entrenamiento.MAX_MINUTOS),
    "kcal" to kcal.coerceIn(0, Entrenamiento.MAX_KCAL),
    "hechos" to hechos,
    "total" to total,
)

// null si falta algo o el enfoque no se reconoce (un documento de una versión futura de la app).
internal fun entrenamientoDeMapa(id: String, d: Map<String, Any?>): Entrenamiento? {
    val enfoque = (d["enfoque"] as? String)?.let { nombre -> EnfoqueDia.entries.firstOrNull { it.name == nombre } } ?: return null
    return Entrenamiento(
        id = id,
        fecha = (d["fecha"] as? Number)?.toLong() ?: return null,
        enfoque = enfoque,
        minutos = (d["minutos"] as? Number)?.toInt() ?: return null,
        kcal = (d["kcal"] as? Number)?.toInt() ?: 0,
        hechos = (d["hechos"] as? Number)?.toInt() ?: return null,
        total = (d["total"] as? Number)?.toInt() ?: return null,
    )
}
