package com.example.nutrigajo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// El enlace del correo de Firebase llega pegado a mano en una casilla: se saca el enlace del texto pegado.
class EnlaceDeConfirmacionTest {
    @Test
    fun saca_el_enlace_de_un_texto_con_espacios_y_saltos_de_linea() {
        assertEquals(
            "https://nutrigajo.firebaseapp.com/__/auth/action?apiKey=k&mode=signIn&oobCode=abc",
            extraerEnlace("  \nhttps://nutrigajo.firebaseapp.com/__/auth/action?apiKey=k&mode=signIn&oobCode=abc \n"),
        )
    }

    @Test
    fun no_corta_un_enlace_con_ampersand_ni_porcentaje() {
        val enlace = "https://x.web.app/__/auth/links?link=https%3A%2F%2Fx.firebaseapp.com%2F__%2Fauth%2Faction%3Fa%3D1%26b%3D2&lang=es"
        assertEquals(enlace, extraerEnlace("Abre $enlace para confirmar"))
    }

    @Test
    fun sin_enlace_es_null() {
        assertNull(extraerEnlace(""))
        assertNull(extraerEnlace("   "))
        assertNull(extraerEnlace("no es un enlace"))
        assertNull(extraerEnlace("http://inseguro.com/x"))
    }
}
