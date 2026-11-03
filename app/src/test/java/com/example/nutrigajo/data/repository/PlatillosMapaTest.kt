package com.example.nutrigajo.data.repository

import com.example.nutrigajo.domain.model.EstadoPlatillo
import com.example.nutrigajo.domain.model.EtiquetaPlatillo
import com.example.nutrigajo.domain.model.MomentoComida
import com.example.nutrigajo.domain.model.Platillo
import com.example.nutrigajo.domain.model.Rol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatillosMapaTest {
    private val completo = Platillo(
        id = "p1",
        autorId = "uid1",
        autorNombre = "Ana Pérez",
        autorRol = Rol.ENTRENADOR,
        nombre = "Avena con fruta",
        resumen = "Desayuno que sacia sin pesar.",
        momento = MomentoComida.DESAYUNO,
        proteinaG = 14,
        grasasG = 9,
        carbosG = 48,
        kcal = 320,
        minutos = 10,
        ingredientes = listOf("½ taza de avena", "1 manzana"),
        pasos = listOf("Cuece la avena", "Agrega la fruta"),
        etiquetas = listOf(EtiquetaPlatillo.VEGETARIANO, EtiquetaPlatillo.ALTO_PROTEINA),
        fotoVersion = 1730000000000L,
        estado = EstadoPlatillo.EN_REVISION,
        creadoEn = 10L,
        actualizadoEn = 20L,
    )

    @Test
    fun un_platillo_vuelve_igual_tras_guardarse_y_leerse() {
        // Firestore devuelve los enteros como Long, aunque se hayan escrito como Int
        val leido = platilloDeMapa("p1", completo.aMapa().mapValues { (_, v) -> if (v is Int) v.toLong() else v })
        assertEquals(completo, leido)
    }

    @Test
    fun el_mapa_lleva_los_nombres_de_los_enums_como_texto() {
        val mapa = completo.aMapa()
        assertEquals("ENTRENADOR", mapa["autorRol"])
        assertEquals("DESAYUNO", mapa["momento"])
        assertEquals(listOf("VEGETARIANO", "ALTO_PROTEINA"), mapa["etiquetas"])
        assertEquals("EN_REVISION", mapa["estado"])
    }

    @Test
    fun sin_foto_el_mapa_no_trae_la_version() {
        val mapa = completo.copy(fotoVersion = null).aMapa()
        assertTrue("fotoVersion" !in mapa)
        assertNull(platilloDeMapa("p1", mapa)?.fotoVersion)
    }

    @Test
    fun el_mapa_solo_lleva_lo_que_escribe_el_autor() {
        val mapa = completo.copy(id = "x", motivoRechazo = "falta foto", publicadoEn = 5L).aMapa()
        for (campo in listOf("id", "motivoRechazo", "publicadoEn", "revisadoPor", "revisadoEn")) assertFalse(campo, campo in mapa)
    }

    @Test
    fun lo_que_puso_el_admin_se_lee_del_documento() {
        val leido = platilloDeMapa("p1", completo.aMapa() + ("estado" to "RECHAZADO") + ("motivoRechazo" to "Falta la receta") + ("publicadoEn" to 99L))!!
        assertEquals(EstadoPlatillo.RECHAZADO, leido.estado)
        assertEquals("Falta la receta", leido.motivoRechazo)
        assertEquals(99L, leido.publicadoEn)
    }

    @Test
    fun un_documento_de_una_version_futura_o_incompleto_no_se_muestra() {
        assertNull(platilloDeMapa("p1", completo.aMapa() + ("estado" to "ARCHIVADO")))
        assertNull(platilloDeMapa("p1", completo.aMapa() - "autorId"))
    }

    @Test
    fun un_valor_desconocido_cae_al_de_por_defecto_y_una_etiqueta_desconocida_se_descarta() {
        val leido = platilloDeMapa(
            "p1",
            completo.aMapa() + ("momento" to "COLACION") + ("autorRol" to "PACIENTE") + ("etiquetas" to listOf("VEGANO", "KETO")),
        )!!
        assertEquals(MomentoComida.COMIDA, leido.momento)
        assertEquals(Rol.NUTRIOLOGO, leido.autorRol)
        assertEquals(listOf(EtiquetaPlatillo.VEGANO), leido.etiquetas)
    }

    @Test
    fun los_numeros_y_las_listas_que_faltan_toman_su_valor_por_defecto() {
        val leido = platilloDeMapa("p1", mapOf("autorId" to "u", "estado" to "BORRADOR", "nombre" to "Té"))!!
        assertEquals("Té", leido.nombre)
        assertEquals(0, leido.kcal)
        assertEquals(15, leido.minutos)
        assertTrue(leido.ingredientes.isEmpty() && leido.pasos.isEmpty() && leido.etiquetas.isEmpty())
    }
}
