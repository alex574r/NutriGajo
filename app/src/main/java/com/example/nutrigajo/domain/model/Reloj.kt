package com.example.nutrigajo.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

// Lo que el reloj manda a Health Connect y la app muestra, un apartado por métrica: el pulso, la actividad del día, el sueño y el
// oxígeno en sangre, más los entrenamientos recientes.

enum class PeriodoPulso { HOY, SEMANA }

// Una barra de una gráfica: `etiqueta` va en el eje (puede estar vacía), `detalle` sale al tocarla y `valor` es null si en ese rato
// el reloj no midió nada.
data class Barra(val etiqueta: String, val detalle: String, val valor: Int?)

// El pulso de un periodo: sus barras (por hora o por día) y el promedio, el mínimo y el máximo de todo el periodo.
data class PulsoPeriodo(val barras: List<Barra>, val promedio: Int?, val minimo: Int?, val maximo: Int?)

// Un entrenamiento del reloj, con el pulso medio y el máximo de ese rato si hubo lecturas. `inicio` en milisegundos desde 1970.
data class SesionReloj(val titulo: String, val inicio: Long, val minutos: Int, val promedio: Int?, val maximo: Int?)

// Lo que lleva el día: pasos, kilómetros, calorías activas (las que gasta el cuerpo moviéndose) y minutos de entrenamiento; los pasos de
// ayer y los de hoy hora por hora (24 valores) para la gráfica.
data class ActividadHoy(
    val pasos: Int,
    val km: Float,
    val kcal: Int,
    val minutos: Int,
    val pasosAyer: Int? = null,
    val pasosPorHora: List<Int> = emptyList(),
)

// Lo dormido en una noche, contada en el día en que despertó, y cuánto fue de cada etapa (en minutos; despierto no cuenta en `minutos`).
data class NocheSueno(
    val fecha: LocalDate,
    val minutos: Int,
    val profundo: Int = 0,
    val ligero: Int = 0,
    val rem: Int = 0,
    val despierto: Int = 0,
)

// La última lectura de oxígeno en sangre (SpO2), el promedio y el mínimo de las últimas 24 horas, en porcentaje, y el promedio de
// cada hora (24 valores, null donde no hubo lectura).
data class Oxigeno(
    val porcentaje: Int,
    val promedio: Int,
    val minimo: Int,
    val lectura: Instant,
    val porHora: List<Int?> = emptyList(),
)

data class ResumenReloj(
    val ultimoBpm: Int?,
    val ultimaLectura: Instant?,
    // la app que sincronizó los datos (Mi Fitness, Samsung Health…)
    val origen: String?,
    val sesiones: List<SesionReloj>,
    // null si no se dio el permiso de esa métrica o el reloj no mandó nada
    val actividad: ActividadHoy? = null,
    // de la más reciente a la más vieja, hasta 7 noches
    val sueno: List<NocheSueno> = emptyList(),
    val oxigeno: Oxigeno? = null,
    // faltan permisos de métricas que la app sabe leer: se ofrece pedirlos
    val faltanPermisos: Boolean = false,
    // los pasos de cada uno de los últimos 60 días (toEpochDay), para la racha
    val pasosPorDia: Map<Long, Int> = emptyMap(),
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
