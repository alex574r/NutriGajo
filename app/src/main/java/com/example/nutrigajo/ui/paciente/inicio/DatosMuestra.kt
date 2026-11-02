package com.example.nutrigajo.ui.paciente.inicio

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import com.example.nutrigajo.R
import java.time.LocalDate

// ponytail: todo este archivo es de muestra, tomado del diseño. Lo reemplazan las mediciones, los planes,
// las rutinas y los mensajes reales cuando existan sus modelos y la sincronización con Firestore.

data class ResumenDia(val score: Int, val avance: Int, val hidratacionMl: Int, val calorias: Int)

enum class TipoComida(@StringRes val etiqueta: Int) {
    DESAYUNO(R.string.comida_desayuno),
    ALMUERZO(R.string.comida_almuerzo),
    CENA(R.string.comida_cena),
}

data class Comida(
    @DrawableRes val foto: Int,
    val titulo: String,
    val proteinaG: Int,
    val grasasG: Int,
    val kcal: Int,
    val carbosG: Int = 0,
    val minutos: Int = 0,
    val tipo: TipoComida = TipoComida.ALMUERZO,
    val resumen: String = "",
    val ingredientes: List<String> = emptyList(),
    // los pasos de la receta: título y texto
    val pasos: List<Pair<String, String>> = emptyList(),
    // la foto que se eligió al agregarla a mano; sin ella se ve `foto`
    val miniatura: ImageBitmap? = null,
    // el día en que se agregó a mano; null es de su plan y se ve todos los días
    val dia: LocalDate? = null,
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

// El plan de comidas de cada día (el mismo todos los días); la sección del inicio enseña las dos del almuerzo. La app solo trae una
// foto de comida, así que todas llevan la misma hasta que haya las reales.
val platos = listOf(
    Comida(
        R.drawable.inicio_comida, "Avena con fruta y nueces", proteinaG = 14, grasasG = 9, kcal = 320, carbosG = 48, minutos = 10,
        tipo = TipoComida.DESAYUNO,
        resumen = "Un desayuno que sacia sin pesar: la avena da energía de liberación lenta y la fruta, fibra y vitaminas.",
        ingredientes = listOf("½ taza de avena", "1 taza de leche descremada", "1 manzana picada", "Un puño de nueces", "Canela al gusto"),
        pasos = listOf(
            "Cocer la avena" to "Calienta la leche con la avena a fuego bajo unos 5 minutos, moviendo de vez en cuando.",
            "Preparar la fruta" to "Pica la manzana y las nueces mientras la avena se espesa.",
            "Servir" to "Pon la avena en un plato hondo, agrega la fruta, las nueces y la canela.",
        ),
    ),
    Comida(
        R.drawable.inicio_comida, "Huevos con espinacas", proteinaG = 20, grasasG = 14, kcal = 240, carbosG = 6, minutos = 12,
        tipo = TipoComida.DESAYUNO,
        resumen = "Proteína completa con hierro y poco carbohidrato, rápido de hacer antes de salir.",
        ingredientes = listOf("2 huevos", "1 taza de espinacas", "1 cucharadita de aceite de oliva", "Sal y pimienta"),
        pasos = listOf(
            "Saltear" to "Dora las espinacas en el aceite durante un minuto.",
            "Cocinar los huevos" to "Agrega los huevos batidos y mueve hasta que cuajen.",
            "Servir" to "Sazona con sal y pimienta y sirve caliente.",
        ),
    ),
    Comida(
        R.drawable.inicio_comida, "Ensalada con pechuga", proteinaG = 25, grasasG = 17, kcal = 415, carbosG = 12, minutos = 20,
        tipo = TipoComida.ALMUERZO,
        resumen = "Hojas verdes con pechuga a la plancha: mucha proteína, poca grasa y un aderezo ligero de limón.",
        ingredientes = listOf("150 g de pechuga de pollo", "2 tazas de lechuga", "½ pepino", "Jitomate cherry", "Jugo de limón y aceite de oliva"),
        pasos = listOf(
            "Cocer el pollo" to "Sazona la pechuga y dórala en la plancha 5 minutos por lado.",
            "Armar la ensalada" to "Mezcla la lechuga, el pepino y los jitomates en un plato.",
            "Aderezar" to "Rebana el pollo, ponlo encima y baña todo con limón y un hilo de aceite.",
        ),
    ),
    Comida(
        R.drawable.inicio_comida, "Ensalada césar", proteinaG = 31, grasasG = 21, kcal = 480, carbosG = 18, minutos = 15,
        tipo = TipoComida.ALMUERZO,
        resumen = "La clásica, con pollo, crutones y queso parmesano; el aderezo va aparte para medirlo.",
        ingredientes = listOf("150 g de pechuga de pollo", "2 tazas de lechuga romana", "2 cucharadas de queso parmesano", "Crutones", "Aderezo césar ligero"),
        pasos = listOf(
            "Cocer el pollo" to "Dora la pechuga en la plancha y déjala reposar un minuto.",
            "Armar" to "Pon la lechuga en un plato con el pollo rebanado, los crutones y el queso.",
            "Aderezar" to "Agrega el aderezo al final y mezcla con cuidado.",
        ),
    ),
    Comida(
        R.drawable.inicio_comida, "Crema de verduras", proteinaG = 8, grasasG = 6, kcal = 210, carbosG = 22, minutos = 25,
        tipo = TipoComida.CENA,
        resumen = "Ligera para la noche: calabacita, zanahoria y apio licuados, sin crema.",
        ingredientes = listOf("2 calabacitas", "1 zanahoria", "1 tallo de apio", "½ cebolla", "2 tazas de caldo de verduras"),
        pasos = listOf(
            "Cocer" to "Hierve las verduras en el caldo hasta que estén suaves.",
            "Licuar" to "Licúa todo hasta que quede una crema tersa.",
            "Servir" to "Calienta de nuevo, rectifica la sal y sirve con pimienta.",
        ),
    ),
    Comida(
        R.drawable.inicio_comida, "Pescado al horno con ensalada", proteinaG = 30, grasasG = 12, kcal = 360, carbosG = 9, minutos = 30,
        tipo = TipoComida.CENA,
        resumen = "Pescado blanco al horno con limón y una ensalada fresca: proteína magra y grasas buenas.",
        ingredientes = listOf("1 filete de pescado blanco", "1 limón", "1 diente de ajo", "Ensalada de hojas verdes", "Aceite de oliva"),
        pasos = listOf(
            "Preparar" to "Marina el filete con limón, ajo, sal y un poco de aceite.",
            "Hornear" to "Hornea a 190 °C unos 15 minutos, hasta que se desmenuce con el tenedor.",
            "Servir" to "Acompaña con la ensalada de hojas verdes.",
        ),
    ),
)

val comidas = platos.filter { it.tipo == TipoComida.ALMUERZO }

// lo que «reconoce» el escáner de muestra
val comidaEscaneada = Comida(
    R.drawable.grupo_frutas, "Granada", proteinaG = 2, grasasG = 1, kcal = 83, carbosG = 19,
    tipo = TipoComida.DESAYUNO,
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

// Los pasos que ven la tarjeta y la pantalla de pasos: los de muestra, o los del reloj cuando está conectado (los da el inicio).
val LocalPasos = androidx.compose.runtime.compositionLocalOf { pasosMuestra }

// lo que resume la pantalla de score tras una sesión
data class SesionCompletada(val scoreSumado: Int, val minutos: Int, val kcal: Int, val kg: Int, val sugerencias: Int)

val sesionMuestra = SesionCompletada(scoreSumado = 8, minutos = 25, kcal = 158, kg = 30, sugerencias = 12)
