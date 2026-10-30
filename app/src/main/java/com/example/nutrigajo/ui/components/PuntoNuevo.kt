package com.example.nutrigajo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R

// El punto naranja de «nuevo» sobre la esquina de una tarjeta o un icono; el aro del fondo lo separa del borde. Quien lo usa lo
// coloca (alineación y desplazamiento) con `modifier`.
@Composable
fun PuntoNuevo(modifier: Modifier = Modifier, tamano: Dp = 12.dp) {
    val colores = MaterialTheme.colorScheme
    val descripcion = stringResource(R.string.avisos_nuevo)
    Box(
        modifier
            .size(tamano)
            .clip(CircleShape)
            .background(colores.primary)
            .border(2.dp, colores.background, CircleShape)
            .semantics { contentDescription = descripcion },
    )
}
