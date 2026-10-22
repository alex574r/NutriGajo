package com.example.nutrigajo.ui.paciente.inicio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.shared.InicioProvisional

// Entrada del paciente: sin nutriólogo pide el código; con uno, abre su inicio.
// ponytail: el inicio del paciente es provisional; el cuestionario de datos y estilo de vida va después del vínculo.
@Composable
fun InicioPacienteScreen(
    perfil: Perfil,
    onCerrarSesion: () -> Unit,
    vm: VinculoViewModel = viewModel(key = perfil.uid),
) {
    val estado by vm.estado.collectAsState()
    LaunchedEffect(Unit) { vm.cargar() }

    val vinculo = estado.vinculo
    when {
        estado.cargando -> PantallaCarga()
        vinculo == null -> VincularScreen(
            estado = estado,
            onCanjear = { vm.canjear(it, perfil) },
            onEditar = vm::limpiarError,
            onCerrarSesion = onCerrarSesion,
        )
        else -> InicioProvisional(
            perfil = perfil,
            detalle = stringResource(R.string.inicio_nutriologo, vinculo.nutriologoNombre),
            onCerrarSesion = onCerrarSesion,
        )
    }
}
