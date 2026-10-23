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
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R

// Aviso informativo del diseño: relleno azul claro, borde azul e icono de información. Es el par azul de AlertaError.
@Composable
fun AvisoInformativo(texto: String, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(colores.secondaryContainer, forma)
            .border(1.dp, colores.secondary.copy(alpha = 0.5f), forma)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_informacion), contentDescription = null, modifier = Modifier.size(20.dp), tint = colores.secondary)
        Text(text = texto, style = MaterialTheme.typography.labelLarge, color = colores.onBackground)
    }
}
