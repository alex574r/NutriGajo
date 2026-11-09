package com.example.nutrigajo.ui.shared.ajustes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// La lista de notas de versión se escribe a mano al publicar cada versión: estas pruebas cuidan que no se repita ni se desordene.
class NotasDeVersionTest {
    private fun numeros(version: String) = version.split(".").map { it.toInt() }

    @Test
    fun hay_al_menos_una_version_con_notas() {
        assertTrue(NOTAS_DE_VERSION.isNotEmpty())
        assertTrue(NOTAS_DE_VERSION.all { it.notas != 0 })
    }

    @Test
    fun las_versiones_no_se_repiten() {
        assertEquals(NOTAS_DE_VERSION.size, NOTAS_DE_VERSION.map { it.version }.distinct().size)
    }

    @Test
    fun van_de_la_mas_nueva_a_la_mas_vieja() {
        val ordenadas = NOTAS_DE_VERSION.sortedWith { a, b ->
            val (x, y) = numeros(a.version) to numeros(b.version)
            (0 until maxOf(x.size, y.size)).map { i -> y.getOrElse(i) { 0 }.compareTo(x.getOrElse(i) { 0 }) }.firstOrNull { it != 0 } ?: 0
        }
        assertEquals(ordenadas.map { it.version }, NOTAS_DE_VERSION.map { it.version })
    }

    @Test
    fun la_instalada_es_la_que_coincide_con_la_version_de_la_app() {
        assertTrue(esLaInstalada("1.3", "1.3"))
        assertTrue(esLaInstalada("1.3", " 1.3 "))
        assertFalse(esLaInstalada("1.3", "1.4"))
        assertFalse(esLaInstalada("1.3", ""))
    }
}
