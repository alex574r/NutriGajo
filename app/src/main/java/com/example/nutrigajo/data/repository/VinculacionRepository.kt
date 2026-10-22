package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.CodigoVinculacion
import com.example.nutrigajo.domain.model.PacienteResumen
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Vinculo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.UUID

class VinculoException(val motivo: Motivo) : Exception(motivo.name) {
    enum class Motivo { INVALIDO, USADO, VENCIDO }
}

private const val ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // sin 0/O ni 1/I: se dictan y se leen sin confusión
private const val LARGO_CODIGO = 8
private const val INTENTOS_CODIGO = 3
private const val VIGENCIA_MS = 7L * 24 * 60 * 60 * 1000

// Vinculación nutriólogo-paciente con códigos de un solo uso. Las reglas de Firestore son las que mandan:
// aquí solo se validan antes los casos comunes para dar un mensaje claro.
// ponytail: sin límite de pacientes por nutriólogo ni de nutriólogos por paciente; la app solo ofrece el canje a quien aún no tiene vínculo.
class VinculacionRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val aleatorio = SecureRandom()
    private val uid: String get() = auth.currentUser?.uid ?: error("Sin sesión")

    // ---- nutriólogo

    // El código más lejano a vencer que siga sin usar, para mostrarlo al volver a abrir la app.
    suspend fun codigoVigente(): CodigoVinculacion? {
        val ahora = System.currentTimeMillis()
        return db.collection(FirestorePaths.CODIGOS_VINCULACION)
            .whereEqualTo("nutriologoId", uid)
            .whereEqualTo("usado", false)
            .get().await().documents
            .mapNotNull { d -> d.getLong("expiraEn")?.takeIf { it > ahora }?.let { CodigoVinculacion(d.id, it) } }
            .maxByOrNull { it.expiraEn }
    }

    suspend fun generarCodigo(nutriologoNombre: String): CodigoVinculacion {
        val ahora = System.currentTimeMillis()
        repeat(INTENTOS_CODIGO) {
            val codigo = nuevoCodigo()
            val ref = db.collection(FirestorePaths.CODIGOS_VINCULACION).document(codigo)
            val creado = db.runTransaction { tx ->
                // en una colisión (improbable) se reintenta con otro código; nunca se pisa uno existente
                if (tx.get(ref).exists()) {
                    false
                } else {
                    tx.set(
                        ref,
                        mapOf(
                            "nutriologoId" to uid,
                            "nutriologoNombre" to nutriologoNombre,
                            "creadoEn" to ahora,
                            "expiraEn" to ahora + VIGENCIA_MS,
                            "usado" to false,
                        ),
                    )
                    true
                }
            }.await()
            if (creado) return CodigoVinculacion(codigo, ahora + VIGENCIA_MS)
        }
        error("No se pudo generar un código único")
    }

    suspend fun pacientes(): List<PacienteResumen> =
        db.collection(FirestorePaths.PACIENTES)
            .whereEqualTo("nutriologoId", uid)
            .get().await().documents
            .map { d ->
                PacienteResumen(
                    id = d.id,
                    nombre = d.getString("nombre").orEmpty(),
                    correo = d.getString("correoVinculacion").orEmpty(),
                    fechaAlta = d.getLong("fechaAlta") ?: 0L,
                )
            }
            .sortedByDescending { it.fechaAlta }

    // ---- paciente

    suspend fun vinculoActual(): Vinculo? =
        db.collection(FirestorePaths.PACIENTES)
            .whereEqualTo("usuarioId", uid)
            .limit(1)
            .get().await().documents
            .firstOrNull()
            ?.let { Vinculo(it.id, it.getString("nutriologoNombre").orEmpty()) }

    // Canjea el código y crea el registro del paciente en un solo lote: o pasan las dos escrituras o ninguna.
    suspend fun canjear(codigoEscrito: String, paciente: Perfil): Vinculo {
        val codigo = codigoEscrito.filter { it.isLetterOrDigit() }.uppercase()
        if (codigo.length != LARGO_CODIGO) throw VinculoException(VinculoException.Motivo.INVALIDO)

        val ref = db.collection(FirestorePaths.CODIGOS_VINCULACION).document(codigo)
        val doc = ref.get().await()
        if (!doc.exists()) throw VinculoException(VinculoException.Motivo.INVALIDO)
        if (doc.getBoolean("usado") == true) throw VinculoException(VinculoException.Motivo.USADO)
        val ahora = System.currentTimeMillis()
        if ((doc.getLong("expiraEn") ?: 0L) <= ahora) throw VinculoException(VinculoException.Motivo.VENCIDO)

        val nutriologoNombre = doc.getString("nutriologoNombre").orEmpty()
        val pacienteId = UUID.randomUUID().toString()
        db.runBatch { lote ->
            lote.update(ref, mapOf("usado" to true, "canjeadoPor" to uid, "canjeadoEn" to ahora))
            lote.set(
                db.collection(FirestorePaths.PACIENTES).document(pacienteId),
                mapOf(
                    "nutriologoId" to doc.getString("nutriologoId"),
                    "nutriologoNombre" to nutriologoNombre,
                    "usuarioId" to uid,
                    "nombre" to paciente.nombre,
                    "correoVinculacion" to paciente.correo.lowercase(),
                    "codigoVinculacion" to codigo,
                    "activo" to true,
                    "fechaAlta" to ahora,
                    "ultimaModificacion" to ahora,
                ),
            )
        }.await()
        return Vinculo(pacienteId, nutriologoNombre)
    }

    private fun nuevoCodigo(): String =
        buildString { repeat(LARGO_CODIGO) { append(ALFABETO[aleatorio.nextInt(ALFABETO.length)]) } }
}
