package com.example.nutrigajo.domain.model

import kotlin.math.pow

// No renombrar valores: Firestore guarda el nombre del enum como texto.
enum class GrupoAlimento { VERDURAS, FRUTAS, CEREALES, ANIMAL, LEGUMINOSAS, LACTEOS, GRASAS_PROTEINA, GRASAS }

// Porciones («equivalentes») de un grupo de alimentos en cada tiempo de comida.
data class Equivalentes(val desayuno: Int = 0, val comida: Int = 0, val cena: Int = 0) {
    val total: Int get() = desayuno + comida + cena
}

// Una opción de menú para los tres tiempos de comida.
data class OpcionMenu(val desayuno: String = "", val comida: String = "", val cena: String = "")

// Un día de la rutina: cuándo, en qué se enfoca y qué se hace.
data class SesionEjercicio(val dia: String = "", val enfoque: String = "", val detalle: String = "")

// De dónde sale un ejercicio asignado: el banco de los entrenadores (Firestore) o, más adelante, el catálogo curado en R2.
// No renombrar valores: el plan guarda el nombre.
enum class FuenteEjercicio { BANCO, CATALOGO }

// Un ejercicio del banco que el nutriólogo asigna a su paciente en un día de la semana (1 = lunes … 7 = domingo), con sus series y
// repeticiones o con sus minutos. `titulo` se copia al asignar para que el plan se vea aunque el ejercicio se retire después.
data class EjercicioAsignado(
    val dia: Int,
    val fuente: FuenteEjercicio = FuenteEjercicio.BANCO,
    val id: String,
    val titulo: String,
    val series: Int? = null,
    val repeticiones: Int? = null,
    val minutos: Int? = null,
    val nota: String = "",
) {
    companion object {
        const val MAX_SERIES = 20
        const val MAX_REPETICIONES = 100
        const val MAX_MINUTOS = 180
        const val MAX_NOTA = 200
    }
}

// `maxDia`: el último día que admite el plan (7 de la semana o la duración).
fun EjercicioAsignado.valido(maxDia: Int = 7): Boolean =
    dia in 1..maxDia && id.isNotBlank() && titulo.isNotBlank() &&
        (series == null || series in 1..EjercicioAsignado.MAX_SERIES) &&
        (repeticiones == null || repeticiones in 1..EjercicioAsignado.MAX_REPETICIONES) &&
        (minutos == null || minutos in 1..EjercicioAsignado.MAX_MINUTOS) &&
        nota.trim().length <= EjercicioAsignado.MAX_NOTA

// Cuánto dura en «Entrenar ahora»: sus minutos o, sin ellos, 2 por serie (2 si no hay series); nunca menos de 1.
// ponytail: los asignados con series no traen duración; la estimación es fija y se afina si hace falta.
fun EjercicioAsignado.duracionMin(): Int = (minutos ?: ((series ?: 1) * 2)).coerceAtLeast(1)

fun List<EjercicioAsignado>.delDia(dia: Int): List<EjercicioAsignado> = filter { it.dia == dia }

// Lo que se le pide al paciente, como se lee en el plan y en el PDF: «3 × 10», «3 series», «15 min», «3 × 10 · 20 min» o «—» si no hay nada.
fun EjercicioAsignado.dosis(): String {
    val partes = listOfNotNull(
        series?.let { if (repeticiones != null) "$it × $repeticiones" else "$it series" },
        minutos?.let { "$it min" },
    )
    return if (partes.isEmpty()) "—" else partes.joinToString(" · ")
}

// Lo que el nutriólogo arma para el paciente: plan de alimentación y de ejercicio. Todo es opcional; el PDF solo lleva las
// secciones que tienen contenido. Se guarda con el plan enviado para partir de él en el siguiente.
data class PlanContenido(
    // saludo y objetivo, en palabras del nutriólogo
    val mensaje: String = "",
    val pesoKg: Double? = null,
    val tallaCm: Double? = null,
    val cinturaCm: Double? = null,
    val caderaCm: Double? = null,
    val grasaPct: Double? = null,
    // texto libre: «13 de noviembre»
    val seguimiento: String = "",
    val metas: List<String> = emptyList(),
    val calorias: Int? = null,
    val equivalentes: Map<GrupoAlimento, Equivalentes> = emptyMap(),
    val opciones: List<OpcionMenu> = emptyList(),
    val ejercicioNotas: String = "",
    val sesiones: List<SesionEjercicio> = emptyList(),
    // los ejercicios concretos del banco que asigna el nutriólogo; los vigentes son los del plan más reciente
    val ejerciciosAsignados: List<EjercicioAsignado> = emptyList(),
    // cuántos días dura el plan (1…MAX_DIAS_PLAN); null = una semana de lunes a domingo. Con duración, el `dia` de cada asignado es el día del plan
    val duracionDias: Int? = null,
    // el menú de cada día del plan: en la posición día − 1, el índice de la opción de `opciones` que toca ese día, o −1 si ninguna
    val menuDias: List<Int> = emptyList(),
    val notas: String = "",
) {
    // cuántos ejercicios asignados caben en el plan entero
    val maxAsignados: Int get() = if (duracionDias == null) MAX_ASIGNADOS else MAX_ASIGNADOS_PERIODO

    val imc: Double? get() = if (pesoKg != null && tallaCm != null && pesoKg > 0 && tallaCm > 0) imc(pesoKg, tallaCm) else null

    // Tiene algo que mandar: sin ninguna sección, el PDF sería una portada vacía.
    val vacio: Boolean
        get() = mensaje.isBlank() && metas.isEmpty() && calorias == null && equivalentes.values.all { it.total == 0 } &&
            opciones.all { it.desayuno.isBlank() && it.comida.isBlank() && it.cena.isBlank() } &&
            ejercicioNotas.isBlank() && sesiones.all { it.detalle.isBlank() && it.enfoque.isBlank() } && ejerciciosAsignados.isEmpty() && notas.isBlank()

    // Lo que se manda: sin espacios sobrantes, sin opciones ni sesiones en blanco y sin grupos de alimentos sin porciones. Con duración se descarta lo
    // que cae fuera de ella y los índices del menú por día se ajustan a las opciones que se quedaron.
    fun limpio(): PlanContenido {
        val duracion = duracionDias?.coerceIn(1, MAX_DIAS_PLAN)
        val tope = duracion ?: 7
        val recortadas = opciones.map { OpcionMenu(it.desayuno.trim(), it.comida.trim(), it.cena.trim()) }
        val conservadas = recortadas.indices.filter { recortadas[it].let { o -> o.desayuno.isNotEmpty() || o.comida.isNotEmpty() || o.cena.isNotEmpty() } }
        val nuevoIndice = conservadas.withIndex().associate { (nuevo, viejo) -> viejo to nuevo }
        return copy(
            mensaje = mensaje.trim(),
            seguimiento = seguimiento.trim(),
            metas = metas.map { it.trim() }.filter { it.isNotEmpty() },
            equivalentes = equivalentes.filterValues { it.total > 0 },
            opciones = conservadas.map { recortadas[it] },
            ejercicioNotas = ejercicioNotas.trim(),
            sesiones = sesiones
                .map { SesionEjercicio(it.dia.trim(), it.enfoque.trim(), it.detalle.trim()) }
                .filter { it.enfoque.isNotEmpty() || it.detalle.isNotEmpty() },
            ejerciciosAsignados = ejerciciosAsignados
                .map { it.copy(id = it.id.trim(), titulo = it.titulo.trim(), nota = it.nota.trim()) }
                .filter { it.valido(tope) }
                .distinctBy { Triple(it.dia, it.fuente, it.id) }
                .acotados(if (duracion == null) MAX_ASIGNADOS else MAX_ASIGNADOS_PERIODO),
            duracionDias = duracion,
            menuDias = if (duracion == null) emptyList() else menuDias.take(duracion).map { nuevoIndice[it] ?: -1 }.dropLastWhile { it < 0 },
            notas = notas.trim(),
        )
    }

    companion object {
        // el mismo tope que SelectorEtiquetas
        const val MAX_METAS = 10
        const val MAX_OPCIONES = 6
        const val MAX_SESIONES = 7
        // con duración puede haber una sesión de texto por día del plan
        const val MAX_SESIONES_PERIODO = 30
        const val MAX_ASIGNADOS = 35
        // en un plan con duración (hasta 90 días) caben más
        const val MAX_ASIGNADOS_PERIODO = 300
        const val MAX_ASIGNADOS_DIA = 10
        const val MAX_TEXTO = 1200
        const val MAX_MENU = 500
    }
}

// Los primeros que caben: hasta MAX_ASIGNADOS_DIA por día y `maxTotal` en total, sin cambiar el orden.
private fun List<EjercicioAsignado>.acotados(maxTotal: Int): List<EjercicioAsignado> {
    val porDia = IntArray(MAX_DIAS_PLAN + 1)
    val salida = mutableListOf<EjercicioAsignado>()
    for (e in this) {
        if (salida.size >= maxTotal) break
        if (porDia[e.dia] >= PlanContenido.MAX_ASIGNADOS_DIA) continue
        porDia[e.dia]++
        salida += e
    }
    return salida
}

// Un plan ya enviado: lo que se mandó y cuándo; el PDF se pide aparte al Worker con su id.
data class PlanEnviado(val id: String, val enviadoEn: Long, val nutriologoNombre: String, val contenido: PlanContenido)

// Índice de masa corporal (kg/m²) y sus rangos, como en el formato de los planes.
enum class RangoImc { BAJO_PESO, SALUDABLE, SOBREPESO, OBESIDAD_I, OBESIDAD_II }

fun imc(pesoKg: Double, tallaCm: Double): Double = pesoKg / (tallaCm / 100).pow(2)

fun rangoImc(imc: Double): RangoImc = when {
    imc < 18.5 -> RangoImc.BAJO_PESO
    imc < 25.0 -> RangoImc.SALUDABLE
    imc < 30.0 -> RangoImc.SOBREPESO
    imc < 35.0 -> RangoImc.OBESIDAD_I
    else -> RangoImc.OBESIDAD_II
}
