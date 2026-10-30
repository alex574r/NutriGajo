package com.example.nutrigajo.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.math.max

// El estilo «cristal líquido» de Bencho (bloque «Glass bubble, liquid», MIT) para lo que está en un rango transparente o de
// blanco con poca opacidad: chips y botones sobre fotos. Aquel es un shader que dobla lo que tiene debajo; eso necesita leer la
// foto de detrás, que Compose no entrega a un botón (y AGSL pide Android 13), así que aquí va lo que sí se puede dibujar en
// cualquier teléfono, con las mismas reglas de luz:
//
//  · el cuerpo es un velo translúcido más claro arriba a la izquierda, de donde viene la luz (L = (−0.55, −0.85) en el shader);
//  · la luz se queda en una banda fina pegada al borde, fuerte donde el borde mira a la luz y con un brillo débil en el lado
//    opuesto, por donde la luz entró y salió otra vez (`far` en el shader);
//  · el color se parte un pelo en el borde, rojo hacia un lado y azul hacia el otro (`fringe`), aquí dos filos de medio dp;
//  · sin sombra propia: la app no lleva sombras decorativas, y lo que el cristal proyecta lo da la foto que tiene debajo.
//
// Se dibuja DETRÁS del contenido y sin recortar: va antes del `clip` de quien lo usa, para que el filo del borde no se corte.

// el velo de siempre de la app sobre fotos: gris claro, no blanco puro, para que el texto blanco encima conserve su contraste
private val VELO = Color(0xFFD6D6D6)
private val FILO_FRIO = Color(0xFF8DF3FF)
private val FILO_CALIDO = Color(0xFFFF9BEA)

fun Modifier.cristal(forma: Shape, velo: Color = VELO): Modifier = drawBehind {
    val ancho = size.width
    val alto = size.height
    val borde = 1.1.dp.toPx()
    val contorno = Path().apply { addOutline(forma.createOutline(size, layoutDirection, this@drawBehind)) }
    val diagonal = Offset(ancho, alto)

    // el cuerpo, con la luz arriba a la izquierda
    drawPath(contorno, Brush.linearGradient(listOf(velo.copy(alpha = 0.40f), velo.copy(alpha = 0.16f)), start = Offset.Zero, end = diagonal))
    drawPath(
        contorno,
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.26f), Color.Transparent),
            center = Offset(ancho * 0.28f, alto * 0.05f),
            radius = max(ancho, alto) * 0.8f,
        ),
    )
    // el color partido en el borde
    translate(left = -0.6.dp.toPx()) { drawPath(contorno, FILO_FRIO.copy(alpha = 0.16f), style = Stroke(borde)) }
    translate(left = 0.6.dp.toPx()) { drawPath(contorno, FILO_CALIDO.copy(alpha = 0.16f), style = Stroke(borde)) }
    // el filo de luz: fuerte donde mira a la luz, tenue en medio y un brillo flojo en el lado de enfrente
    drawPath(
        contorno,
        Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.9f),
            0.4f to Color.White.copy(alpha = 0.10f),
            0.62f to Color.White.copy(alpha = 0.08f),
            1f to Color.White.copy(alpha = 0.5f),
            start = Offset.Zero,
            end = diagonal,
        ),
        style = Stroke(borde),
    )
    // la banda de luz un poco más adentro
    val dentro = 1.6.dp.toPx()
    if (ancho > dentro * 2 && alto > dentro * 2) {
        inset(dentro) {
            val interior = Path().apply { addOutline(forma.createOutline(size, layoutDirection, this@inset)) }
            drawPath(interior, Color.White.copy(alpha = 0.14f), style = Stroke(1.dp.toPx()))
        }
    }
}
