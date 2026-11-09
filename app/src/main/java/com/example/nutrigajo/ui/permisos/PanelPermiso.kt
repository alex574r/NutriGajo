package com.example.nutrigajo.ui.permisos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.EstadoPermiso
import com.example.nutrigajo.domain.PermisoApp
import com.example.nutrigajo.ui.components.BotonPrimario

// Explica un permiso donde hace falta: para qué se pide, qué pasa sin él y el botón que corresponde al estado («Dar permiso», «Abrir ajustes» o
// «Instalar»). Es el mismo en todas las pantallas. `onAhoraNo` agrega un «Ahora no» para los permisos que se pueden dejar para después.
@Composable
fun PanelPermiso(
    permiso: PermisoApp,
    estado: EstadoPermiso,
    onAccion: () -> Unit,
    modifier: Modifier = Modifier,
    onAhoraNo: (() -> Unit)? = null,
) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colores.primaryContainer).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(permiso.titulo()), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
        Text(stringResource(permiso.paraQue()), style = MaterialTheme.typography.bodyMedium, color = colores.onBackground)
        Text(stringResource(permiso.sinEl()), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
        if (estado == EstadoPermiso.BLOQUEADO) {
            Text(stringResource(R.string.permiso_bloqueado_nota), style = MaterialTheme.typography.bodySmall, color = colores.primary)
        }
        estado.textoAccion()?.let { BotonPrimario(texto = stringResource(it), onClick = onAccion, modifier = Modifier.fillMaxWidth(), altura = 48.dp) }
        onAhoraNo?.let { TextButton(onClick = it, modifier = Modifier.fillMaxWidth().padding(top = 0.dp)) { Text(stringResource(R.string.permiso_ahora_no)) } }
    }
}
