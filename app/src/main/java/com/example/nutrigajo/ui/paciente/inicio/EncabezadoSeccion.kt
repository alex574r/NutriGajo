package com.example.nutrigajo.ui.paciente.inicio

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R

// Título de sección del inicio: contador gris junto al título, "Ver todas" o los tres puntos a la derecha.
// ponytail: "Ver todas" y los tres puntos no abren nada todavía; las listas completas llegan con sus pantallas.
@Composable
fun EncabezadoSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    contador: String? = null,
    verTodas: Boolean = false,
    menu: Boolean = false,
) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = colores.tertiary)
        contador?.let {
            Spacer(Modifier.width(8.dp))
            Text(it, style = MaterialTheme.typography.titleMedium, color = colores.outline)
        }
        Spacer(Modifier.weight(1f))
        if (verTodas) {
            Text(stringResource(R.string.inicio_ver_todas), style = MaterialTheme.typography.labelLarge, color = colores.primary)
        }
        if (menu) {
            Icon(painterResource(R.drawable.ic_mas), contentDescription = null, tint = colores.onSurfaceVariant)
        }
    }
}
