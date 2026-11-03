package com.example.nutrigajo.data.repository

import com.example.nutrigajo.domain.model.EnfoqueDia
import com.example.nutrigajo.domain.model.Entrenamiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EntrenamientosMapaTest {
    private val hecho = Entrenamiento(id = "t1", fecha = 1_793_000_000_000L, enfoque = EnfoqueDia.ESPALDA_Y_HOMBROS, minutos = 42, kcal = 310, hechos = 5, total = 6)

    @Test
    fun un_entrenamiento_vuelve_igual_tras_guardarse_y_leerse() {
        // Firestore devuelve los enteros como Long, aunque se hayan escrito como Int
        val leido = entrenamientoDeMapa("t1", hecho.aMapa().mapValues { (_, v) -> if (v is Int) v.toLong() else v })
        assertEquals(hecho, leido)
    }

    @Test
    fun lo_que_se_escribe_respeta_los_topes_de_las_reglas() {
        val mapa = hecho.copy(minutos = 5_000, kcal = 99_999).aMapa()
        assertEquals(Entrenamiento.MAX_MINUTOS, mapa["minutos"])
        assertEquals(Entrenamiento.MAX_KCAL, mapa["kcal"])
    }

    @Test
    fun un_documento_incompleto_o_de_una_version_futura_no_se_muestra() {
        assertNull(entrenamientoDeMapa("t1", hecho.aMapa() + ("enfoque" to "BAILE")))
        assertNull(entrenamientoDeMapa("t1", hecho.aMapa() - "fecha"))
        assertNull(entrenamientoDeMapa("t1", hecho.aMapa() - "hechos"))
    }

    @Test
    fun cuenta_para_la_racha_con_la_mitad_de_la_rutina() {
        assertTrue(hecho.copy(hechos = 3, total = 6).cuentaParaLaRacha)
        assertFalse(hecho.copy(hechos = 2, total = 6).cuentaParaLaRacha)
        assertTrue(hecho.copy(hechos = 1, total = 1).cuentaParaLaRacha)
        assertFalse(hecho.copy(hechos = 0, total = 0).cuentaParaLaRacha)
    }

    @Test
    fun el_dia_es_el_de_la_zona_del_telefono() {
        // las 23:30 de México (UTC-6) del 2 de noviembre ya son el 3 en UTC
        val tarde = LocalDate.of(2026, 11, 2).atTime(23, 30).atZone(ZoneId.of("America/Mexico_City")).toInstant().toEpochMilli()
        val e = hecho.copy(fecha = tarde)
        assertEquals(LocalDate.of(2026, 11, 2).toEpochDay(), e.dia(ZoneId.of("America/Mexico_City")))
        assertEquals(LocalDate.of(2026, 11, 3).toEpochDay(), e.dia(ZoneId.of("UTC")))
    }
}
