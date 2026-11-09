package com.example.nutrigajo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActualizacionTest {
    private val sha = "a".repeat(64)

    // el manifiesto que publica el script, con los campos que se quieran cambiar (null quita el campo); los valores van como JSON crudo
    private fun manifiesto(vararg cambios: Pair<String, String?>): String {
        val base = linkedMapOf<String, String?>(
            "versionCode" to "2",
            "versionName" to "\"1.1\"",
            "sha256" to "\"$sha\"",
            "bytes" to "13349873",
            "notas" to "[\"Citas agendadas\",\"Arreglos de rendimiento\"]",
            "minVersionCode" to "0",
            "publicadaEn" to "1793900000000",
        )
        cambios.forEach { (clave, valor) -> base[clave] = valor }
        return base.filterValues { it != null }.entries.joinToString(",", "{", "}") { "\"${it.key}\":${it.value}" }
    }

    private fun act(versionCode: Long = 2, minVersionCode: Long = 0) =
        Actualizacion(versionCode, "1.1", sha, 1000, emptyList(), minVersionCode, 1L)

    @Test
    fun un_manifiesto_valido_se_lee_completo() {
        val a = actualizacionDeJson(manifiesto())!!
        assertEquals(2L, a.versionCode)
        assertEquals("1.1", a.versionName)
        assertEquals(sha, a.sha256)
        assertEquals(13349873L, a.bytes)
        assertEquals(listOf("Citas agendadas", "Arreglos de rendimiento"), a.notas)
        assertEquals(0L, a.minVersionCode)
        assertEquals(1793900000000L, a.publicadaEn)
    }

    @Test
    fun un_manifiesto_sin_minVersionCode_lo_deja_en_cero() {
        assertEquals(0L, actualizacionDeJson(manifiesto("minVersionCode" to null))!!.minVersionCode)
        assertEquals(2L, actualizacionDeJson(manifiesto("minVersionCode" to "2"))!!.minVersionCode)
    }

    @Test
    fun cada_campo_malo_lo_descarta() {
        val malos = mapOf(
            "versionCode 0" to manifiesto("versionCode" to "0", "minVersionCode" to "0"),
            "versionCode negativo" to manifiesto("versionCode" to "-1", "minVersionCode" to "0"),
            "versionCode con decimales" to manifiesto("versionCode" to "2.5"),
            "versionCode como texto" to manifiesto("versionCode" to "\"2\""),
            "sin versionCode" to manifiesto("versionCode" to null),
            "versionName vacío" to manifiesto("versionName" to "\"\""),
            "versionName de espacios" to manifiesto("versionName" to "\"   \""),
            "versionName de 41 caracteres" to manifiesto("versionName" to "\"${"v".repeat(41)}\""),
            "sha256 de 63 caracteres" to manifiesto("sha256" to "\"${"a".repeat(63)}\""),
            "sha256 con mayúsculas" to manifiesto("sha256" to "\"${"A".repeat(64)}\""),
            "sha256 con una letra no hexadecimal" to manifiesto("sha256" to "\"${"a".repeat(63)}g\""),
            "sin sha256" to manifiesto("sha256" to null),
            "bytes 0" to manifiesto("bytes" to "0"),
            "bytes de más" to manifiesto("bytes" to "${MAX_APK_BYTES + 1}"),
            "bytes como texto" to manifiesto("bytes" to "\"1000\""),
            "11 notas" to manifiesto("notas" to (1..11).joinToString(",", "[", "]") { "\"nota $it\"" }),
            "una nota de 201 caracteres" to manifiesto("notas" to "[\"${"n".repeat(201)}\"]"),
            "una nota que no es texto" to manifiesto("notas" to "[1]"),
            "notas que no son una lista" to manifiesto("notas" to "\"hola\""),
            "sin notas" to manifiesto("notas" to null),
            "minVersionCode mayor que versionCode" to manifiesto("minVersionCode" to "3"),
            "minVersionCode negativo" to manifiesto("minVersionCode" to "-1"),
            "sin publicadaEn" to manifiesto("publicadaEn" to null),
            "una lista en vez de un objeto" to "[]",
            "texto que no es JSON" to "no soy json",
            "vacío" to "",
        )
        malos.forEach { (que, texto) -> assertNull("debía descartarse: $que", actualizacionDeJson(texto)) }
        // y en el límite sí pasa
        assertNotNull(actualizacionDeJson(manifiesto("bytes" to "$MAX_APK_BYTES", "versionName" to "\"${"v".repeat(40)}\"")))
        assertNotNull(actualizacionDeJson(manifiesto("notas" to (1..10).joinToString(",", "[", "]") { "\"${"n".repeat(200)}\"" })))
    }

    @Test
    fun un_manifiesto_guardado_se_lee_igual_que_el_publicado() {
        val a = actualizacionDeJson(manifiesto("minVersionCode" to "2", "notas" to "[\"Una\",\"Con \\\"comillas\\\"\"]"))!!
        assertEquals(a, actualizacionDeJson(jsonDe(a)))
    }

    @Test
    fun una_obligatoria_guardada_se_sigue_exigiendo_sin_red_y_lo_demas_no() {
        val obligatoria = jsonDe(act(versionCode = 3, minVersionCode = 2))
        assertEquals(3L, obligatoriaGuardada(versionActual = 1, guardado = obligatoria)?.versionCode)
        // ya actualizado, o la publicada solo es opcional, o no hay nada guardado, o lo guardado está roto: no hay obligación
        assertNull(obligatoriaGuardada(versionActual = 3, guardado = obligatoria))
        assertNull(obligatoriaGuardada(versionActual = 2, guardado = obligatoria))
        assertNull(obligatoriaGuardada(versionActual = 1, guardado = jsonDe(act(versionCode = 3, minVersionCode = 0))))
        assertNull(obligatoriaGuardada(versionActual = 1, guardado = null))
        assertNull(obligatoriaGuardada(versionActual = 1, guardado = "no soy json"))
    }

    @Test
    fun una_instalacion_sin_noticias_pasados_5_segundos_se_da_por_abandonada() {
        assertFalse(instalacionSinNoticias(desde = 1_000, ahora = 1_000 + ESPERA_INSTALACION_MS))
        assertTrue(instalacionSinNoticias(desde = 1_000, ahora = 1_000 + ESPERA_INSTALACION_MS + 1))
        assertEquals(5_000L, ESPERA_INSTALACION_MS)
    }

    @Test
    fun estado_al_dia_si_la_publicada_no_es_mayor() {
        assertEquals(EstadoActualizacion.AL_DIA, estadoDe(2, act(versionCode = 2)))
        assertEquals(EstadoActualizacion.AL_DIA, estadoDe(5, act(versionCode = 2)))
        // aunque la mínima diga otra cosa: nunca se ofrece retroceder
        assertEquals(EstadoActualizacion.AL_DIA, estadoDe(5, act(versionCode = 2, minVersionCode = 2)))
    }

    @Test
    fun estado_disponible_y_obligatoria() {
        assertEquals(EstadoActualizacion.DISPONIBLE, estadoDe(1, act(versionCode = 2)))
        assertEquals(EstadoActualizacion.DISPONIBLE, estadoDe(1, act(versionCode = 3, minVersionCode = 1)))
        assertEquals(EstadoActualizacion.OBLIGATORIA, estadoDe(1, act(versionCode = 3, minVersionCode = 2)))
        assertEquals(EstadoActualizacion.OBLIGATORIA, estadoDe(1, act(versionCode = 2, minVersionCode = 2)))
    }

    @Test
    fun toca_consultar_a_las_12_horas_y_siempre_si_nunca_se_consulto_o_el_reloj_va_atras() {
        val ahora = 1_000_000_000_000L
        assertTrue(tocaConsultar(ahora, 0L))
        assertFalse(tocaConsultar(ahora, ahora - CADA_MS + 1))
        assertTrue(tocaConsultar(ahora, ahora - CADA_MS))
        assertTrue(tocaConsultar(ahora, ahora + 1))
        assertEquals(12L * 3_600_000, CADA_MS)
    }

    @Test
    fun una_version_pospuesta_no_se_vuelve_a_mostrar_pero_la_obligatoria_si() {
        assertTrue(debeMostrar(EstadoActualizacion.DISPONIBLE, versionCode = 2, aplazada = 0))
        assertFalse(debeMostrar(EstadoActualizacion.DISPONIBLE, versionCode = 2, aplazada = 2))
        // otra versión nueva vuelve a avisar
        assertTrue(debeMostrar(EstadoActualizacion.DISPONIBLE, versionCode = 3, aplazada = 2))
        assertTrue(debeMostrar(EstadoActualizacion.OBLIGATORIA, versionCode = 2, aplazada = 2))
        assertFalse(debeMostrar(EstadoActualizacion.AL_DIA, versionCode = 2, aplazada = 0))
    }
}
