package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.Cita
import com.example.nutrigajo.domain.model.EstadoCita
import com.example.nutrigajo.domain.model.NutriologoEnMapa
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Citas entre un paciente y un nutriólogo del directorio. Las reglas de Firestore son las que mandan:
// el paciente pide, el nutriólogo confirma o rechaza y cualquiera de los dos cancela.
// La hora pedida se reserva en directorio/{nutriologoId}/ocupados/{inicio} (sin datos del paciente) en el mismo lote
// que la cita: si otro paciente ya la tomó, el lote entero falla y la cita no se crea.
// Un paciente solo puede tener una cita vigente a la vez: el lote que la crea también pone citasActivas/{uid}, y las reglas
// no dejan crear otra mientras ese candado exista. Para cambiarla puede reagendarla (otra hora del mismo nutriólogo, que
// vuelve a esperar su confirmación) o cancelarla, que quita el candado.
class CitasRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val uid: String get() = auth.currentUser?.uid ?: error("Sin sesión")

    private fun ocupados(nutriologoId: String) =
        db.collection(FirestorePaths.DIRECTORIO).document(nutriologoId).collection(FirestorePaths.OCUPADOS)

    private fun candado(usuarioId: String) = db.collection(FirestorePaths.CITAS_ACTIVAS).document(usuarioId)

    // ---- paciente

    // Horas que ya pidieron otros pacientes a este nutriólogo, de ahora en adelante.
    suspend fun horasOcupadas(nutriologoId: String): Set<Long> =
        ocupados(nutriologoId)
            .whereGreaterThanOrEqualTo("inicio", System.currentTimeMillis())
            .get().await().documents
            .mapNotNull { it.getLong("inicio") }
            .toSet()

    // La cita vigente del paciente (la más próxima que no pasó ni se cerró), o null si no tiene: con una, no puede pedir otra.
    suspend fun citaVigente(): Cita? {
        val ahora = System.currentTimeMillis()
        return citas(Rol.PACIENTE).firstOrNull { it.vigente(ahora) }
    }

    suspend fun agendar(nutriologoId: String, nutriologoNombre: String, paciente: Perfil, inicio: Long) {
        val ahora = System.currentTimeMillis()
        val cita = db.collection(FirestorePaths.CITAS).document()
        db.runBatch { lote ->
            lote.set(
                cita,
                mapOf(
                    "nutriologoId" to nutriologoId,
                    "nutriologoNombre" to nutriologoNombre,
                    "usuarioId" to uid,
                    "pacienteNombre" to paciente.nombre,
                    "inicio" to inicio,
                    "estado" to EstadoCita.PENDIENTE.name,
                    "creadaEn" to ahora,
                    "actualizadaEn" to ahora,
                ),
            )
            lote.set(ocupados(nutriologoId).document(inicio.toString()), mapOf("citaId" to cita.id, "inicio" to inicio))
            lote.set(candado(uid), mapOf("citaId" to cita.id))
        }.await()
    }

    // Pasa la cita a otra hora del mismo nutriólogo: vuelve a esperar su confirmación, la hora nueva se reserva y la vieja se
    // libera en el mismo lote. Si otro paciente ya tomó la nueva, el lote entero falla y la cita se queda como estaba.
    suspend fun reagendar(cita: Cita, inicio: Long) {
        db.runBatch { lote ->
            lote.update(
                db.collection(FirestorePaths.CITAS).document(cita.id),
                mapOf("inicio" to inicio, "estado" to EstadoCita.PENDIENTE.name, "actualizadaEn" to System.currentTimeMillis()),
            )
            lote.set(ocupados(cita.nutriologoId).document(inicio.toString()), mapOf("citaId" to cita.id, "inicio" to inicio))
            lote.delete(ocupados(cita.nutriologoId).document(cita.inicio.toString()))
        }.await()
    }

    // ---- ambos

    // Las citas del usuario: las que pidió (paciente) o las que le pidieron (nutriólogo), de la más próxima a la más lejana.
    suspend fun citas(rol: Rol): List<Cita> =
        db.collection(FirestorePaths.CITAS)
            .whereEqualTo(if (rol == Rol.NUTRIOLOGO) "nutriologoId" else "usuarioId", uid)
            .get().await().documents
            .mapNotNull { it.aCita() }
            .sortedBy { it.inicio }

    // Cambia el estado de la cita; si deja de ocupar la hora (rechazada o cancelada), la libera en el mismo lote, y con ella el
    // candado del paciente para que pueda pedir otra.
    suspend fun cambiarEstado(cita: Cita, estado: EstadoCita) {
        db.runBatch { lote ->
            lote.update(
                db.collection(FirestorePaths.CITAS).document(cita.id),
                mapOf("estado" to estado.name, "actualizadaEn" to System.currentTimeMillis()),
            )
            if (estado == EstadoCita.RECHAZADA || estado == EstadoCita.CANCELADA) {
                lote.delete(ocupados(cita.nutriologoId).document(cita.inicio.toString()))
                lote.delete(candado(cita.usuarioId))
            }
        }.await()
    }

    private fun DocumentSnapshot.aCita(): Cita? {
        return Cita(
            id = id,
            nutriologoId = getString("nutriologoId") ?: return null,
            nutriologoNombre = getString("nutriologoNombre").orEmpty(),
            usuarioId = getString("usuarioId") ?: return null,
            pacienteNombre = getString("pacienteNombre").orEmpty(),
            inicio = getLong("inicio") ?: return null,
            estado = getString("estado")?.let { runCatching { EstadoCita.valueOf(it) }.getOrNull() } ?: return null,
        )
    }
}
