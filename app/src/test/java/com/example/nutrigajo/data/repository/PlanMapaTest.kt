package com.example.nutrigajo.data.repository

import com.example.nutrigajo.domain.model.EjercicioAsignado
import com.example.nutrigajo.domain.model.Equivalentes
import com.example.nutrigajo.domain.model.GrupoAlimento
import com.example.nutrigajo.domain.model.OpcionMenu
import com.example.nutrigajo.domain.model.PlanContenido
import com.example.nutrigajo.domain.model.RangoImc
import com.example.nutrigajo.domain.model.SesionEjercicio
import com.example.nutrigajo.domain.model.imc
import com.example.nutrigajo.domain.model.rangoImc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanMapaTest {
    private val plan = PlanContenido(
        mensaje = "Hola Ana",
        pesoKg = 84.0,
        tallaCm = 174.0,
        cinturaCm = 108.0,
        seguimiento = "13 de noviembre",
        metas = listOf("Bebe 1.5 litros de agua", "Aumenta tu consumo de fibra"),
        calorias = 1800,
        equivalentes = mapOf(GrupoAlimento.VERDURAS to Equivalentes(2, 2, 1), GrupoAlimento.LEGUMINOSAS to Equivalentes(0, 1, 0)),
        opciones = listOf(OpcionMenu("Tortilla española", "Chile relleno", "Bowl de quinoa")),
        ejercicioNotas = "Calienta 5 minutos",
        sesiones = listOf(SesionEjercicio("Lunes", "Fuerza", "3 x 12 sentadillas")),
        notas = "Toma agua",
    )

    @Test
    fun el_plan_ida_y_vuelta_por_el_mapa_de_firestore_queda_igual() {
        assertEquals(plan, plan.aMapa().aPlanContenido())
    }

    @Test
    fun la_duracion_y_el_menu_por_dia_van_y_vuelven_por_el_mapa() {
        val periodo = plan.copy(duracionDias = 14, menuDias = listOf(0, -1, 0))
        val mapa = periodo.aMapa()
        assertEquals(14, mapa["duracionDias"])
        assertEquals(listOf(0, -1, 0), mapa["menuDias"])
        assertEquals(periodo, mapa.aPlanContenido())
    }

    @Test
    fun un_plan_semanal_no_escribe_duracion_ni_menu_y_uno_viejo_se_lee_sin_ellos() {
        val mapa = plan.aMapa()
        assertTrue("duracionDias" !in mapa && "menuDias" !in mapa)
        val leido = mapa.aPlanContenido()
        assertNull(leido.duracionDias)
        assertEquals(emptyList<Int>(), leido.menuDias)
    }

    @Test
    fun los_ejercicios_asignados_van_y_vuelven_por_el_mapa() {
        val asignados = listOf(
            EjercicioAsignado(dia = 1, id = "e1", titulo = "Sentadilla", series = 3, repeticiones = 10, nota = "con calma"),
            EjercicioAsignado(dia = 7, id = "e2", titulo = "Caminata", minutos = 20),
        )
        val mapa = plan.copy(ejerciciosAsignados = asignados).aMapa()
        assertEquals(asignados, mapa.aPlanContenido().ejerciciosAsignados)
        // lo que no se llenó no se escribe
        val sinNada = (mapa["ejerciciosAsignados"] as List<*>)[1] as Map<*, *>
        assertTrue("series" !in sinNada && "repeticiones" !in sinNada && "nota" !in sinNada)
    }

    @Test
    fun un_plan_de_antes_de_los_asignados_se_lee_con_lista_vacia() {
        assertEquals(emptyList<EjercicioAsignado>(), plan.aMapa().aPlanContenido().ejerciciosAsignados)
        assertTrue("ejerciciosAsignados" !in plan.aMapa())
    }

    @Test
    fun un_asignado_ajeno_o_incompleto_se_descarta_sin_romper_el_resto() {
        val bueno = mapOf("dia" to 2L, "fuente" to "BANCO", "id" to "e1", "titulo" to "Remo")
        val mapa = mapOf(
            "ejerciciosAsignados" to listOf(
                bueno,
                mapOf("dia" to 2L, "fuente" to "FUTURA", "id" to "e2", "titulo" to "X"),
                mapOf("dia" to 2L, "fuente" to "BANCO", "titulo" to "sin id"),
                "no soy un mapa",
            ),
        )
        assertEquals(listOf(EjercicioAsignado(dia = 2, id = "e1", titulo = "Remo")), mapa.aPlanContenido().ejerciciosAsignados)
    }

    @Test
    fun lo_vacio_no_se_escribe_y_un_mapa_ajeno_no_rompe() {
        assertTrue(PlanContenido().aMapa().isEmpty())
        assertEquals(PlanContenido(), emptyMap<String, Any>().aPlanContenido())
        // un grupo que ya no existe se ignora y las porciones que faltan valen 0
        val mapa = mapOf("equivalentes" to mapOf("PASTELES" to listOf(1, 1, 1), "FRUTAS" to listOf(3)))
        assertEquals(mapOf(GrupoAlimento.FRUTAS to Equivalentes(3, 0, 0)), mapa.aPlanContenido().equivalentes)
    }

    @Test
    fun limpio_quita_espacios_y_lo_que_no_tiene_nada() {
        val sucio = plan.copy(
            mensaje = "  Hola  ",
            metas = listOf(" una ", "   "),
            equivalentes = mapOf(GrupoAlimento.GRASAS to Equivalentes()),
            opciones = listOf(OpcionMenu("  ", "", ""), OpcionMenu(" a ", "", "")),
            sesiones = listOf(SesionEjercicio("Lunes", "", "")),
        ).limpio()
        assertEquals("Hola", sucio.mensaje)
        assertEquals(listOf("una"), sucio.metas)
        assertTrue(sucio.equivalentes.isEmpty())
        assertEquals(listOf(OpcionMenu("a", "", "")), sucio.opciones)
        assertTrue(sucio.sesiones.isEmpty())
        assertTrue(PlanContenido(mensaje = "  ").limpio().vacio)
    }

    @Test
    fun el_imc_y_su_rango_siguen_el_formato_de_los_planes() {
        assertEquals(27.74, imc(84.0, 174.0), 0.01)
        assertEquals(27.74, plan.imc!!, 0.01)
        assertNull(PlanContenido(pesoKg = 80.0).imc)
        assertEquals(RangoImc.BAJO_PESO, rangoImc(18.4))
        assertEquals(RangoImc.SALUDABLE, rangoImc(18.5))
        assertEquals(RangoImc.SALUDABLE, rangoImc(24.9))
        assertEquals(RangoImc.SOBREPESO, rangoImc(25.0))
        assertEquals(RangoImc.OBESIDAD_I, rangoImc(30.0))
        assertEquals(RangoImc.OBESIDAD_II, rangoImc(35.0))
        assertEquals(RangoImc.OBESIDAD_II, rangoImc(45.0))
    }
}
