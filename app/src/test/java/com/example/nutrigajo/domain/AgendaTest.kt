package com.example.nutrigajo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AgendaTest {
    private val zona = ZoneId.of("America/Mexico_City")
    private val hoy = LocalDate.of(2026, 11, 4) // miércoles
    private fun a(dia: LocalDate, hora: Int, minuto: Int = 0) = LocalDateTime.of(dia, java.time.LocalTime.of(hora, minuto)).atZone(zona).toInstant()

    @Test
    fun hoy_solo_ofrece_las_horas_que_faltan_y_no_las_ocupadas() {
        val ocupada = a(hoy, 11).toEpochMilli()
        val horarios = Agenda.horarios(hoy, setOf(ocupada), a(hoy, 10, 30), zona)
        assertEquals(9, horarios.size)
        val libres = horarios.filter { it.libre }.map { java.time.Instant.ofEpochMilli(it.inicio).atZone(zona).hour }
        assertEquals((12..17).toList(), libres)
    }

    @Test
    fun los_dias_no_incluyen_domingos_ni_un_hoy_sin_horas() {
        val dias = Agenda.dias(emptySet(), a(hoy, 10), zona)
        assertEquals(hoy, dias.first())
        assertEquals(12, dias.size) // 14 días menos los dos domingos
        assertTrue(dias.none { it.dayOfWeek == DayOfWeek.SUNDAY })
        assertFalse(hoy in Agenda.dias(emptySet(), a(hoy, 18), zona))
    }
}
