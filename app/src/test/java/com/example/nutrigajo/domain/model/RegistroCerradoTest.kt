package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// La lista de vínculos anteriores: del cierre más reciente al más viejo, y cómo se lee quién cerró cada uno.
class RegistroCerradoTest {
    private fun registro(id: String, cerradoEn: Long, por: CerradoPor? = CerradoPor.PACIENTE) = RegistroCerrado(id, "Kelly", cerradoEn, por)

    @Test
    fun los_mas_recientes_van_primero_y_los_sin_fecha_al_final() {
        val lista = listOf(registro("viejo", 100), registro("sin-fecha", 0), registro("nuevo", 900), registro("medio", 500))
        assertEquals(listOf("nuevo", "medio", "viejo", "sin-fecha"), lista.masRecientesPrimero().map { it.pacienteId })
    }

    @Test
    fun cerradoPor_se_lee_del_texto_y_lo_desconocido_es_null() {
        assertEquals(CerradoPor.PACIENTE, cerradoPorDeTexto("PACIENTE"))
        assertEquals(CerradoPor.NUTRIOLOGO, cerradoPorDeTexto("NUTRIOLOGO"))
        assertEquals(CerradoPor.CUENTA_ELIMINADA, cerradoPorDeTexto("CUENTA_ELIMINADA"))
        assertNull(cerradoPorDeTexto("OTRO"))
        assertNull(cerradoPorDeTexto(null))
    }

    @Test
    fun solo_el_cierre_por_cuenta_eliminada_lo_dice() {
        assertEquals(true, registro("a", 1, CerradoPor.CUENTA_ELIMINADA).porCuentaEliminada)
        assertEquals(false, registro("b", 1, CerradoPor.NUTRIOLOGO).porCuentaEliminada)
        assertEquals(false, registro("c", 1, null).porCuentaEliminada)
    }
}
