package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EjercicioTest {
    private val listo = Ejercicio(
        titulo = "Sentadilla con peso corporal",
        descripcion = "Trabaja piernas y glúteos sin equipo.",
        zona = ZonaCuerpo.PIERNAS,
        pasos = listOf("Pies al ancho de los hombros", "Baja hasta 90 grados", "Sube empujando el piso"),
        duracionMin = 10,
        kcal = 60,
        fotoVersion = 1L,
    )

    @Test
    fun un_ejercicio_completo_se_puede_mandar_a_revision() {
        assertNull(listo.problemaParaRevision())
    }

    @Test
    fun cada_campo_obligatorio_que_falta_se_reporta() {
        assertEquals(ProblemaEjercicio.TITULO, listo.copy(titulo = "ab").problemaParaRevision())
        assertEquals(ProblemaEjercicio.DESCRIPCION, listo.copy(descripcion = "corta").problemaParaRevision())
        assertEquals(ProblemaEjercicio.PASOS, listo.copy(pasos = emptyList()).problemaParaRevision())
        assertEquals(ProblemaEjercicio.PASOS, listo.copy(pasos = listOf("uno", "  ")).problemaParaRevision())
        assertEquals(ProblemaEjercicio.DURACION, listo.copy(duracionMin = 0).problemaParaRevision())
        assertEquals(ProblemaEjercicio.DURACION, listo.copy(duracionMin = 181).problemaParaRevision())
        assertEquals(ProblemaEjercicio.FOTO, listo.copy(fotoVersion = null).problemaParaRevision())
    }

    @Test
    fun los_topes_de_texto_y_de_pasos_se_respetan() {
        assertEquals(ProblemaEjercicio.PASOS, listo.copy(pasos = List(Ejercicio.MAX_PASOS + 1) { "paso $it" }).problemaParaRevision())
        assertEquals(ProblemaEjercicio.PASOS, listo.copy(pasos = listOf("x".repeat(Ejercicio.MAX_PASO + 1))).problemaParaRevision())
        assertEquals(ProblemaEjercicio.TEXTO_LARGO, listo.copy(consejos = "x".repeat(Ejercicio.MAX_TEXTO + 1)).problemaParaRevision())
        assertNull(listo.copy(consejos = "x".repeat(Ejercicio.MAX_TEXTO), pasos = List(Ejercicio.MAX_PASOS) { "paso" }).problemaParaRevision())
    }

    @Test
    fun un_borrador_solo_pide_el_titulo() {
        assertTrue(Ejercicio(titulo = "Plancha").puedeGuardarseComoBorrador())
        assertFalse(Ejercicio(titulo = "  ").puedeGuardarseComoBorrador())
    }

    @Test
    fun explorar_filtra_por_texto_y_por_los_filtros_elegidos() {
        assertTrue(listo.coincide("", null, null, null))
        assertTrue(listo.coincide("SENTADILLA", null, null, null))
        assertTrue(listo.coincide("glúteos", null, null, null))
        assertFalse(listo.coincide("remo", null, null, null))
        assertTrue(listo.coincide("", ZonaCuerpo.PIERNAS, TipoEjercicio.FUERZA, NivelEjercicio.PRINCIPIANTE))
        assertFalse(listo.coincide("", ZonaCuerpo.ESPALDA, null, null))
        assertFalse(listo.coincide("", null, TipoEjercicio.CARDIO, null))
        assertFalse(listo.coincide("", null, null, NivelEjercicio.AVANZADO))
    }

    @Test
    fun el_clip_acepta_justo_los_limites_y_rechaza_lo_que_los_pasa() {
        assertNull(validarClip(Ejercicio.MAX_CLIP_MS, Ejercicio.MAX_CLIP_BYTES, "video/mp4"))
        assertEquals(ProblemaClip.MUY_LARGO, validarClip(Ejercicio.MAX_CLIP_MS + 1, 1, "video/mp4"))
        assertEquals(ProblemaClip.MUY_PESADO, validarClip(3_000, Ejercicio.MAX_CLIP_BYTES + 1, "video/mp4"))
    }

    @Test
    fun un_clip_sin_duracion_legible_o_de_otro_tipo_se_rechaza() {
        assertEquals(ProblemaClip.SIN_DURACION, validarClip(null, 1, "video/mp4"))
        assertEquals(ProblemaClip.SIN_DURACION, validarClip(0, 1, "video/mp4"))
        assertEquals(ProblemaClip.TIPO, validarClip(3_000, 1, "video/quicktime"))
        assertEquals(ProblemaClip.TIPO, validarClip(3_000, 1, null))
    }

    @Test
    fun el_texto_para_el_plan_cabe_en_el_tope_y_conserva_los_minutos() {
        val largo = listo.copy(pasos = List(3) { "x".repeat(200) })
        val texto = largo.textoParaPlan()
        assertTrue("mide ${texto.length}", texto.length <= PlanContenido.MAX_MENU)
        assertTrue(texto.endsWith(" · 10 min"))
        assertTrue(texto.contains("…"))
    }

    @Test
    fun con_pasos_cortos_el_texto_para_el_plan_empieza_por_el_titulo_y_los_une() {
        assertEquals(
            "Sentadilla con peso corporal: Pies al ancho de los hombros; Baja hasta 90 grados; Sube empujando el piso · 10 min",
            listo.textoParaPlan(),
        )
        assertEquals("Plancha · 10 min", Ejercicio(titulo = "Plancha", pasos = emptyList(), duracionMin = 10).textoParaPlan())
    }

    @Test
    fun un_ejercicio_global_se_convierte_en_item_del_banco_de_ejercicios() {
        val item = listo.copy(titulo = "x".repeat(120)).comoItemDelBanco()
        assertEquals(TipoBanco.EJERCICIO, item.tipo)
        assertEquals(ItemBanco.MAX_NOMBRE, item.nombre.length)
        assertNull(item.momento)
        assertTrue(item.texto.length <= PlanContenido.MAX_MENU)
    }

    @Test
    fun duplicar_no_arrastra_el_clip() {
        assertNull(listo.copy(clipVersion = 7L).duplicado().clipVersion)
    }
}
