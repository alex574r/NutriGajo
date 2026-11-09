package com.example.nutrigajo.ui.nutriologo.plan

import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.EjercicioAsignado
import com.example.nutrigajo.domain.model.FuenteEjercicio
import com.example.nutrigajo.domain.model.PlanContenido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AsignarEjercicioTest {
    private val sentadilla = Ejercicio(id = "e1", titulo = "Sentadilla")

    @Test
    fun series_y_repeticiones_se_leen_de_los_campos_de_texto() {
        val a = EjercicioAsignado.desde(sentadilla, 1, "3", "10", "", " con calma ")!!
        assertEquals(EjercicioAsignado(dia = 1, fuente = FuenteEjercicio.BANCO, id = "e1", titulo = "Sentadilla", series = 3, repeticiones = 10, nota = "con calma"), a)
    }

    @Test
    fun un_ejercicio_del_catalogo_se_asigna_con_su_fuente() {
        val delCatalogo = Ejercicio(id = "0025", titulo = "Press de banca", fuente = FuenteEjercicio.CATALOGO)
        assertEquals(FuenteEjercicio.CATALOGO, EjercicioAsignado.desde(delCatalogo, 1, "3", "10", "", "")!!.fuente)
        assertEquals(FuenteEjercicio.BANCO, EjercicioAsignado.desde(sentadilla, 1, "3", "10", "", "")!!.fuente)
    }

    @Test
    fun los_minutos_bastan_sin_series() {
        val a = EjercicioAsignado.desde(sentadilla, 2, "", "", "15", "")!!
        assertEquals(15, a.minutos)
        assertNull(a.series)
    }

    @Test
    fun sin_series_ni_minutos_no_se_asigna() {
        assertNull(EjercicioAsignado.desde(sentadilla, 1, "", "10", "", ""))
        assertNull(EjercicioAsignado.desde(sentadilla, 1, "", "", "", ""))
        assertNotNull(EjercicioAsignado.desde(sentadilla, 1, "3", "", "", ""))
    }

    @Test
    fun lo_fuera_de_rango_o_que_no_es_un_entero_se_rechaza() {
        assertNull(EjercicioAsignado.desde(sentadilla, 1, "0", "", "", ""))
        assertNull(EjercicioAsignado.desde(sentadilla, 9, "3", "", "", ""))
        assertNull(EjercicioAsignado.desde(sentadilla, 0, "3", "", "", ""))
        assertNull(EjercicioAsignado.desde(sentadilla, 1, "3.5", "", "", ""))
        assertNull(EjercicioAsignado.desde(sentadilla, 1, "abc", "", "", ""))
        assertNull(EjercicioAsignado.desde(sentadilla, 1, "", "", "181", ""))
    }

    private fun asignado(dia: Int, n: Int) = EjercicioAsignado(dia = dia, id = "e$n", titulo = "E$n", series = 3)

    @Test
    fun un_dia_lleno_no_admite_mas_pero_otro_dia_si() {
        val plan = PlanContenido(ejerciciosAsignados = List(PlanContenido.MAX_ASIGNADOS_DIA) { asignado(1, it) })
        assertFalse(plan.puedeAsignar(1))
        assertTrue(plan.puedeAsignar(2))
    }

    @Test
    fun un_plan_con_el_tope_total_no_admite_mas_en_ningun_dia() {
        // 35 repartidos en cuatro días (9, 9, 9 y 8): ningún día está lleno, pero el total sí
        val lista = List(PlanContenido.MAX_ASIGNADOS) { asignado(it % 4 + 1, it) }
        val plan = PlanContenido(ejerciciosAsignados = lista)
        assertEquals(35, plan.ejerciciosAsignados.size)
        for (dia in 1..7) assertFalse("el día $dia", plan.puedeAsignar(dia))
    }
}
