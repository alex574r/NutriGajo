package com.example.nutrigajo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlatilloFotoTest {
    private fun grupo(nombre: String, vararg ingredientes: String) = Platillo(nombre = nombre, ingredientes = ingredientes.toList()).grupoParaFoto()

    @Test
    fun los_seis_platillos_sembrados_caen_en_un_grupo_que_les_va() {
        assertEquals(GrupoAlimento.CEREALES, grupo("Avena con fruta y nueces", "½ taza de avena", "1 manzana picada"))
        assertEquals(GrupoAlimento.ANIMAL, grupo("Huevos con espinacas", "2 huevos", "1 taza de espinacas"))
        assertEquals(GrupoAlimento.VERDURAS, grupo("Ensalada con pechuga", "150 g de pechuga de pollo", "2 tazas de lechuga"))
        assertEquals(GrupoAlimento.VERDURAS, grupo("Ensalada césar", "150 g de pechuga de pollo"))
        assertEquals(GrupoAlimento.VERDURAS, grupo("Crema de verduras", "2 calabacitas", "2 tazas de caldo de verduras"))
        assertEquals(GrupoAlimento.ANIMAL, grupo("Pescado al horno con ensalada", "1 filete de pescado blanco"))
    }

    @Test
    fun el_primer_alimento_del_nombre_decide() {
        // aunque «fruta» y «avena» sean de grupos distintos, manda el que va primero
        assertEquals(GrupoAlimento.FRUTAS, grupo("Fruta con avena"))
        assertEquals(GrupoAlimento.CEREALES, grupo("Avena con fruta"))
    }

    @Test
    fun el_plural_y_los_acentos_no_importan() {
        assertEquals(GrupoAlimento.LEGUMINOSAS, grupo("Frijoles con queso"))
        assertEquals(GrupoAlimento.FRUTAS, grupo("Limones y PIÑA"))
        assertEquals(GrupoAlimento.GRASAS_PROTEINA, grupo("Mezcla de nueces"))
    }

    @Test
    fun la_palabra_cuenta_entera() {
        // «pan» no es «panela» ni «pantalla»
        assertNull(grupo("Panela dulce"))
        assertEquals(GrupoAlimento.CEREALES, grupo("Pan integral"))
    }

    @Test
    fun si_el_nombre_no_dice_nada_se_mira_el_primer_ingrediente_que_se_reconoce() {
        assertEquals(GrupoAlimento.LACTEOS, grupo("Desayuno ligero", "1 taza de leche", "una pizca de sal"))
    }

    @Test
    fun sin_nada_reconocible_no_hay_grupo() {
        assertNull(grupo("Té de jengibre", "agua", "jengibre"))
        assertNull(Platillo().grupoParaFoto())
    }
}
