package com.example.nutrigajo.ui.paciente.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.components.PantallaPdf

// El plan de alimentación y ejercicio que el nutriólogo envió al paciente, en el visor de PDF de la app.
@Composable
fun PlanPacienteScreen(pacienteId: String, onVolver: () -> Unit, vm: PlanPacienteViewModel) {
    val estado by vm.estado.collectAsState()
    val cache = LocalContext.current.cacheDir
    LaunchedEffect(pacienteId) { vm.abrir(pacienteId, cache) }
    val salir = {
        vm.cerrar()
        onVolver()
    }
    val pdf = estado.pdf

    when {
        pdf != null -> PantallaPdf(titulo = stringResource(R.string.plan_paciente_titulo), archivo = pdf, onVolver = salir)
        estado.error != null -> {
            BackHandler(onBack = salir)
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = salir) { Text(stringResource(R.string.mapa_volver)) }
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AlertaError(stringResource(estado.error ?: R.string.error_generico))
                    BotonPrimario(
                        texto = stringResource(R.string.plan_paciente_reintentar),
                        onClick = { vm.abrir(pacienteId, cache) },
                        modifier = Modifier.fillMaxWidth(),
                        cargando = estado.abriendo,
                    )
                }
            }
        }
        else -> {
            BackHandler(onBack = salir)
            PantallaCarga()
        }
    }
}
