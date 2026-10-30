package com.example.nutrigajo.ui.paciente.inicio

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.data.push.Avisos
import com.example.nutrigajo.data.repository.ChatRepository
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
import com.example.nutrigajo.ui.shared.chat.ChatScreen
import com.example.nutrigajo.ui.shared.chat.chatVistoHasta
import com.example.nutrigajo.ui.shared.chat.sinLeer
import com.example.nutrigajo.ui.shared.chat.vistaPreviaChat
import com.example.nutrigajo.ui.paciente.avisos.NotificacionesScreen
import com.example.nutrigajo.ui.paciente.avisos.marcarVisto
import com.example.nutrigajo.ui.paciente.avisos.vistoHasta
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
import com.example.nutrigajo.ui.shared.metas.MetasSheet
import com.example.nutrigajo.ui.shared.metas.MetasViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private const val PAUSA_EXITO_MS = 1400L

// Entrada del paciente: sin nutriólogo pide el código (o deja buscarlo en el mapa); con uno, abre su inicio.
// ponytail: las secciones del inicio salvo el expediente, el plan y el coach usan datos de muestra (DatosMuestra.kt).
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
    metasVm: MetasViewModel = viewModel(key = "metas-${perfil.uid}"),
    // se tocó la notificación de un plan nuevo: se abre directo; `onPlanConsumido` avisa que ya se atendió
    abrirPlan: Boolean = false,
    onPlanConsumido: () -> Unit = {},
    // se tocó el aviso de un mensaje nuevo (el id del paciente de la conversación): se abre el chat con su nutriólogo
    abrirChat: String? = null,
    onChatConsumido: () -> Unit = {},
) {
    val estado by vm.estado.collectAsState()
    val mapa by mapaVm.estado.collectAsState()
    val agenda by agendaVm.estado.collectAsState()
    val citas by citasVm.estado.collectAsState()
    val plan by planVm.estado.collectAsState()
    val metasEstado by metasVm.estado.collectAsState()
    var enMapa by rememberSaveable { mutableStateOf(false) }
    var enExpediente by rememberSaveable { mutableStateOf(false) }
    var enPerfil by rememberSaveable { mutableStateOf(false) }
    var enCitas by rememberSaveable { mutableStateOf(false) }
    var enPlan by rememberSaveable { mutableStateOf(false) }
    var enAvisos by rememberSaveable { mutableStateOf(false) }
    var enChat by rememberSaveable { mutableStateOf(false) }
    // la pantalla de métricas abierta desde las tarjetas y el agua de hoy, que sube con el + y se ve también en la tarjeta
    var metrica by rememberSaveable { mutableStateOf<Metrica?>(null) }
    // «Ver todas» de las métricas: la cuadrícula se queda abierta debajo de la pantalla de la métrica que se abre desde ella
    var todasMetricas by rememberSaveable { mutableStateOf(false) }
    // «Dieta & nutrición»: -1 abre Mis comidas y desde 0 la comida (su lugar en `platos`) cuya tarjeta se tocó; lo que se agrega
    // a mano vive mientras la app esté abierta (ponytail: de muestra, como el resto de la sección)
    var dietaAbierta by rememberSaveable { mutableStateOf(false) }
    var dietaInicio by rememberSaveable { mutableIntStateOf(-1) }
    val comidasAgregadas = remember { mutableStateListOf<Comida>() }
    var aguaMl by rememberSaveable { mutableIntStateOf(resumenDia.hidratacionMl) }
    // la hoja de las metas diarias, abierta desde el engrane de las pantallas de métricas
    var metasAbiertas by rememberSaveable { mutableStateOf(false) }
    // sube al volver del chat para que los pendientes de la tarjeta se cuenten de nuevo
    var chatVersion by remember { mutableIntStateOf(0) }
    // el plan que se abrió desde las notificaciones; sin él se abre el último
    var planAbierto by rememberSaveable { mutableStateOf<String?>(null) }
    // hasta qué plan ha visto en sus notificaciones (guardado en el teléfono); `vistoAlAbrir` congela ese valor mientras se ve la
    // lista, para que los puntos de «nuevo» no desaparezcan en cuanto se abre
    val contexto = LocalContext.current
    // sus planes le llegan por aviso: desde Android 13 el permiso se pide al abrir su inicio, no solo al agendar una cita
    val pedirAvisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { if (Avisos.sinPermiso(contexto)) pedirAvisos.launch(Manifest.permission.POST_NOTIFICATIONS) }
    var visto by remember(perfil.uid) { mutableLongStateOf(vistoHasta(contexto, perfil.uid)) }
    var vistoAlAbrir by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(enAvisos, enPlan, plan.planes) {
        val masNuevo = plan.planes.firstOrNull()?.enviadoEn
        if ((enAvisos || enPlan) && masNuevo != null && masNuevo > visto) {
            visto = masNuevo
            marcarVisto(contexto, perfil.uid, masNuevo)
        }
    }
    LaunchedEffect(Unit) { vm.cargar() }
    LaunchedEffect(enMapa) { if (enMapa) mapaVm.cargar() }
    LaunchedEffect(enCitas) { if (enCitas) citasVm.cargar(Rol.PACIENTE) }

    val vinculo = estado.vinculo
    LaunchedEffect(vinculo?.pacienteId) { vinculo?.let { planVm.cargar(it.pacienteId) } }
    // los mensajes de su nutriólogo que aún no ha visto, para la tarjeta del chat; solo se consultan si el último resumen es suyo y nuevo
    val chats = remember { ChatRepository() }
    var chatPendientes by remember { mutableIntStateOf(0) }
    LaunchedEffect(vinculo?.ultimoMensaje, chatVersion) {
        val v = vinculo ?: return@LaunchedEffect
        val chatVisto = chatVistoHasta(contexto, perfil.uid, v.pacienteId)
        chatPendientes = when {
            !v.ultimoMensaje.sinLeer(perfil.uid, chatVisto) -> 0
            // sin red, al menos se sabe que hay uno
            else -> try {
                chats.pendientes(v.pacienteId, chatVisto)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                1
            }
        }
    }
    LaunchedEffect(abrirPlan, vinculo) {
        if (abrirPlan && vinculo != null) {
            planAbierto = null
            enPlan = true
            onPlanConsumido()
        }
    }
    LaunchedEffect(abrirChat, vinculo) {
        if (abrirChat != null && vinculo != null) {
            enChat = true
            onChatConsumido()
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
    val metricaAbierta = metrica
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
                planAbierto = null
                // volver refresca la tarjeta del plan y las notificaciones (pudo llegar uno nuevo)
                planVm.cargar(vinculo.pacienteId)
            },
            vm = planVm,
            planId = planAbierto,
        )
        // las notificaciones: cada plan recibido se puede abrir de nuevo; al volver del PDF se regresa a la lista
        enAvisos && vinculo != null -> {
            BackHandler { enAvisos = false }
            NotificacionesScreen(
                planes = plan.planes,
                cargando = plan.cargando,
                error = plan.error,
                vistoAlAbrir = vistoAlAbrir,
                onActualizar = { planVm.cargar(vinculo.pacienteId) },
                onPlan = {
                    planAbierto = it.id
                    enPlan = true
                },
                onVolver = { enAvisos = false },
            )
        }
        // el chat con su nutriólogo; al volver se lee otra vez el vínculo, que trae el último mensaje
        enChat && vinculo != null -> ChatScreen(
            pacienteId = vinculo.pacienteId,
            nombre = vinculo.nutriologoNombre,
            subtitulo = stringResource(R.string.chat_subtitulo_nutriologo),
            onVolver = {
                enChat = false
                chatVersion++
                vm.refrescar()
            },
        )
        // las pantallas de «Métricas fitness»
        metricaAbierta != null && vinculo != null -> MetricaScreen(
            metrica = metricaAbierta,
            resumen = resumenDia.copy(hidratacionMl = aguaMl.coerceAtMost(vinculo.metas.aguaMl)),
            metas = vinculo.metas,
            onAgregarAgua = { delta -> aguaMl = (aguaMl.coerceAtMost(vinculo.metas.aguaMl) + delta).coerceIn(0, vinculo.metas.aguaMl) },
            onAbrir = { metrica = it },
            onAjustes = {
                metasVm.limpiar()
                metasAbiertas = true
            },
            onVolver = { metrica = null },
        )
        todasMetricas && vinculo != null -> TodasLasMetricasScreen(
            resumen = resumenDia.copy(hidratacionMl = aguaMl.coerceAtMost(vinculo.metas.aguaMl)),
            onAbrir = { metrica = it },
            onVolver = { todasMetricas = false },
        )
        // las pantallas de «Dieta & nutrición»
        dietaAbierta && vinculo != null -> DietaFlujo(
            inicio = dietaInicio,
            agregadas = comidasAgregadas,
            onAgregar = { comidasAgregadas.add(it) },
            onSalir = { dietaAbierta = false },
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
                    onPlan = {
                        planAbierto = null
                        enPlan = true
                    },
                    sinLeer = plan.planes.count { it.enviadoEn > visto },
                    onNotificaciones = {
                        vistoAlAbrir = visto
                        planVm.cargar(vinculo.pacienteId)
                        enAvisos = true
                    },
                    onActualizarPlan = {
                        planVm.cargar(vinculo.pacienteId)
                        vm.refrescar()
                    },
                    chatPendientes = chatPendientes,
                    onChat = { enChat = true },
                    aguaMl = aguaMl,
                    onMetrica = { metrica = it },
                    onVerTodasMetricas = { todasMetricas = true },
                    onDieta = {
                        dietaInicio = it
                        dietaAbierta = true
                    },
                )
            }
        }
    }

    // las metas se ajustan en una hoja encima de la pantalla de métricas; al guardar se vuelve a leer el vínculo, que las trae
    if (metasAbiertas && vinculo != null) {
        MetasSheet(
            metas = vinculo.metas,
            estado = metasEstado,
            onGuardar = { metasVm.guardar(vinculo.pacienteId, it, Rol.PACIENTE) },
            onGuardado = {
                metasVm.limpiar()
                metasAbiertas = false
                vm.refrescar()
            },
            onCerrar = {
                metasVm.limpiar()
                metasAbiertas = false
            },
        )
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
    // los planes nuevos que aún no ha visto: la campana lleva un punto y abre las notificaciones
    sinLeer: Int,
    onNotificaciones: () -> Unit,
    onActualizarPlan: () -> Unit,
    // cuántos mensajes de su nutriólogo no ha visto: la tarjeta del coach los cuenta y abre el chat
    chatPendientes: Int,
    onChat: () -> Unit,
    // el agua de hoy (la tarjeta de hidratación la muestra) y la apertura de la pantalla de cada métrica
    aguaMl: Int,
    onMetrica: (Metrica) -> Unit,
    onVerTodasMetricas: () -> Unit,
    // abre Mis comidas (-1) o la comida que se tocó (su lugar en `platos`)
    onDieta: (Int) -> Unit,
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

    // deslizar hacia abajo vuelve a pedir el expediente, el plan y el último mensaje, lo único del inicio que no es de muestra
    ActualizarAlDeslizar(
        refrescando = expediente.cargando,
        onActualizar = {
            expedienteVm.cargar(vinculo.pacienteId)
            onActualizarPlan()
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(colores.background),
            state = lista,
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item { CabeceraInicio(perfil.nombre, resumenDia, onPerfil, imagenAvatar = rememberFotoAvatar(perfil), imagenPortada = rememberFotoPortada(perfil), transicion = transicion, sinLeer = sinLeer, onNotificaciones = onNotificaciones) }
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
            item { MetricasFitness(resumenDia.copy(hidratacionMl = aguaMl.coerceAtMost(vinculo.metas.aguaMl)), onMetrica, onVerTodasMetricas) }
            item { RutinaDeHoy(rutinaDeHoy) }
            item { DietaNutricion(comidas, onVerTodas = { onDieta(-1) }, onComida = { onDieta(platos.indexOf(it)) }) }
            item { ActividadReciente() }
            item {
                // las etiquetas dicen qué trae el plan que aún no ha visto
                val nuevo = if (sinLeer > 0) plan?.contenido else null
                TarjetaCoach(
                    nutriologo = vinculo.nutriologoNombre,
                    pendientes = chatPendientes,
                    ultimo = vistaPreviaChat(vinculo.ultimoMensaje, perfil.uid, stringResource(R.string.chat_entrada_vacia)),
                    nuevaRutina = nuevo?.sesiones?.isNotEmpty() == true,
                    nuevaDieta = nuevo?.let { it.calorias != null || it.equivalentes.isNotEmpty() || it.opciones.isNotEmpty() } == true,
                    onChat = onChat,
                )
            }
            item { EjerciciosExtra(ejerciciosExtra) }
        }
    }
}
