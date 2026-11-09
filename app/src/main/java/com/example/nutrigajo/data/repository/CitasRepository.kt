package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.Cita
import com.example.nutrigajo.domain.model.EstadoCita
import com.example.nutrigajo.domain.model.NOTA_MAX
import com.example.nutrigajo.domain.model.NutriologoEnMapa
import com.example.nutrigajo.domain.model.OrigenCita
import com.example.nutrigajo.domain.model.PacienteResumen
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await

// Citas entre un paciente y un nutriólogo del directorio. Las reglas de Firestore son las que mandan:
// el paciente pide, el nutriólogo confirma o rechaza (o agenda una ya confirmada a un paciente suyo) y cualquiera de los dos cancela.
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

    // ---- nutriólogo

    // Agenda una cita a un paciente suyo: nace confirmada y reserva la hora en el mismo lote (si otro la tomó, el lote entero falla).
    // No usa el candado de «una cita vigente» del paciente: es de quien pide, no de quien agenda. Devuelve el id de la cita para avisarle.
    suspend fun agendarComoNutriologo(nutriologo: Perfil, paciente: PacienteResumen, inicio: Long, nota: String): String {
        // las reglas comparan el nombre de la cita con el de `directorio/{uid}`, que puede no ser el del perfil si el nutriólogo cambió su nombre
        // después de ponerse en el mapa
        val consultorio = db.collection(FirestorePaths.DIRECTORIO).document(nutriologo.uid).get().await().getString("nombre") ?: nutriologo.nombre
        val cita = db.collection(FirestorePaths.CITAS).document()
        db.runBatch { lote ->
            lote.set(cita, citaDelNutriologoAMapa(nutriologo.uid, consultorio, paciente, inicio, nota, System.currentTimeMillis()))
            lote.set(ocupados(nutriologo.uid).document(inicio.toString()), mapOf("citaId" to cita.id, "inicio" to inicio))
        }.await()
        return cita.id
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
        db.runBatch { lote -> escribirCambioDeEstado(db, lote, cita, estado) }.await()
    }

    private fun DocumentSnapshot.aCita(): Cita? = citaDeMapa(id, data.orEmpty())
}

// Pone en `lote` el cambio de estado de la cita; si deja de ocupar la hora (rechazada o cancelada) libera la hora y, si era del paciente, el
// candado. Lo usa `cambiarEstado` y también el cierre de un vínculo, que cierra sus citas en el mismo lote.
internal fun escribirCambioDeEstado(db: FirebaseFirestore, lote: WriteBatch, cita: Cita, estado: EstadoCita) {
    lote.update(
        db.collection(FirestorePaths.CITAS).document(cita.id),
        mapOf("estado" to estado.name, "actualizadaEn" to System.currentTimeMillis()),
    )
    if (estado == EstadoCita.RECHAZADA || estado == EstadoCita.CANCELADA) {
        lote.delete(db.collection(FirestorePaths.DIRECTORIO).document(cita.nutriologoId).collection(FirestorePaths.OCUPADOS).document(cita.inicio.toString()))
        if (liberaCandado(cita, estado)) lote.delete(db.collection(FirestorePaths.CITAS_ACTIVAS).document(cita.usuarioId))
    }
}

// El candado `citasActivas/{uid}` es de la cita que pidió el paciente: solo esa lo libera al cerrarse. La que agenda el nutriólogo no lo
// puso, y borrarlo con ella haría fallar el lote entero (las reglas exigen que la cita del candado sea la que se cierra).
internal fun liberaCandado(cita: Cita, nuevo: EstadoCita): Boolean =
    cita.origen == OrigenCita.PACIENTE && (nuevo == EstadoCita.RECHAZADA || nuevo == EstadoCita.CANCELADA)

// La cita a partir de los campos de su documento; null si falta alguno de los obligatorios o el estado no se conoce. Una cita sin
// `origen` (las de antes) o con uno desconocido es del paciente.
internal fun citaDeMapa(id: String, d: Map<String, Any?>): Cita? {
    return Cita(
        id = id,
        nutriologoId = d["nutriologoId"] as? String ?: return null,
        nutriologoNombre = d["nutriologoNombre"] as? String ?: "",
        usuarioId = d["usuarioId"] as? String ?: return null,
        pacienteNombre = d["pacienteNombre"] as? String ?: "",
        inicio = (d["inicio"] as? Number)?.toLong() ?: return null,
        estado = (d["estado"] as? String)?.let { runCatching { EstadoCita.valueOf(it) }.getOrNull() } ?: return null,
        origen = (d["origen"] as? String)?.let { runCatching { OrigenCita.valueOf(it) }.getOrNull() } ?: OrigenCita.PACIENTE,
        pacienteId = d["pacienteId"] as? String,
        nota = d["nota"] as? String ?: "",
    )
}

// Los campos de la cita que agenda el nutriólogo: nace confirmada, con el paciente de su lista y, si la hay, la nota ya recortada
// (una nota vacía no se manda). Son exactamente los que aceptan las reglas de Firestore.
internal fun citaDelNutriologoAMapa(
    nutriologoId: String,
    nutriologoNombre: String,
    paciente: PacienteResumen,
    inicio: Long,
    nota: String,
    ahora: Long,
): Map<String, Any> = buildMap {
    put("nutriologoId", nutriologoId)
    put("nutriologoNombre", nutriologoNombre)
    put("usuarioId", paciente.usuarioId)
    put("pacienteNombre", paciente.nombre)
    put("pacienteId", paciente.id)
    put("inicio", inicio)
    put("estado", EstadoCita.CONFIRMADA.name)
    put("origen", OrigenCita.NUTRIOLOGO.name)
    put("creadaEn", ahora)
    put("actualizadaEn", ahora)
    nota.trim().take(NOTA_MAX).trim().takeIf { it.isNotEmpty() }?.let { put("nota", it) }
}
