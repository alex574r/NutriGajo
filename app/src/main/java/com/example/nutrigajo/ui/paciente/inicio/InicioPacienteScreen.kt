package com.example.nutrigajo.ui.paciente.inicio

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.paciente.mapa.MapaScreen
import com.example.nutrigajo.ui.paciente.mapa.MapaViewModel
import com.example.nutrigajo.ui.shared.InicioProvisional

// Entrada del paciente: sin nutriólogo pide el código (o deja buscarlo en el mapa); con uno, abre su inicio.
// ponytail: el inicio del paciente es provisional; el cuestionario de datos y estilo de vida va después del vínculo.
@Composable
fun InicioPacienteScreen(
    perfil: Perfil,
    onCerrarSesion: () -> Unit,
    vm: VinculoViewModel = viewModel(key = perfil.uid),
    // clave propia: dos ViewModels con la misma clave se pisarían
    mapaVm: MapaViewModel = viewModel(key = "mapa-${perfil.uid}"),
) {
    val estado by vm.estado.collectAsState()
    val mapa by mapaVm.estado.collectAsState()
    var enMapa by rememberSaveable { mutableStateOf(false) }
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
        else -> InicioProvisional(
            perfil = perfil,
            detalle = stringResource(R.string.inicio_nutriologo, vinculo.nutriologoNombre),
            onCerrarSesion = onCerrarSesion,
        )
    }
}
