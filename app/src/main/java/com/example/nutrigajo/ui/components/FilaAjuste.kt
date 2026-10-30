package com.example.nutrigajo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R

// Fila del perfil: tarjeta blanca con borde fino. Sin `onClick` es un dato (la etiqueta pequeña arriba y el valor en `titulo`,
// con el icono en un cuadro gris); con `onClick` es un ajuste (icono en un cuadro naranja suave, flecha en un círculo y un
// resorte que la encoge al pulsarla), y `destructiva` la tiñe de rojo como la acción de borrar del diseño. Con `activa` es un
// interruptor: tocar la fila lo cambia y, en vez de la flecha, lleva el Switch. `descripcion` va bajo el título.
@Composable
fun FilaAjuste(
    @DrawableRes icono: Int,
    titulo: String,
    modifier: Modifier = Modifier,
    etiqueta: String? = null,
    descripcion: String? = null,
    destructiva: Boolean = false,
    activa: Boolean? = null,
    onActivaChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colores = MaterialTheme.colorScheme
    val accion = onClick ?: if (activa != null && onActivaChange != null) ({ onActivaChange(!activa) }) else null
    val forma = RoundedCornerShape(22.dp)
    val interaccion = remember { MutableInteractionSource() }
    val pulsada by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pulsada) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "pulsacion",
    )
    val fondoIcono = when {
        destructiva -> colores.background
        accion != null -> colores.primaryContainer
        else -> colores.surfaceVariant
    }
    val colorIcono = when {
        destructiva -> colores.error
        accion != null -> colores.onPrimaryContainer
        else -> colores.onBackground
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clip(forma)
            .background(if (destructiva) colores.errorContainer else colores.background)
            .border(1.dp, if (destructiva) colores.error.copy(alpha = 0.35f) else colores.outlineVariant, forma)
            .then(
                if (accion != null) {
                    Modifier.clickable(
                        interactionSource = interaccion,
                        indication = LocalIndication.current,
                        role = if (activa != null) Role.Switch else Role.Button,
                        onClick = accion,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(fondoIcono),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(22.dp), tint = colorIcono)
        }
        Column(Modifier.weight(1f)) {
            etiqueta?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant) }
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = if (destructiva) colores.error else colores.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            descripcion?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant, maxLines = 3) }
        }
        if (activa != null) {
            // la fila entera es la zona de toque: el Switch solo muestra el estado
            Switch(
                checked = activa,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colores.primary,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = colores.surfaceVariant,
                    uncheckedThumbColor = colores.outline,
                    uncheckedBorderColor = colores.outline,
                ),
            )
        } else if (onClick != null && !destructiva) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(colores.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = null, modifier = Modifier.size(16.dp), tint = colores.onBackground)
            }
        }
    }
}
