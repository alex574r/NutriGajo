package com.example.nutrigajo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

// La foto de perfil si la cuenta tiene una; si no, la inicial del nombre sobre naranja.
@Composable
fun AvatarInicial(
    nombre: String,
    tamano: Dp,
    estilo: TextStyle,
    modifier: Modifier = Modifier,
    forma: Shape = CircleShape,
    imagen: ImageBitmap? = null,
) {
    val colores = MaterialTheme.colorScheme
    Box(
        modifier = modifier.size(tamano).clip(forma).background(colores.primary),
        contentAlignment = Alignment.Center,
    ) {
        if (imagen != null) {
            Image(bitmap = imagen, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        } else {
            Text(text = nombre.firstOrNull()?.uppercase().orEmpty(), style = estilo, color = colores.onPrimary)
        }
    }
}
