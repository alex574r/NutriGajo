package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AsignadosDelPacienteTest {
    private val publicado = Ejercicio(id = "e1", titulo = "Sentadilla", estado = EstadoEjercicio.PUBLICADO)
    private val retirado = Ejercicio(id = "e2", titulo = "Remo", estado = EstadoEjercicio.RETIRADO)
    private val enRevision = Ejercicio(id = "e3", titulo = "Plancha", estado = EstadoEjercicio.EN_REVISION)
    private val banco = listOf(publicado, retirado, enRevision)

    private fun asignado(id: String, fuente: FuenteEjercicio = FuenteEjercicio.BANCO) =
        EjercicioAsignado(dia = 1, fuente = fuente, id = id, titulo = "T", series = 3)

    @Test
    fun un_asignado_publicado_se_resuelve_con_su_ejercicio() {
        assertEquals(publicado, resolverDelBanco(banco)(asignado("e1")))
    }

    @Test
    fun uno_retirado_en_revision_o_inexistente_no_se_resuelve() {
        val resolver = resolverDelBanco(banco)
        assertNull(resolver(asignado("e2")))
        assertNull(resolver(asignado("e3")))
        assertNull(resolver(asignado("nada")))
    }

    private val fichaCatalogo = Ejercicio(id = "0025", titulo = "Press de banca", estado = EstadoEjercicio.PUBLICADO, fuente = FuenteEjercicio.CATALOGO)

    @Test
    fun un_asignado_del_catalogo_se_resuelve_con_su_ficha() {
        assertEquals(fichaCatalogo, resolverDelBanco(banco, mapOf("0025" to fichaCatalogo))(asignado("0025", FuenteEjercicio.CATALOGO)))
    }

    @Test
    fun uno_del_catalogo_sin_su_ficha_no_se_resuelve() {
        assertNull(resolverDelBanco(banco)(asignado("0025", FuenteEjercicio.CATALOGO)))
        assertNull(resolverDelBanco(banco, mapOf("0025" to fichaCatalogo))(asignado("0026", FuenteEjercicio.CATALOGO)))
    }

    @Test
    fun la_fuente_no_se_confunde_aunque_la_id_sea_la_misma() {
        val resolver = resolverDelBanco(banco, mapOf("e1" to fichaCatalogo))
        // «e1» del banco es la sentadilla; «e1» del catálogo es la ficha del catálogo
        assertEquals(publicado, resolver(asignado("e1", FuenteEjercicio.BANCO)))
        assertEquals(fichaCatalogo, resolver(asignado("e1", FuenteEjercicio.CATALOGO)))
        // y un asignado de BANCO nunca toma una ficha del catálogo
        assertNull(resolverDelBanco(emptyList(), mapOf("0025" to fichaCatalogo))(asignado("0025", FuenteEjercicio.BANCO)))
    }

    @Test
    fun solo_asignados_del_catalogo_sin_fichas_dan_null_y_el_inicio_cae_al_baraje() {
        val lunes = LocalDate.of(2026, 11, 2)
        val soloCatalogo = listOf(asignado("0025", FuenteEjercicio.CATALOGO))
        assertNull(rutinaAsignada(lunes, soloCatalogo, resolverDelBanco(banco)))
        assertEquals(listOf("0025"), rutinaAsignada(lunes, soloCatalogo, resolverDelBanco(banco, mapOf("0025" to fichaCatalogo)))!!.ejercicios.map { it.id })
    }

    @Test
    fun si_ningun_asignado_se_resuelve_no_hay_rutina_y_el_inicio_cae_al_baraje() {
        val lunes = LocalDate.of(2026, 11, 2)
        assertNull(rutinaAsignada(lunes, listOf(asignado("e2"), asignado("nada")), resolverDelBanco(banco)))
        assertEquals(listOf("e1"), rutinaAsignada(lunes, listOf(asignado("e2"), asignado("e1")), resolverDelBanco(banco))!!.ejercicios.map { it.id })
    }
}
