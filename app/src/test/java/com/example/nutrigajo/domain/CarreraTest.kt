package com.example.nutrigajo.domain

import com.example.nutrigajo.domain.model.CALENTAMIENTO_SEG
import com.example.nutrigajo.domain.model.ENFRIAMIENTO_SEG
import com.example.nutrigajo.domain.model.PlanCaminata
import com.example.nutrigajo.domain.model.PuntoRuta
import com.example.nutrigajo.domain.model.Ritmo
import com.example.nutrigajo.domain.model.Ruta
import com.example.nutrigajo.domain.model.TipoActividad
import com.example.nutrigajo.domain.model.Tramo
import com.example.nutrigajo.domain.model.avance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarreraTest {
    // un grado de latitud son ~111.19 km
    private val origen = PuntoRuta(19.4326, -99.1332)
    private fun alNorte(metros: Double) = PuntoRuta(origen.lat + metros / 111_195.0, origen.lng)

    @Test
    fun distancia_entre_dos_puntos_es_la_del_mapa() {
        assertEquals(1000.0, Ruta.distanciaM(origen, alNorte(1000.0)), 2.0)
        assertEquals(0.0, Ruta.distanciaM(origen, origen), 0.001)
    }

    @Test
    fun el_temblor_el_salto_y_el_desorden_no_cuentan() {
        assertNull("1 m es temblor", Ruta.paso(origen, 0, alNorte(1.0), 1000))
        assertNull("500 m en 1 s es un salto del GPS", Ruta.paso(origen, 0, alNorte(500.0), 1000))
        assertNull("un fix anterior al último", Ruta.paso(origen, 5000, alNorte(10.0), 4000))
        assertEquals(5.0, Ruta.paso(origen, 0, alNorte(5.0), 2000)!!, 0.1)
    }

    @Test
    fun con_mala_precision_el_paso_minimo_sube_a_la_mitad_del_error() {
        assertNull("6 m con un error de ±20 m es ruido", Ruta.paso(origen, 0, alNorte(6.0), 4000, 20f))
        assertEquals(12.0, Ruta.paso(origen, 0, alNorte(12.0), 6000, 20f)!!, 0.2)
        assertEquals(4.0, Ruta.paso(origen, 0, alNorte(4.0), 3000, 5f)!!, 0.1)
    }

    @Test
    fun el_ritmo_sale_de_distancia_y_tiempo_y_no_con_menos_de_50_metros() {
        assertEquals(360, Ruta.ritmoSegPorKm(5000.0, 1800)) // 6:00 por km
        assertNull(Ruta.ritmoSegPorKm(40.0, 60))
        assertNull(Ruta.ritmoSegPorKm(1000.0, 0))
    }

    @Test
    fun las_calorias_dependen_del_peso_y_de_la_velocidad() {
        // caminar 1 h a 5 km/h con 70 kg: MET 3.5 → 245 kcal
        assertEquals(245, Ruta.kcal(TipoActividad.CAMINAR, 70.0, 3600, 5000.0))
        // correr 1 h a 10 km/h con 70 kg: MET 10 → 700 kcal
        assertEquals(700, Ruta.kcal(TipoActividad.CORRER, 70.0, 3600, 10_000.0))
        assertEquals(0, Ruta.kcal(TipoActividad.CAMINAR, 70.0, 0, 0.0))
        assertTrue(Ruta.kcal(TipoActividad.CAMINAR, 90.0, 1800, 2500.0) > Ruta.kcal(TipoActividad.CAMINAR, 60.0, 1800, 2500.0))
    }

    @Test
    fun aligerar_respeta_el_tope_y_conserva_los_extremos() {
        val ruta = (0 until 5000).map { alNorte(it * 5.0) }
        val corta = Ruta.aligerar(ruta, 2000)
        assertTrue(corta.size <= 2001)
        assertEquals(ruta.first(), corta.first())
        assertEquals(ruta.last(), corta.last())
        val pocas = ruta.take(10)
        assertEquals(pocas, Ruta.aligerar(pocas, 2000))
    }

    @Test
    fun la_sesion_empieza_y_termina_caminando_y_repite_la_ronda() {
        val plan = PlanCaminata.INTERVALOS_INTERMEDIO // 5 × (1 min caminar, 2 trotar, 2 correr)
        val tramos = plan.tramos
        assertEquals(1 + 5 * 3 + 1, tramos.size)
        assertEquals(Tramo(Ritmo.CAMINAR, CALENTAMIENTO_SEG), tramos.first())
        assertEquals(Tramo(Ritmo.CAMINAR, ENFRIAMIENTO_SEG), tramos.last())
        assertEquals(listOf(Ritmo.CAMINAR, Ritmo.TROTAR, Ritmo.CORRER), tramos.subList(1, 4).map { it.ritmo })
        assertEquals(35, plan.minutos)
    }

    @Test
    fun todos_los_planes_suman_minutos_exactos_y_cambian_de_ritmo() {
        PlanCaminata.entries.forEach { plan ->
            assertEquals(plan.name, plan.minutos * 60, plan.tramos.sumOf { it.segundos })
            assertTrue(plan.name, plan.minutos in 20..60)
            assertTrue("${plan.name} cambia de ritmo", plan.ronda.map { it.ritmo }.toSet().size >= 2)
        }
    }

    @Test
    fun los_planes_de_caminata_no_trotan_y_los_de_intervalos_si() {
        val caminata = listOf(PlanCaminata.PRIMEROS_PASOS, PlanCaminata.RITMO_CONSTANTE, PlanCaminata.RESISTENCIA)
        caminata.forEach { plan ->
            assertEquals(TipoActividad.CAMINAR, plan.tipo)
            assertTrue(plan.tramos.none { it.ritmo == Ritmo.TROTAR || it.ritmo == Ritmo.CORRER })
            assertTrue(plan.tramos.any { it.ritmo == Ritmo.RAPIDO })
        }
        (PlanCaminata.entries - caminata.toSet()).forEach { assertEquals(it.name, TipoActividad.CORRER, it.tipo) }
        assertTrue("principiante no corre", PlanCaminata.INTERVALOS_PRINCIPIANTE.tramos.none { it.ritmo == Ritmo.CORRER })
        assertTrue(PlanCaminata.INTERVALOS_AVANZADO.tramos.any { it.ritmo == Ritmo.CORRER })
    }

    @Test
    fun el_avance_dice_en_que_tramo_se_va_y_cuanto_falta() {
        val tramos = listOf(Tramo(Ritmo.CAMINAR, 60), Tramo(Ritmo.TROTAR, 30), Tramo(Ritmo.CORRER, 30))
        val inicio = tramos.avance(0)!!
        assertEquals(0, inicio.indice)
        assertEquals(60_000L, inicio.restanteMs)
        assertEquals(Ritmo.TROTAR, inicio.siguiente?.ritmo)
        val justo = tramos.avance(60_000)!! // en el segundo exacto ya es el siguiente
        assertEquals(1, justo.indice)
        assertEquals(30_000L, justo.restanteMs)
        val ultimo = tramos.avance(119_999)!!
        assertEquals(2, ultimo.indice)
        assertNull(ultimo.siguiente)
        assertNull("pasados todos los tramos", tramos.avance(120_000))
        assertNull(emptyList<Tramo>().avance(0))
    }
}
