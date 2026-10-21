package com.example.nutrigajo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Foto a pantalla completa con degradado hacia el fondo para que el texto inferior se lea.
@Composable
fun FondoFoto(
    @DrawableRes imagen: Int,
    modifier: Modifier = Modifier,
    blancoYNegro: Boolean = false,
    alturaDegradado: Dp = 480.dp,
    contenido: @Composable BoxScope.() -> Unit,
) {
    val fondo = MaterialTheme.colorScheme.background
    Box(modifier = modifier.fillMaxSize().background(fondo)) {
        Image(
            painter = painterResource(imagen),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            colorFilter = if (blancoYNegro) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(alturaDegradado)
                .background(
                    // opaco desde la mitad: el texto no debe perder contraste sobre zonas claras de la foto
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to fondo.copy(alpha = 0.8f),
                        1f to fondo,
                    ),
                ),
        )
        contenido()
    }
}
