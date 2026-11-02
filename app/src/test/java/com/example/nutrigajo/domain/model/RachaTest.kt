package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RachaTest {
    private val hoy = LocalDate.of(2026, 11, 2)
    private fun dia(atras: Long) = hoy.minusDays(atras).toEpochDay()

    @Test
    fun sin_nada_no_hay_racha() {
        assertEquals(Racha(0, false), racha(hoy, emptyMap(), emptySet(), 8000))
    }

    @Test
    fun los_dias_seguidos_con_la_meta_de_pasos_suman() {
        val pasos = mapOf(dia(0) to 8200, dia(1) to 9000, dia(2) to 8000, dia(3) to 3000, dia(4) to 12000)
        // el de hace 3 días no llegó: ahí se corta
        assertEquals(Racha(3, true), racha(hoy, pasos, emptySet(), 8000))
    }

    @Test
    fun hoy_aun_sin_cumplir_la_racha_sigue_viva_con_lo_de_ayer() {
        val pasos = mapOf(dia(0) to 1200, dia(1) to 9000, dia(2) to 8500)
        assertEquals(Racha(2, false), racha(hoy, pasos, emptySet(), 8000))
    }

    @Test
    fun si_ayer_tampoco_se_cumplio_la_racha_se_rompe() {
        val pasos = mapOf(dia(0) to 0, dia(1) to 100, dia(2) to 9000)
        assertEquals(Racha(0, false), racha(hoy, pasos, emptySet(), 8000))
    }

    @Test
    fun una_caminata_guardada_cuenta_aunque_no_haya_reloj() {
        assertEquals(Racha(2, true), racha(hoy, emptyMap(), setOf(dia(0), dia(1), dia(3)), 8000))
    }

    @Test
    fun un_dia_se_cumple_con_pasos_o_con_caminata_y_se_mezclan() {
        val pasos = mapOf(dia(0) to 9000, dia(2) to 8000)
        // el de ayer lo salva una carrera
        assertEquals(Racha(3, true), racha(hoy, pasos, setOf(dia(1)), 8000))
    }

    @Test
    fun con_una_meta_de_cero_pasos_los_pasos_no_cuentan_y_no_se_cicla() {
        assertEquals(Racha(0, false), racha(hoy, mapOf(dia(0) to 5000), emptySet(), 0))
    }
}
