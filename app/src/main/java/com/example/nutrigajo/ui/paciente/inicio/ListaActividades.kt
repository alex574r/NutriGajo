package com.example.nutrigajo.ui.paciente.inicio

import com.example.nutrigajo.domain.model.Carrera
import com.example.nutrigajo.domain.model.EnfoqueDia
import com.example.nutrigajo.domain.model.Entrenamiento
import com.example.nutrigajo.domain.model.TipoActividad
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Lo que lista «Ver todas» de Actividades: lo que la app guardó y suma a las calorías (lógica pura, sin Android).

enum class ClaseActividad { CAMINATA, CARRERA, RUTINA }

// Una fila. `instante` es cuándo empezó (caminata o carrera) o terminó (rutina), en ms desde 1970; `distanciaM` es 0 en las rutinas y
// `enfoque` solo lo tienen ellas.
data class ActividadFila(
    val clase: ClaseActividad,
    val instante: Long,
    val minutos: Int,
    val kcal: Int,
    val distanciaM: Int = 0,
    val enfoque: EnfoqueDia? = null,
)

fun Carrera.aFila() = ActividadFila(
    clase = if (tipo == TipoActividad.CORRER) ClaseActividad.CARRERA else ClaseActividad.CAMINATA,
    instante = inicio,
    // redondeado al minuto más cercano
    minutos = (duracionSeg + 30) / 60,
    kcal = kcal,
    distanciaM = distanciaM,
)

fun Entrenamiento.aFila() = ActividadFila(ClaseActividad.RUTINA, instante = fecha, minutos = minutos, kcal = kcal, enfoque = enfoque)

// Los días con actividad, el más reciente primero, y dentro de cada día lo más reciente primero; el día se cuenta en la zona del teléfono.
fun actividadesPorDia(filas: List<ActividadFila>, zona: ZoneId): List<Pair<LocalDate, List<ActividadFila>>> =
    filas.sortedByDescending { it.instante }
        .groupBy { Instant.ofEpochMilli(it.instante).atZone(zona).toLocalDate() }
        .toList()
