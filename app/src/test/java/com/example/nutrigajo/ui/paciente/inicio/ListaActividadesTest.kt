package com.example.nutrigajo.ui.paciente.inicio

import com.example.nutrigajo.domain.model.Carrera
import com.example.nutrigajo.domain.model.EnfoqueDia
import com.example.nutrigajo.domain.model.Entrenamiento
import com.example.nutrigajo.domain.model.TipoActividad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class ListaActividadesTest {
    private fun ms(texto: String) = Instant.parse(texto).toEpochMilli()

    private fun fila(texto: String, kcal: Int = 100) = ActividadFila(ClaseActividad.CARRERA, ms(texto), minutos = 30, kcal = kcal)

    @Test
    fun una_carrera_pasa_a_fila_con_su_distancia_y_los_minutos_redondeados() {
        val carrera = Carrera(tipo = TipoActividad.CORRER, inicio = 1_000L, duracionSeg = 1_829, distanciaM = 5_100, kcal = 312)
        val f = carrera.aFila()
        assertEquals(ClaseActividad.CARRERA, f.clase)
        assertEquals(30, f.minutos) // 30 min 29 s
        assertEquals(5_100, f.distanciaM)
        assertEquals(312, f.kcal)
        assertEquals(1_000L, f.instante)
        assertEquals(ClaseActividad.CAMINATA, carrera.copy(tipo = TipoActividad.CAMINAR).aFila().clase)
        assertEquals(31, carrera.copy(duracionSeg = 1_830).aFila().minutos) // 30 min 30 s sube
    }

    @Test
    fun una_rutina_pasa_a_fila_sin_distancia_y_con_su_enfoque() {
        val rutina = Entrenamiento(fecha = 5_000L, enfoque = EnfoqueDia.entries.first(), minutos = 25, kcal = 140, hechos = 4, total = 5)
        val f = rutina.aFila()
        assertEquals(ClaseActividad.RUTINA, f.clase)
        assertEquals(0, f.distanciaM)
        assertEquals(EnfoqueDia.entries.first(), f.enfoque)
        assertEquals(25, f.minutos)
        assertEquals(140, f.kcal)
        assertEquals(5_000L, f.instante)
        assertNull(carreraSinEnfoque().enfoque)
    }

    private fun carreraSinEnfoque() = Carrera(tipo = TipoActividad.CAMINAR, inicio = 0, duracionSeg = 60, distanciaM = 0, kcal = 0).aFila()

    @Test
    fun agrupa_por_dia_con_lo_mas_reciente_primero_en_los_dos_niveles() {
        val filas = listOf(
            fila("2026-11-04T10:00:00Z"),
            fila("2026-11-06T08:00:00Z"),
            fila("2026-11-06T19:00:00Z"),
            fila("2026-11-05T12:00:00Z"),
        )
        val dias = actividadesPorDia(filas, ZoneOffset.UTC)
        assertEquals(listOf(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 4)), dias.map { it.first })
        assertEquals(listOf(ms("2026-11-06T19:00:00Z"), ms("2026-11-06T08:00:00Z")), dias[0].second.map { it.instante })
    }

    @Test
    fun el_dia_se_cuenta_en_la_zona_del_telefono() {
        // 03:00 UTC del 6 son las 21:00 del 5 en México (UTC-6)
        val dias = actividadesPorDia(listOf(fila("2026-11-06T03:00:00Z")), ZoneId.of("America/Mexico_City"))
        assertEquals(LocalDate.of(2026, 11, 5), dias.single().first)
    }

    @Test
    fun sin_actividades_no_hay_dias() {
        assertEquals(emptyList<Any>(), actividadesPorDia(emptyList(), ZoneOffset.UTC))
    }
}
