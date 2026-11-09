package com.example.nutrigajo.ui.actualizacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// La actualización obligatoria: pantalla completa que tapa la app, sin «más tarde» y sin que «atrás» la cierre. Si la descarga falla ofrece
// reintentar, y siempre deja cerrar la app: nadie queda atrapado sin red.
@Composable
fun ActualizacionObligatoria(
    fase: FaseActualizacion,
    onActualizar: () -> Unit,
    onReintentar: () -> Unit,
    onAbrirConfiguracion: () -> Unit,
    onCerrarApp: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                PanelActualizacion(
                    fase = fase,
                    onActualizar = onActualizar,
                    onMasTarde = {},
                    onReintentar = onReintentar,
                    onAbrirConfiguracion = onAbrirConfiguracion,
                    onCerrar = {},
                    onCerrarApp = onCerrarApp,
                )
            }
        }
    }
}
