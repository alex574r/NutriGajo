package com.example.nutrigajo.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// Lo que el contenido debe dejar libre abajo para no quedar tapado por la barra (sin contar la barra del sistema).
val ALTO_BARRA_INFERIOR = 112.dp

class ItemBarra(
    @param:DrawableRes val icono: Int,
    @param:StringRes val descripcion: Int,
    val seleccionado: Boolean,
    val onClick: () -> Unit,
)

// Barra flotante del diseño: dos destinos a cada lado y el botón naranja de acción en el centro.
@Composable
fun BarraInferior(
    izquierda: List<ItemBarra>,
    derecha: List<ItemBarra>,
    @StringRes descripcionCentro: Int,
    @DrawableRes iconoCentro: Int,
    onCentro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(32.dp)
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(12.dp, forma, ambientColor = colores.onBackground.copy(alpha = 0.2f), spotColor = colores.onBackground.copy(alpha = 0.2f))
                .background(colores.background, forma)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            izquierda.forEach { Destino(it, Modifier.weight(1f)) }
            // hueco del botón central
            Spacer(Modifier.weight(1f))
            derecha.forEach { Destino(it, Modifier.weight(1f)) }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
                .size(68.dp)
                .background(colores.background, CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(colores.primary)
                .clickable(role = Role.Button, onClickLabel = stringResource(descripcionCentro), onClick = onCentro),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconoCentro),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = colores.onPrimary,
            )
        }
    }
}

@Composable
private fun Destino(item: ItemBarra, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (item.seleccionado) colores.onBackground else androidx.compose.ui.graphics.Color.Transparent)
                .selectable(selected = item.seleccionado, role = Role.Tab, onClick = item.onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(item.icono),
                contentDescription = stringResource(item.descripcion),
                modifier = Modifier.size(24.dp),
                tint = if (item.seleccionado) colores.background else colores.onSurfaceVariant,
            )
        }
    }
}
