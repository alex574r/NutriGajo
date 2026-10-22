package com.example.nutrigajo.data.repository

import com.example.nutrigajo.domain.model.CalidadSueno
import com.example.nutrigajo.domain.model.Dieta
import com.example.nutrigajo.domain.model.EjercicioPreferido
import com.example.nutrigajo.domain.model.Expediente
import com.example.nutrigajo.domain.model.Objetivo
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.domain.model.Sexo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpedienteMapaTest {
    private val completo = Expediente(
        objetivo = Objetivo.BAJAR_PESO,
        sexo = Sexo.FEMENINO,
        fechaNacimiento = "1995-04-02",
        pesoKg = 68.5,
        estaturaCm = 165.0,
        nivelActividad = 3,
        condiciones = listOf("Asma"),
        alergias = listOf("Lactosa", "Gluten"),
        dieta = Dieta.PLANTAS,
        diasEjercicio = 4,
        ejercicios = listOf(EjercicioPreferido.YOGA, EjercicioPreferido.PESAS),
        suplementos = listOf("Omega-3"),
        caloriasObjetivo = 1800,
        calidadSueno = CalidadSueno.BUENA,
        paso = 5,
        completo = false,
        actualizadoPor = Rol.PACIENTE,
        actualizadoEn = 123L,
    )

    @Test
    fun ida_y_vuelta_conserva_todas_las_respuestas() {
        assertEquals(completo, completo.aMapa().aExpediente())
    }

    @Test
    fun lo_sin_responder_no_se_escribe_y_se_lee_como_vacio() {
        val mapa = Expediente().aMapa()
        assertFalse("objetivo" in mapa)
        assertFalse("condiciones" in mapa)
        assertEquals(Expediente(), mapa.aExpediente())
    }

    @Test
    fun valores_desconocidos_se_ignoran_en_vez_de_romper() {
        val leido = mapOf("objetivo" to "NO_EXISTE", "pesoKg" to 70L, "ejercicios" to listOf("YOGA", "X")).aExpediente()
        assertNull(leido.objetivo)
        assertEquals(70.0, leido.pesoKg!!, 0.0)
        assertEquals(listOf(EjercicioPreferido.YOGA), leido.ejercicios)
    }

    @Test
    fun vacio_ignora_el_avance_del_asistente() {
        assertTrue(Expediente(paso = 3, completo = true, actualizadoPor = Rol.NUTRIOLOGO, actualizadoEn = 9L).vacio)
        assertFalse(Expediente(pesoKg = 70.0).vacio)
    }
}
