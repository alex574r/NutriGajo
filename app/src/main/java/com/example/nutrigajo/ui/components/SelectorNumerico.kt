package com.example.nutrigajo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// Fila de números de un solo toque (días por semana, nivel de actividad); el elegido se rellena de azul.
@Composable
fun SelectorNumerico(
    valores: IntRange,
    seleccionado: Int?,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            // las opciones llevan su anillo de foco: se suma aquí para alinear los bordes
            .padding(horizontal = ANILLO_FOCO)
            .height(56.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(colores.surfaceVariant)
            .padding(6.dp)
            .selectableGroup(),
    ) {
        valores.forEach { numero ->
            val elegido = numero == seleccionado
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (elegido) colores.secondary else androidx.compose.ui.graphics.Color.Transparent)
                    .selectable(selected = elegido, onClick = { onSeleccion(numero) }, role = Role.RadioButton),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = numero.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (elegido) colores.onSecondary else colores.onSurfaceVariant,
                )
            }
        }
    }
}
