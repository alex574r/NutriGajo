package com.example.nutrigajo.ui.paciente.inicio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

class CaloriasSerieTest {
    private val zona = ZoneOffset.UTC
    private val hoy = LocalDate.of(2026, 11, 6)

    // viernes 6 de noviembre de 2026, 15:30: la serie de hoy llega hasta la hora 15
    private val ahora = Instant.parse("2026-11-06T15:30:00Z")
    private val es = Locale.forLanguageTag("es-MX")

    private fun dia(fecha: LocalDate) = fecha.toEpochDay()
    private fun app(fecha: LocalDate, hora: Int, kcal: Int) = GastoApp(fecha.atTime(hora, 0).toInstant(ZoneOffset.UTC).toEpochMilli(), kcal)

    @Test
    fun hoy_va_hora_por_hora_hasta_la_hora_en_curso() {
        val porHora = List(24) { if (it == 7) 120 else 10 }
        val s = serieKcal(Rango.DIA, DatosKcal(relojPorHora = porHora), ahora, zona, es)
        assertEquals(16, s.valores.size)
        assertEquals(120, s.valores[7])
        assertEquals("07:00", s.rotulos[7])
        assertEquals(15 * 10 + 120, s.total)
        assertEquals(s.total, kcalDeHoy(DatosKcal(relojPorHora = porHora), ahora, zona))
    }

    @Test
    fun cada_hora_toma_lo_mayor_entre_reloj_y_app_y_no_los_suma() {
        // a las 10 el reloj contó 300 y la app guardó una carrera de 280: son la misma carrera, cuenta 300
        val porHora = List(24) { if (it == 10) 300 else 0 }
        val datos = DatosKcal(relojPorHora = porHora, app = listOf(app(hoy, 10, 280), app(hoy, 18, 999), app(hoy, 12, 90)))
        val s = serieKcal(Rango.DIA, datos, ahora, zona, es)
        assertEquals(300, s.valores[10])
        // lo de la app que el reloj no vio (a las 12) sí suma; lo de las 18 aún no llega
        assertEquals(90, s.valores[12])
        assertEquals(390, s.total)
    }

    @Test
    fun sin_datos_todo_es_cero_y_el_total_tambien() {
        for (r in Rango.entries) {
            val s = serieKcal(r, DatosKcal(), ahora, zona, es)
            assertEquals(0, s.total)
            assertEquals(s.valores.size, s.rotulos.size)
        }
        assertEquals(emptyList<Int>(), serieKcal(Rango.TODO, DatosKcal(), ahora, zona, es).valores)
    }

    @Test
    fun el_dia_compara_con_el_total_de_ayer() {
        val datos = DatosKcal(relojPorDia = mapOf(dia(hoy.minusDays(1)) to 500), relojPorHora = List(24) { if (it == 8) 200 else 0 })
        assertEquals(200 - 500, serieKcal(Rango.DIA, datos, ahora, zona, es).variacion)
    }

    @Test
    fun la_semana_son_siete_dias_que_terminan_hoy_y_se_compara_con_los_siete_de_antes() {
        val porDia = buildMap {
            for (i in 0L..6L) put(dia(hoy.minusDays(i)), 100 + i.toInt()) // hoy 100, ayer 101 ... hace 6 días 106
            for (i in 7L..13L) put(dia(hoy.minusDays(i)), 50)
            put(dia(hoy.minusDays(14)), 9999) // fuera de las dos semanas
        }
        val s = serieKcal(Rango.SEMANA, DatosKcal(relojPorDia = porDia), ahora, zona, es)
        assertEquals(7, s.valores.size)
        // el más viejo primero y hoy al final
        assertEquals(listOf(106, 105, 104, 103, 102, 101, 100), s.valores)
        assertEquals(721, s.total)
        assertEquals(721 - 7 * 50, s.variacion)
        // el rótulo de hoy termina en el número del día (el nombre abreviado cambia según la versión de Java)
        assertTrue(s.rotulos.last().endsWith(" 6"))
    }

    @Test
    fun un_dia_toma_lo_mayor_entre_el_reloj_y_la_suma_de_la_app() {
        val datos = DatosKcal(
            relojPorDia = mapOf(dia(hoy.minusDays(1)) to 400, dia(hoy.minusDays(2)) to 100),
            app = listOf(app(hoy.minusDays(1), 9, 150), app(hoy.minusDays(1), 17, 100), app(hoy.minusDays(2), 9, 250)),
        )
        val v = serieKcal(Rango.SEMANA, datos, ahora, zona, es).valores
        // ayer: reloj 400 contra app 250 -> 400; antier: reloj 100 contra app 250 -> 250
        assertEquals(400, v[5])
        assertEquals(250, v[4])
    }

    @Test
    fun el_mes_son_treinta_dias_y_se_compara_con_los_treinta_de_antes() {
        val porDia = buildMap {
            for (i in 0L..29L) put(dia(hoy.minusDays(i)), 10)
            for (i in 30L..59L) put(dia(hoy.minusDays(i)), 4)
        }
        val s = serieKcal(Rango.MES, DatosKcal(relojPorDia = porDia), ahora, zona, es)
        assertEquals(30, s.valores.size)
        assertEquals(300, s.total)
        assertEquals(300 - 120, s.variacion)
    }

    @Test
    fun el_anio_son_doce_meses_que_terminan_en_el_actual_y_no_compara() {
        val porDia = mapOf(
            dia(LocalDate.of(2026, 11, 2)) to 100,
            dia(LocalDate.of(2026, 11, 5)) to 50,
            dia(LocalDate.of(2026, 3, 10)) to 70,
            // hace más de doce meses: no entra
            dia(LocalDate.of(2025, 10, 31)) to 1000,
        )
        val s = serieKcal(Rango.ANIO, DatosKcal(relojPorDia = porDia), ahora, zona, es)
        assertEquals(12, s.valores.size)
        assertEquals(150, s.valores.last())
        assertEquals(70, s.valores[s.valores.size - 9]) // marzo, ocho meses antes de noviembre
        assertEquals(220, s.total)
        assertNull(s.variacion)
    }

    @Test
    fun todo_empieza_en_el_primer_mes_con_datos() {
        val datos = DatosKcal(
            relojPorDia = mapOf(dia(LocalDate.of(2026, 9, 12)) to 80),
            app = listOf(app(LocalDate.of(2026, 10, 20), 8, 40)),
        )
        val s = serieKcal(Rango.TODO, datos, ahora, zona, es)
        assertEquals(listOf(80, 40, 0), s.valores)
        assertEquals(120, s.total)
        assertNull(s.variacion)
    }

    @Test
    fun la_hora_de_la_app_se_cuenta_en_la_zona_del_telefono() {
        // 03:00 UTC del 6 son las 21:00 del 5 en México (UTC-6): es una actividad de ayer
        val mexico = ZoneId.of("America/Mexico_City")
        val ahoraMx = Instant.parse("2026-11-06T18:00:00Z") // 12:00 del 6
        val datos = DatosKcal(app = listOf(GastoApp(Instant.parse("2026-11-06T03:00:00Z").toEpochMilli(), 200)))
        val s = serieKcal(Rango.SEMANA, datos, ahoraMx, mexico, es)
        assertEquals(200, s.valores[5])
        assertEquals(0, s.valores[6])
    }

    @Test
    fun el_eje_sigue_a_los_datos_en_cinco_tramos_redondos() {
        assertEquals(listOf(50, 40, 30, 20, 10, 0), ejeKcal(0))
        assertEquals(listOf(250, 200, 150, 100, 50, 0), ejeKcal(250))
        assertEquals(listOf(1000, 800, 600, 400, 200, 0), ejeKcal(900))
        assertEquals(listOf(2000, 1600, 1200, 800, 400, 0), ejeKcal(1503))
        assertEquals(listOf(25000, 20000, 15000, 10000, 5000, 0), ejeKcal(24000))
    }

    @Test
    fun las_rayas_grandes_se_abrevian_para_caber_en_el_margen() {
        assertEquals("900", etiquetaEje(900))
        assertEquals("9999", etiquetaEje(9999))
        assertEquals("10k", etiquetaEje(10_000))
        assertEquals("12.5k", etiquetaEje(12_500))
        assertEquals("25k", etiquetaEje(25_000))
    }

    @Test
    fun el_eje_siempre_alcanza_el_maximo() {
        for (max in listOf(1, 7, 49, 51, 99, 501, 1999, 2001, 12345, 99999)) {
            val eje = ejeKcal(max)
            assertEquals(6, eje.size)
            assertTrue("el eje $eje no llega a $max", eje.first() >= max)
        }
    }
}
