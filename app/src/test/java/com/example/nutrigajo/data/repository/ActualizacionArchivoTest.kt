package com.example.nutrigajo.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import java.io.ByteArrayInputStream
import java.io.File

class ActualizacionArchivoTest {
    // SHA-256 de «abc»
    private val sha = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

    private fun archivo(contenido: String) = File.createTempFile("apk", ".tmp").apply {
        writeText(contenido)
        deleteOnExit()
    }

    @Test
    fun un_archivo_con_el_tamano_y_el_hash_publicados_coincide() {
        assertTrue(archivoCoincide(archivo("abc"), bytes = 3, sha256 = sha))
    }

    @Test
    fun un_manifiesto_corto_se_lee_entero_y_uno_que_pasa_del_limite_se_descarta_sin_leerlo_todo() {
        // antes se leía con InputStream.readNBytes, que no existe en Android 10 a 12 (minSdk 29) y tumbaba la app
        assertEquals("{\"a\":1}", leerAcotado(ByteArrayInputStream("{\"a\":1}".toByteArray()), maximo = 100))
        assertEquals("", leerAcotado(ByteArrayInputStream(ByteArray(0)), maximo = 10))
        assertEquals("1234567890", leerAcotado(ByteArrayInputStream("1234567890".toByteArray()), maximo = 10))
        var leidos = 0
        val enorme = object : ByteArrayInputStream(ByteArray(10_000)) {
            override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also { if (it > 0) leidos += it }
        }
        assertNull(leerAcotado(enorme, maximo = 10))
        assertTrue("no debía leer más de un byte por encima del límite: $leidos", leidos <= 11)
    }

    @Test
    fun un_byte_de_mas_otro_contenido_o_un_archivo_ausente_no_coinciden() {
        assertFalse("tamaño distinto", archivoCoincide(archivo("abcd"), bytes = 3, sha256 = sha))
        assertFalse("mismo tamaño, otro contenido", archivoCoincide(archivo("abd"), bytes = 3, sha256 = sha))
        assertFalse("hash distinto", archivoCoincide(archivo("abc"), bytes = 3, sha256 = "0".repeat(64)))
        assertFalse("archivo que no existe", archivoCoincide(File("no-existe-${System.nanoTime()}.apk"), bytes = 3, sha256 = sha))
    }
}
