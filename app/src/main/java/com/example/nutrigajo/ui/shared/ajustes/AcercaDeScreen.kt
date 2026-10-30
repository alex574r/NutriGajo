package com.example.nutrigajo.ui.shared.ajustes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.FilaAjuste

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
        FilaAjuste(R.drawable.ic_informacion, titulo = versionApp, etiqueta = stringResource(R.string.perfil_version))
    }
}
