package com.example.nutrigajo.domain.model

import java.time.LocalDate
import kotlin.math.roundToInt

// El score del día (0 a 100): el promedio de lo cumplido de cada meta de hoy, cada una con tope de 100 %. Cada logro es un par
// (lo que llevas, la meta); una meta de 0 no cuenta y sin ninguna el score es 0.
fun scoreDelDia(logros: List<Pair<Int, Int>>): Int {
    val validos = logros.filter { it.second > 0 }
    if (validos.isEmpty()) return 0
    val suma = validos.sumOf { (hecho, meta) -> (hecho.toDouble() / meta).coerceIn(0.0, 1.0) }
    return (suma / validos.size * 100).roundToInt()
}

const val DIAS_AVANCE = 7

// El avance de la semana (0 a 100): de los últimos 7 días, hoy incluido, en cuántos se cumplió algo (los mismos que cuentan para la
// racha), en porcentaje.
fun avanceSemanal(hoy: LocalDate, pasosPorDia: Map<Long, Int>, diasCarrera: Set<Long>, metaPasos: Int): Int {
    val hoyDia = hoy.toEpochDay()
    val cumplidos = (0 until DIAS_AVANCE).count { diaCumplido(hoyDia - it, pasosPorDia, diasCarrera, metaPasos) }
    return (cumplidos * 100.0 / DIAS_AVANCE).roundToInt()
}
