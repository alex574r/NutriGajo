package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CitaTest {
    private val ahora = 1_000_000_000L
    private val hora = 3_600_000L

    private fun cita(id: String, inicio: Long, estado: EstadoCita) = Cita(id, "n1", "Kelly", "u1", "Ana", inicio, estado)

    @Test
    fun proxima_es_la_vigente_mas_cercana_y_ignora_canceladas_rechazadas_y_pasadas() {
        val citas = listOf(
            cita("pasada", ahora - hora, EstadoCita.CONFIRMADA),
            cita("cancelada", ahora + hora / 2, EstadoCita.CANCELADA),
            cita("rechazada", ahora + hora / 2, EstadoCita.RECHAZADA),
            cita("pendiente", ahora + 2 * hora, EstadoCita.PENDIENTE),
            cita("confirmada", ahora + hora, EstadoCita.CONFIRMADA),
        )
        assertEquals("confirmada", citas.proxima(ahora)?.id)
        assertNull(emptyList<Cita>().proxima(ahora))
        assertNull(citas.filter { it.id in setOf("pasada", "cancelada", "rechazada") }.proxima(ahora))
    }
}
