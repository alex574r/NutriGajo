package com.example.nutrigajo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

// Tarjeta de rejilla (dieta, ejercicio): título arriba, casilla abajo. Con `imagen`, la foto llena la tarjeta,
// el texto va en blanco sobre un degradado oscuro y, al elegirla, se tiñe de naranja.
@Composable
fun OpcionTarjeta(
    titulo: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    multiple: Boolean = false,
    @DrawableRes imagen: Int? = null,
) {
    val colores = MaterialTheme.colorScheme
    val conFoto = imagen != null
    val contenido = if (seleccionada || conFoto) Color.White else colores.onBackground
    val secundario = if (seleccionada || conFoto) Color.White.copy(alpha = 0.85f) else colores.onSurfaceVariant
    AnilloFoco(activo = seleccionada, radio = RADIO_OPCION, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = if (conFoto) 136.dp else 104.dp)
                .clip(RoundedCornerShape(RADIO_OPCION))
                .background(if (seleccionada && !conFoto) colores.primary else colores.surfaceVariant)
                .elegible(seleccionada, onClick, multiple),
        ) {
            if (imagen != null) {
                Image(
                    painter = painterResource(imagen),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
                // degradado para que el texto y la casilla se lean sobre cualquier foto
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.55f), 0.5f to Color.Black.copy(alpha = 0.1f), 1f to Color.Black.copy(alpha = 0.55f)),
                    ),
                )
                if (seleccionada) Box(Modifier.matchParentSize().background(colores.primary.copy(alpha = 0.78f)))
            }
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(titulo, style = MaterialTheme.typography.titleSmall, color = contenido)
                    subtitulo?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = secundario) }
                }
                Casilla(seleccionada, contenido)
            }
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
private fun Casilla(seleccionada: Boolean, color: Color) {
    Box(
        modifier = Modifier.size(24.dp).border(2.dp, color, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (seleccionada) Box(Modifier.size(10.dp).background(color, CircleShape))
    }
}
