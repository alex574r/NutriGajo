package com.example.nutrigajo.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// Pestañas del diseño: contenedor gris y la elegida en negro con anillo de foco negro al 25%.
@Composable
fun SelectorPestanas(
    @StringRes titulos: List<Int>,
    seleccionada: Int,
    onSeleccionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().background(colores.surfaceVariant, RoundedCornerShape(28.dp))) {
        titulos.forEachIndexed { indice, titulo ->
            val activa = indice == seleccionada
            AnilloFoco(
                activo = activa,
                radio = 20.dp,
                modifier = Modifier.weight(1f),
                anillo = colores.onBackground.copy(alpha = 0.25f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (activa) colores.onBackground else Color.Transparent)
                        .selectable(selected = activa, role = Role.Tab, onClick = { onSeleccionar(indice) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(titulo),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (activa) colores.background else colores.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
