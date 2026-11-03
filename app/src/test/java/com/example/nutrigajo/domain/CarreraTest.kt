package com.example.nutrigajo.domain

import com.example.nutrigajo.domain.model.CALENTAMIENTO_SEG
import com.example.nutrigajo.domain.model.Carrera
import com.example.nutrigajo.domain.model.DIA_MS
import com.example.nutrigajo.domain.model.ENFRIAMIENTO_SEG
import com.example.nutrigajo.domain.model.PlanCaminata
import com.example.nutrigajo.domain.model.PuntoRuta
import com.example.nutrigajo.domain.model.Ritmo
import com.example.nutrigajo.domain.model.Ruta
import com.example.nutrigajo.domain.model.TipoActividad
import com.example.nutrigajo.domain.model.Tramo
import com.example.nutrigajo.domain.model.avance
import com.example.nutrigajo.domain.model.progreso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    private fun caminata(dia: Long, minutos: Int, inicio: Long) =
        Carrera(tipo = TipoActividad.CAMINAR, inicio = inicio + dia * DIA_MS, duracionSeg = minutos * 60, distanciaM = 2000, kcal = 100)

    @Test
    fun el_plan_cuenta_las_caminatas_de_la_semana_en_curso() {
        val plan = PlanCaminata.PRIMEROS_PASOS // semana 1: 3 de 20 min
        val inicio = 1_000_000_000_000L
        val carreras = listOf(
            caminata(0, 25, inicio),
            caminata(2, 18, inicio), // 18 min ≥ 90 % de 20
            caminata(3, 10, inicio), // muy corta
            caminata(4, 30, inicio).copy(tipo = TipoActividad.CORRER), // correr no cuenta en un plan de caminata
        )
        val p = plan.progreso(inicio, carreras, inicio + 4 * DIA_MS)
        assertEquals(0, p.semana)
        assertEquals(2, p.hechas)
        assertEquals(3, p.meta.sesiones)
        assertFalse(p.terminado)
    }

    @Test
    fun la_sesion_con_intervalos_empieza_y_termina_caminando_y_repite_la_ronda() {
        val semana = PlanCaminata.INTERVALOS_INTERMEDIO.semanas.first() // 5 × (1 min caminar, 2 trotar, 1 correr)
        val tramos = semana.tramos
        assertEquals(1 + 5 * 3 + 1, tramos.size)
        assertEquals(Tramo(Ritmo.CAMINAR, CALENTAMIENTO_SEG), tramos.first())
        assertEquals(Tramo(Ritmo.CAMINAR, ENFRIAMIENTO_SEG), tramos.last())
        assertEquals(listOf(Ritmo.CAMINAR, Ritmo.TROTAR, Ritmo.CORRER), tramos.subList(1, 4).map { it.ritmo })
        assertEquals(semana.minutos * 60, tramos.sumOf { it.segundos }) // 30 min exactos
        assertTrue("una caminata libre no trae tramos", PlanCaminata.PRIMEROS_PASOS.semanas.all { it.tramos.isEmpty() })
    }

    @Test
    fun los_planes_con_intervalos_van_de_menos_a_mas_y_principiante_no_corre() {
        val con = PlanCaminata.entries.filter { it.conIntervalos }
        assertEquals(4, con.size)
        con.forEach { plan -> assertTrue(plan.name, plan.semanas.all { it.tramos.sumOf { t -> t.segundos } <= it.minutos * 60 }) }
        val principiante = PlanCaminata.INTERVALOS_PRINCIPIANTE.semanas.flatMap { it.tramos }
        assertTrue(principiante.none { it.ritmo == Ritmo.CORRER })
        // los tramos de correr de cada semana no se acortan
        val avanzado = PlanCaminata.INTERVALOS_AVANZADO.semanas.map { s -> s.ronda.first { it.ritmo == Ritmo.CORRER }.segundos }
        assertEquals(avanzado.sorted(), avanzado)
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

    @Test
    fun en_un_plan_con_intervalos_suma_la_carrera_y_en_uno_de_caminata_la_caminata() {
        val inicio = 1_000_000_000_000L
        val semana = PlanCaminata.INTERVALOS_PRINCIPIANTE.semanas.first() // 3 sesiones de 28 min
        val sesion = caminata(1, semana.minutos, inicio).copy(tipo = TipoActividad.CORRER)
        val p = PlanCaminata.INTERVALOS_PRINCIPIANTE.progreso(inicio, listOf(sesion, caminata(2, 40, inicio)), inicio + 3 * DIA_MS)
        assertEquals("la caminata no cuenta en un plan de intervalos", 1, p.hechas)
        assertEquals(0, PlanCaminata.PRIMEROS_PASOS.progreso(inicio, listOf(sesion), inicio + 3 * DIA_MS).hechas)
    }

    @Test
    fun el_plan_avanza_de_semana_y_termina() {
        val plan = PlanCaminata.PRIMEROS_PASOS // 4 semanas
        val inicio = 1_000_000_000_000L
        val semana2 = plan.progreso(inicio, listOf(caminata(1, 40, inicio), caminata(8, 26, inicio)), inicio + 9 * DIA_MS)
        assertEquals(1, semana2.semana)
        assertEquals(1, semana2.hechas) // la de la semana 1 ya no cuenta
        val fin = plan.progreso(inicio, emptyList(), inicio + 40 * DIA_MS)
        assertTrue(fin.terminado)
        assertEquals(3, fin.semana)
        assertNotNull(fin.meta)
    }
}
