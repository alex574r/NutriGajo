package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.MetasDiarias
import com.example.nutrigajo.domain.model.Rol
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Las metas diarias viven en el registro del paciente (pacientes/{id}.metas). Las escriben su nutriólogo y él mismo, cada uno
// firmando con su rol; las reglas de Firestore son las que mandan.
class MetasRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun guardar(pacienteId: String, metas: MetasDiarias, por: Rol) {
        db.collection(FirestorePaths.PACIENTES).document(pacienteId).update(
            "metas",
            mapOf(
                "aguaMl" to metas.aguaMl,
                "pasos" to metas.pasos,
                "kcal" to metas.kcal,
                "minutos" to metas.minutos,
                "por" to por.name,
                "actualizadoEn" to System.currentTimeMillis(),
            ),
        ).await()
    }
}

// Las metas guardadas en el registro del paciente; sin nada guardado (o con algo fuera de rango) valen las de siempre.
internal fun DocumentSnapshot.aMetas(): MetasDiarias {
    val guardadas = get("metas") as? Map<*, *> ?: return MetasDiarias()
    val porDefecto = MetasDiarias()
    fun entero(clave: String, rango: IntRange, defecto: Int) = (guardadas[clave] as? Number)?.toInt()?.takeIf { it in rango } ?: defecto
    return MetasDiarias(
        aguaMl = entero("aguaMl", MetasDiarias.AGUA_ML, porDefecto.aguaMl),
        pasos = entero("pasos", MetasDiarias.PASOS, porDefecto.pasos),
        kcal = entero("kcal", MetasDiarias.KCAL, porDefecto.kcal),
        minutos = entero("minutos", MetasDiarias.MINUTOS, porDefecto.minutos),
        por = (guardadas["por"] as? String)?.let { runCatching { Rol.valueOf(it) }.getOrNull() },
    )
}
