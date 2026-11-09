package com.example.nutrigajo.data.repository

import com.example.nutrigajo.domain.model.EjercicioAsignado
import com.example.nutrigajo.domain.model.Equivalentes
import com.example.nutrigajo.domain.model.FuenteEjercicio
import com.example.nutrigajo.domain.model.GrupoAlimento
import com.example.nutrigajo.domain.model.OpcionMenu
import com.example.nutrigajo.domain.model.PlanContenido
import com.example.nutrigajo.domain.model.SesionEjercicio

// El contenido del plan como mapa de Firestore (pacientes/{id}/planes/{planId}.contenido). Lo vacío no se escribe.
internal fun PlanContenido.aMapa(): Map<String, Any> = buildMap {
    if (mensaje.isNotBlank()) put("mensaje", mensaje)
    pesoKg?.let { put("pesoKg", it) }
    tallaCm?.let { put("tallaCm", it) }
    cinturaCm?.let { put("cinturaCm", it) }
    caderaCm?.let { put("caderaCm", it) }
    grasaPct?.let { put("grasaPct", it) }
    if (seguimiento.isNotBlank()) put("seguimiento", seguimiento)
    if (metas.isNotEmpty()) put("metas", metas)
    calorias?.let { put("calorias", it) }
    val porciones = equivalentes.filterValues { it.total > 0 }
    if (porciones.isNotEmpty()) {
        put("equivalentes", porciones.entries.associate { (grupo, e) -> grupo.name to listOf(e.desayuno, e.comida, e.cena) })
    }
    if (opciones.isNotEmpty()) {
        put("opciones", opciones.map { mapOf("desayuno" to it.desayuno, "comida" to it.comida, "cena" to it.cena) })
    }
    if (ejercicioNotas.isNotBlank()) put("ejercicioNotas", ejercicioNotas)
    if (sesiones.isNotEmpty()) {
        put("sesiones", sesiones.map { mapOf("dia" to it.dia, "enfoque" to it.enfoque, "detalle" to it.detalle) })
    }
    if (ejerciciosAsignados.isNotEmpty()) {
        put(
            "ejerciciosAsignados",
            ejerciciosAsignados.map { a ->
                buildMap<String, Any> {
                    put("dia", a.dia)
                    put("fuente", a.fuente.name)
                    put("id", a.id)
                    put("titulo", a.titulo)
                    a.series?.let { put("series", it) }
                    a.repeticiones?.let { put("repeticiones", it) }
                    a.minutos?.let { put("minutos", it) }
                    if (a.nota.isNotBlank()) put("nota", a.nota)
                }
            },
        )
    }
    duracionDias?.let { put("duracionDias", it) }
    if (menuDias.isNotEmpty()) put("menuDias", menuDias)
    if (notas.isNotBlank()) put("notas", notas)
}

private fun Any?.aTextos(): List<String> = (this as? List<*>)?.filterIsInstance<String>().orEmpty()

private fun Any?.aNumero(): Double? = (this as? Number)?.toDouble()

internal fun Map<*, *>.aPlanContenido(): PlanContenido {
    val porciones = (this["equivalentes"] as? Map<*, *>).orEmpty().mapNotNull { (clave, valor) ->
        val grupo = GrupoAlimento.entries.firstOrNull { it.name == clave } ?: return@mapNotNull null
        val n = (valor as? List<*>).orEmpty().map { (it as? Number)?.toInt() ?: 0 }
        grupo to Equivalentes(n.getOrElse(0) { 0 }, n.getOrElse(1) { 0 }, n.getOrElse(2) { 0 })
    }.toMap()
    return PlanContenido(
        mensaje = this["mensaje"] as? String ?: "",
        pesoKg = this["pesoKg"].aNumero(),
        tallaCm = this["tallaCm"].aNumero(),
        cinturaCm = this["cinturaCm"].aNumero(),
        caderaCm = this["caderaCm"].aNumero(),
        grasaPct = this["grasaPct"].aNumero(),
        seguimiento = this["seguimiento"] as? String ?: "",
        metas = this["metas"].aTextos(),
        calorias = (this["calorias"] as? Number)?.toInt(),
        equivalentes = porciones,
        opciones = (this["opciones"] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>().map {
            OpcionMenu(it["desayuno"] as? String ?: "", it["comida"] as? String ?: "", it["cena"] as? String ?: "")
        },
        ejercicioNotas = this["ejercicioNotas"] as? String ?: "",
        sesiones = (this["sesiones"] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>().map {
            SesionEjercicio(it["dia"] as? String ?: "", it["enfoque"] as? String ?: "", it["detalle"] as? String ?: "")
        },
        ejerciciosAsignados = (this["ejerciciosAsignados"] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>().mapNotNull { it.aAsignado() },
        duracionDias = (this["duracionDias"] as? Number)?.toInt(),
        menuDias = (this["menuDias"] as? List<*>).orEmpty().map { (it as? Number)?.toInt() ?: -1 },
        notas = this["notas"] as? String ?: "",
    )
}

// null si falta el id o la fuente no se reconoce (un plan de una versión futura de la app): se descarta solo ese ejercicio.
private fun Map<*, *>.aAsignado(): EjercicioAsignado? {
    val fuente = FuenteEjercicio.entries.firstOrNull { it.name == this["fuente"] } ?: return null
    val id = (this["id"] as? String)?.takeIf { it.isNotBlank() } ?: return null
    return EjercicioAsignado(
        dia = (this["dia"] as? Number)?.toInt() ?: return null,
        fuente = fuente,
        id = id,
        titulo = this["titulo"] as? String ?: "",
        series = (this["series"] as? Number)?.toInt(),
        repeticiones = (this["repeticiones"] as? Number)?.toInt(),
        minutos = (this["minutos"] as? Number)?.toInt(),
        nota = this["nota"] as? String ?: "",
    )
}
