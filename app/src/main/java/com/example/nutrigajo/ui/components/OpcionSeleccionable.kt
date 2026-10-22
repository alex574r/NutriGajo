package com.example.nutrigajo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val RADIO_OPCION = 19.dp

// Fila de un asistente: tarjeta gris con casilla; elegida se rellena de naranja.
// `multiple` cambia la semántica (varias a la vez); el aspecto es el del diseño en ambos casos.
@Composable
fun OpcionLista(
    texto: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // texto corto a la derecha (p. ej. las horas de sueño)
    detalle: String? = null,
    multiple: Boolean = false,
) {
    val colores = MaterialTheme.colorScheme
    val contenido = if (seleccionada) colores.onPrimary else colores.onBackground
    AnilloFoco(activo = seleccionada, radio = RADIO_OPCION, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(RADIO_OPCION))
                .background(if (seleccionada) colores.primary else colores.surfaceVariant)
                .elegible(seleccionada, onClick, multiple)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(texto, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = contenido)
            detalle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = if (seleccionada) colores.onPrimary else colores.onSurfaceVariant)
            }
            Casilla(seleccionada, contenido)
        }
    }
}

// Tarjeta de rejilla (dieta, ejercicio): título arriba, casilla abajo.
@Composable
fun OpcionTarjeta(
    titulo: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    multiple: Boolean = false,
) {
    val colores = MaterialTheme.colorScheme
    val contenido = if (seleccionada) colores.onPrimary else colores.onBackground
    AnilloFoco(activo = seleccionada, radio = RADIO_OPCION, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 104.dp)
                .clip(RoundedCornerShape(RADIO_OPCION))
                .background(if (seleccionada) colores.primary else colores.surfaceVariant)
                .elegible(seleccionada, onClick, multiple)
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(titulo, style = MaterialTheme.typography.titleSmall, color = contenido)
                subtitulo?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (seleccionada) colores.onPrimary else colores.onSurfaceVariant)
                }
            }
            Casilla(seleccionada, contenido)
        }
    }
}

private fun Modifier.elegible(seleccionada: Boolean, onClick: () -> Unit, multiple: Boolean): Modifier =
    if (multiple) {
        toggleable(value = seleccionada, onValueChange = { onClick() }, role = Role.Checkbox)
    } else {
        selectable(selected = seleccionada, onClick = onClick, role = Role.RadioButton)
    }

// Casilla cuadrada con punto, como en la selección de rol.
@Composable
private fun Casilla(seleccionada: Boolean, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier.size(24.dp).border(2.dp, color, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (seleccionada) Box(Modifier.size(10.dp).background(color, CircleShape))
    }
}
