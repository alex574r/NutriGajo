package com.example.nutrigajo.data.carrera

import com.example.nutrigajo.domain.model.PuntoRuta
import com.example.nutrigajo.domain.model.TipoActividad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RastreoRutaTest {
    private var reloj = 0L
    private val rastreo = RastreoRuta { reloj }
    private fun punto(metrosAlNorte: Double) = PuntoRuta(19.4326 + metrosAlNorte / 111_195.0, -99.1332)

    @Test
    fun suma_distancia_y_tiempo_sin_contar_las_pausas() {
        reloj = 1_000
        rastreo.iniciar(TipoActividad.CORRER, 5_000_000)
        rastreo.ubicar(punto(0.0), 1_000, 5f)
        rastreo.ubicar(punto(10.0), 4_000, 5f)
        reloj = 11_000
        rastreo.pausar()
        reloj = 111_000 // cien segundos en pausa no cuentan
        rastreo.reanudar()
        reloj = 121_000
        rastreo.terminar()
        val e = rastreo.estado.value
        assertEquals(Fase.RESUMEN, e.fase)
        assertEquals(20_000, e.duracionMs(reloj + 99_999)) // 10 s + 10 s
        assertEquals(10.0, e.distanciaM, 0.2)
        assertEquals(2, e.puntos.size)
    }

    @Test
    fun el_primer_punto_tras_reanudar_no_suma_distancia_y_los_malos_se_ignoran() {
        rastreo.iniciar(TipoActividad.CAMINAR, 0)
        rastreo.ubicar(punto(0.0), 1_000, 5f)
        rastreo.pausar()
        rastreo.reanudar()
        rastreo.ubicar(punto(400.0), 600_000, 5f) // avanzó con la pausa puesta: se toma como nuevo inicio
        rastreo.ubicar(punto(405.0), 603_000, 80f) // precisión mala
        rastreo.ubicar(punto(401.0), 604_000, 5f) // temblor
        rastreo.ubicar(punto(410.0), 606_000, 5f)
        val e = rastreo.estado.value
        assertEquals(10.0, e.distanciaM, 0.2)
        assertEquals(3, e.puntos.size)
    }

    @Test
    fun un_gps_que_dice_estar_quieto_no_suma_aunque_la_posicion_baile() {
        rastreo.iniciar(TipoActividad.CAMINAR, 0)
        rastreo.ubicar(punto(0.0), 1_000, 5f, 0f) // el primer punto entra siempre
        rastreo.ubicar(punto(8.0), 4_000, 5f, 0.1f) // 8 m de baile con velocidad 0.1 m/s
        rastreo.ubicar(punto(9.0), 8_000, 5f, 1.4f) // ahora sí camina: 9 m desde la salida
        val e = rastreo.estado.value
        assertEquals(9.0, e.distanciaM, 0.2)
        assertEquals(2, e.puntos.size)
    }

    @Test
    fun en_pausa_o_sin_empezar_no_registra_y_reiniciar_limpia() {
        rastreo.ubicar(punto(0.0), 1_000, 5f)
        assertTrue(rastreo.estado.value.puntos.isEmpty())
        rastreo.iniciar(TipoActividad.CAMINAR, 0)
        rastreo.pausar()
        rastreo.ubicar(punto(0.0), 1_000, 5f)
        assertTrue(rastreo.estado.value.puntos.isEmpty())
        rastreo.reiniciar()
        assertEquals(Fase.INICIO, rastreo.estado.value.fase)
    }
}
