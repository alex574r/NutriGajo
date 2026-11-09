package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Qué le pasa a cada cita cuando se cierra el vínculo entre el paciente y su nutriólogo.
class CitasAlCerrarTest {
    private val ahora = 1_000_000_000L
    private val hora = 3_600_000L

    private fun cita(
        id: String,
        estado: EstadoCita,
        inicio: Long = ahora + hora,
        nutriologo: String = "n1",
        usuario: String = "u1",
    ) = Cita(id, nutriologo, "Kelly", usuario, "Ana", inicio, estado)

    @Test
    fun el_paciente_cancela_la_pendiente_y_la_confirmada() {
        assertEquals(EstadoCita.CANCELADA, estadoAlCerrar(cita("a", EstadoCita.PENDIENTE), Rol.PACIENTE, ahora))
        assertEquals(EstadoCita.CANCELADA, estadoAlCerrar(cita("b", EstadoCita.CONFIRMADA), Rol.PACIENTE, ahora))
    }

    @Test
    fun el_nutriologo_rechaza_la_pendiente_y_cancela_la_confirmada() {
        assertEquals(EstadoCita.RECHAZADA, estadoAlCerrar(cita("a", EstadoCita.PENDIENTE), Rol.NUTRIOLOGO, ahora))
        assertEquals(EstadoCita.CANCELADA, estadoAlCerrar(cita("b", EstadoCita.CONFIRMADA), Rol.NUTRIOLOGO, ahora))
    }

    @Test
    fun una_cita_pasada_o_ya_cerrada_no_se_toca() {
        for (rol in listOf(Rol.PACIENTE, Rol.NUTRIOLOGO)) {
            assertNull(estadoAlCerrar(cita("p", EstadoCita.CONFIRMADA, inicio = ahora - hora), rol, ahora))
            assertNull(estadoAlCerrar(cita("p", EstadoCita.CONFIRMADA, inicio = ahora), rol, ahora))
            assertNull(estadoAlCerrar(cita("r", EstadoCita.RECHAZADA), rol, ahora))
            assertNull(estadoAlCerrar(cita("c", EstadoCita.CANCELADA), rol, ahora))
        }
    }

    @Test
    fun solo_el_paciente_y_el_nutriologo_cierran() {
        assertNull(estadoAlCerrar(cita("a", EstadoCita.PENDIENTE), Rol.ADMIN, ahora))
        assertNull(estadoAlCerrar(cita("a", EstadoCita.PENDIENTE), Rol.ENTRENADOR, ahora))
    }

    @Test
    fun citasVigentesEntre_deja_solo_las_vigentes_de_ese_par() {
        val citas = listOf(
            cita("de-ellos", EstadoCita.CONFIRMADA),
            cita("pendiente", EstadoCita.PENDIENTE, inicio = ahora + 2 * hora),
            cita("otro-nutriologo", EstadoCita.CONFIRMADA, nutriologo = "n2"),
            cita("otro-paciente", EstadoCita.CONFIRMADA, usuario = "u2"),
            cita("pasada", EstadoCita.CONFIRMADA, inicio = ahora - hora),
            cita("cancelada", EstadoCita.CANCELADA),
        )
        assertEquals(listOf("de-ellos", "pendiente"), citasVigentesEntre(citas, "n1", "u1", ahora).map { it.id })
    }
}
