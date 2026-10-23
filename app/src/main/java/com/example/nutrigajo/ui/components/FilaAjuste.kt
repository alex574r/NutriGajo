package com.example.nutrigajo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R

// Fila del perfil sobre superficie gris. Sin `onClick` es un dato (la etiqueta pequeña arriba y el valor en `titulo`);
// con `onClick` es un ajuste con flecha, y `destructiva` la tiñe de rojo como la acción de borrar del diseño.
@Composable
fun FilaAjuste(
    @DrawableRes icono: Int,
    titulo: String,
    modifier: Modifier = Modifier,
    etiqueta: String? = null,
    destructiva: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colores = MaterialTheme.colorScheme
    val acento = if (destructiva) colores.error else colores.onBackground
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (destructiva) colores.errorContainer else colores.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colores.background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(20.dp), tint = acento)
        }
        Column(Modifier.weight(1f)) {
            etiqueta?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant) }
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = acento,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null && !destructiva) {
            Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = null, modifier = Modifier.size(18.dp), tint = colores.onSurfaceVariant)
        }
    }
}
