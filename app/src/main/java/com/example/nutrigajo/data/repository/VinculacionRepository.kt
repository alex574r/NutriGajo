package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.CerradoPor
import com.example.nutrigajo.domain.model.Cita
import com.example.nutrigajo.domain.model.CodigoVinculacion
import com.example.nutrigajo.domain.model.PacienteResumen
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.RegistroCerrado
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.domain.model.Vinculo
import com.example.nutrigajo.domain.model.cerradoPorDeTexto
import com.example.nutrigajo.domain.model.estadoAlCerrar
import com.example.nutrigajo.domain.model.masRecientesPrimero
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.UUID

class VinculoException(val motivo: Motivo) : Exception(motivo.name) {
    enum class Motivo { INVALIDO, USADO, VENCIDO }
}

// ponytail: 4 dígitos son solo 10 000 códigos; con el código de un solo uso y vigencia basta para el piloto,
// si hace falta más margen se sube LARGO_CODIGO (CampoCodigo y VincularScreen lo leen de aquí).
private const val ALFABETO = "0123456789"
private const val LARGO_CODIGO = 4
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
            // los códigos largos de antes ya no se pueden canjear: se ignoran y el nutriólogo genera uno nuevo
            .filter { it.id.length == LARGO_CODIGO }
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

    // Solo los pacientes con vínculo activo: los cerrados (el paciente se fue o se le dio de baja) ya no salen en la lista.
    suspend fun pacientes(): List<PacienteResumen> =
        db.collection(FirestorePaths.PACIENTES)
            .whereEqualTo("nutriologoId", uid)
            .whereEqualTo("activo", true)
            .get().await().documents
            .map { d ->
                PacienteResumen(
                    id = d.id,
                    usuarioId = d.getString("usuarioId").orEmpty(),
                    nombre = d.getString("nombre").orEmpty(),
                    correo = d.getString("correoVinculacion").orEmpty(),
                    fechaAlta = d.getLong("fechaAlta") ?: 0L,
                    ultimoMensaje = d.aUltimoMensaje(),
                    metas = d.aMetas(),
                )
            }
            .sortedByDescending { it.fechaAlta }

    // ---- paciente

    // Los vínculos ya cerrados de quien pide, del más reciente al más viejo: los del paciente (con cada nutriólogo) o los del nutriólogo (con cada
    // paciente al que dio de baja o que se fue). Las reglas dejan leerlos a las dos partes; el nutriólogo no ve los de una cuenta que se borró.
    suspend fun vinculosCerrados(comoPaciente: Boolean): List<RegistroCerrado> =
        db.collection(FirestorePaths.PACIENTES)
            .whereEqualTo(if (comoPaciente) "usuarioId" else "nutriologoId", uid)
            .whereEqualTo("activo", false)
            .get().await().documents
            .map { d ->
                RegistroCerrado(
                    pacienteId = d.id,
                    nombre = d.getString(if (comoPaciente) "nutriologoNombre" else "nombre").orEmpty(),
                    cerradoEn = d.getLong("cerradoEn") ?: 0L,
                    cerradoPor = cerradoPorDeTexto(d.getString("cerradoPor")),
                )
            }
            .masRecientesPrimero()

    // El vínculo activo del paciente; uno cerrado ya no cuenta (puede vincularse de nuevo).
    suspend fun vinculoActual(): Vinculo? =
        db.collection(FirestorePaths.PACIENTES)
            .whereEqualTo("usuarioId", uid)
            .whereEqualTo("activo", true)
            .limit(1)
            .get().await().documents
            .firstOrNull()
            ?.let { Vinculo(it.id, it.getString("nutriologoNombre").orEmpty(), it.aUltimoMensaje(), it.aMetas(), it.getString("nutriologoId").orEmpty()) }

    // Canjea el código y crea el registro del paciente en un solo lote: o pasan las dos escrituras o ninguna.
    suspend fun canjear(codigoEscrito: String, paciente: Perfil): Vinculo {
        val ahora = System.currentTimeMillis()
        val (codigo, ref, doc) = codigoCanjeable(codigoEscrito, ahora)

        val nutriologoNombre = doc.getString("nutriologoNombre").orEmpty()
        val pacienteId = UUID.randomUUID().toString()
        db.runBatch { lote ->
            lote.update(ref, mapOf("usado" to true, "canjeadoPor" to uid, "canjeadoEn" to ahora))
            lote.set(
                db.collection(FirestorePaths.PACIENTES).document(pacienteId),
                nuevoRegistroAMapa(doc.getString("nutriologoId"), nutriologoNombre, uid, paciente.nombre, paciente.correo, codigo, ahora),
            )
        }.await()
        return Vinculo(pacienteId, nutriologoNombre, nutriologoId = doc.getString("nutriologoId").orEmpty())
    }

    // Cierra un vínculo (no lo borra: queda `activo = false`, con quién y cuándo) y las citas vigentes entre los dos, todo en un lote. Lo usa el
    // paciente al desvincularse (`PACIENTE`) y el nutriólogo al darlo de baja (`NUTRIOLOGO`); las citas se cierran como lo dejan hacer las
    // reglas de cada uno (ver `estadoAlCerrar`). `CUENTA_ELIMINADA` no se escribe desde la app: lo hace el Worker.
    suspend fun cerrarVinculo(pacienteId: String, por: CerradoPor, citasACerrar: List<Cita>) {
        require(por != CerradoPor.CUENTA_ELIMINADA) { "CUENTA_ELIMINADA solo la escribe el Worker" }
        val ahora = System.currentTimeMillis()
        db.runBatch { lote ->
            lote.update(db.collection(FirestorePaths.PACIENTES).document(pacienteId), cierreAMapa(por, ahora))
            agregarCierreDeCitas(lote, citasACerrar, if (por == CerradoPor.PACIENTE) Rol.PACIENTE else Rol.NUTRIOLOGO, ahora)
        }.await()
    }

    // Cambia de nutriólogo en un solo lote: cierra el vínculo anterior (y sus citas vigentes), canjea el código nuevo y abre el vínculo nuevo,
    // con el expediente del anterior solo si `compartirExpediente` (nunca el chat, los planes ni las mediciones). El código se valida antes
    // de tocar nada y, si el lote falla, todo se queda como estaba: el paciente nunca se queda sin nutriólogo a medias.
    suspend fun cambiarNutriologo(
        codigoEscrito: String,
        paciente: Perfil,
        anterior: Vinculo,
        compartirExpediente: Boolean,
        citasACerrar: List<Cita>,
    ): Vinculo {
        val ahora = System.currentTimeMillis()
        val (codigo, ref, doc) = codigoCanjeable(codigoEscrito, ahora)
        // las reglas piden que el expediente copiado vaya firmado por el paciente
        val expediente = if (compartirExpediente) {
            ExpedienteRepository(db).leer(anterior.pacienteId).copy(actualizadoPor = Rol.PACIENTE, actualizadoEn = ahora).aMapa()
        } else {
            null
        }

        val nutriologoNombre = doc.getString("nutriologoNombre").orEmpty()
        val pacienteId = UUID.randomUUID().toString()
        db.runBatch { lote ->
            lote.update(db.collection(FirestorePaths.PACIENTES).document(anterior.pacienteId), cierreAMapa(CerradoPor.PACIENTE, ahora))
            agregarCierreDeCitas(lote, citasACerrar, Rol.PACIENTE, ahora)
            lote.update(ref, mapOf("usado" to true, "canjeadoPor" to uid, "canjeadoEn" to ahora))
            lote.set(
                db.collection(FirestorePaths.PACIENTES).document(pacienteId),
                nuevoRegistroAMapa(doc.getString("nutriologoId"), nutriologoNombre, uid, paciente.nombre, paciente.correo, codigo, ahora, expediente),
            )
        }.await()
        return Vinculo(pacienteId, nutriologoNombre, nutriologoId = doc.getString("nutriologoId").orEmpty())
    }

    // El nombre del nutriólogo de ese código si todavía se puede canjear (sin canjearlo); si no, VinculoException con el motivo. Sirve para
    // enseñar «pasarás con X» antes de confirmar un cambio de nutriólogo.
    suspend fun nutriologoDelCodigo(codigoEscrito: String): String =
        codigoCanjeable(codigoEscrito, System.currentTimeMillis()).third.getString("nutriologoNombre").orEmpty()

    // El código escrito, su referencia y su documento, si se puede canjear; si no, VinculoException con el motivo.
    private suspend fun codigoCanjeable(codigoEscrito: String, ahora: Long): Triple<String, DocumentReference, DocumentSnapshot> {
        val codigo = codigoEscrito.filter { it.isLetterOrDigit() }.uppercase()
        if (codigo.length != LARGO_CODIGO) throw VinculoException(VinculoException.Motivo.INVALIDO)

        val ref = db.collection(FirestorePaths.CODIGOS_VINCULACION).document(codigo)
        val doc = ref.get().await()
        if (!doc.exists()) throw VinculoException(VinculoException.Motivo.INVALIDO)
        if (doc.getBoolean("usado") == true) throw VinculoException(VinculoException.Motivo.USADO)
        if ((doc.getLong("expiraEn") ?: 0L) <= ahora) throw VinculoException(VinculoException.Motivo.VENCIDO)
        return Triple(codigo, ref, doc)
    }

    private fun agregarCierreDeCitas(lote: WriteBatch, citas: List<Cita>, por: Rol, ahora: Long) {
        for (cita in citas) estadoAlCerrar(cita, por, ahora)?.let { escribirCambioDeEstado(db, lote, cita, it) }
    }

    private fun nuevoCodigo(): String =
        buildString { repeat(LARGO_CODIGO) { append(ALFABETO[aleatorio.nextInt(ALFABETO.length)]) } }
}

// Los campos que se escriben al cerrar un vínculo: son exactamente los que aceptan las reglas de Firestore.
internal fun cierreAMapa(por: CerradoPor, ahora: Long): Map<String, Any> =
    mapOf("activo" to false, "cerradoEn" to ahora, "cerradoPor" to por.name, "ultimaModificacion" to ahora)

// El registro de un paciente con un nutriólogo, tal como lo crea el canje de un código; con `expediente` (ya firmado por el paciente) cuando
// cambia de nutriólogo y decide compartirlo. El correo va en minúsculas, como lo piden las reglas.
internal fun nuevoRegistroAMapa(
    nutriologoId: String?,
    nutriologoNombre: String,
    usuarioId: String,
    nombre: String,
    correo: String,
    codigo: String,
    ahora: Long,
    expediente: Map<String, Any>? = null,
): Map<String, Any?> = buildMap {
    put("nutriologoId", nutriologoId)
    put("nutriologoNombre", nutriologoNombre)
    put("usuarioId", usuarioId)
    put("nombre", nombre)
    put("correoVinculacion", correo.lowercase())
    put("codigoVinculacion", codigo)
    put("activo", true)
    put("fechaAlta", ahora)
    put("ultimaModificacion", ahora)
    if (expediente != null) put("expediente", expediente)
}
