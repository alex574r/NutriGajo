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
// `asignada` es true cuando sale de lo que el nutriólogo asignó ese día y false cuando es la rutina que se arma sola.
data class RutinaDelDia(val enfoque: EnfoqueDia, val ejercicios: List<Ejercicio>, val asignada: Boolean = false) {
    val minutos: Int get() = ejercicios.sumOf { it.duracionMin }
    val kcal: Int get() = ejercicios.sumOf { it.kcal }

    // el nivel medio de sus ejercicios, redondeado
    val nivel: NivelEjercicio
        get() = NivelEjercicio.entries[ejercicios.map { it.nivel.ordinal }.average().let { if (it.isNaN()) 0 else Math.round(it).toInt() }]
}

// La rutina de `fecha` con lo que el nutriólogo asignó a ese día de la semana (`dayOfWeek.value`: 1 lunes … 7 domingo), en el orden del
// plan. `resolver` da el ejercicio de cada asignado (null si ya no está disponible: se salta). Cada ejercicio sale con la duración asignada
// y sus kcal escaladas a ella. null si ese día no queda ningún asignado resoluble: el inicio cae entonces a `rutinaDelDia`.
// `dia` es el día de los asignados que toca hoy: el de la semana (por defecto) o, en un plan con duración, el día del plan.
fun rutinaAsignada(
    fecha: LocalDate,
    asignados: List<EjercicioAsignado>,
    resolver: (EjercicioAsignado) -> Ejercicio?,
    dia: Int = fecha.dayOfWeek.value,
): RutinaDelDia? {
    val ejercicios = asignados.delDia(dia).mapNotNull { asignado ->
        resolver(asignado)?.let { ejercicio ->
            val minutos = asignado.duracionMin()
            ejercicio.copy(duracionMin = minutos, kcal = if (ejercicio.duracionMin > 0) ejercicio.kcal * minutos / ejercicio.duracionMin else ejercicio.kcal)
        }
    }
    if (ejercicios.isEmpty()) return null
    return RutinaDelDia(EnfoqueDia.de(fecha.dayOfWeek), ejercicios, asignada = true)
}

// De dónde saca el paciente el ejercicio de cada asignado: del banco ya leído, y solo si sigue PUBLICADO (uno retirado o aún en revisión
// ya no se entrena). Los de fuente CATALOGO se resuelven con la ficha ya bajada del catálogo (`catalogo`, por id): sin ficha todavía, o sin red,
// no se resuelven y el inicio cae al baraje, igual que con uno retirado.
fun resolverDelBanco(banco: List<Ejercicio>, catalogo: Map<String, Ejercicio> = emptyMap()): (EjercicioAsignado) -> Ejercicio? {
    val publicados = banco.filter { it.estado == EstadoEjercicio.PUBLICADO }.associateBy { it.id }
    return { asignado -> if (asignado.fuente == FuenteEjercicio.BANCO) publicados[asignado.id] else catalogo[asignado.id] }
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
