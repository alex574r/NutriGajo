package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.Rol
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Los comentarios que la gente manda desde Configuración (comentarios/{id}): de qué partes de la app hablan y su texto. Solo los
// escribe quien los envía y solo los lee el ADMIN.
class ComentariosRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun enviar(rol: Rol, areas: List<String>, mensaje: String, versionApp: String) {
        val uid = auth.currentUser?.uid ?: error("Sin sesión")
        db.collection(FirestorePaths.COMENTARIOS).add(
            mapOf(
                "usuarioId" to uid,
                "rol" to rol.name,
                "areas" to areas,
                "mensaje" to mensaje,
                "versionApp" to versionApp,
                "creadoEn" to System.currentTimeMillis(),
            ),
        ).await()
    }
}
