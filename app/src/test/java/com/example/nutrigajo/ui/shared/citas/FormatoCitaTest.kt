package com.example.nutrigajo.ui.shared.citas

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

class FormatoCitaTest {
    @Test
    fun el_formato_corto_de_la_cita_cabe_en_una_linea() {
        // la fila «Próxima cita» del inicio mostraba «Viernes 6 de noviembre · 1…»: la hora quedaba cortada
        val zona = ZoneId.of("America/Mexico_City")
        val anterior = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(zona))
        try {
            val inicio = LocalDateTime.of(2026, 11, 6, 10, 0).atZone(zona).toInstant().toEpochMilli()
            // el teléfono (ICU) escribe «nov.» y la JVM «nov»: se compara lo demás
            assertEquals("Vie 6 nov · 10:00", diaYHoraCorto(inicio).replace("nov.", "nov"))
        } finally {
            TimeZone.setDefault(anterior)
        }
    }
}
