package com.example.nutrigajo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreverificacionTest {
    @Test
    fun cedula_acepta_de_6_a_8_digitos_y_quita_espacios() {
        assertEquals("1234567", Preverificacion.cedula("1234567"))
        assertEquals("12345678", Preverificacion.cedula(" 12 345 678 "))
        assertNull(Preverificacion.cedula("12345"))
        assertNull(Preverificacion.cedula("123456789"))
        assertNull(Preverificacion.cedula("12a4567"))
    }

    @Test
    fun nombre_coincide_ignora_acentos_mayusculas_y_segundos_nombres() {
        assertTrue(Preverificacion.nombreCoincide("Ana López", "ANA MARÍA LÓPEZ GARCÍA"))
        assertTrue(Preverificacion.nombreCoincide("Muñoz Peña", "PEÑA MUÑOZ JOSÉ"))
        assertFalse(Preverificacion.nombreCoincide("Pedro Pérez", "Ana López"))
        assertFalse(Preverificacion.nombreCoincide("Ana Pérez", "Ana López"))
        assertFalse(Preverificacion.nombreCoincide("  ", "Ana López"))
    }
}
