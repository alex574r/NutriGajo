package com.example.nutrigajo.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogoRepositoryTest {
    @Test
    fun los_archivos_de_la_cache_llevan_el_tipo_y_la_id() {
        assertEquals("catalogo-foto-0025.webp", nombreArchivoCatalogo("foto", "0025"))
        assertEquals("catalogo-clip-0025.mp4", nombreArchivoCatalogo("clip", "0025"))
        assertEquals("catalogo-ficha-0025.json", nombreArchivoCatalogo("ficha", "0025"))
    }

    @Test
    fun solo_pasan_las_ids_de_cuatro_o_cinco_digitos() {
        assertTrue(esIdCatalogo("0025"))
        assertTrue(esIdCatalogo("10189"))
        assertFalse(esIdCatalogo("../x"))
        assertFalse(esIdCatalogo("abc"))
        assertFalse(esIdCatalogo("123"))
        assertFalse(esIdCatalogo("123456"))
        assertFalse(esIdCatalogo(""))
    }
}
