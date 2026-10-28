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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.domain.model.Vinculo
import com.example.nutrigajo.ui.shared.perfil.rememberFotoAvatar
import com.example.nutrigajo.ui.shared.perfil.rememberFotoPortada
import com.example.nutrigajo.ui.components.ActualizarAlDeslizar
import com.example.nutrigajo.ui.components.BarraEstadoClara
import com.example.nutrigajo.ui.components.CambioConAvatar
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.components.TransicionPerfil
import androidx.compose.ui.res.stringResource
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.PlanEnviado
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.nutriologo.inicio.fechaMedia
import com.example.nutrigajo.ui.paciente.citas.AgendaViewModel
import com.example.nutrigajo.ui.paciente.plan.PlanPacienteScreen
import com.example.nutrigajo.ui.paciente.plan.PlanPacienteViewModel
import com.example.nutrigajo.ui.paciente.citas.AgendarCitaSheet
import com.example.nutrigajo.ui.paciente.mapa.MapaScreen
import com.example.nutrigajo.ui.paciente.mapa.MapaViewModel
import com.example.nutrigajo.ui.shared.citas.CitasScreen
import com.example.nutrigajo.ui.shared.citas.CitasViewModel
import com.example.nutrigajo.ui.shared.expediente.ExpedienteEstado
import com.example.nutrigajo.ui.shared.expediente.ExpedienteFlujo
import com.example.nutrigajo.ui.shared.expediente.ExpedienteViewModel
import kotlinx.coroutines.delay

private const val PAUSA_EXITO_MS = 1400L

// Entrada del paciente: sin nutriólogo pide el código (o deja buscarlo en el mapa); con uno, abre su inicio.
// ponytail: las secciones del inicio salvo el expediente y el coach usan datos de muestra (DatosMuestra.kt).
@Composable
fun InicioPacienteScreen(
    perfil: Perfil,
    onCerrarSesion: () -> Unit,
    vm: VinculoViewModel = viewModel(key = perfil.uid),
    // claves propias: dos ViewModels con la misma clave se pisarían
    mapaVm: MapaViewModel = viewModel(key = "mapa-${perfil.uid}"),
    agendaVm: AgendaViewModel = viewModel(key = "agenda-${perfil.uid}"),
    citasVm: CitasViewModel = viewModel(key = "citas-${perfil.uid}"),
    planVm: PlanPacienteViewModel = viewModel(key = "plan-${perfil.uid}"),
    // se tocó la notificación de un plan nuevo: se abre directo; `onPlanConsumido` avisa que ya se atendió
    abrirPlan: Boolean = false,
    onPlanConsumido: () -> Unit = {},
) {
    val estado by vm.estado.collectAsState()
    val mapa by mapaVm.estado.collectAsState()
    val agenda by agendaVm.estado.collectAsState()
    val citas by citasVm.estado.collectAsState()
    val plan by planVm.estado.collectAsState()
    var enMapa by rememberSaveable { mutableStateOf(false) }
    var enExpediente by rememberSaveable { mutableStateOf(false) }
    var enPerfil by rememberSaveable { mutableStateOf(false) }
    var enCitas by rememberSaveable { mutableStateOf(false) }
    var enPlan by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.cargar() }
    LaunchedEffect(enMapa) { if (enMapa) mapaVm.cargar() }
    LaunchedEffect(enCitas) { if (enCitas) citasVm.cargar(Rol.PACIENTE) }

    val vinculo = estado.vinculo
    LaunchedEffect(vinculo?.pacienteId) { vinculo?.let { planVm.cargar(it.pacienteId) } }
    LaunchedEffect(abrirPlan, vinculo) {
        if (abrirPlan && vinculo != null) {
            enPlan = true
            onPlanConsumido()
        }
    }
    // al canjear el código se deja ver el acierto un momento antes de abrir el inicio
    var canjeado by remember { mutableStateOf(false) }
    var celebrado by remember { mutableStateOf(false) }
    val enExito = vinculo != null && canjeado
    LaunchedEffect(enExito) {
        if (enExito) {
            delay(PAUSA_EXITO_MS)
            celebrado = true
        }
    }
    when {
        estado.cargando -> PantallaCarga(conCabecera = true)
        enCitas -> {
            BackHandler { enCitas = false }
            CitasScreen(
                rol = Rol.PACIENTE,
                estado = citas,
                onActualizar = { citasVm.cargar(Rol.PACIENTE) },
                onCambiar = citasVm::cambiarEstado,
                onVolver = { enCitas = false },
                onReagendar = agendaVm::reagendar,
            )
        }
        enPlan && vinculo != null -> PlanPacienteScreen(
            pacienteId = vinculo.pacienteId,
            onVolver = {
                enPlan = false
                // volver al inicio refresca la tarjeta del plan (pudo llegar uno nuevo)
                planVm.cargar(vinculo.pacienteId)
            },
            vm = planVm,
        )
        vinculo == null && enMapa -> {
            BackHandler { enMapa = false }
            val cache = LocalContext.current.cacheDir
            MapaScreen(
                estado = mapa,
                onActualizar = mapaVm::cargar,
                onVolver = { enMapa = false },
                onVerMas = { mapaVm.abrirFicha(it, cache) },
                onCerrarFicha = mapaVm::cerrarFicha,
                onAgendar = agendaVm::abrir,
            )
        }
        vinculo == null || (enExito && !celebrado) -> VincularScreen(
            estado = estado,
            vinculado = vinculo != null,
            onCanjear = {
                canjeado = true
                vm.canjear(it, perfil)
            },
            onEditar = vm::limpiarError,
            onMapa = { enMapa = true },
            onCitas = { enCitas = true },
            onCerrarSesion = onCerrarSesion,
        )
        enExpediente -> ExpedienteFlujo(
            pacienteId = vinculo.pacienteId,
            nombrePaciente = perfil.nombre,
            editor = Rol.PACIENTE,
            onSalir = { enExpediente = false },
        )
        // inicio y perfil comparten el avatar: al tocarlo viaja de un sitio al otro y se transforma de círculo en cuadrado redondeado
        else -> CambioConAvatar(enPerfil, Modifier.fillMaxSize()) { perfilAbierto, transicion ->
            if (perfilAbierto) {
                BackHandler { enPerfil = false }
                PerfilPacienteScreen(
                    perfil = perfil,
                    vinculo = vinculo,
                    onExpediente = { enExpediente = true },
                    onCitas = { enCitas = true },
                    onVolver = { enPerfil = false },
                    onCerrarSesion = onCerrarSesion,
                    transicion = transicion,
                )
            } else {
                InicioPaciente(
                    perfil,
                    vinculo,
                    onExpediente = { enExpediente = true },
                    onPerfil = { enPerfil = true },
                    transicion = transicion,
                    plan = plan.plan,
                    onPlan = { enPlan = true },
                )
            }
        }
    }

    // la agenda se abre desde el mapa (pedir cita) y desde «Mis citas» (reagendar): va encima de lo que haya
    AgendarCitaSheet(
        estado = agenda,
        onDia = agendaVm::elegirDia,
        onHora = agendaVm::elegirHora,
        onEnviar = { agendaVm.enviar(perfil) },
        onReagendar = agendaVm::reagendar,
        onCancelarVigente = agendaVm::cancelarVigente,
        onCerrar = {
            agendaVm.cerrar()
            // lo que se pidió, reagendó o canceló desde la hoja se ve en «Mis citas»
            if (enCitas) citasVm.cargar(Rol.PACIENTE)
        },
    )
}

@Composable
private fun InicioPaciente(
    perfil: Perfil,
    vinculo: Vinculo,
    onExpediente: () -> Unit,
    onPerfil: () -> Unit,
    transicion: TransicionPerfil,
    // el último plan que le envió su nutriólogo, si lo hay
    plan: PlanEnviado?,
    onPlan: () -> Unit,
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

    // deslizar hacia abajo vuelve a pedir el expediente, lo único del inicio que no es de muestra
    ActualizarAlDeslizar(
        refrescando = expediente.cargando,
        onActualizar = { expedienteVm.cargar(vinculo.pacienteId) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(colores.background),
            state = lista,
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item { CabeceraInicio(perfil.nombre, resumenDia, onPerfil, imagenAvatar = rememberFotoAvatar(perfil), imagenPortada = rememberFotoPortada(perfil), transicion = transicion) }
            // una vez completo, el expediente se consulta desde el perfil
            if (expediente.cargando || !expediente.expediente.completo) {
                item { TarjetaExpediente(expediente, onExpediente, Modifier.padding(horizontal = 16.dp)) }
            }
            plan?.let {
                item {
                    FilaAjuste(
                        icono = R.drawable.ic_documento,
                        titulo = stringResource(R.string.plan_tarjeta_titulo),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        etiqueta = stringResource(R.string.plan_tarjeta_etiqueta, it.nutriologoNombre, fechaMedia(it.enviadoEn)),
                        onClick = onPlan,
                    )
                }
            }
            item { MetricasFitness(resumenDia) }
            item { RutinaDeHoy(rutinaDeHoy) }
            item { DietaNutricion(comidas) }
            item { ActividadReciente() }
            item { TarjetaCoach(vinculo.nutriologoNombre) }
            item { EjerciciosExtra(ejerciciosExtra) }
        }
    }
}
