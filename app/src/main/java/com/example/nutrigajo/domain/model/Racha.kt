package com.example.nutrigajo.domain.model

import java.time.LocalDate

// La racha: los días seguidos en que se cumplió algo, sea llegar a la meta de pasos que mide el reloj, guardar una caminata o una
// carrera en el Modo carrera o terminar (al menos la mitad de) una rutina de «Entrenar ahora». `hoyCumplido` dice si hoy ya cuenta; mientras no, la racha sigue viva con lo de ayer (el día aún
// no termina) y se rompe solo si ayer tampoco se cumplió.
data class Racha(val dias: Int, val hoyCumplido: Boolean)

// `pasosPorDia` y `diasCarrera` (los días con actividad guardada: caminatas, carreras y rutinas) van por día (LocalDate.toEpochDay). Con una meta de 0 pasos los pasos no cuentan: si no, todos los
// días serían de racha.
fun racha(hoy: LocalDate, pasosPorDia: Map<Long, Int>, diasCarrera: Set<Long>, metaPasos: Int): Racha {
    fun cumplido(dia: Long) = (metaPasos > 0 && (pasosPorDia[dia] ?: 0) >= metaPasos) || dia in diasCarrera
    val hoyDia = hoy.toEpochDay()
    val cumpleHoy = cumplido(hoyDia)
    var dia = if (cumpleHoy) hoyDia else hoyDia - 1
    var dias = 0
    // el tope es solo por si algún día la regla de arriba deja de acabarse
    while (dias < MAX_RACHA && cumplido(dia)) {
        dias++
        dia--
    }
    return Racha(dias, cumpleHoy)
}

private const val MAX_RACHA = 3650
