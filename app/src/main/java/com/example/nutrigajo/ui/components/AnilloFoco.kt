package com.example.nutrigajo.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val ANILLO_FOCO = 4.dp

// Anillo de foco del diseño: 4dp de naranja al 25% alrededor del elemento activo.
// Ocupa su espacio aunque esté inactivo, para que nada se mueva al enfocar o elegir.
@Composable
fun AnilloFoco(
    activo: Boolean,
    radio: Dp,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    val color = if (activo) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent
    Box(
        modifier = modifier
            .border(ANILLO_FOCO, color, RoundedCornerShape(radio + ANILLO_FOCO))
            .padding(ANILLO_FOCO),
    ) {
        contenido()
    }
}
