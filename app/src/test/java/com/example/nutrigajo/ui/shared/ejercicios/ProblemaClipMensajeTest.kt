package com.example.nutrigajo.ui.shared.ejercicios

import com.example.nutrigajo.domain.model.ProblemaClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProblemaClipMensajeTest {
    @Test
    fun cada_problema_del_clip_tiene_su_propio_mensaje() {
        val mensajes = ProblemaClip.entries.map { it.mensaje() }
        mensajes.forEach { assertNotEquals("un problema quedó sin texto", 0, it) }
        assertEquals("cada problema dice algo distinto", mensajes.size, mensajes.toSet().size)
    }
}
