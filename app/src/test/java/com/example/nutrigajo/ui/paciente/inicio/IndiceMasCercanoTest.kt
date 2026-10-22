package com.example.nutrigajo.ui.paciente.inicio

import org.junit.Assert.assertEquals
import org.junit.Test

class IndiceMasCercanoTest {
    @Test
    fun elige_el_punto_mas_cercano_y_respeta_los_extremos() {
        // 5 puntos en 400 px: están en 0, 100, 200, 300 y 400
        assertEquals(0, indiceMasCercano(-30f, 400f, 5))
        assertEquals(1, indiceMasCercano(140f, 400f, 5))
        assertEquals(2, indiceMasCercano(160f, 400f, 5))
        assertEquals(4, indiceMasCercano(999f, 400f, 5))
    }

    @Test
    fun con_un_solo_punto_o_sin_ancho_devuelve_cero() {
        assertEquals(0, indiceMasCercano(50f, 400f, 1))
        assertEquals(0, indiceMasCercano(50f, 0f, 5))
    }
}
