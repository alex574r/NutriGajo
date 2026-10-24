package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.DatosConsultorio
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

    // Reemplaza el documento entero: un dato del consultorio que se vació deja de publicarse.
    suspend fun publicar(nombre: String, lat: Double, lng: Double, consultorio: DatosConsultorio) {
        // la cédula sale de la solicitud aprobada, las reglas la comparan con ella
        val cedula = db.collection(FirestorePaths.VERIFICACIONES).document(uid).get().await().getString("cedula")
            ?: error("Sin solicitud de verificación")
        val datos = mutableMapOf<String, Any>(
            "nutriologoId" to uid,
            "nombre" to nombre,
            "lat" to lat,
            "lng" to lng,
            "cedula" to cedula,
            "actualizadoEn" to System.currentTimeMillis(),
        )
        mapOf(
            "especialidad" to consultorio.especialidad,
            "direccion" to consultorio.direccion,
            "telefono" to consultorio.telefono,
            "horario" to consultorio.horario,
            "descripcion" to consultorio.descripcion,
        ).forEach { (campo, valor) -> if (valor.isNotBlank()) datos[campo] = valor.trim() }
        mio().set(datos).await()
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
            cedula = getString("cedula").orEmpty(),
            consultorio = DatosConsultorio(
                especialidad = getString("especialidad").orEmpty(),
                direccion = getString("direccion").orEmpty(),
                telefono = getString("telefono").orEmpty(),
                horario = getString("horario").orEmpty(),
                descripcion = getString("descripcion").orEmpty(),
            ),
        )
    }
}
