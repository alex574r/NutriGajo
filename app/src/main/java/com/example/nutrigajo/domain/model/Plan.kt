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
    val notas: String = "",
) {
    val imc: Double? get() = if (pesoKg != null && tallaCm != null && pesoKg > 0 && tallaCm > 0) imc(pesoKg, tallaCm) else null

    // Tiene algo que mandar: sin ninguna sección, el PDF sería una portada vacía.
    val vacio: Boolean
        get() = mensaje.isBlank() && metas.isEmpty() && calorias == null && equivalentes.values.all { it.total == 0 } &&
            opciones.all { it.desayuno.isBlank() && it.comida.isBlank() && it.cena.isBlank() } &&
            ejercicioNotas.isBlank() && sesiones.all { it.detalle.isBlank() && it.enfoque.isBlank() } && notas.isBlank()

    // Lo que se manda: sin espacios sobrantes, sin opciones ni sesiones en blanco y sin grupos de alimentos sin porciones.
    fun limpio(): PlanContenido = copy(
        mensaje = mensaje.trim(),
        seguimiento = seguimiento.trim(),
        metas = metas.map { it.trim() }.filter { it.isNotEmpty() },
        equivalentes = equivalentes.filterValues { it.total > 0 },
        opciones = opciones
            .map { OpcionMenu(it.desayuno.trim(), it.comida.trim(), it.cena.trim()) }
            .filter { it.desayuno.isNotEmpty() || it.comida.isNotEmpty() || it.cena.isNotEmpty() },
        ejercicioNotas = ejercicioNotas.trim(),
        sesiones = sesiones
            .map { SesionEjercicio(it.dia.trim(), it.enfoque.trim(), it.detalle.trim()) }
            .filter { it.enfoque.isNotEmpty() || it.detalle.isNotEmpty() },
        notas = notas.trim(),
    )

    companion object {
        // el mismo tope que SelectorEtiquetas
        const val MAX_METAS = 10
        const val MAX_OPCIONES = 6
        const val MAX_SESIONES = 7
        const val MAX_TEXTO = 1200
        const val MAX_MENU = 500
    }
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
