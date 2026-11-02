package com.example.nutrigajo.domain.model

import java.time.Duration
import java.time.Instant

// Lo que el reloj manda a Health Connect y la app muestra: el pulso del día o de la semana en barras, la última lectura y los
// entrenamientos recientes.

enum class PeriodoPulso { HOY, SEMANA }

// Una barra de la gráfica del pulso: `etiqueta` va en el eje (puede estar vacía), `detalle` sale al tocarla y `bpm` es null si en
// ese rato el reloj no midió nada.
data class BarraPulso(val etiqueta: String, val detalle: String, val bpm: Int?)

// Un entrenamiento del reloj, con el pulso medio y el máximo de ese rato si hubo lecturas. `inicio` en milisegundos desde 1970.
data class SesionReloj(val titulo: String, val inicio: Long, val minutos: Int, val promedio: Int?, val maximo: Int?)

data class ResumenReloj(
    val ultimoBpm: Int?,
    val ultimaLectura: Instant?,
    // la app que sincronizó los datos (Mi Fitness, Samsung Health…)
    val origen: String?,
    val sesiones: List<SesionReloj>,
)

// «hace 5 min», «hace 2 h», «hace 3 d»
// ponytail: texto en español dentro del código, como el resto de formatos de fecha; con más idiomas pasa a strings.xml
fun haceCuanto(desde: Instant, ahora: Instant): String {
    val minutos = Duration.between(desde, ahora).toMinutes()
    return when {
        minutos < 1 -> "hace un momento"
        minutos < 60 -> "hace $minutos min"
        minutos < 24 * 60 -> "hace ${minutos / 60} h"
        else -> "hace ${minutos / (24 * 60)} d"
    }
}
