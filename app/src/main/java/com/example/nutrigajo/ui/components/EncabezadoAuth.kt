package com.example.nutrigajo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R

// Foto: "Modern Pilates Studio with Reformers", Paulina Vargas, Pexels (licencia Pexels: uso libre, sin atribución obligatoria).
// https://www.pexels.com/photo/modern-pilates-studio-with-reformers-36833353/
// Encabezado de login y registro: foto clara que se funde con el fondo y el logo sobre el borde inferior.
@Composable
fun EncabezadoAuth(modifier: Modifier = Modifier, alto: Dp = 260.dp) {
    val colores = MaterialTheme.colorScheme
    Box(modifier = modifier.fillMaxWidth().height(alto)) {
        Image(
            painter = painterResource(R.drawable.login_encabezado),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to colores.background)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(56.dp)
                .background(colores.primary, RoundedCornerShape(21.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_logo_mono),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = colores.onPrimary,
            )
        }
    }
}
