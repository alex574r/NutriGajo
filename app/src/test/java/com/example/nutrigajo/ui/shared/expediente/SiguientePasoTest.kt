package com.example.nutrigajo.ui.shared.expediente

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SiguientePasoTest {
    @Test
    fun el_orden_del_asistente_tiene_los_catorce_pasos_una_sola_vez() {
        assertEquals(TOTAL_PASOS, ORDEN_PASOS.size)
        assertEquals((0 until TOTAL_PASOS).toList(), ORDEN_PASOS.sorted())
    }

    @Test
    fun el_asistente_completo_va_modulo_por_modulo() {
        assertEquals(1, siguientePaso(null, 0))
        // del último paso del cuerpo (estatura) a salud (condiciones); la actividad se pregunta junta, al final de alimentación
        assertEquals(6, siguientePaso(null, 4))
        assertEquals(5, siguientePaso(null, 12))
        assertEquals(13, siguientePaso(null, 10))
        assertNull(siguientePaso(null, 13))
    }

    @Test
    fun regresar_deshace_el_mismo_orden() {
        assertNull(anteriorPaso(null, 0))
        assertEquals(4, anteriorPaso(null, 6))
        assertEquals(12, anteriorPaso(null, 5))
        assertEquals(10, anteriorPaso(null, 13))
    }

    @Test
    fun una_categoria_salta_solo_entre_sus_pasos() {
        // alimentación: dieta, suplementos y calorías no son pasos seguidos del asistente
        val alimentacion = listOf(8, 11, 12)
        assertEquals(11, siguientePaso(alimentacion, 8))
        assertEquals(12, siguientePaso(alimentacion, 11))
        assertNull(siguientePaso(alimentacion, 12))
        assertNull(siguientePaso(listOf(0), 0))
        assertNull(anteriorPaso(alimentacion, 8))
    }

    @Test
    fun cada_paso_es_de_un_modulo() {
        assertEquals(ModuloExpediente.CUERPO, moduloDe(3))
        assertEquals(ModuloExpediente.ACTIVIDAD, moduloDe(5))
        assertEquals(ModuloExpediente.ALIMENTACION, moduloDe(11))
        assertEquals(ModuloExpediente.DESCANSO, moduloDe(13))
    }
}
