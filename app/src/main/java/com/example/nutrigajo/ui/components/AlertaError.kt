package com.example.nutrigajo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R

// Alerta de validación del diseño: relleno rojo claro, borde rojo, icono de advertencia y texto.
@Composable
fun AlertaError(texto: String, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colores.errorContainer, forma)
            .border(1.dp, colores.error, forma)
            .padding(12.dp)
            // los lectores de pantalla anuncian el error al aparecer
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_warning),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = colores.error,
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            color = colores.onErrorContainer,
        )
    }
}
