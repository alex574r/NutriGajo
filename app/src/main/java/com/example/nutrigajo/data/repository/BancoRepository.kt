package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.ItemBanco
import com.example.nutrigajo.domain.model.MomentoComida
import com.example.nutrigajo.domain.model.TipoBanco
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

// Banco de comidas y ejercicios del nutriólogo (usuarios/{uid}/banco/{id}): lo que guarda para reutilizar al armar planes.
// Es solo suyo; ningún paciente ni otro nutriólogo lo ve.
class BancoRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun banco() = db.collection(FirestorePaths.USUARIOS)
        .document(auth.currentUser?.uid ?: error("Sin sesión"))
        .collection(FirestorePaths.BANCO)

    // Todo el banco, lo más reciente primero.
    suspend fun listar(): List<ItemBanco> =
        banco().orderBy("actualizadoEn", Query.Direction.DESCENDING).get().await().documents.mapNotNull { it.aItem() }

    // Crea el elemento (sin id) o actualiza el que ya existe.
    suspend fun guardar(item: ItemBanco) {
        val ahora = System.currentTimeMillis()
        if (item.id.isEmpty()) {
            val datos = buildMap<String, Any> {
                put("tipo", item.tipo.name)
                put("nombre", item.nombre)
                put("texto", item.texto)
                item.momento?.let { put("momento", it.name) }
                put("creadoEn", ahora)
                put("actualizadoEn", ahora)
            }
            banco().add(datos).await()
        } else {
            banco().document(item.id).update(
                mapOf(
                    "nombre" to item.nombre,
                    "texto" to item.texto,
                    "momento" to (item.momento?.name ?: FieldValue.delete()),
                    "actualizadoEn" to ahora,
                ),
            ).await()
        }
    }

    suspend fun eliminar(id: String) {
        banco().document(id).delete().await()
    }

    private fun DocumentSnapshot.aItem(): ItemBanco? {
        val tipo = TipoBanco.entries.firstOrNull { it.name == getString("tipo") } ?: return null
        return ItemBanco(
            id = id,
            tipo = tipo,
            nombre = getString("nombre") ?: return null,
            texto = getString("texto").orEmpty(),
            momento = MomentoComida.entries.firstOrNull { it.name == getString("momento") },
            creadoEn = getLong("creadoEn") ?: 0,
        )
    }
}
