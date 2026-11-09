package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogoTest {
    private val indiceJson = """
        [["0025","Press de banca con barra","PECHO","FUERZA","BARRA"],
         ["0100","Sentadilla búlgara","PIERNAS","FUERZA","MANCUERNAS"],
         ["0200","Estiramiento de cuádriceps","PIERNAS","FLEXIBILIDAD","SIN_EQUIPO"],
         ["0300","Trote suave","CUERPO_COMPLETO","CARDIO","SIN_EQUIPO"]]
    """.trimIndent()

    @Test
    fun el_indice_se_lee_fila_por_fila_y_se_salta_lo_que_no_sirve() {
        val json = """[["0025","Press","PECHO","FUERZA","BARRA"],["x"],["0026","Mal","CEREBRO","FUERZA","BARRA"],"no soy fila",["0027","","PECHO","FUERZA","BARRA"]]"""
        val entradas = parsearIndice(json)
        assertEquals(listOf("0025"), entradas.map { it.id })
        assertEquals(EntradaCatalogo("0025", "Press", ZonaCuerpo.PECHO, TipoEjercicio.FUERZA, EquipoEjercicio.BARRA), entradas.single())
    }

    @Test
    fun un_indice_ilegible_es_una_lista_vacia() {
        assertEquals(emptyList<EntradaCatalogo>(), parsearIndice("esto no es json"))
        assertEquals(emptyList<EntradaCatalogo>(), parsearIndice("{}"))
        assertEquals(emptyList<EntradaCatalogo>(), parsearIndice(""))
    }

    @Test
    fun una_entrada_se_convierte_en_un_ejercicio_del_catalogo_con_los_valores_por_defecto() {
        val e = EntradaCatalogo("0025", "Press de banca", ZonaCuerpo.PECHO, TipoEjercicio.FUERZA, EquipoEjercicio.BARRA).aEjercicio()
        assertEquals(FuenteEjercicio.CATALOGO, e.fuente)
        assertEquals("0025", e.id)
        assertEquals(EstadoEjercicio.PUBLICADO, e.estado)
        assertEquals(NivelEjercicio.PRINCIPIANTE, e.nivel)
        assertEquals(5, e.duracionMin)
        assertEquals(25, e.kcal)
        assertNull(e.fotoVersion)
        assertNull(e.clipVersion)
        assertEquals(45, EntradaCatalogo("1", "Trote", ZonaCuerpo.CUERPO_COMPLETO, TipoEjercicio.CARDIO, EquipoEjercicio.SIN_EQUIPO).aEjercicio().kcal)
        assertEquals(15, EntradaCatalogo("2", "Estirar", ZonaCuerpo.PIERNAS, TipoEjercicio.YOGA, EquipoEjercicio.SIN_EQUIPO).aEjercicio().kcal)
    }

    @Test
    fun la_ficha_completa_trae_la_descripcion_y_los_pasos() {
        val json = """{"id":"0025","titulo":"Press de banca con barra","zona":"PECHO","tipo":"FUERZA","equipo":"BARRA","musculos":["pectorals"],"descripcion":"Clásica.","pasos":["Túmbate","Empuja"],"duracionMin":5,"kcal":25}"""
        val e = fichaAEjercicio(json)!!
        assertEquals("Press de banca con barra", e.titulo)
        assertEquals("Clásica.", e.descripcion)
        assertEquals(listOf("Túmbate", "Empuja"), e.pasos)
        assertEquals(FuenteEjercicio.CATALOGO, e.fuente)
        assertEquals(ZonaCuerpo.PECHO, e.zona)
    }

    @Test
    fun una_ficha_sin_id_o_sin_titulo_o_ilegible_no_sirve() {
        assertNull(fichaAEjercicio("""{"titulo":"x","zona":"PECHO","tipo":"FUERZA","equipo":"BARRA"}"""))
        assertNull(fichaAEjercicio("""{"id":"0025","zona":"PECHO","tipo":"FUERZA","equipo":"BARRA"}"""))
        assertNull(fichaAEjercicio("basura"))
    }

    @Test
    fun la_busqueda_ignora_acentos_y_mayusculas_y_filtra_por_zona_y_tipo() {
        val indice = parsearIndice(indiceJson)
        assertEquals(listOf("0025"), indice.buscar("PRESS", null, null).map { it.id })
        assertEquals(listOf("0100"), indice.buscar("sentadilla bulgara", null, null).map { it.id })
        assertEquals(listOf("0200"), indice.buscar("cuadriceps", null, null).map { it.id })
        assertEquals(listOf("0100", "0200"), indice.buscar("", ZonaCuerpo.PIERNAS, null).map { it.id })
        assertEquals(listOf("0300"), indice.buscar("", null, TipoEjercicio.CARDIO).map { it.id })
        assertTrue(indice.buscar("zzz", null, null).isEmpty())
    }

    @Test
    fun la_busqueda_de_varias_palabras_pide_todas_en_cualquier_orden() {
        val indice = parsearIndice(indiceJson)
        // «Press de banca con barra»: las palabras no tienen que ir seguidas ni en ese orden
        assertEquals(listOf("0025"), indice.buscar("press banca", null, null).map { it.id })
        assertEquals(listOf("0025"), indice.buscar("  barra   PRESS ", null, null).map { it.id })
        // si falta una sola palabra, no coincide
        assertTrue(indice.buscar("press sentadilla", null, null).isEmpty())
    }

    @Test
    fun la_busqueda_vacia_da_los_primeros_y_respeta_el_limite() {
        val indice = parsearIndice(indiceJson)
        assertEquals(listOf("0025", "0100"), indice.buscar("", null, null, limite = 2).map { it.id })
        assertEquals(4, indice.buscar("  ", null, null).size)
    }

    @Test
    fun el_indice_solo_se_baja_si_hay_uno_mas_nuevo() {
        assertTrue(debeActualizarIndice(null, 1))
        assertTrue(debeActualizarIndice(3, 4))
        assertFalse(debeActualizarIndice(4, 4))
        assertFalse(debeActualizarIndice(5, 4))
    }
}
