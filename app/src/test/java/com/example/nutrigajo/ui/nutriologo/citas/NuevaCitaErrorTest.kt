package com.example.nutrigajo.ui.nutriologo.citas

import com.example.nutrigajo.R
import org.junit.Assert.assertEquals
import org.junit.Test

class NuevaCitaErrorTest {
    @Test
    fun si_la_hora_quedo_ocupada_el_error_es_el_de_ocupada() {
        assertEquals(R.string.nueva_cita_error_ocupada, errorAlRechazarse(hora = 5L, ocupados = setOf(5L, 9L), generico = 777))
    }

    @Test
    fun si_la_hora_sigue_libre_el_rechazo_es_otro_y_se_muestra_el_generico() {
        // por ejemplo el nombre del consultorio que ya no coincide: decir «ocupada» dejaría al nutriólogo sin salida
        assertEquals(777, errorAlRechazarse(hora = 5L, ocupados = setOf(9L), generico = 777))
    }
}
