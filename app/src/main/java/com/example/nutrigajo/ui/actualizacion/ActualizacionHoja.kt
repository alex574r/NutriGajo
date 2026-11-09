package com.example.nutrigajo.ui.actualizacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario

// Hoja inferior de una actualización opcional: la versión nueva, sus notas y los botones. Mientras se descarga o se instala no se cierra
// (ni deslizándola ni con «atrás»): lo que pase tiene que verse en esta hoja.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActualizacionHoja(
    fase: FaseActualizacion,
    onActualizar: () -> Unit,
    onMasTarde: () -> Unit,
    onReintentar: () -> Unit,
    onAbrirConfiguracion: () -> Unit,
    onCerrar: () -> Unit,
) {
    val ocupada by rememberUpdatedState(fase is FaseActualizacion.Descargando || fase is FaseActualizacion.Instalando)
    ModalBottomSheet(
        onDismissRequest = { if (!ocupada) onCerrar() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden || !ocupada }),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        PanelActualizacion(
            fase = fase,
            onActualizar = onActualizar,
            onMasTarde = onMasTarde,
            onReintentar = onReintentar,
            onAbrirConfiguracion = onAbrirConfiguracion,
            onCerrar = onCerrar,
            onCerrarApp = {},
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(),
        )
    }
}

// Lo que se ve de una actualización, en la hoja y en la pantalla obligatoria: versión, notas y, según la fase, el botón o el avance.
@Composable
internal fun PanelActualizacion(
    fase: FaseActualizacion,
    onActualizar: () -> Unit,
    onMasTarde: () -> Unit,
    onReintentar: () -> Unit,
    onAbrirConfiguracion: () -> Unit,
    onCerrar: () -> Unit,
    onCerrarApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actualizacion = fase.actualizacion ?: return
    val obligatoria = fase.obligatoria
    val colores = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(if (obligatoria) R.string.actualiza_obligatoria_titulo else R.string.actualiza_titulo),
                style = MaterialTheme.typography.headlineMedium,
                color = colores.onBackground,
            )
            Text(stringResource(R.string.actualiza_version, actualizacion.versionName), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
        }
        if (obligatoria) Text(stringResource(R.string.actualiza_obligatoria_texto), style = MaterialTheme.typography.bodyMedium, color = colores.onBackground)
        if (actualizacion.notas.isNotEmpty()) {
            Text(stringResource(R.string.actualiza_notas_titulo), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                actualizacion.notas.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant) }
            }
        }
        when (fase) {
            is FaseActualizacion.Disponible -> BotonPrimario(stringResource(R.string.actualiza_boton), onActualizar, Modifier.fillMaxWidth())
            is FaseActualizacion.Descargando -> {
                val avance = (fase.bytes.toFloat() / actualizacion.bytes).coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { avance }, modifier = Modifier.fillMaxWidth(), color = colores.primary, trackColor = colores.surfaceVariant)
                Text(stringResource(R.string.actualiza_descargando, (avance * 100).toInt()), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
            }
            is FaseActualizacion.Instalando -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = colores.primary, trackColor = colores.surfaceVariant)
                Text(stringResource(R.string.actualiza_instalando), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
            }
            is FaseActualizacion.PermisoPendiente -> {
                Text(stringResource(R.string.actualiza_permiso), style = MaterialTheme.typography.bodyMedium, color = colores.onBackground)
                BotonPrimario(stringResource(R.string.actualiza_abrir_configuracion), onAbrirConfiguracion, Modifier.fillMaxWidth())
            }
            is FaseActualizacion.Error -> {
                AlertaError(stringResource(fase.mensaje))
                BotonPrimario(stringResource(R.string.actualiza_reintentar), onReintentar, Modifier.fillMaxWidth())
            }
            else -> Unit
        }
        // siempre hay una salida, también mientras se instala (la ventana de Android puede no volver): la obligatoria deja cerrar la app; la opcional,
        // dejarlo para después, o cerrar la hoja si ya está instalándose o falló
        when {
            obligatoria -> TextButton(onClick = onCerrarApp, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.actualiza_cerrar_app)) }
            fase is FaseActualizacion.Error || fase is FaseActualizacion.Instalando -> TextButton(onClick = onCerrar, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.actualiza_cerrar)) }
            else -> TextButton(onClick = onMasTarde, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.actualiza_mas_tarde)) }
        }
    }
}
