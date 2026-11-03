package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatilloTest {
    private val listo = Platillo(
        nombre = "Ensalada con pechuga",
        resumen = "Hojas verdes con pechuga a la plancha y limón.",
        momento = MomentoComida.COMIDA,
        proteinaG = 25,
        grasasG = 17,
        carbosG = 12,
        kcal = 300,
        minutos = 20,
        ingredientes = listOf("150 g de pechuga", "2 tazas de lechuga"),
        pasos = listOf("Dora la pechuga", "Mezcla todo"),
        fotoVersion = 1L,
    )

    @Test
    fun un_platillo_completo_se_puede_mandar_a_revision() {
        assertNull(listo.problemaParaRevision())
    }

    @Test
    fun cada_campo_obligatorio_que_falta_se_reporta() {
        assertEquals(ProblemaPlatillo.NOMBRE, listo.copy(nombre = "ab").problemaParaRevision())
        assertEquals(ProblemaPlatillo.RESUMEN, listo.copy(resumen = "corto").problemaParaRevision())
        assertEquals(ProblemaPlatillo.INGREDIENTES, listo.copy(ingredientes = emptyList()).problemaParaRevision())
        assertEquals(ProblemaPlatillo.INGREDIENTES, listo.copy(ingredientes = listOf("uno", "  ")).problemaParaRevision())
        assertEquals(ProblemaPlatillo.PASOS, listo.copy(pasos = emptyList()).problemaParaRevision())
        assertEquals(ProblemaPlatillo.PASOS, listo.copy(pasos = listOf("uno", " ")).problemaParaRevision())
        assertEquals(ProblemaPlatillo.FOTO, listo.copy(fotoVersion = null).problemaParaRevision())
    }

    @Test
    fun los_topes_se_respetan_y_el_primer_problema_gana_en_el_orden_del_enum() {
        assertEquals(ProblemaPlatillo.RESUMEN, listo.copy(resumen = "x".repeat(Platillo.MAX_RESUMEN + 1)).problemaParaRevision())
        assertEquals(ProblemaPlatillo.INGREDIENTES, listo.copy(ingredientes = List(Platillo.MAX_INGREDIENTES + 1) { "i$it" }).problemaParaRevision())
        assertEquals(ProblemaPlatillo.INGREDIENTES, listo.copy(ingredientes = listOf("x".repeat(Platillo.MAX_INGREDIENTE + 1))).problemaParaRevision())
        assertEquals(ProblemaPlatillo.PASOS, listo.copy(pasos = List(Platillo.MAX_PASOS + 1) { "p$it" }).problemaParaRevision())
        assertEquals(ProblemaPlatillo.PASOS, listo.copy(pasos = listOf("x".repeat(Platillo.MAX_PASO + 1))).problemaParaRevision())
        assertEquals(ProblemaPlatillo.MACROS, listo.copy(proteinaG = Platillo.MAX_MACRO_G + 1).problemaParaRevision())
        assertEquals(ProblemaPlatillo.MACROS, listo.copy(grasasG = -1).problemaParaRevision())
        assertEquals(ProblemaPlatillo.KCAL, listo.copy(kcal = Platillo.MAX_KCAL + 1).problemaParaRevision())
        assertEquals(ProblemaPlatillo.MINUTOS, listo.copy(minutos = 0).problemaParaRevision())
        assertEquals(ProblemaPlatillo.MINUTOS, listo.copy(minutos = Platillo.MAX_MINUTOS + 1).problemaParaRevision())
        assertEquals(ProblemaPlatillo.ETIQUETAS, listo.copy(etiquetas = EtiquetaPlatillo.entries.take(Platillo.MAX_ETIQUETAS + 1)).problemaParaRevision())
        // con foto y macros mal a la vez, el de macros va primero
        assertEquals(ProblemaPlatillo.MACROS, listo.copy(fotoVersion = null, carbosG = 9999).problemaParaRevision())
    }

    @Test
    fun un_borrador_solo_pide_el_nombre() {
        assertTrue(Platillo(nombre = "Té").copy(nombre = "Avena").puedeGuardarseComoBorrador())
        assertFalse(Platillo(nombre = "ab").puedeGuardarseComoBorrador())
        assertFalse(Platillo(nombre = "x".repeat(Platillo.MAX_NOMBRE + 1)).puedeGuardarseComoBorrador())
    }

    @Test
    fun limpio_recorta_textos_y_quita_vacios_y_etiquetas_repetidas() {
        val sucio = listo.copy(
            nombre = "  Ensalada  ",
            resumen = " Hojas verdes con limón. ",
            ingredientes = listOf(" lechuga ", "", "  "),
            pasos = listOf("  Mezcla  ", " "),
            etiquetas = listOf(EtiquetaPlatillo.VEGANO, EtiquetaPlatillo.VEGANO, EtiquetaPlatillo.SIN_GLUTEN),
        )
        val limpio = sucio.limpio()
        assertEquals("Ensalada", limpio.nombre)
        assertEquals("Hojas verdes con limón.", limpio.resumen)
        assertEquals(listOf("lechuga"), limpio.ingredientes)
        assertEquals(listOf("Mezcla"), limpio.pasos)
        assertEquals(listOf(EtiquetaPlatillo.VEGANO, EtiquetaPlatillo.SIN_GLUTEN), limpio.etiquetas)
    }

    @Test
    fun un_duplicado_es_un_borrador_sin_id_foto_motivo_ni_fechas() {
        val publicado = listo.copy(
            id = "abc", estado = EstadoPlatillo.RECHAZADO, motivoRechazo = "falta la receta", creadoEn = 5, actualizadoEn = 6, publicadoEn = 7,
        )
        val copia = publicado.duplicado()
        assertEquals("", copia.id)
        assertEquals(EstadoPlatillo.BORRADOR, copia.estado)
        assertNull(copia.fotoVersion)
        assertNull(copia.motivoRechazo)
        assertNull(copia.publicadoEn)
        assertEquals(0L, copia.creadoEn)
        assertEquals(0L, copia.actualizadoEn)
        assertEquals(listo.nombre, copia.nombre)
    }

    @Test
    fun las_kcal_se_comparan_con_4_4_9_y_un_15_por_ciento() {
        assertEquals(4 * 30 + 4 * 40 + 9 * 10, kcalSugeridas(proteinaG = 30, grasasG = 10, carbosG = 40))
        assertFalse(Platillo(proteinaG = 30, grasasG = 10, carbosG = 40, kcal = 380).kcalNoCuadran())
        assertTrue(Platillo(proteinaG = 30, grasasG = 10, carbosG = 40, kcal = 600).kcalNoCuadran())
        assertTrue(Platillo(proteinaG = 30, grasasG = 10, carbosG = 40, kcal = 200).kcalNoCuadran())
        assertFalse("sin macros no se avisa", Platillo(kcal = 5).kcalNoCuadran())
    }

    @Test
    fun coincide_busca_en_nombre_resumen_e_ingredientes_y_filtra_por_momento_y_etiqueta() {
        val p = listo.copy(etiquetas = listOf(EtiquetaPlatillo.ALTO_PROTEINA), ingredientes = listOf("Queso de lácteos"))
        assertTrue(p.coincide("", null, null))
        assertTrue(p.coincide("ENSALADA", null, null))
        assertTrue(p.coincide("limon", null, null)) // resumen, sin acento
        assertTrue(p.coincide("lacteos", null, null)) // ingrediente, sin acento
        assertFalse(p.coincide("pescado", null, null))
        assertTrue(p.coincide("", MomentoComida.COMIDA, EtiquetaPlatillo.ALTO_PROTEINA))
        assertFalse(p.coincide("", MomentoComida.CENA, null))
        assertFalse(p.coincide("", null, EtiquetaPlatillo.VEGANO))
    }

    @Test
    fun el_texto_para_el_plan_cabe_en_el_tope_del_menu_y_conserva_las_kcal() {
        assertEquals("Té: hierbabuena · 5 kcal", Platillo(nombre = "Té", kcal = 5, ingredientes = listOf("hierbabuena")).textoParaPlan())
        assertEquals("Agua", Platillo(nombre = "Agua").textoParaPlan())
        val largo = Platillo(nombre = "Plato", kcal = 400, ingredientes = List(20) { "ingrediente número $it con nombre largo" })
        val texto = largo.textoParaPlan()
        assertTrue(texto.length <= PlanContenido.MAX_MENU)
        assertTrue(texto.contains("…"))
        assertTrue(texto.endsWith(" · 400 kcal"))
    }

    @Test
    fun alergia_con_acentos_y_mayusculas_esconde_el_platillo() {
        val p = Platillo(nombre = "Ensalada", ingredientes = listOf("Queso de Lácteos de cabra", "lechuga"))
        assertTrue(listOf(p).paraPaciente(listOf("lacteos"), null).isEmpty())
        assertTrue(listOf(Platillo(nombre = "Mole", ingredientes = listOf("CACAHUATE"))).paraPaciente(listOf("Cacahuate"), null).isEmpty())
        assertTrue("también por el nombre", listOf(Platillo(nombre = "Sopa de camarón")).paraPaciente(listOf("CAMARON"), null).isEmpty())
        assertEquals("alergias en blanco no esconden nada", 1, listOf(p).paraPaciente(listOf("", "  "), null).size)
        assertEquals("otra alergia no lo esconde", 1, listOf(p).paraPaciente(listOf("cacahuate"), null).size)
    }

    @Test
    fun la_dieta_de_plantas_pone_primero_lo_vegetariano_y_no_quita_nada() {
        val carne = Platillo(id = "a", nombre = "Carne")
        val vegano = Platillo(id = "b", nombre = "Lentejas", etiquetas = listOf(EtiquetaPlatillo.VEGANO))
        val vegetariano = Platillo(id = "c", nombre = "Omelette", etiquetas = listOf(EtiquetaPlatillo.VEGETARIANO))
        val lista = listOf(carne, vegano, vegetariano)
        assertEquals(listOf("b", "c", "a"), lista.paraPaciente(emptyList(), Dieta.PLANTAS).map { it.id })
        assertEquals(listOf("a", "b", "c"), lista.paraPaciente(emptyList(), Dieta.TRADICIONAL).map { it.id })
        assertEquals(listOf("a", "b", "c"), lista.paraPaciente(emptyList(), null).map { it.id })
        val bajo = Platillo(id = "d", nombre = "Pollo", etiquetas = listOf(EtiquetaPlatillo.BAJO_CARBOHIDRATOS))
        assertEquals("d", (lista + bajo).paraPaciente(emptyList(), Dieta.BAJA_CARBOHIDRATOS).first().id)
    }

    @Test
    fun un_platillo_elegido_se_convierte_en_item_del_banco_con_texto_que_cabe() {
        val p = Platillo(nombre = "Ensalada con pechuga", momento = MomentoComida.CENA, kcal = 415, ingredientes = List(20) { "ingrediente número $it con nombre largo" })
        val item = p.comoItemDelBanco()
        assertEquals(TipoBanco.COMIDA, item.tipo)
        assertEquals(MomentoComida.CENA, item.momento)
        assertEquals("Ensalada con pechuga", item.nombre)
        assertTrue(item.texto.length <= PlanContenido.MAX_MENU)
        assertEquals("al ponerlo en el plan no se repite el nombre", item.texto, item.textoParaPlan())
        assertTrue(Platillo(nombre = "x".repeat(Platillo.MAX_NOMBRE)).comoItemDelBanco().nombre.length <= ItemBanco.MAX_NOMBRE)
    }

    private fun oculto(alergia: String, ingrediente: String) =
        listOf(Platillo(nombre = "Plato", ingredientes = listOf(ingrediente))).paraPaciente(listOf(alergia), null).isEmpty()

    @Test
    fun las_alergias_que_ofrece_el_expediente_esconden_los_ingredientes_que_las_llevan() {
        assertTrue(oculto("Lactosa", "1 taza de leche"))
        assertTrue(oculto("Lactosa", "Queso panela"))
        assertTrue(oculto("Gluten", "2 rebanadas de pan"))
        assertTrue(oculto("Gluten", "Harina de trigo"))
        assertTrue(oculto("Frutos secos", "Un puño de nueces"))
        assertTrue(oculto("Frutos secos", "10 almendras"))
        assertTrue(oculto("Cacahuate", "Crema de cacahuate"))
        assertTrue(oculto("Mariscos", "200 g de camarones"))
        assertTrue(oculto("Huevo", "2 huevos"))
        assertTrue(oculto("Soya", "Tofu firme"))
        assertTrue("una alergia escrita a mano busca esa palabra", oculto("penicilina", "Salsa con Penicilina"))
    }

    @Test
    fun las_palabras_cuentan_enteras_y_en_singular_o_en_plural() {
        assertTrue("el paciente escribe el plural", oculto("Cacahuates", "cacahuate"))
        assertTrue("el ingrediente va en singular", oculto("Mariscos", "marisco"))
        assertTrue("nueces es el plural de nuez", oculto("nuez", "nueces"))
        assertFalse("«pan» no es «panela»", oculto("Gluten", "Queso panela"))
        assertFalse(oculto("Huevo", "Aceite de oliva"))
        assertFalse(oculto("Lactosa", "Pechuga de pollo"))
        assertFalse("una alergia de varias palabras sin sinónimos solo busca la frase", oculto("frutos rojos", "Manzana y frutos secos"))
    }

    @Test
    fun sin_saber_las_alergias_no_se_muestra_ningun_platillo() {
        val mole = Platillo(id = "m", nombre = "Mole", ingredientes = listOf("cacahuate"))
        val ensalada = Platillo(id = "e", nombre = "Ensalada", ingredientes = listOf("lechuga"))
        val lista = listOf(mole, ensalada)
        assertTrue("el expediente aún carga o no se pudo leer", lista.paraPaciente(emptyList(), null, expedienteListo = false).isEmpty())
        assertEquals("ya leído y sin alergias se ve todo", listOf("m", "e"), lista.paraPaciente(emptyList(), null, expedienteListo = true).map { it.id })
        assertEquals("ya leído y con la alergia se esconde", listOf("e"), lista.paraPaciente(listOf("Cacahuate"), null, expedienteListo = true).map { it.id })
    }
}
