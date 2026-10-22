package com.example.nutrigajo.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// Marcador gris con un brillo que lo recorre de izquierda a derecha: ocupa el lugar de lo que aún está cargando.
// Es solo visual; la pantalla que lo contiene describe el estado ("Cargando") para los lectores de pantalla.
@Composable
fun Esqueleto(modifier: Modifier = Modifier, forma: Shape = RoundedCornerShape(12.dp)) {
    val colores = MaterialTheme.colorScheme
    val base = colores.outlineVariant.copy(alpha = 0.7f)
    val brillo = colores.background.copy(alpha = 0.75f)
    val avance by rememberInfiniteTransition(label = "esqueleto").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1300, easing = LinearEasing), RepeatMode.Restart),
        label = "brillo",
    )
    Box(
        modifier = modifier.clip(forma).drawBehind {
            // la banda de brillo mide lo mismo que el elemento y entra por la izquierda y sale por la derecha
            val inicio = -size.width + avance * 2f * size.width
            drawRect(Brush.linearGradient(listOf(base, brillo, base), Offset(inicio, 0f), Offset(inicio + size.width, 0f)))
        },
    )
}
