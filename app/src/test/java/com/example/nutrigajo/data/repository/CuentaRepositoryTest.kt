package com.example.nutrigajo.data.repository

import java.io.IOException
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Qué le pasa a la app según lo que conteste el Worker al pedirle borrar la cuenta.
class CuentaRepositoryTest {
    @Test
    fun un_200_no_es_error() {
        assertNull(excepcionDeRespuesta(200))
    }

    @Test
    fun un_401_pide_confirmar_la_identidad_otra_vez() {
        assertTrue(excepcionDeRespuesta(401) is ConfirmacionVencidaException)
    }

    @Test
    fun cualquier_otro_codigo_es_un_error_para_reintentar() {
        for (codigo in listOf(403, 404, 500, 502, 503)) {
            val e = excepcionDeRespuesta(codigo)
            assertTrue("codigo $codigo", e is IOException && e !is ConfirmacionVencidaException)
        }
    }
}
