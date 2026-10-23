package com.example.nutrigajo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Etiqueta de cristal que va sobre las fotos y las tarjetas de color.
@Composable
fun Etiqueta(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        modifier = modifier.background(Color(0xFFBDBDBD).copy(alpha = 0.35f), RoundedCornerShape(7.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
        color = Color.White,
    )
}
