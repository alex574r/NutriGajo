package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RutinaDelDiaTest {
    private var n = 0

    private fun ej(zona: ZonaCuerpo, tipo: TipoEjercicio, nivel: NivelEjercicio = NivelEjercicio.PRINCIPIANTE, minutos: Int = 10, estado: EstadoEjercicio = EstadoEjercicio.PUBLICADO) =
        Ejercicio(id = "e${n++}", titulo = "Ejercicio $n", zona = zona, tipo = tipo, nivel = nivel, duracionMin = minutos, kcal = minutos * 5, estado = estado)

    // un banco con varios ejercicios de cada zona y tipo
    private val banco = buildList {
        for (zona in ZonaCuerpo.entries) repeat(8) { add(ej(zona, TipoEjercicio.FUERZA)) }
        repeat(8) { add(ej(ZonaCuerpo.CUERPO_COMPLETO, TipoEjercicio.CARDIO)) }
        repeat(6) { add(ej(ZonaCuerpo.PIERNAS, TipoEjercicio.FLEXIBILIDAD, minutos = 4)) }
        repeat(6) { add(ej(ZonaCuerpo.CUERPO_COMPLETO, TipoEjercicio.YOGA, minutos = 5)) }
    }

    // un lunes de 2026
    private val lunes = LocalDate.of(2026, 11, 2)

    @Test
    fun el_mismo_dia_da_siempre_la_misma_rutina() {
        assertEquals(rutinaDelDia(lunes, banco), rutinaDelDia(lunes, banco.reversed()))
    }

    @Test
    fun otro_dia_de_la_misma_semana_cambia_de_enfoque() {
        assertEquals(EnfoqueDia.PIERNAS, rutinaDelDia(lunes, banco)?.enfoque)
        assertEquals(EnfoqueDia.PECHO_Y_BRAZOS, rutinaDelDia(lunes.plusDays(1), banco)?.enfoque)
        assertEquals(EnfoqueDia.MOVILIDAD, rutinaDelDia(lunes.plusDays(6), banco)?.enfoque)
        assertEquals(DayOfWeek.SUNDAY, lunes.plusDays(6).dayOfWeek)
    }

    @Test
    fun los_ejercicios_son_del_enfoque_del_dia() {
        for (d in 0L..6L) {
            val rutina = rutinaDelDia(lunes.plusDays(d), banco)!!
            assertTrue("${rutina.enfoque} trae uno que no es suyo", rutina.ejercicios.all { rutina.enfoque.admite(it) })
        }
    }

    @Test
    fun lo_de_estirar_va_al_final() {
        // el lunes pide piernas de fuerza; con solo dos, la rutina se completa con estiramientos, que quedan detrás
        val poco = listOf(ej(ZonaCuerpo.ESPALDA, TipoEjercicio.FLEXIBILIDAD), ej(ZonaCuerpo.PIERNAS, TipoEjercicio.FUERZA), ej(ZonaCuerpo.PIERNAS, TipoEjercicio.FUERZA), ej(ZonaCuerpo.CUERPO_COMPLETO, TipoEjercicio.YOGA))
        val tipos = rutinaDelDia(lunes, poco)!!.ejercicios.map { it.tipo }
        assertEquals(listOf(TipoEjercicio.FUERZA, TipoEjercicio.FUERZA), tipos.take(2))
        assertTrue(tipos.drop(2).all { it == TipoEjercicio.FLEXIBILIDAD || it == TipoEjercicio.YOGA })
    }

    @Test
    fun la_rutina_cabe_en_una_hora_y_trae_al_menos_tres() {
        val largos = List(20) { ej(ZonaCuerpo.PIERNAS, TipoEjercicio.FUERZA, minutos = 30) }
        val rutina = rutinaDelDia(lunes, largos)!!
        assertEquals(3, rutina.ejercicios.size)
        val normal = rutinaDelDia(lunes, banco)!!
        assertTrue(normal.minutos <= MINUTOS_MAX_RUTINA)
        assertTrue(normal.ejercicios.size in 3..EJERCICIOS_POR_RUTINA)
    }

    @Test
    fun con_un_banco_pequeno_completa_con_lo_que_haya() {
        val poco = listOf(ej(ZonaCuerpo.ESPALDA, TipoEjercicio.FUERZA), ej(ZonaCuerpo.PECHO, TipoEjercicio.FUERZA))
        assertEquals(2, rutinaDelDia(lunes, poco)?.ejercicios?.size)
    }

    @Test
    fun sin_ejercicios_publicados_no_hay_rutina() {
        assertNull(rutinaDelDia(lunes, emptyList()))
        assertNull(rutinaDelDia(lunes, listOf(ej(ZonaCuerpo.PIERNAS, TipoEjercicio.FUERZA, estado = EstadoEjercicio.EN_REVISION))))
    }

    @Test
    fun minutos_calorias_y_nivel_salen_de_sus_ejercicios() {
        val rutina = RutinaDelDia(
            EnfoqueDia.PIERNAS,
            listOf(
                ej(ZonaCuerpo.PIERNAS, TipoEjercicio.FUERZA, NivelEjercicio.PRINCIPIANTE, 10),
                ej(ZonaCuerpo.PIERNAS, TipoEjercicio.FUERZA, NivelEjercicio.AVANZADO, 20),
            ),
        )
        assertEquals(30, rutina.minutos)
        assertEquals(150, rutina.kcal)
        // media de 0 y 2 = 1
        assertEquals(NivelEjercicio.INTERMEDIO, rutina.nivel)
    }

    private fun asig(dia: Int, e: Ejercicio, minutos: Int? = null, series: Int? = 3) =
        EjercicioAsignado(dia = dia, id = e.id, titulo = e.titulo, series = series, minutos = minutos)

    private fun resolver(vararg lista: Ejercicio): (EjercicioAsignado) -> Ejercicio? {
        val porId = lista.associateBy { it.id }
        return { porId[it.id] }
    }

    @Test
    fun un_asignado_del_dia_arma_la_rutina_y_el_dia_sin_asignados_no() {
        val a = ej(ZonaCuerpo.PIERNAS, TipoEjercicio.FUERZA)
        val asignados = listOf(asig(1, a))
        val rutina = rutinaAsignada(lunes, asignados, resolver(a))!!
        assertTrue(rutina.asignada)
        assertEquals(EnfoqueDia.de(DayOfWeek.MONDAY), rutina.enfoque)
        assertEquals(listOf(a.id), rutina.ejercicios.map { it.id })
        assertNull(rutinaAsignada(lunes.plusDays(1), asignados, resolver(a)))
    }

    @Test
    fun el_domingo_es_el_dia_siete_y_no_se_mezcla_con_el_lunes() {
        val a = ej(ZonaCuerpo.PIERNAS, TipoEjercicio.FUERZA)
        val b = ej(ZonaCuerpo.PECHO, TipoEjercicio.FUERZA)
        val asignados = listOf(asig(7, a), asig(1, b))
        val domingo = lunes.plusDays(6)
        assertEquals(listOf(a.id), rutinaAsignada(domingo, asignados, resolver(a, b))!!.ejercicios.map { it.id })
        assertEquals(listOf(b.id), rutinaAsignada(lunes, asignados, resolver(a, b))!!.ejercicios.map { it.id })
    }

    @Test
    fun un_asignado_que_ya_no_se_resuelve_se_salta_y_sin_ninguno_no_hay_rutina() {
        val a = ej(ZonaCuerpo.PIERNAS, TipoEjercicio.FUERZA)
        val retirado = ej(ZonaCuerpo.PECHO, TipoEjercicio.FUERZA)
        val asignados = listOf(asig(1, retirado), asig(1, a))
        assertEquals(listOf(a.id), rutinaAsignada(lunes, asignados, resolver(a))!!.ejercicios.map { it.id })
        assertNull(rutinaAsignada(lunes, listOf(asig(1, retirado)), resolver(a)))
    }

    @Test
    fun la_rutina_asignada_conserva_el_orden_del_plan() {
        val a = ej(ZonaCuerpo.PIERNAS, TipoEjercicio.FUERZA)
        val b = ej(ZonaCuerpo.PECHO, TipoEjercicio.FUERZA)
        val c = ej(ZonaCuerpo.ESPALDA, TipoEjercicio.FUERZA)
        val rutina = rutinaAsignada(lunes, listOf(asig(1, c), asig(1, a), asig(1, b)), resolver(a, b, c))!!
        assertEquals(listOf(c.id, a.id, b.id), rutina.ejercicios.map { it.id })
    }

    @Test
    fun los_minutos_asignados_reemplazan_la_duracion_y_escalan_las_kcal() {
        // 10 min y 50 kcal en el banco; asignado 20 min: 100 kcal
        val a = ej(ZonaCuerpo.PIERNAS, TipoEjercicio.FUERZA, minutos = 10)
        val rutina = rutinaAsignada(lunes, listOf(asig(1, a, minutos = 20)), resolver(a))!!
        assertEquals(20, rutina.ejercicios.single().duracionMin)
        assertEquals(100, rutina.ejercicios.single().kcal)
        // sin minutos, 2 por serie: 3 series = 6 min
        val porSeries = rutinaAsignada(lunes, listOf(asig(1, a, series = 3)), resolver(a))!!
        assertEquals(6, porSeries.ejercicios.single().duracionMin)
    }

    @Test
    fun la_rutina_al_azar_no_se_marca_como_asignada() {
        assertFalse(rutinaDelDia(lunes, banco)!!.asignada)
    }
}
