package com.example.nutrigajo.ui.shared.ajustes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.BotonPrimario

// Tapa toda la app mientras está bloqueada (huella o rostro al abrir). Se come los toques para que nada de abajo responda.
@Composable
fun PantallaBloqueo(onDesbloquear: () -> Unit, onCerrarSesion: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.background)
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(96.dp))
        Text(stringResource(R.string.bloqueo_titulo), style = MaterialTheme.typography.headlineMedium, color = colores.onBackground, textAlign = TextAlign.Center)
        Text(stringResource(R.string.bloqueo_texto), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant, textAlign = TextAlign.Center)
        BotonPrimario(stringResource(R.string.bloqueo_boton), onDesbloquear, Modifier.fillMaxWidth().padding(top = 12.dp))
        TextButton(onClick = onCerrarSesion) { Text(stringResource(R.string.inicio_cerrar_sesion)) }
    }
}
