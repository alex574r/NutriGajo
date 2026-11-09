package com.example.nutrigajo.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Una hora de consulta: `libre` es que todavía no llega y nadie la ha pedido.
data class Horario(val inicio: Long, val libre: Boolean)

// Horario fijo de consulta: de lunes a sábado, citas de una hora que empiezan entre las 9:00 y las 17:00, en los próximos 14 días para el
// paciente que pide cita y 30 para el nutriólogo que agenda (seguimientos a tres semanas).
// ponytail: el nutriólogo aún no configura su disponibilidad (ni su zona horaria: se usa la del teléfono del paciente);
// cuando pueda, estos valores salen de su ficha. Las reglas de Firestore solo exigen que `inicio` caiga en cuartos de hora.
object Agenda {
    const val DIAS_PACIENTE = 14L
    const val DIAS_NUTRIOLOGO = 30L
    private val HORAS = 9..17

    fun horarios(dia: LocalDate, ocupados: Set<Long>, ahora: Instant, zona: ZoneId): List<Horario> =
        HORAS.map { hora ->
            val inicio = dia.atTime(hora, 0).atZone(zona).toInstant().toEpochMilli()
            Horario(inicio, inicio > ahora.toEpochMilli() && inicio !in ocupados)
        }

    // Días en los que aún queda alguna hora libre.
    fun dias(ocupados: Set<Long>, ahora: Instant, zona: ZoneId, ventana: Long = DIAS_PACIENTE): List<LocalDate> {
        val hoy = ahora.atZone(zona).toLocalDate()
        return (0 until ventana).map { hoy.plusDays(it) }
            .filter { it.dayOfWeek != DayOfWeek.SUNDAY && horarios(it, ocupados, ahora, zona).any(Horario::libre) }
    }
}
