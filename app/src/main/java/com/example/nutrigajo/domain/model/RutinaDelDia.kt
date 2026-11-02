package com.example.nutrigajo.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

// A cada día de la semana le toca un enfoque (de lunes a domingo); de él salen los ejercicios de la rutina del día.
enum class EnfoqueDia(private val admite: (Ejercicio) -> Boolean) {
    PIERNAS({ it.zona == ZonaCuerpo.PIERNAS && !it.tipo.esDeEstirar() }),
    PECHO_Y_BRAZOS({ (it.zona == ZonaCuerpo.PECHO || it.zona == ZonaCuerpo.BRAZOS) && !it.tipo.esDeEstirar() }),
    ESPALDA_Y_HOMBROS({ (it.zona == ZonaCuerpo.ESPALDA || it.zona == ZonaCuerpo.HOMBROS) && !it.tipo.esDeEstirar() }),
    ABDOMEN_Y_CARDIO({ it.zona == ZonaCuerpo.ABDOMEN || it.tipo == TipoEjercicio.CARDIO }),
    CUERPO_COMPLETO({ it.zona == ZonaCuerpo.CUERPO_COMPLETO && !it.tipo.esDeEstirar() }),
    CARDIO_ACTIVO({ it.tipo == TipoEjercicio.CARDIO }),
    MOVILIDAD({ it.tipo.esDeEstirar() });

    fun admite(ejercicio: Ejercicio): Boolean = admite.invoke(ejercicio)

    companion object {
        // DayOfWeek va de lunes (0) a domingo (6), igual que las entradas
        fun de(dia: DayOfWeek): EnfoqueDia = entries[dia.ordinal]
    }
}

private fun TipoEjercicio.esDeEstirar() = this == TipoEjercicio.FLEXIBILIDAD || this == TipoEjercicio.YOGA

// La rutina que el inicio propone hoy: ejercicios del banco global según el enfoque del día.
data class RutinaDelDia(val enfoque: EnfoqueDia, val ejercicios: List<Ejercicio>) {
    val minutos: Int get() = ejercicios.sumOf { it.duracionMin }
    val kcal: Int get() = ejercicios.sumOf { it.kcal }

    // el nivel medio de sus ejercicios, redondeado
    val nivel: NivelEjercicio
        get() = NivelEjercicio.entries[ejercicios.map { it.nivel.ordinal }.average().let { if (it.isNaN()) 0 else Math.round(it).toInt() }]
}

const val EJERCICIOS_POR_RUTINA = 6
const val MINUTOS_MAX_RUTINA = 60
private const val EJERCICIOS_MIN_RUTINA = 3

// Arma la rutina de `fecha`: del banco toma lo que admite el enfoque de ese día de la semana, lo baraja con la fecha como semilla (así
// cada día es distinto y todos los teléfonos ven la misma) y suma ejercicios hasta `cantidad` o hasta pasar de una hora, con tres como
// mínimo. Lo de estirar o de yoga va al final. Con un banco pequeño completa con lo que haya; sin ejercicios no hay rutina.
fun rutinaDelDia(fecha: LocalDate, banco: List<Ejercicio>, cantidad: Int = EJERCICIOS_POR_RUTINA): RutinaDelDia? {
    val publicados = banco.filter { it.estado == EstadoEjercicio.PUBLICADO }.sortedBy { it.id }
    if (publicados.isEmpty()) return null
    val enfoque = EnfoqueDia.de(fecha.dayOfWeek)
    val delEnfoque = publicados.filter(enfoque::admite)
    val ordenados = (delEnfoque.shuffled(Random(fecha.toEpochDay())) + (publicados - delEnfoque.toSet()).shuffled(Random(fecha.toEpochDay())))
    val elegidos = mutableListOf<Ejercicio>()
    for (ejercicio in ordenados) {
        if (elegidos.size >= cantidad) break
        if (elegidos.size >= EJERCICIOS_MIN_RUTINA && elegidos.sumOf { it.duracionMin } + ejercicio.duracionMin > MINUTOS_MAX_RUTINA) continue
        elegidos += ejercicio
    }
    return RutinaDelDia(enfoque, elegidos.sortedBy { if (it.tipo.esDeEstirar()) 1 else 0 })
}
