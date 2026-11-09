package com.example.nutrigajo.ui.paciente.inicio

import androidx.annotation.StringRes
import androidx.compose.runtime.compositionLocalOf
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

// Las calorías gastadas que enseñan la tarjeta «Actividades» y la pantalla «Calorías». Todo lo de este archivo es lógica pura (sin
// Android): el inicio arma los `DatosKcal` con lo del reloj y lo de la app y cada rango se calcula aquí.

// Lo gastado según la app (una caminata, una carrera o una rutina), puesto en su momento: ms desde 1970.
data class GastoApp(val instante: Long, val kcal: Int)

// Las dos fuentes. `relojPorDia`: calorías activas de Health Connect por día (LocalDate.toEpochDay); `relojPorHora`: las de hoy hora por
// hora (24 valores, o vacío si no hay permiso); `app`: lo que guardó el Modo carrera y «Entrenar ahora».
data class DatosKcal(
    val relojPorDia: Map<Long, Int> = emptyMap(),
    val relojPorHora: List<Int> = emptyList(),
    val app: List<GastoApp> = emptyList(),
)

// Lo que se dibuja de un rango: un valor por cubeta (hora, día o mes) con su rótulo, el total y cuánto cambió contra el periodo
// anterior (null si no hay con qué comparar).
data class SerieKcal(val valores: List<Int>, val rotulos: List<String>, val total: Int, val variacion: Int?)

// Lo que el inicio reparte con `LocalCalorias`: los datos, si aún se leen y el error de la lectura de la app, si lo hubo.
data class EstadoKcal(val datos: DatosKcal = DatosKcal(), val cargando: Boolean = false, @StringRes val error: Int? = null)

val LocalCalorias = compositionLocalOf { EstadoKcal() }

// Hoy, desde las 0:00 hasta la hora en curso.
fun kcalDeHoy(datos: DatosKcal, ahora: Instant, zona: ZoneId): Int = serieKcal(Rango.DIA, datos, ahora, zona).total

// ponytail: el día, la hora y el mes caen donde empezó la actividad (una carrera que cruza la hora cuenta entera en la primera); si dos
// fuentes difieren, gana la mayor de cada hora o día, no la suma, para no contar dos veces lo que el reloj y la app vieron a la vez. Con
// horas y días repartidos entre fuentes, el total de hoy puede diferir unas kcal del punto de hoy en semana/mes.
fun serieKcal(rango: Rango, datos: DatosKcal, ahora: Instant, zona: ZoneId, locale: Locale = Locale.getDefault()): SerieKcal {
    val ahoraLocal = ahora.atZone(zona)
    val hoy = ahoraLocal.toLocalDate()
    val app = datos.app.groupBy { fechaDe(it.instante, zona).toEpochDay() }.mapValues { (_, l) -> l.sumOf { it.kcal } }
    val dias = (datos.relojPorDia.keys + app.keys).associateWith { maxOf(datos.relojPorDia[it] ?: 0, app[it] ?: 0) }
    fun dia(d: LocalDate) = dias[d.toEpochDay()] ?: 0
    fun suma(desde: LocalDate, hasta: LocalDate) = generateSequence(desde) { it.plusDays(1) }.takeWhile { !it.isAfter(hasta) }.sumOf(::dia)

    return when (rango) {
        Rango.DIA -> {
            val horasApp = IntArray(24)
            datos.app.forEach { g ->
                val momento = Instant.ofEpochMilli(g.instante).atZone(zona)
                if (momento.toLocalDate() == hoy) horasApp[momento.hour] += g.kcal
            }
            val valores = (0..ahoraLocal.hour).map { maxOf(datos.relojPorHora.getOrElse(it) { 0 }, horasApp[it]) }
            val total = valores.sum()
            SerieKcal(valores, valores.indices.map { String.format(Locale.US, "%02d:00", it) }, total, total - dia(hoy.minusDays(1)))
        }
        Rango.SEMANA -> porDias(7, hoy, ::dia, ::suma) { it.dayOfWeek.getDisplayName(TextStyle.SHORT, locale) + " " + it.dayOfMonth }
        Rango.MES -> porDias(30, hoy, ::dia, ::suma) { it.dayOfMonth.toString() + " " + it.month.getDisplayName(TextStyle.SHORT, locale) }
        Rango.ANIO, Rango.TODO -> {
            val porMes = dias.entries.groupBy({ YearMonth.from(LocalDate.ofEpochDay(it.key)) }, { it.value }).mapValues { (_, l) -> l.sum() }
            val ultimo = YearMonth.from(hoy)
            val primero = if (rango == Rango.ANIO) ultimo.minusMonths(11) else porMes.filterValues { it > 0 }.keys.minOrNull() ?: ultimo.plusMonths(1)
            val meses = generateSequence(primero) { it.plusMonths(1) }.takeWhile { !it.isAfter(ultimo) }.toList()
            val valores = meses.map { porMes[it] ?: 0 }
            SerieKcal(valores, meses.map { it.month.getDisplayName(TextStyle.SHORT, locale) + " " + it.year }, valores.sum(), null)
        }
    }
}

// Los últimos `cantidad` días hasta hoy, uno por cubeta; la variación es contra los `cantidad` días de antes.
private fun porDias(
    cantidad: Int,
    hoy: LocalDate,
    dia: (LocalDate) -> Int,
    suma: (LocalDate, LocalDate) -> Int,
    rotulo: (LocalDate) -> String,
): SerieKcal {
    val primero = hoy.minusDays(cantidad - 1L)
    val dias = (0 until cantidad).map { primero.plusDays(it.toLong()) }
    val total = dias.sumOf(dia)
    return SerieKcal(dias.map(dia), dias.map(rotulo), total, total - suma(primero.minusDays(cantidad.toLong()), primero.minusDays(1)))
}

private fun fechaDe(instante: Long, zona: ZoneId): LocalDate = Instant.ofEpochMilli(instante).atZone(zona).toLocalDate()

// El número de una raya del eje: los miles se abrevian desde 10 000 (12,5k) para que quepan en el margen de la gráfica.
internal fun etiquetaEje(valor: Int): String = when {
    valor < 10_000 -> valor.toString()
    valor % 1000 == 0 -> "${valor / 1000}k"
    else -> "${valor / 1000}.${valor % 1000 / 100}k"
}

// Las rayas de la gráfica de arriba abajo: de 0 al máximo, en 5 tramos iguales de un paso «redondo» (1, 2, 2,5, 3, 4, 5, 6 u 8 por
// una potencia de 10), para que la escala siga a los datos en vez de ser fija.
internal fun ejeKcal(maximo: Int): List<Int> {
    val minimo = maximo / 5.0
    var base = 10
    while (true) {
        for (m in listOf(1.0, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0)) {
            val paso = (m * base).toInt()
            if (paso >= minimo) return (5 downTo 0).map { it * paso }
        }
        base *= 10
    }
}
