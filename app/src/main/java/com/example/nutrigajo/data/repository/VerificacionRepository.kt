package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.Preverificacion
import com.example.nutrigajo.domain.model.EstadoVerificacion
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.SolicitudVerificacion
import com.example.nutrigajo.domain.model.Verificacion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class VerificacionException(val motivo: Motivo) : Exception(motivo.name) {
    enum class Motivo { CEDULA_INVALIDA, NOMBRE_DISTINTO, CEDULA_EN_USO }
}

// Verificación de nutriólogos: el nutriólogo envía su cédula, un filtro automático descarta lo evidente
// y el ADMIN aprueba o rechaza en un clic tras consultar el registro de la SEP.
// Las reglas de Firestore son las que mandan: aquí se validan antes los casos comunes para dar un mensaje claro.
// ponytail: la SEP no ofrece API pública, así que la consulta la hace el ADMIN; si hubiera convenio, solo cambia quién aprueba (campo metodo).
class VerificacionRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val uid: String get() = auth.currentUser?.uid ?: error("Sin sesión")

    private fun verificacion(id: String) = db.collection(FirestorePaths.VERIFICACIONES).document(id)

    // ---- nutriólogo

    suspend fun miVerificacion(): Verificacion? {
        val doc = verificacion(uid).get().await()
        if (!doc.exists()) return null
        val estado = doc.getString("estado")?.let { runCatching { EstadoVerificacion.valueOf(it) }.getOrNull() } ?: return null
        return Verificacion(
            estado = estado,
            cedula = doc.getString("cedula").orEmpty(),
            nombreCedula = doc.getString("nombreCedula").orEmpty(),
            motivo = doc.getString("motivo"),
        )
    }

    // Filtro automático y envío. La solicitud y la reserva de la cédula se escriben en un lote: o pasan las dos o ninguna.
    suspend fun enviar(perfil: Perfil, nombreCedulaEscrito: String, cedulaEscrita: String) {
        val cedula = Preverificacion.cedula(cedulaEscrita) ?: throw VerificacionException(VerificacionException.Motivo.CEDULA_INVALIDA)
        val nombreCedula = nombreCedulaEscrito.trim().replace(Regex("\\s+"), " ")
        if (!Preverificacion.nombreCoincide(perfil.nombre, nombreCedula)) {
            throw VerificacionException(VerificacionException.Motivo.NOMBRE_DISTINTO)
        }

        val reserva = db.collection(FirestorePaths.CEDULAS).document(cedula)
        if (reserva.get().await().exists()) throw VerificacionException(VerificacionException.Motivo.CEDULA_EN_USO)

        val ahora = System.currentTimeMillis()
        db.runBatch { lote ->
            lote.set(reserva, mapOf("nutriologoId" to uid, "creadoEn" to ahora))
            lote.set(
                verificacion(uid),
                mapOf(
                    "nutriologoId" to uid,
                    "nombreCuenta" to perfil.nombre,
                    "correo" to perfil.correo.lowercase(),
                    "nombreCedula" to nombreCedula,
                    "cedula" to cedula,
                    "estado" to EstadoVerificacion.PENDIENTE.name,
                    "enviadoEn" to ahora,
                ),
            )
        }.await()
    }

    // ---- ADMIN

    suspend fun pendientes(): List<SolicitudVerificacion> =
        db.collection(FirestorePaths.VERIFICACIONES)
            .whereEqualTo("estado", EstadoVerificacion.PENDIENTE.name)
            .get().await().documents
            .map { d ->
                SolicitudVerificacion(
                    nutriologoId = d.id,
                    nombreCuenta = d.getString("nombreCuenta").orEmpty(),
                    correo = d.getString("correo").orEmpty(),
                    nombreCedula = d.getString("nombreCedula").orEmpty(),
                    cedula = d.getString("cedula").orEmpty(),
                    enviadoEn = d.getLong("enviadoEn") ?: 0L,
                )
            }
            .sortedBy { it.enviadoEn }

    // Aprueba la solicitud y marca verificado al nutriólogo en un solo lote.
    suspend fun aprobar(solicitud: SolicitudVerificacion) {
        val ahora = System.currentTimeMillis()
        db.runBatch { lote ->
            lote.update(verificacion(solicitud.nutriologoId), revision(EstadoVerificacion.APROBADA, ahora))
            lote.update(
                db.collection(FirestorePaths.USUARIOS).document(solicitud.nutriologoId),
                mapOf("verificado" to true, "ultimaModificacion" to ahora),
            )
        }.await()
    }

    // Rechaza con motivo y libera la cédula para que su dueño real pueda registrarla.
    suspend fun rechazar(solicitud: SolicitudVerificacion, motivo: String) {
        val ahora = System.currentTimeMillis()
        db.runBatch { lote ->
            lote.update(verificacion(solicitud.nutriologoId), revision(EstadoVerificacion.RECHAZADA, ahora) + ("motivo" to motivo.trim()))
            lote.delete(db.collection(FirestorePaths.CEDULAS).document(solicitud.cedula))
        }.await()
    }

    private fun revision(estado: EstadoVerificacion, ahora: Long): Map<String, Any> =
        mapOf("estado" to estado.name, "revisadoPor" to uid, "revisadoEn" to ahora, "metodo" to "MANUAL")
}
