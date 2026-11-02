package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RelojTest {
    private val ahora = Instant.parse("2026-11-01T12:00:00Z")

    private fun hace(d: Duration) = haceCuanto(ahora.minus(d), ahora)

    @Test
    fun la_ultima_lectura_se_cuenta_en_minutos_horas_o_dias() {
        assertEquals("hace un momento", hace(Duration.ofSeconds(30)))
        assertEquals("hace 5 min", hace(Duration.ofMinutes(5)))
        assertEquals("hace 59 min", hace(Duration.ofMinutes(59)))
        assertEquals("hace 1 h", hace(Duration.ofMinutes(60)))
        assertEquals("hace 23 h", hace(Duration.ofHours(23).plusMinutes(59)))
        assertEquals("hace 3 d", hace(Duration.ofDays(3).plusHours(5)))
    }
}
