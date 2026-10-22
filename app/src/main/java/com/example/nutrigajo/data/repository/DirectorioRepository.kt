package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.NutriologoEnMapa
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val MAX_NUTRIOLOGOS = 200L

// Consultorios de nutriólogos verificados para el mapa. Las reglas de Firestore son las que mandan:
// solo un nutriólogo verificado publica su punto, y lo ve cualquier sesión.
// ponytail: el paciente baja hasta MAX_NUTRIOLOGOS puntos de una vez; con cientos de nutriólogos, consultar por zona (geohash).
class DirectorioRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val uid: String get() = auth.currentUser?.uid ?: error("Sin sesión")

    private fun mio() = db.collection(FirestorePaths.DIRECTORIO).document(uid)

    // ---- nutriólogo verificado

    suspend fun miUbicacion(): NutriologoEnMapa? = mio().get().await().aNutriologo()

    suspend fun publicar(nombre: String, lat: Double, lng: Double) {
        mio().set(
            mapOf(
                "nutriologoId" to uid,
                "nombre" to nombre,
                "lat" to lat,
                "lng" to lng,
                "actualizadoEn" to System.currentTimeMillis(),
            ),
        ).await()
    }

    suspend fun quitar() {
        mio().delete().await()
    }

    // ---- paciente

    suspend fun nutriologos(): List<NutriologoEnMapa> =
        db.collection(FirestorePaths.DIRECTORIO)
            .limit(MAX_NUTRIOLOGOS)
            .get().await().documents
            .mapNotNull { it.aNutriologo() }

    private fun DocumentSnapshot.aNutriologo(): NutriologoEnMapa? {
        if (!exists()) return null
        return NutriologoEnMapa(
            id = id,
            nombre = getString("nombre").orEmpty(),
            lat = getDouble("lat") ?: return null,
            lng = getDouble("lng") ?: return null,
        )
    }
}
