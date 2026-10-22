package com.example.nutrigajo.ui.paciente.inicio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.domain.model.Vinculo
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.paciente.mapa.MapaScreen
import com.example.nutrigajo.ui.paciente.mapa.MapaViewModel
import com.example.nutrigajo.ui.shared.expediente.ExpedienteEstado
import com.example.nutrigajo.ui.shared.expediente.ExpedienteFlujo
import com.example.nutrigajo.ui.shared.expediente.ExpedienteViewModel

// Entrada del paciente: sin nutriólogo pide el código (o deja buscarlo en el mapa); con uno, abre su inicio.
// ponytail: el inicio del paciente es provisional; planes, mediciones y citas llegan después.
@Composable
fun InicioPacienteScreen(
    perfil: Perfil,
    onCerrarSesion: () -> Unit,
    vm: VinculoViewModel = viewModel(key = perfil.uid),
    // claves propias: dos ViewModels con la misma clave se pisarían
    mapaVm: MapaViewModel = viewModel(key = "mapa-${perfil.uid}"),
) {
    val estado by vm.estado.collectAsState()
    val mapa by mapaVm.estado.collectAsState()
    var enMapa by rememberSaveable { mutableStateOf(false) }
    var enExpediente by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.cargar() }
    LaunchedEffect(enMapa) { if (enMapa) mapaVm.cargar() }

    val vinculo = estado.vinculo
    when {
        estado.cargando -> PantallaCarga()
        vinculo == null && enMapa -> {
            BackHandler { enMapa = false }
            MapaScreen(estado = mapa, onActualizar = mapaVm::cargar, onVolver = { enMapa = false })
        }
        vinculo == null -> VincularScreen(
            estado = estado,
            onCanjear = { vm.canjear(it, perfil) },
            onEditar = vm::limpiarError,
            onMapa = { enMapa = true },
            onCerrarSesion = onCerrarSesion,
        )
        enExpediente -> ExpedienteFlujo(
            pacienteId = vinculo.pacienteId,
            nombrePaciente = perfil.nombre,
            editor = Rol.PACIENTE,
            onSalir = { enExpediente = false },
        )
        else -> InicioPaciente(perfil, vinculo, onExpediente = { enExpediente = true }, onCerrarSesion = onCerrarSesion)
    }
}

@Composable
private fun InicioPaciente(
    perfil: Perfil,
    vinculo: Vinculo,
    onExpediente: () -> Unit,
    onCerrarSesion: () -> Unit,
    // misma clave que el flujo del expediente: la tarjeta refleja lo que se guarda ahí
    expedienteVm: ExpedienteViewModel = viewModel(key = "expediente-${vinculo.pacienteId}-${Rol.PACIENTE.name}"),
) {
    val colores = MaterialTheme.colorScheme
    val expediente: ExpedienteEstado by expedienteVm.estado.collectAsState()
    LaunchedEffect(vinculo.pacienteId) { expedienteVm.cargar(vinculo.pacienteId) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.inicio_hola, perfil.nombre),
                    modifier = Modifier.weight(1f),
                    // el 100% de la escala no sirve para texto de varias líneas
                    style = MaterialTheme.typography.headlineMedium.copy(lineHeight = 1.2.em),
                    color = colores.onBackground,
                )
                TextButton(onClick = onCerrarSesion) { Text(stringResource(R.string.inicio_cerrar_sesion)) }
            }
        }
        item {
            Text(
                text = stringResource(R.string.inicio_nutriologo, vinculo.nutriologoNombre),
                style = MaterialTheme.typography.bodyLarge,
                color = colores.onSurfaceVariant,
            )
        }
        if (!expediente.cargando) item { TarjetaExpediente(expediente, onExpediente) }
    }
}
