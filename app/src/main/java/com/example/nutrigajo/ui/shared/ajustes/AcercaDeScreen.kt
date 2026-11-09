package com.example.nutrigajo.ui.shared.ajustes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.BuildConfig
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.actualizacion.ActualizacionViewModel
import com.example.nutrigajo.ui.actualizacion.FaseActualizacion
import com.example.nutrigajo.ui.actualizacion.actualizacion
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.TarjetaAjustes

@Composable
fun AcercaDeScreen(versionApp: String, onVolver: () -> Unit, espacioInferior: Dp) {
    val colores = MaterialTheme.colorScheme
    PantallaAjuste(stringResource(R.string.acerca_titulo), onVolver, espacioInferior = espacioInferior) {
        Spacer(Modifier.height(16.dp))
        Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(96.dp).align(Alignment.CenterHorizontally))
        Text(
            stringResource(R.string.app_name),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = colores.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.app_tagline),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = colores.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.acerca_descripcion), style = MaterialTheme.typography.bodyMedium, color = colores.onBackground)
        Spacer(Modifier.height(8.dp))
        TarjetaAjustes {
            FilaAjuste(R.drawable.ic_informacion, titulo = versionApp, etiqueta = stringResource(R.string.perfil_version), plana = true)
            FilaActualizacion()
        }
        GrupoAjustes(stringResource(R.string.acerca_notas_titulo))
        NOTAS_DE_VERSION.forEach { TarjetaNotas(it, instalada = esLaInstalada(it.version, versionApp)) }
    }
}

// Las notas de una versión en una tarjeta gris: su número, si es la instalada, cuándo salió y una viñeta por nota.
@Composable
private fun TarjetaNotas(version: VersionNotas, instalada: Boolean) {
    val colores = MaterialTheme.colorScheme
    val notas = stringArrayResource(version.notas)
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colores.surfaceVariant).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.acerca_notas_version, version.version), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
            if (instalada) {
                Text(
                    text = stringResource(R.string.acerca_notas_instalada),
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(colores.primaryContainer).padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = colores.primary,
                )
            }
        }
        version.fecha?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant) }
        notas.forEach { nota ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•", style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
                Text(nota, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colores.onBackground)
            }
        }
    }
}

// «Buscar actualizaciones»: dice si estás al día o que hay una versión nueva (tocarla enseña la hoja) y deja buscar a mano. Comparte el
// ViewModel de la actividad con la hoja del inicio.
@Composable
private fun FilaActualizacion() {
    if (!BuildConfig.ACTUALIZACION_INTERNA) return
    val vm: ActualizacionViewModel = viewModel()
    val fase by vm.fase.collectAsState()
    val actual = fase
    val nueva = actual.actualizacion
    val fila: Pair<String, (() -> Unit)?> = when {
        actual is FaseActualizacion.Consultando -> stringResource(R.string.actualiza_fila_buscando) to null
        nueva != null -> stringResource(R.string.actualiza_fila_disponible, nueva.versionName) to vm::mostrarHoja
        actual is FaseActualizacion.Error -> stringResource(actual.mensaje) to { vm.verificar(manual = true) }
        actual is FaseActualizacion.AlDia -> stringResource(R.string.actualiza_fila_al_dia) to { vm.verificar(manual = true) }
        else -> stringResource(R.string.actualiza_fila_buscar) to { vm.verificar(manual = true) }
    }
    FilaAjuste(R.drawable.ic_refrescar, titulo = fila.first, etiqueta = stringResource(R.string.actualiza_fila_buscar), plana = true, onClick = fila.second)
}
