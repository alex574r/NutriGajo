package com.example.nutrigajo.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FotosBancoClipTest {
    @Test
    fun el_archivo_del_clip_lleva_autor_id_y_version() {
        assertEquals("ejercicio-clip-a-b-3.mp4", nombreArchivoClip("a", "b", 3L))
    }

    @Test
    fun limpiar_las_versiones_de_la_foto_no_borra_el_clip_del_mismo_ejercicio() {
        // la caché de fotos borra lo que empieza por «ejercicio-{autor}-{id}-»; el clip no puede caer ahí
        assertFalse(nombreArchivoClip("a", "b", 3L).startsWith("ejercicio-a-b-"))
    }

    @Test
    fun dos_versiones_del_mismo_clip_tienen_archivos_distintos() {
        assertFalse(nombreArchivoClip("a", "b", 3L) == nombreArchivoClip("a", "b", 4L))
    }
}
