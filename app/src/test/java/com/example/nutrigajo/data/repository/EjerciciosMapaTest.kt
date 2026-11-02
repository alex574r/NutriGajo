package com.example.nutrigajo.data.repository

import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.EquipoEjercicio
import com.example.nutrigajo.domain.model.EstadoEjercicio
import com.example.nutrigajo.domain.model.NivelEjercicio
import com.example.nutrigajo.domain.model.TipoEjercicio
import com.example.nutrigajo.domain.model.ZonaCuerpo
import com.example.nutrigajo.domain.model.duplicado
import com.example.nutrigajo.domain.model.limpio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EjerciciosMapaTest {
    private val completo = Ejercicio(
        id = "e1",
        autorId = "uid1",
        autorNombre = "Ana Pérez",
        titulo = "Remo con mancuerna",
        descripcion = "Trabaja la espalda media.",
        zona = ZonaCuerpo.ESPALDA,
        tipo = TipoEjercicio.FUERZA,
        nivel = NivelEjercicio.INTERMEDIO,
        equipo = EquipoEjercicio.MANCUERNAS,
        duracionMin = 15,
        pasos = listOf("Apoya la rodilla", "Jala hacia la cadera"),
        consejos = "Espalda recta",
        contraindicaciones = "Lesión de hombro",
        kcal = 90,
        fotoVersion = 1730000000000L,
        estado = EstadoEjercicio.EN_REVISION,
        creadoEn = 10L,
        actualizadoEn = 20L,
    )

    @Test
    fun un_ejercicio_vuelve_igual_tras_guardarse_y_leerse() {
        // Firestore devuelve los enteros como Long, aunque se hayan escrito como Int
        val leido = ejercicioDeMapa("e1", completo.aMapa().mapValues { (_, v) -> if (v is Int) v.toLong() else v })
        assertEquals(completo, leido)
    }

    @Test
    fun sin_foto_el_mapa_no_trae_la_version() {
        val mapa = completo.copy(fotoVersion = null).aMapa()
        assertTrue("fotoVersion" !in mapa)
        assertNull(ejercicioDeMapa("e1", mapa)?.fotoVersion)
    }

    @Test
    fun un_documento_de_una_version_futura_o_incompleto_no_se_muestra() {
        assertNull(ejercicioDeMapa("e1", completo.aMapa() + ("estado" to "ARCHIVADO")))
        assertNull(ejercicioDeMapa("e1", completo.aMapa() - "autorId"))
    }

    @Test
    fun un_valor_desconocido_cae_al_de_por_defecto() {
        val leido = ejercicioDeMapa("e1", completo.aMapa() + ("zona" to "CEREBRO") + ("nivel" to "DIOS"))
        assertEquals(ZonaCuerpo.CUERPO_COMPLETO, leido?.zona)
        assertEquals(NivelEjercicio.PRINCIPIANTE, leido?.nivel)
    }

    @Test
    fun limpio_quita_espacios_y_pasos_vacios() {
        val sucio = completo.copy(titulo = "  Remo  ", pasos = listOf(" uno ", "  ", "dos"))
        assertEquals("Remo", sucio.limpio().titulo)
        assertEquals(listOf("uno", "dos"), sucio.limpio().pasos)
    }

    @Test
    fun duplicar_da_un_borrador_nuevo_sin_foto_ni_fechas() {
        val copia = completo.copy(estado = EstadoEjercicio.PUBLICADO, publicadoEn = 30L, motivoRechazo = "x").duplicado()
        assertEquals("", copia.id)
        assertEquals(EstadoEjercicio.BORRADOR, copia.estado)
        assertNull(copia.fotoVersion)
        assertNull(copia.publicadoEn)
        assertNull(copia.motivoRechazo)
        assertEquals(0L, copia.creadoEn)
        assertEquals(completo.titulo, copia.titulo)
    }
}
