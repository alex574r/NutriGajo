package com.example.nutrigajo.ui.paciente.inicio

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.nutrigajo.R

// ponytail: todo este archivo es de muestra, tomado del diseño. Lo reemplazan las mediciones, los planes,
// las rutinas y los mensajes reales cuando existan sus modelos y la sincronización con Firestore.

data class ResumenDia(val score: Int, val avance: Int, val hidratacionMl: Int, val calorias: Int)

data class Rutina(
    @DrawableRes val foto: Int,
    val titulo: String,
    val series: Int,
    val minutos: Int,
    val kcal: Int,
    val intensidad: String,
)

data class Comida(@DrawableRes val foto: Int, val titulo: String, val proteinaG: Int, val grasasG: Int, val kcal: Int)

data class EjercicioExtra(
    @DrawableRes val foto: Int,
    val titulo: String,
    val subtitulo: String,
    val valoracion: String,
    val favoritos: Int,
    val vistas: String,
)

enum class Rango(@StringRes val etiqueta: Int) {
    DIA(R.string.rango_dia),
    SEMANA(R.string.rango_semana),
    MES(R.string.rango_mes),
    ANIO(R.string.rango_anio),
    TODO(R.string.rango_todo),
}

// valores en kcal dentro de 400..900 (el rango de la gráfica)
data class SerieActividad(val valores: List<Float>, val total: Int, val variacion: Int)

val resumenDia = ResumenDia(score = 80, avance = 32, hidratacionMl = 781, calorias = 1503)

val rutinaDeHoy = Rutina(
    foto = R.drawable.inicio_rutina,
    titulo = "Ejercicio de estiramiento",
    series = 8,
    minutos = 25,
    kcal = 415,
    intensidad = "Intenso",
)

val comidas = listOf(
    Comida(R.drawable.inicio_comida, "Ensalada con pechuga", proteinaG = 25, grasasG = 17, kcal = 415),
    Comida(R.drawable.inicio_comida, "Ensalada césar", proteinaG = 31, grasasG = 21, kcal = 480),
)

val seriesActividad = mapOf(
    Rango.DIA to SerieActividad(
        listOf(800f, 790f, 660f, 640f, 790f, 830f, 900f, 840f, 780f, 720f, 430f, 420f, 550f, 830f, 840f, 700f),
        total = 1548,
        variacion = 285,
    ),
    Rango.SEMANA to SerieActividad(listOf(620f, 700f, 540f, 810f, 760f, 690f, 850f), total = 10420, variacion = 640),
    Rango.MES to SerieActividad(
        listOf(700f, 640f, 720f, 600f, 780f, 820f, 700f, 650f, 730f, 810f, 760f, 690f),
        total = 43870,
        variacion = -1200,
    ),
    Rango.ANIO to SerieActividad(
        listOf(580f, 640f, 700f, 660f, 720f, 780f, 840f, 810f, 760f, 790f, 830f, 870f),
        total = 512300,
        variacion = 8400,
    ),
    Rango.TODO to SerieActividad(listOf(520f, 600f, 680f, 740f, 790f, 830f), total = 1204500, variacion = 21000),
)

val ejerciciosExtra = listOf(
    EjercicioExtra(R.drawable.inicio_yoga, "Yoga y relajación", "Sesión tranquila", "4.1", 251, "121K"),
    EjercicioExtra(R.drawable.inicio_yoga, "Estiramiento matutino", "Sesión suave", "4.4", 198, "86K"),
    EjercicioExtra(R.drawable.inicio_yoga, "Movilidad de cadera", "Sesión corta", "4.0", 164, "64K"),
)

const val FRASE_DEL_DIA = "El dolor que sientes hoy será la fuerza que sentirás mañana"

// ---- pantallas de métricas

// lo que suma el «+» del agua; la meta del día es la de MetasDiarias
const val VASO_ML = 250

const val IMC_MUESTRA = 29.8f

// 5 semanas de lunes a domingo: 0 sin dato, 1..3 sobrepeso (de menos a más), 4 bajo peso, 5 normal
val mapaImc = listOf(
    0, 0, 4, 1, 0, 0, 2,
    1, 2, 2, 3, 1, 2, 3,
    2, 3, 3, 3, 3, 2, 3,
    1, 2, 3, 2, 1, 5, 0,
    1, 1, 2, 1, 0, 0, 0,
)

// porcentaje y gramos del mes por macronutriente
data class Macro(@StringRes val nombre: Int, val porcentaje: Int, val gramos: Int, @DrawableRes val icono: Int)

val macros = listOf(
    Macro(R.string.macro_grasas, 20, 34, R.drawable.ic_corazon),
    Macro(R.string.macro_proteinas, 30, 116, R.drawable.ic_corredor),
    Macro(R.string.macro_carbohidratos, 40, 155, R.drawable.ic_llama),
    Macro(R.string.macro_fibra, 10, 15, R.drawable.ic_montana),
)

// lo que lleva el paciente hoy; sus anillos son el avance de cada cifra hacia su meta (MetasDiarias)
data class Pasos(val total: Int, val kcal: Int, val km: Float, val minutos: Int)

val pasosMuestra = Pasos(total = 2574, kcal = 578, km = 1.9f, minutos = 25)

// lo que resume la pantalla de score tras una sesión
data class SesionCompletada(val scoreSumado: Int, val minutos: Int, val kcal: Int, val kg: Int, val sugerencias: Int)

val sesionMuestra = SesionCompletada(scoreSumado = 8, minutos = 25, kcal = 158, kg = 30, sugerencias = 12)
