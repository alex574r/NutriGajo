package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BancoTest {
    @Test
    fun el_nombre_sugerido_es_lo_que_va_antes_de_los_dos_puntos() {
        assertEquals("Tortilla española", nombreSugerido("Tortilla española: corta 1 papa en rodajas, agrega 2 huevos."))
    }

    @Test
    fun sin_dos_puntos_el_nombre_es_el_principio_cortado_en_una_palabra() {
        assertEquals("Avena con fruta", nombreSugerido("Avena con fruta"))
        val largo = nombreSugerido("Ensalada de pollo con verduras salteadas y aderezo de limón\nsegunda línea")
        assertEquals("Ensalada de pollo con verduras…", largo)
    }

    @Test
    fun una_comida_elegida_lleva_su_nombre_solo_si_el_texto_no_lo_trae() {
        val guardada = ItemBanco(tipo = TipoBanco.COMIDA, nombre = "Tortilla española", texto = "Tortilla española: corta 1 papa.")
        assertEquals("Tortilla española: corta 1 papa.", guardada.textoParaPlan())
        val nueva = ItemBanco(tipo = TipoBanco.COMIDA, nombre = "Avena", texto = "1 taza con fruta")
        assertEquals("Avena: 1 taza con fruta", nueva.textoParaPlan())
        // un ejercicio va tal cual: su nombre es el enfoque, que se escribe aparte
        assertEquals("3 series de 12", ItemBanco(tipo = TipoBanco.EJERCICIO, nombre = "Piernas", texto = "3 series de 12").textoParaPlan())
    }

    @Test
    fun agregar_reemplaza_lo_vacio_y_si_no_va_en_otra_linea_con_tope() {
        assertEquals("nuevo", "  ".agregando("nuevo", 500))
        assertEquals("uno\ndos", "uno ".agregando("dos", 500))
        assertEquals("uno\nd", "uno".agregando("dos", 5))
    }

    @Test
    fun cada_momento_lee_y_escribe_su_campo() {
        val opcion = OpcionMenu(desayuno = "a", comida = "b", cena = "c")
        assertEquals("b", opcion.de(MomentoComida.COMIDA))
        assertEquals(OpcionMenu("a", "b", "x"), opcion.con(MomentoComida.CENA, "x"))
    }
}
