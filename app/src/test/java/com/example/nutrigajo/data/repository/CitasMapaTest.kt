package com.example.nutrigajo.data.repository

import com.example.nutrigajo.domain.model.EstadoCita
import com.example.nutrigajo.domain.model.NOTA_MAX
import com.example.nutrigajo.domain.model.OrigenCita
import com.example.nutrigajo.domain.model.PacienteResumen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CitasMapaTest {
    // los ocho campos de las citas de antes de que el nutriólogo pudiera agendar
    private val deAntes: Map<String, Any?> = mapOf(
        "nutriologoId" to "n1",
        "nutriologoNombre" to "Kelly",
        "usuarioId" to "u1",
        "pacienteNombre" to "Ana",
        "inicio" to 1000L,
        "estado" to "CONFIRMADA",
        "creadaEn" to 1L,
        "actualizadaEn" to 1L,
    )
    private val paciente = PacienteResumen(id = "p1", usuarioId = "u1", nombre = "Ana", correo = "ana@prueba.test", fechaAlta = 1L)

    private fun delNutriologo(nota: String = "", ahora: Long = 5000L) =
        citaDelNutriologoAMapa("n1", "Kelly", paciente, inicio = 9000L, nota = nota, ahora = ahora)

    @Test
    fun una_cita_de_antes_sin_origen_se_lee_como_del_paciente() {
        val cita = citaDeMapa("c1", deAntes)!!
        assertEquals(OrigenCita.PACIENTE, cita.origen)
        assertNull(cita.pacienteId)
        assertEquals("", cita.nota)
        assertEquals(EstadoCita.CONFIRMADA, cita.estado)
    }

    @Test
    fun citaDeMapa_devuelve_null_con_un_campo_obligatorio_ausente_o_un_estado_desconocido() {
        assertNull(citaDeMapa("c1", deAntes - "inicio"))
        assertNull(citaDeMapa("c1", deAntes - "nutriologoId"))
        assertNull(citaDeMapa("c1", deAntes - "usuarioId"))
        assertNull(citaDeMapa("c1", deAntes + ("estado" to "ARCHIVADA")))
    }

    @Test
    fun la_cita_del_nutriologo_nace_confirmada_con_origen_y_paciente() {
        val d = delNutriologo(nota = "Seguimiento")
        assertEquals(
            setOf(
                "nutriologoId", "nutriologoNombre", "usuarioId", "pacienteNombre", "pacienteId",
                "inicio", "estado", "origen", "creadaEn", "actualizadaEn", "nota",
            ),
            d.keys,
        )
        assertEquals("CONFIRMADA", d["estado"])
        assertEquals("NUTRIOLOGO", d["origen"])
        assertEquals("u1", d["usuarioId"])
        assertEquals("p1", d["pacienteId"])
        assertEquals("Ana", d["pacienteNombre"])
        assertEquals(9000L, d["inicio"])
        assertEquals(5000L, d["creadaEn"])
        assertEquals(5000L, d["actualizadaEn"])
        // y lo que se escribe se lee de vuelta como lo que es
        val leida = citaDeMapa("c9", d)!!
        assertEquals(OrigenCita.NUTRIOLOGO, leida.origen)
        assertEquals("p1", leida.pacienteId)
        assertEquals("Seguimiento", leida.nota)
    }

    @Test
    fun la_nota_se_recorta_y_si_queda_vacia_no_se_manda() {
        assertEquals("Trae tus análisis", delNutriologo(nota = "  Trae tus análisis  ")["nota"])
        assertFalse(delNutriologo(nota = "   ").containsKey("nota"))
        assertFalse(delNutriologo(nota = "").containsKey("nota"))
    }

    private fun cita(origen: OrigenCita) = citaDeMapa("c1", deAntes + ("origen" to origen.name))!!

    @Test
    fun rechazar_o_cancelar_una_cita_del_paciente_libera_su_candado() {
        val cita = cita(OrigenCita.PACIENTE)
        assertTrue(liberaCandado(cita, EstadoCita.RECHAZADA))
        assertTrue(liberaCandado(cita, EstadoCita.CANCELADA))
        assertFalse(liberaCandado(cita, EstadoCita.CONFIRMADA))
    }

    @Test
    fun la_cita_del_nutriologo_nunca_toca_el_candado_del_paciente() {
        // el candado es de la cita que pidió el paciente: borrarlo al cancelar la del nutriólogo hace fallar el lote entero
        assertFalse(liberaCandado(cita(OrigenCita.NUTRIOLOGO), EstadoCita.CANCELADA))
    }

    @Test
    fun la_nota_se_corta_a_120_caracteres() {
        assertEquals(120, NOTA_MAX)
        assertEquals(120, (delNutriologo(nota = "a".repeat(150))["nota"] as String).length)
    }
}
