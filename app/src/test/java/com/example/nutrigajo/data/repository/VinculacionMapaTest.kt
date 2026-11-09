package com.example.nutrigajo.data.repository

import com.example.nutrigajo.domain.model.CerradoPor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Los campos que escribe la app al cerrar un vínculo y al abrir uno nuevo; son los que aceptan las reglas de Firestore.
class VinculacionMapaTest {
    @Test
    fun cierre_dice_quien_lo_cerro_y_cuando() {
        assertEquals(
            mapOf("activo" to false, "cerradoEn" to 5L, "cerradoPor" to "PACIENTE", "ultimaModificacion" to 5L),
            cierreAMapa(CerradoPor.PACIENTE, 5L),
        )
        assertEquals("NUTRIOLOGO", cierreAMapa(CerradoPor.NUTRIOLOGO, 9L)["cerradoPor"])
    }

    @Test
    fun registro_nuevo_conserva_las_claves_del_alta_y_pasa_el_correo_a_minusculas() {
        assertEquals(
            mapOf(
                "nutriologoId" to "n1",
                "nutriologoNombre" to "Kelly",
                "usuarioId" to "u1",
                "nombre" to "Ana Pérez",
                "correoVinculacion" to "ana@mail.com",
                "codigoVinculacion" to "1234",
                "activo" to true,
                "fechaAlta" to 7L,
                "ultimaModificacion" to 7L,
            ),
            nuevoRegistroAMapa("n1", "Kelly", "u1", "Ana Pérez", "ANA@Mail.com", "1234", 7L),
        )
    }

    @Test
    fun registro_nuevo_suma_el_expediente_solo_si_se_pasa() {
        assertFalse(nuevoRegistroAMapa("n1", "Kelly", "u1", "Ana", "a@m.com", "1234", 7L).containsKey("expediente"))
        val expediente = mapOf<String, Any>("paso" to 3, "completo" to true)
        val mapa = nuevoRegistroAMapa("n1", "Kelly", "u1", "Ana", "a@m.com", "1234", 7L, expediente)
        assertTrue(mapa.containsKey("expediente"))
        assertEquals(expediente, mapa["expediente"])
    }
}
