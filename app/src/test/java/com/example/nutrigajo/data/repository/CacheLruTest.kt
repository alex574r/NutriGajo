package com.example.nutrigajo.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CacheLruTest {
    // el «tamaño» de cada valor es el valor mismo, para ver el reparto del límite sin bitmaps
    private fun cache(limite: Long) = CacheLru<Int>(limite) { it.toLong() }

    @Test
    fun devuelve_lo_guardado_y_null_si_no_esta() {
        val c = cache(10)
        c["a"] = 4
        assertEquals(4, c["a"])
        assertNull(c["b"])
    }

    @Test
    fun al_pasarse_del_limite_saca_lo_que_menos_se_uso() {
        val c = cache(10)
        c["a"] = 4
        c["b"] = 4
        c["a"] // «a» se usó después que «b»
        c["c"] = 4 // 12 > 10: sale «b», no «a»
        assertEquals(4, c["a"])
        assertNull(c["b"])
        assertEquals(4, c["c"])
    }

    @Test
    fun reemplazar_una_clave_no_cuenta_dos_veces() {
        val c = cache(10)
        c["a"] = 6
        c["a"] = 2
        c["b"] = 8 // 2 + 8 = 10: caben los dos
        assertEquals(2, c["a"])
        assertEquals(8, c["b"])
    }

    @Test
    fun un_valor_mayor_que_el_limite_no_se_guarda_ni_saca_a_los_demas() {
        val c = cache(10)
        c["a"] = 4
        c["grande"] = 11
        assertNull(c["grande"])
        assertEquals(4, c["a"])
    }
}
