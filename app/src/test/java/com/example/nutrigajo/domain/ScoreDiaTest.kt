package com.example.nutrigajo.domain

import com.example.nutrigajo.domain.model.avanceSemanal
import com.example.nutrigajo.domain.model.scoreDelDia
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ScoreDiaTest {
    @Test
    fun el_score_es_el_promedio_de_lo_cumplido_con_tope_por_meta() {
        // agua a la mitad, movimiento hecho y pasos pasados de la meta (cuentan 100 %, no 150 %): (50 + 100 + 100) / 3
        assertEquals(83, scoreDelDia(listOf(1000 to 2000, 1 to 1, 12000 to 8000)))
        assertEquals(0, scoreDelDia(listOf(0 to 2000, 0 to 1)))
        assertEquals(100, scoreDelDia(listOf(2000 to 2000, 1 to 1)))
    }

    @Test
    fun una_meta_en_cero_no_cuenta_y_sin_metas_el_score_es_cero() {
        assertEquals(50, scoreDelDia(listOf(1000 to 2000, 500 to 0)))
        assertEquals(0, scoreDelDia(emptyList()))
    }

    @Test
    fun el_avance_cuenta_los_dias_cumplidos_de_los_ultimos_siete() {
        val hoy = LocalDate.of(2026, 11, 2)
        val dia = hoy.toEpochDay()
        val pasos = mapOf(dia to 9000, dia - 1 to 3000, dia - 2 to 8000) // hoy y antier llegan a 8000
        val carreras = setOf(dia - 4, dia - 10) // una caminata dentro de la semana y otra fuera
        assertEquals(43, avanceSemanal(hoy, pasos, carreras, 8000)) // 3 de 7
        assertEquals(0, avanceSemanal(hoy, emptyMap(), emptySet(), 8000))
        assertEquals(100, avanceSemanal(hoy, emptyMap(), (0L..6L).map { dia - it }.toSet(), 8000))
    }

    @Test
    fun con_meta_de_pasos_en_cero_solo_cuentan_las_actividades_guardadas() {
        val hoy = LocalDate.of(2026, 11, 2)
        assertEquals(14, avanceSemanal(hoy, mapOf(hoy.toEpochDay() to 20000), setOf(hoy.toEpochDay() - 1), 0)) // 1 de 7
    }
}
