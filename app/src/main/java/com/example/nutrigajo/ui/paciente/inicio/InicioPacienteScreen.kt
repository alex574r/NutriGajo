package com.example.nutrigajo.ui.paciente.inicio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.domain.model.Vinculo
import com.example.nutrigajo.ui.components.BarraEstadoClara
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.paciente.mapa.MapaScreen
import com.example.nutrigajo.ui.paciente.mapa.MapaViewModel
import com.example.nutrigajo.ui.shared.expediente.ExpedienteEstado
import com.example.nutrigajo.ui.shared.expediente.ExpedienteFlujo
import com.example.nutrigajo.ui.shared.expediente.ExpedienteViewModel

// Entrada del paciente: sin nutriólogo pide el código (o deja buscarlo en el mapa); con uno, abre su inicio.
// ponytail: las secciones del inicio salvo el expediente y el coach usan datos de muestra (DatosMuestra.kt).
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

    val lista = rememberLazyListState()
    val densidad = LocalDensity.current
    // ponytail: umbral fijo; con la cabecera ya fuera de la pantalla los íconos de la barra vuelven a ser oscuros
    val sobreFoto by remember {
        derivedStateOf { lista.firstVisibleItemIndex == 0 && lista.firstVisibleItemScrollOffset < with(densidad) { 150.dp.toPx() } }
    }
    BarraEstadoClara(sobreFoto)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.background),
        state = lista,
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { CabeceraInicio(perfil.nombre, resumenDia, onExpediente, onCerrarSesion) }
        // una vez completo, el expediente se consulta desde el menú de la cuenta
        if (!expediente.cargando && !expediente.expediente.completo) {
            item { TarjetaExpediente(expediente, onExpediente, Modifier.padding(horizontal = 16.dp)) }
        }
        item { MetricasFitness(resumenDia) }
        item { RutinaDeHoy(rutinaDeHoy) }
        item { DietaNutricion(comidas) }
        item { ActividadReciente() }
        item { TarjetaCoach(vinculo.nutriologoNombre) }
        item { EjerciciosExtra(ejerciciosExtra) }
    }
}
