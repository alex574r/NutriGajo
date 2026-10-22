package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.CalidadSueno
import com.example.nutrigajo.domain.model.Dieta
import com.example.nutrigajo.domain.model.EjercicioPreferido
import com.example.nutrigajo.domain.model.Expediente
import com.example.nutrigajo.domain.model.Objetivo
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.domain.model.Sexo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Expediente del paciente, guardado como mapa dentro de pacientes/{id}. Lo leen y escriben el paciente y su nutriólogo
// (las reglas de Firestore deciden quién). Cada escritura reemplaza el mapa entero: la pantalla parte de lo ya guardado.
// ponytail: sin copia en Room todavía; el expediente se lee de Firestore al abrirlo.
class ExpedienteRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun paciente(id: String) = db.collection(FirestorePaths.PACIENTES).document(id)

    suspend fun leer(pacienteId: String): Expediente {
        val mapa = paciente(pacienteId).get().await().get("expediente") as? Map<*, *> ?: return Expediente()
        return mapa.aExpediente()
    }

    suspend fun guardar(pacienteId: String, expediente: Expediente, editor: Rol) {
        val ahora = System.currentTimeMillis()
        val guardado = expediente.copy(actualizadoPor = editor, actualizadoEn = ahora)
        paciente(pacienteId).update("expediente", guardado.aMapa(), "ultimaModificacion", ahora).await()
    }
}

// Los campos sin respuesta no se escriben: al reemplazar el mapa, desaparecen.
internal fun Expediente.aMapa(): Map<String, Any> = buildMap {
    objetivo?.let { put("objetivo", it.name) }
    sexo?.let { put("sexo", it.name) }
    fechaNacimiento?.let { put("fechaNacimiento", it) }
    pesoKg?.let { put("pesoKg", it) }
    estaturaCm?.let { put("estaturaCm", it) }
    nivelActividad?.let { put("nivelActividad", it) }
    if (condiciones.isNotEmpty()) put("condiciones", condiciones)
    if (alergias.isNotEmpty()) put("alergias", alergias)
    dieta?.let { put("dieta", it.name) }
    diasEjercicio?.let { put("diasEjercicio", it) }
    if (ejercicios.isNotEmpty()) put("ejercicios", ejercicios.map { it.name })
    if (suplementos.isNotEmpty()) put("suplementos", suplementos)
    caloriasObjetivo?.let { put("caloriasObjetivo", it) }
    calidadSueno?.let { put("calidadSueno", it.name) }
    put("paso", paso)
    put("completo", completo)
    actualizadoPor?.let { put("actualizadoPor", it.name) }
    put("actualizadoEn", actualizadoEn)
}

private inline fun <reified E : Enum<E>> Any?.aEnum(): E? =
    (this as? String)?.let { nombre -> enumValues<E>().firstOrNull { it.name == nombre } }

private fun Any?.aTextos(): List<String> = (this as? List<*>)?.filterIsInstance<String>().orEmpty()

internal fun Map<*, *>.aExpediente() = Expediente(
    objetivo = this["objetivo"].aEnum<Objetivo>(),
    sexo = this["sexo"].aEnum<Sexo>(),
    fechaNacimiento = this["fechaNacimiento"] as? String,
    pesoKg = (this["pesoKg"] as? Number)?.toDouble(),
    estaturaCm = (this["estaturaCm"] as? Number)?.toDouble(),
    nivelActividad = (this["nivelActividad"] as? Number)?.toInt(),
    condiciones = this["condiciones"].aTextos(),
    alergias = this["alergias"].aTextos(),
    dieta = this["dieta"].aEnum<Dieta>(),
    diasEjercicio = (this["diasEjercicio"] as? Number)?.toInt(),
    ejercicios = this["ejercicios"].aTextos().mapNotNull { it.aEnum<EjercicioPreferido>() },
    suplementos = this["suplementos"].aTextos(),
    caloriasObjetivo = (this["caloriasObjetivo"] as? Number)?.toInt(),
    calidadSueno = this["calidadSueno"].aEnum<CalidadSueno>(),
    paso = (this["paso"] as? Number)?.toInt() ?: 0,
    completo = this["completo"] as? Boolean ?: false,
    actualizadoPor = this["actualizadoPor"].aEnum<Rol>(),
    actualizadoEn = (this["actualizadoEn"] as? Number)?.toLong() ?: 0L,
)
