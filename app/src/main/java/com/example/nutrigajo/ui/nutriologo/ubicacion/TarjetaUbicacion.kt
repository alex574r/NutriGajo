package com.example.nutrigajo.ui.nutriologo.ubicacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R

// Invita al nutriólogo verificado a poner su consultorio en el mapa de los pacientes.
@Composable
fun TarjetaUbicacion(onUbicar: () -> Unit, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colores.surfaceVariant, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.ubicacion_titulo), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
        Text(
            text = stringResource(R.string.ubicacion_tarjeta_ayuda),
            style = MaterialTheme.typography.bodySmall,
            color = colores.onSurfaceVariant,
        )
        Button(
            onClick = onUbicar,
            shape = RoundedCornerShape(21.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
        ) { Text(stringResource(R.string.ubicacion_tarjeta_boton)) }
    }
}
