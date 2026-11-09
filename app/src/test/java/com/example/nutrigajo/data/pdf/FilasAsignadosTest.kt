package com.example.nutrigajo.data.pdf

import com.example.nutrigajo.domain.model.EjercicioAsignado
import org.junit.Assert.assertEquals
import org.junit.Test

class FilasAsignadosTest {
    private val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    private val nombreDia = { dia: Int -> dias[dia - 1] }

    private fun a(dia: Int, id: String, series: Int? = null, repeticiones: Int? = null, minutos: Int? = null, nota: String = "") =
        EjercicioAsignado(dia = dia, id = id, titulo = "Ejercicio $id", series = series, repeticiones = repeticiones, minutos = minutos, nota = nota)

    @Test
    fun salen_por_dia_con_el_lunes_primero_y_en_el_orden_del_plan_dentro_de_cada_dia() {
        val filas = filasAsignadas(listOf(a(3, "x", series = 3), a(1, "y", series = 3), a(3, "z", series = 3)), nombreDia)
        assertEquals(listOf("Lunes", "Miércoles", "Miércoles"), filas.map { it[0] })
        assertEquals(listOf("Ejercicio y", "Ejercicio x", "Ejercicio z"), filas.map { it[1] })
    }

    @Test
    fun la_tercera_columna_es_la_dosis_y_la_cuarta_la_nota() {
        val filas = filasAsignadas(
            listOf(
                a(1, "a", series = 3, repeticiones = 10, nota = "con calma"),
                a(2, "b", minutos = 15),
                a(3, "c"),
            ),
            nombreDia,
        )
        assertEquals(listOf("Lunes", "Ejercicio a", "3 × 10", "con calma"), filas[0])
        assertEquals(listOf("Martes", "Ejercicio b", "15 min", ""), filas[1])
        assertEquals("—", filas[2][2])
    }

    @Test
    fun sin_asignados_no_hay_filas() {
        assertEquals(emptyList<List<String>>(), filasAsignadas(emptyList(), nombreDia))
    }
}
