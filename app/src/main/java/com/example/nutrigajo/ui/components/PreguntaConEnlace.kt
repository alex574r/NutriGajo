package com.example.nutrigajo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

// "¿Ya tienes una cuenta? ingresar": pregunta y enlace naranja subrayado. Toma los colores del tema.
@Composable
fun PreguntaConEnlace(
    pregunta: String,
    enlace: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$pregunta ",
            style = MaterialTheme.typography.bodySmall,
            color = colores.onBackground,
        )
        Text(
            text = enlace,
            style = MaterialTheme.typography.bodySmall,
            color = colores.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
        )
    }
}
