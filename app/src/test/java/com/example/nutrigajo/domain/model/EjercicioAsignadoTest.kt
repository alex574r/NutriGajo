package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EjercicioAsignadoTest {
    private fun asignado(dia: Int = 1, id: String = "e1", titulo: String = "Sentadilla", series: Int? = 3, repeticiones: Int? = 10, minutos: Int? = null, nota: String = "") =
        EjercicioAsignado(dia = dia, id = id, titulo = titulo, series = series, repeticiones = repeticiones, minutos = minutos, nota = nota)

    @Test
    fun un_asignado_completo_es_valido() {
        assertTrue(asignado().valido())
        assertTrue(asignado(series = null, repeticiones = null, minutos = 15).valido())
        assertTrue("el día 7 es el domingo", asignado(dia = 7).valido())
    }

    @Test
    fun cada_campo_fuera_de_rango_lo_invalida() {
        assertFalse(asignado(dia = 0).valido())
        assertFalse(asignado(dia = 8).valido())
        assertFalse(asignado(id = " ").valido())
        assertFalse(asignado(titulo = "").valido())
        assertFalse(asignado(series = 0).valido())
        assertFalse(asignado(series = EjercicioAsignado.MAX_SERIES + 1).valido())
        assertFalse(asignado(repeticiones = EjercicioAsignado.MAX_REPETICIONES + 1).valido())
        assertFalse(asignado(series = null, repeticiones = null, minutos = 0).valido())
        assertFalse(asignado(series = null, repeticiones = null, minutos = EjercicioAsignado.MAX_MINUTOS + 1).valido())
        assertFalse(asignado(nota = "x".repeat(EjercicioAsignado.MAX_NOTA + 1)).valido())
    }

    @Test
    fun la_duracion_sale_de_los_minutos_o_de_las_series() {
        assertEquals(15, asignado(minutos = 15).duracionMin())
        assertEquals(6, asignado(series = 3, minutos = null).duracionMin())
        assertEquals(2, asignado(series = null, repeticiones = null, minutos = null).duracionMin())
        assertEquals(2, asignado(series = 1, minutos = null).duracionMin())
    }

    @Test
    fun la_dosis_se_lee_como_series_por_repeticiones_o_minutos() {
        assertEquals("3 × 10", asignado(series = 3, repeticiones = 10).dosis())
        assertEquals("3 series", asignado(series = 3, repeticiones = null).dosis())
        assertEquals("15 min", asignado(series = null, repeticiones = null, minutos = 15).dosis())
        assertEquals("3 × 10 · 20 min", asignado(series = 3, repeticiones = 10, minutos = 20).dosis())
        assertEquals("—", asignado(series = null, repeticiones = null, minutos = null).dosis())
    }

    @Test
    fun delDia_filtra_por_dia_y_conserva_el_orden() {
        val lista = listOf(asignado(dia = 2, id = "a"), asignado(dia = 1, id = "b"), asignado(dia = 2, id = "c"))
        assertEquals(listOf("a", "c"), lista.delDia(2).map { it.id })
        assertEquals(emptyList<EjercicioAsignado>(), lista.delDia(3))
    }

    @Test
    fun limpio_deja_diez_por_dia_y_treinta_y_cinco_en_total() {
        val muchosUnDia = PlanContenido(ejerciciosAsignados = List(12) { asignado(dia = 1, id = "e$it") })
        assertEquals(PlanContenido.MAX_ASIGNADOS_DIA, muchosUnDia.limpio().ejerciciosAsignados.size)
        assertEquals((0 until 10).map { "e$it" }, muchosUnDia.limpio().ejerciciosAsignados.map { it.id })

        // 40 repartidos en cinco días (8 por día): ninguno pasa de 10, pero el total se recorta a 35
        val repartidos = PlanContenido(ejerciciosAsignados = List(40) { asignado(dia = it % 5 + 1, id = "e$it") })
        assertEquals(PlanContenido.MAX_ASIGNADOS, repartidos.limpio().ejerciciosAsignados.size)
    }

    @Test
    fun limpio_quita_repetidos_invalidos_y_espacios_sin_cambiar_el_orden() {
        val plan = PlanContenido(
            ejerciciosAsignados = listOf(
                asignado(dia = 1, id = "a", nota = "  con calma  "),
                asignado(dia = 1, id = "a", nota = "repetido"),
                asignado(dia = 2, id = "a"),
                asignado(dia = 9, id = "b"),
                asignado(dia = 1, id = "c"),
            ),
        )
        val limpio = plan.limpio().ejerciciosAsignados
        assertEquals(listOf(1 to "a", 2 to "a", 1 to "c"), limpio.map { it.dia to it.id })
        assertEquals("con calma", limpio.first().nota)
    }

    @Test
    fun un_plan_solo_con_asignados_ya_no_esta_vacio() {
        assertTrue(PlanContenido().vacio)
        assertFalse(PlanContenido(ejerciciosAsignados = listOf(asignado())).vacio)
    }
}
