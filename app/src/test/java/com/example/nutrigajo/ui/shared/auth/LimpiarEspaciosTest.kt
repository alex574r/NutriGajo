package com.example.nutrigajo.ui.shared.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class LimpiarEspaciosTest {
    @Test
    fun quita_los_espacios_de_los_extremos_y_junta_los_de_en_medio() {
        assertEquals("Ana María", limpiarEspacios("  Ana   María "))
        assertEquals("Hernández Maya", limpiarEspacios("Hernández\tMaya"))
    }
}
