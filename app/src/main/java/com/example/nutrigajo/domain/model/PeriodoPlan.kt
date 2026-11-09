package com.example.nutrigajo.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Un plan puede durar de 1 a 90 días. Sin `duracionDias` es la semana de siempre (lunes = 1 … domingo = 7); con ella los ejercicios asignados y
// el menú se cuentan por «día del plan» (1…N) desde el día en que se envió.
const val MAX_DIAS_PLAN = 90

// El día del plan (1…N) que es `hoy`; null si el plan aún no empieza o ya terminó.
fun diaDelPlan(hoy: LocalDate, inicio: LocalDate, duracionDias: Int): Int? =
    (ChronoUnit.DAYS.between(inicio, hoy).toInt() + 1).takeIf { it in 1..duracionDias }

// Cómo se cuentan los días de un plan ya enviado: por semana o, con duración, desde `inicio` (el día en que se envió).
data class CalendarioPlan(val duracionDias: Int?, val inicio: LocalDate) {
    // cuántos días tiene el plan: los de la duración o los siete de la semana
    val dias: Int get() = duracionDias ?: 7

    // el día de los asignados que es `fecha`: el del plan, o el de la semana si el plan no tiene duración; null fuera del periodo
    fun diaDe(fecha: LocalDate): Int? = if (duracionDias == null) fecha.dayOfWeek.value else diaDelPlan(fecha, inicio, duracionDias)

    // la fecha de un día del plan; null si el plan es semanal (ese «día» es un día de la semana, sin fecha)
    fun fechaDe(dia: Int): LocalDate? = duracionDias?.let { inicio.plusDays((dia - 1).toLong()) }
}

fun PlanEnviado.calendario(zona: ZoneId = ZoneId.systemDefault()): CalendarioPlan =
    CalendarioPlan(contenido.duracionDias, Instant.ofEpochMilli(enviadoEn).atZone(zona).toLocalDate())

// Cambia la duración (null = semana de lunes a domingo) y recorta lo que queda fuera: los ejercicios de días que ya no existen y el menú de esos
// días. Con una semana el menú por día no aplica (se cuenta por días del plan) y se quita.
fun PlanContenido.conDuracion(dias: Int?): PlanContenido {
    val n = dias?.coerceIn(1, MAX_DIAS_PLAN)
    val maxDia = n ?: 7
    return copy(
        duracionDias = n,
        menuDias = if (n == null) emptyList() else menuDias.take(n),
        ejerciciosAsignados = ejerciciosAsignados.filter { it.dia <= maxDia },
    )
}

// Elige la opción de menú (índice en `opciones`) de un día del plan; null la quita. `menuDias` se rellena con -1 hasta ese día y no guarda
// los -1 del final.
fun PlanContenido.conMenuDelDia(dia: Int, opcion: Int?): PlanContenido {
    if (dia < 1) return this
    val lista = menuDias.toMutableList()
    while (lista.size < dia) lista.add(-1)
    lista[dia - 1] = opcion ?: -1
    return copy(menuDias = lista.dropLastWhile { it < 0 })
}

// La opción de menú (índice en `opciones`) de ese día del plan; null si ese día no tiene o apunta a una opción que ya no existe.
fun PlanContenido.opcionDelDia(dia: Int): Int? = menuDias.getOrNull(dia - 1)?.takeIf { it in opciones.indices }

// Quita la opción de menú `indice` y ajusta el menú por día: los días que la usaban se quedan sin menú y los de las opciones siguientes bajan un lugar.
fun PlanContenido.sinOpcion(indice: Int): PlanContenido {
    if (indice !in opciones.indices) return this
    return copy(
        opciones = opciones.filterIndexed { i, _ -> i != indice },
        menuDias = menuDias.map { if (it == indice) -1 else if (it > indice) it - 1 else it }.dropLastWhile { it < 0 },
    )
}
