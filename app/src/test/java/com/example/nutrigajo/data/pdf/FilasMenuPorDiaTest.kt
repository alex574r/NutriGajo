package com.example.nutrigajo.data.pdf

import com.example.nutrigajo.domain.model.OpcionMenu
import com.example.nutrigajo.domain.model.PlanContenido
import org.junit.Assert.assertEquals
import org.junit.Test

class FilasMenuPorDiaTest {
    private val etiquetaDia = { dia: Int -> "Día $dia" }
    private val etiquetaOpcion = { n: Int -> "Opción $n" }

    @Test
    fun sale_una_fila_por_cada_dia_con_menu_y_se_salta_los_que_no_tienen() {
        val plan = PlanContenido(
            duracionDias = 5,
            opciones = listOf(OpcionMenu("a"), OpcionMenu("b")),
            menuDias = listOf(0, -1, 1, 1),
        )
        assertEquals(
            listOf(listOf("Día 1", "Opción 1"), listOf("Día 3", "Opción 2"), listOf("Día 4", "Opción 2")),
            filasMenuPorDia(plan, etiquetaDia, etiquetaOpcion),
        )
    }

    @Test
    fun sin_duracion_o_sin_menu_no_hay_filas() {
        assertEquals(emptyList<List<String>>(), filasMenuPorDia(PlanContenido(opciones = listOf(OpcionMenu("a")), menuDias = listOf(0)), etiquetaDia, etiquetaOpcion))
        assertEquals(emptyList<List<String>>(), filasMenuPorDia(PlanContenido(duracionDias = 3, opciones = listOf(OpcionMenu("a"))), etiquetaDia, etiquetaOpcion))
    }

    @Test
    fun una_opcion_que_ya_no_existe_se_salta() {
        val plan = PlanContenido(duracionDias = 3, opciones = listOf(OpcionMenu("a")), menuDias = listOf(0, 5))
        assertEquals(listOf(listOf("Día 1", "Opción 1")), filasMenuPorDia(plan, etiquetaDia, etiquetaOpcion))
    }
}
