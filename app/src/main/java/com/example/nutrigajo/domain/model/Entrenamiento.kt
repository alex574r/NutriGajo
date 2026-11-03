package com.example.nutrigajo.domain.model

import java.time.Instant
import java.time.ZoneId

// Una rutina de «Entrenar ahora» terminada; vive en usuarios/{uid}/entrenamientos/{id}. `hechos` son los ejercicios que se llegaron a
// terminar de los `total` de la rutina.
data class Entrenamiento(
    val id: String = "",
    // cuándo se terminó, en milisegundos desde 1970
    val fecha: Long,
    val enfoque: EnfoqueDia,
    val minutos: Int,
    val kcal: Int,
    val hechos: Int,
    val total: Int,
) {
    // Cuenta para la racha cuando se hizo al menos la mitad de la rutina: pasar los ejercicios sin hacerlos no suma.
    val cuentaParaLaRacha: Boolean get() = total > 0 && hechos * 2 >= total

    // El día (LocalDate.toEpochDay) en que se terminó, en la zona del teléfono: así lo cuentan la racha y los widgets.
    fun dia(zona: ZoneId = ZoneId.systemDefault()): Long = Instant.ofEpochMilli(fecha).atZone(zona).toLocalDate().toEpochDay()

    companion object {
        // los topes de las reglas de Firestore (firebase/firestore.rules, entrenamientoValido)
        const val MAX_MINUTOS = 600
        const val MAX_KCAL = 10_000
        const val MAX_EJERCICIOS = 20
    }
}
