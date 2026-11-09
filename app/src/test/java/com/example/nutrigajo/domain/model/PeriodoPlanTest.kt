package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PeriodoPlanTest {
    // sábado 7 de noviembre de 2026
    private val inicio = LocalDate.of(2026, 11, 7)

    private fun asignado(dia: Int, id: String = "e$dia") = EjercicioAsignado(dia = dia, id = id, titulo = "T$id", series = 3)

    @Test
    fun el_dia_del_plan_cuenta_desde_el_de_inicio_y_acaba_en_el_ultimo() {
        assertEquals(1, diaDelPlan(inicio, inicio, 14))
        assertEquals(3, diaDelPlan(inicio.plusDays(2), inicio, 14))
        assertEquals(14, diaDelPlan(inicio.plusDays(13), inicio, 14))
    }

    @Test
    fun antes_de_empezar_o_despues_de_acabar_no_hay_dia_del_plan() {
        assertNull(diaDelPlan(inicio.minusDays(1), inicio, 14))
        assertNull(diaDelPlan(inicio.plusDays(14), inicio, 14))
    }

    @Test
    fun sin_duracion_el_dia_es_el_de_la_semana_y_con_duracion_el_del_plan() {
        val semanal = CalendarioPlan(duracionDias = null, inicio = inicio)
        // el sábado es el 6; el lunes, el 1
        assertEquals(6, semanal.diaDe(inicio))
        assertEquals(1, semanal.diaDe(LocalDate.of(2026, 11, 9)))
        assertNull(semanal.fechaDe(3))
        assertEquals(7, semanal.dias)

        val de14 = CalendarioPlan(duracionDias = 14, inicio = inicio)
        assertEquals(1, de14.diaDe(inicio))
        assertNull(de14.diaDe(inicio.plusDays(20)))
        assertEquals(inicio.plusDays(2), de14.fechaDe(3))
        assertEquals(14, de14.dias)
    }

    @Test
    fun el_calendario_sale_del_dia_en_que_se_envio_el_plan() {
        val enviado = PlanEnviado("p1", enviadoEn = LocalDate.of(2026, 11, 7).atTime(10, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), nutriologoNombre = "K", contenido = PlanContenido(duracionDias = 7))
        assertEquals(CalendarioPlan(7, inicio), enviado.calendario())
    }

    @Test
    fun cambiar_la_duracion_recorta_lo_que_cae_fuera() {
        val plan = PlanContenido(
            duracionDias = 10,
            opciones = listOf(OpcionMenu("a"), OpcionMenu("b")),
            menuDias = listOf(0, 1, 0, 1, 0, 1, 0, 1, 0, 1),
            ejerciciosAsignados = listOf(asignado(2), asignado(9)),
        )
        val corto = plan.conDuracion(3)
        assertEquals(3, corto.duracionDias)
        assertEquals(listOf(0, 1, 0), corto.menuDias)
        assertEquals(listOf(2), corto.ejerciciosAsignados.map { it.dia })
        // volver a una semana de lunes a domingo deja el menú por día sin sentido: se quita, y los ejercicios de los días 1-7 se quedan
        val semanal = plan.conDuracion(null)
        assertNull(semanal.duracionDias)
        assertEquals(emptyList<Int>(), semanal.menuDias)
        assertEquals(listOf(2), semanal.ejerciciosAsignados.map { it.dia })
    }

    @Test
    fun la_duracion_se_acota_entre_uno_y_noventa_dias() {
        assertEquals(1, PlanContenido().conDuracion(0).duracionDias)
        assertEquals(MAX_DIAS_PLAN, PlanContenido().conDuracion(500).duracionDias)
    }

    @Test
    fun elegir_el_menu_de_un_dia_rellena_los_anteriores_y_se_puede_quitar() {
        val plan = PlanContenido(duracionDias = 5, opciones = listOf(OpcionMenu("a"), OpcionMenu("b")))
        val con = plan.conMenuDelDia(dia = 3, opcion = 1)
        assertEquals(listOf(-1, -1, 1), con.menuDias)
        assertEquals(1, con.opcionDelDia(3))
        assertNull(con.opcionDelDia(1))
        assertNull(con.opcionDelDia(5))
        // quitarlo (null) vuelve a dejarlo sin menú
        assertEquals(emptyList<Int>(), con.conMenuDelDia(3, null).menuDias)
    }

    @Test
    fun limpio_descarta_lo_que_cae_fuera_de_la_duracion() {
        val plan = PlanContenido(duracionDias = 3, ejerciciosAsignados = listOf(asignado(2), asignado(5)), menuDias = listOf(0, 0, 0, 0, 0))
        val limpio = plan.copy(opciones = listOf(OpcionMenu("a"))).limpio()
        assertEquals(listOf(2), limpio.ejerciciosAsignados.map { it.dia })
        assertEquals(3, limpio.menuDias.size)
        // sin duración un asignado del día 5 sigue valiendo (viernes) y no hay menú por día
        val semanal = PlanContenido(ejerciciosAsignados = listOf(asignado(5)), menuDias = listOf(0, 0)).limpio()
        assertEquals(listOf(5), semanal.ejerciciosAsignados.map { it.dia })
        assertEquals(emptyList<Int>(), semanal.menuDias)
    }

    @Test
    fun limpio_corrige_los_indices_del_menu_al_quitar_opciones_en_blanco() {
        val plan = PlanContenido(
            duracionDias = 3,
            opciones = listOf(OpcionMenu("a"), OpcionMenu(), OpcionMenu("c")),
            menuDias = listOf(0, 1, 2),
        )
        val limpio = plan.limpio()
        assertEquals(listOf("a", "c"), limpio.opciones.map { it.desayuno })
        // el día 2 apuntaba a la opción en blanco, que se quitó: queda sin menú; el día 3 ahora es la opción 2
        assertEquals(listOf(0, -1, 1), limpio.menuDias)
    }

    @Test
    fun un_menu_que_apunta_a_una_opcion_que_no_existe_se_descarta() {
        val limpio = PlanContenido(duracionDias = 2, opciones = listOf(OpcionMenu("a")), menuDias = listOf(0, 4)).limpio()
        assertEquals(listOf(0), limpio.menuDias)
    }

    @Test
    fun quitar_una_opcion_ajusta_el_menu_por_dia() {
        val plan = PlanContenido(
            duracionDias = 4,
            opciones = listOf(OpcionMenu("a"), OpcionMenu("b"), OpcionMenu("c")),
            menuDias = listOf(0, 1, 2, 1),
        )
        val sin = plan.sinOpcion(1)
        assertEquals(listOf("a", "c"), sin.opciones.map { it.desayuno })
        // los días de la opción quitada se quedan sin menú y las siguientes bajan un lugar
        assertEquals(listOf(0, -1, 1), sin.menuDias)
        // quitar un índice que no existe no cambia nada
        assertEquals(plan, plan.sinOpcion(7))
    }

    @Test
    fun el_tope_de_asignados_es_mayor_con_duracion() {
        assertEquals(PlanContenido.MAX_ASIGNADOS, PlanContenido().maxAsignados)
        assertEquals(PlanContenido.MAX_ASIGNADOS_PERIODO, PlanContenido(duracionDias = 30).maxAsignados)
        assertTrue(PlanContenido.MAX_ASIGNADOS_PERIODO > PlanContenido.MAX_ASIGNADOS)
        // un plan de 30 días acepta 5 por día sin recortar
        val plan = PlanContenido(duracionDias = 30, ejerciciosAsignados = List(150) { asignado(dia = it % 30 + 1, id = "e$it") })
        assertEquals(150, plan.limpio().ejerciciosAsignados.size)
    }

    @Test
    fun un_asignado_vale_hasta_el_ultimo_dia_del_plan() {
        assertTrue(asignado(30).valido(maxDia = 30))
        assertFalse(asignado(31).valido(maxDia = 30))
        assertFalse(asignado(8).valido())
    }

    @Test
    fun la_rutina_asignada_usa_el_dia_que_se_le_pide() {
        val resolver = { a: EjercicioAsignado -> Ejercicio(id = a.id, titulo = a.titulo, estado = EstadoEjercicio.PUBLICADO) }
        val asignados = listOf(asignado(3, "tres"), asignado(1, "uno"))
        // un sábado (día 6 de la semana) con el día 3 del plan: sale el del día 3, no el del sábado
        assertEquals(listOf("tres"), rutinaAsignada(inicio, asignados, resolver, dia = 3)!!.ejercicios.map { it.id })
        assertNull(rutinaAsignada(inicio, asignados, resolver, dia = 5))
        // sin pedir día se sigue usando el de la semana, como antes
        assertNull(rutinaAsignada(inicio, asignados, resolver))
    }
}
