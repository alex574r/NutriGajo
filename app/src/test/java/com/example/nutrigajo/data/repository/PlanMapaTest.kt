package com.example.nutrigajo.data.repository

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
