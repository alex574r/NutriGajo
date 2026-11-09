package com.example.nutrigajo.ui.paciente.inicio

import com.example.nutrigajo.ui.shared.platillos.PlatillosViewModel
import com.example.nutrigajo.ui.shared.platillos.PlatillosEstado
import com.example.nutrigajo.ui.paciente.platillos.ExplorarPlatillosScreen
import com.example.nutrigajo.domain.model.paraPaciente
import com.example.nutrigajo.ui.paciente.rutina.HistorialEntrenamientosScreen
import com.example.nutrigajo.ui.shared.ejercicios.EjerciciosViewModel
import com.example.nutrigajo.ui.shared.ejercicios.EjerciciosEstado
import com.example.nutrigajo.ui.paciente.rutina.EntrenarScreen
import com.example.nutrigajo.domain.model.EjercicioAsignado
import com.example.nutrigajo.domain.model.FuenteEjercicio
import com.example.nutrigajo.domain.model.resolverDelBanco
import com.example.nutrigajo.ui.shared.ejercicios.CatalogoViewModel
import com.example.nutrigajo.domain.model.rutinaAsignada
import com.example.nutrigajo.domain.model.rutinaDelDia
import com.example.nutrigajo.domain.model.RutinaDelDia
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.example.nutrigajo.widgets.PerfilWidget
import com.example.nutrigajo.widgets.DatosWidget
import com.example.nutrigajo.data.local.AguaDeHoy
import com.example.nutrigajo.domain.model.MetasDiarias
import com.example.nutrigajo.domain.model.avanceSemanal
import com.example.nutrigajo.domain.model.calendario
import com.example.nutrigajo.domain.model.opcionDelDia
import com.example.nutrigajo.domain.model.diasDePasos
import com.example.nutrigajo.domain.model.racha
import com.example.nutrigajo.data.repository.sincronizarDiasRacha
import com.example.nutrigajo.domain.model.scoreDelDia
import com.example.nutrigajo.domain.model.Racha
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.data.carrera.Fase
import com.example.nutrigajo.data.carrera.RastreoRuta
import com.example.nutrigajo.data.push.Avisos
import com.example.nutrigajo.data.repository.ChatRepository
import com.example.nutrigajo.domain.model.Cita
import com.example.nutrigajo.domain.model.OrigenCita
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.proxima
import com.example.nutrigajo.ui.paciente.citas.citaVistaId
import com.example.nutrigajo.ui.paciente.citas.marcarCitaVista
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.domain.model.Vinculo
import com.example.nutrigajo.ui.shared.perfil.rememberFotoAvatar
import com.example.nutrigajo.ui.shared.perfil.rememberFotoPortada
import com.example.nutrigajo.ui.components.ActualizarAlDeslizar
import com.example.nutrigajo.ui.components.BarraEstadoClara
import com.example.nutrigajo.ui.components.CambioConAvatar
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.components.TransicionPerfil
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.permisos.rememberPermiso
import com.example.nutrigajo.ui.permisos.HojaAvisos
import com.example.nutrigajo.domain.debePedirseSolo
import com.example.nutrigajo.domain.PermisoApp
import com.example.nutrigajo.data.local.AjustesApp
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
import com.example.nutrigajo.ui.paciente.carrera.CarreraFlujo
import com.example.nutrigajo.ui.paciente.ejercicios.EjerciciosAsignadosScreen
import com.example.nutrigajo.ui.paciente.ejercicios.ExplorarEjerciciosScreen
import com.example.nutrigajo.ui.paciente.carrera.CarreraViewModel
import com.example.nutrigajo.ui.paciente.reloj.FaseReloj
import com.example.nutrigajo.ui.paciente.reloj.RelojScreen
import com.example.nutrigajo.ui.paciente.reloj.RelojViewModel
import com.example.nutrigajo.widgets.AlmacenWidget
import com.example.nutrigajo.widgets.actualizarWidgets
import com.example.nutrigajo.ui.paciente.citas.AgendaViewModel
import com.example.nutrigajo.ui.paciente.plan.PlanPacienteScreen
import com.example.nutrigajo.ui.paciente.plan.PlanPacienteViewModel
import com.example.nutrigajo.ui.paciente.citas.AgendarCitaSheet
import com.example.nutrigajo.ui.paciente.mapa.MapaScreen
import com.example.nutrigajo.ui.paciente.mapa.MapaViewModel
import com.example.nutrigajo.ui.shared.agenda.AgendaScreen
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
    carreraVm: CarreraViewModel = viewModel(key = "carrera-${perfil.uid}"),
    relojVm: RelojViewModel = viewModel(key = "reloj-${perfil.uid}"),
    caloriasVm: CaloriasViewModel = viewModel(key = "calorias-${perfil.uid}"),
    // se tocó la notificación de un plan nuevo: se abre directo; `onPlanConsumido` avisa que ya se atendió
    abrirPlan: Boolean = false,
    onPlanConsumido: () -> Unit = {},
    // se tocó el aviso de un mensaje nuevo (el id del paciente de la conversación): se abre el chat con su nutriólogo
    abrirChat: String? = null,
    onChatConsumido: () -> Unit = {},
    // se tocó el aviso de una cita (confirmada, agendada o cancelada): se abre «Mis citas»
    abrirCitas: Boolean = false,
    onCitasConsumido: () -> Unit = {},
) {
    val estado by vm.estado.collectAsState()
    val mapa by mapaVm.estado.collectAsState()
    val agenda by agendaVm.estado.collectAsState()
    val citas by citasVm.estado.collectAsState()
    val plan by planVm.estado.collectAsState()
    val metasEstado by metasVm.estado.collectAsState()
    // el reloj: si está conectado y dio pasos, los de las métricas son los suyos; si no, los de muestra de siempre
    val reloj by relojVm.estado.collectAsState()
    val pasosReloj = reloj.resumen?.actividad?.let { Pasos(it.pasos, it.kcal, it.km, it.minutos) } ?: pasosMuestra
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { relojVm.cargar() }
    // lo que la app anotó (Modo carrera y rutinas), que junto con el del reloj da las calorías gastadas
    val caloriasApp by caloriasVm.estado.collectAsState()
    var enMapa by rememberSaveable { mutableStateOf(false) }
    var enExpediente by rememberSaveable { mutableStateOf(false) }
    var enPerfil by rememberSaveable { mutableStateOf(false) }
    var enCitas by rememberSaveable { mutableStateOf(false) }
    var enPlan by rememberSaveable { mutableStateOf(false) }
    var enAvisos by rememberSaveable { mutableStateOf(false) }
    var enChat by rememberSaveable { mutableStateOf(false) }
    var enCarrera by rememberSaveable { mutableStateOf(false) }
    var enReloj by rememberSaveable { mutableStateOf(false) }
    var enEjercicios by rememberSaveable { mutableStateOf(false) }
    // el banco global de platillos: Explorar abre la lista (null) o el platillo que se tocó en el inicio
    var enPlatillos by rememberSaveable { mutableStateOf(false) }
    var platilloInicial by rememberSaveable { mutableStateOf<String?>(null) }
    // el ejercicio que se tocó en el inicio: Explorar lo abre directo; null abre la lista
    var ejercicioInicial by rememberSaveable { mutableStateOf<String?>(null) }
    var enRutina by rememberSaveable { mutableStateOf(false) }
    var enHistorial by rememberSaveable { mutableStateOf(false) }
    // la lista de actividades que se abre con «Ver todas» de Actividades
    var enActividades by rememberSaveable { mutableStateOf(false) }
    // la pantalla de métricas abierta desde las tarjetas y el agua de hoy, que sube con el + y se ve también en la tarjeta
    var metrica by rememberSaveable { mutableStateOf<Metrica?>(null) }
    // «Ver todas» de las métricas: la cuadrícula se queda abierta debajo de la pantalla de la métrica que se abre desde ella
    var todasMetricas by rememberSaveable { mutableStateOf(false) }
    // «Dieta & nutrición»: -1 abre Mis comidas y desde 0 la comida (su lugar en `platos`) cuya tarjeta se tocó; lo que se agrega
    // a mano vive mientras la app esté abierta (ponytail: de muestra, como el resto de la sección)
    var dietaAbierta by rememberSaveable { mutableStateOf(false) }
    var dietaInicio by rememberSaveable { mutableIntStateOf(-1) }
    val comidasAgregadas = remember { mutableStateListOf<Comida>() }
    // la hoja de las metas diarias, abierta desde el engrane de las pantallas de métricas
    var metasAbiertas by rememberSaveable { mutableStateOf(false) }
    // sube al volver del chat para que los pendientes de la tarjeta se cuenten de nuevo
    var chatVersion by remember { mutableIntStateOf(0) }
    // el plan que se abrió desde las notificaciones; sin él se abre el último
    var planAbierto by rememberSaveable { mutableStateOf<String?>(null) }
    // hasta qué plan ha visto en sus notificaciones (guardado en el teléfono); `vistoAlAbrir` congela ese valor mientras se ve la
    // lista, para que los puntos de «nuevo» no desaparezcan en cuanto se abre
    val contexto = LocalContext.current
    // el agua de hoy: se guarda en el teléfono (por cuenta) y vale solo el día en que se anotó
    var aguaMl by remember(perfil.uid) { mutableIntStateOf(AguaDeHoy.leer(contexto, perfil.uid)) }
    LaunchedEffect(aguaMl) { AguaDeHoy.guardar(contexto, perfil.uid, aguaMl) }
    // sus planes le llegan por aviso: la primera vez que abre su inicio se le ofrece activarlos con una hoja que lo explica; no se vuelve a ofrecer
    // sola (el estado queda en Configuración → Permisos)
    val avisos = rememberPermiso(PermisoApp.NOTIFICACIONES)
    var hojaAvisos by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (debePedirseSolo(avisos.estado, AjustesApp.yaSeOfrecio(contexto, PermisoApp.NOTIFICACIONES.grupo))) {
            AjustesApp.marcarOfrecido(contexto, PermisoApp.NOTIFICACIONES.grupo)
            hojaAvisos = true
        }
    }
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
    // «Próxima cita» del inicio: las citas se piden al abrirlo; la que agendó el nutriólogo es «Nueva» hasta que se abre «Mis citas»
    LaunchedEffect(Unit) { citasVm.cargar(Rol.PACIENTE) }
    // derivada: jalar para actualizar vuelve a pedir las citas (`cargando` cambia dos veces) y esto no debe recomponer el inicio entero
    val citaProxima by remember { derivedStateOf { citas.citas.proxima(System.currentTimeMillis()) } }
    var citaVista by remember(perfil.uid) { mutableStateOf(citaVistaId(contexto, perfil.uid)) }
    LaunchedEffect(enCitas, citaProxima?.id) {
        val proxima = citaProxima
        if (enCitas && proxima != null) {
            marcarCitaVista(contexto, perfil.uid, proxima.id)
            citaVista = proxima.id
        }
    }
    LaunchedEffect(abrirCitas) {
        if (abrirCitas) {
            enCitas = true
            onCitasConsumido()
        }
    }
    // el banco global alimenta «Rutinas de hoy» y «Más ejercicios extra»; Explorar usa la misma instancia
    val ejerciciosVm: EjerciciosViewModel = viewModel(key = "explorar-ejercicios")
    val ejercicios by ejerciciosVm.estado.collectAsState()
    LaunchedEffect(Unit) { ejerciciosVm.cargarPublicados() }
    // lo que su nutriólogo le asignó (el plan más reciente) manda el día que tiene algo; los demás días, la rutina al azar de siempre
    val planActual = plan.planes.firstOrNull()
    val asignados = planActual?.contenido?.ejerciciosAsignados.orEmpty()
    // cómo se cuentan los días de ese plan (semana o desde el día de envío) y qué día es hoy dentro de él
    val calendario = remember(planActual?.id, planActual?.enviadoEn) { planActual?.calendario() }
    val diaDeHoy = calendario?.diaDe(LocalDate.now())
    // en un plan con duración, la opción de menú que toca hoy
    val menuDeHoy = remember(planActual?.id, diaDeHoy) {
        val c = planActual?.contenido
        val total = c?.duracionDias
        val dia = diaDeHoy
        val indice = if (c != null && total != null && dia != null) c.opcionDelDia(dia) else null
        if (c != null && total != null && dia != null && indice != null) MenuDeHoy(dia, total, indice + 1, c.opciones[indice]) else null
    }
    // lo asignado del catálogo en la nube se resuelve con su ficha, que se baja al abrir el inicio (la foto y el clip, al abrir el ejercicio)
    val catalogoVm: CatalogoViewModel = viewModel(key = "inicio-catalogo")
    val catalogo by catalogoVm.estado.collectAsState()
    LaunchedEffect(asignados) { catalogoVm.cargarFichas(asignados.filter { it.fuente == FuenteEjercicio.CATALOGO }.map { it.id }) }
    val rutina = remember(ejercicios.items, asignados, catalogo.fichas, diaDeHoy) {
        val hoy = LocalDate.now()
        // fuera del periodo de un plan con duración (aún no empieza o ya acabó) no hay día asignado: cae a la rutina de siempre
        val asignada = diaDeHoy?.let { rutinaAsignada(hoy, asignados, resolverDelBanco(ejercicios.items, catalogo.fichas), dia = it) }
        asignada ?: rutinaDelDia(hoy, ejercicios.items)
    }
    var enAsignados by rememberSaveable { mutableStateOf(false) }
    // el banco global de platillos alimenta «Dieta & nutrición»; Explorar usa la misma instancia
    val platillosVm: PlatillosViewModel = viewModel(key = "explorar-platillos")
    val platillos by platillosVm.estado.collectAsState()
    LaunchedEffect(Unit) { platillosVm.cargarPublicados() }

    val vinculo = estado.vinculo
    // tras desvincularse ya no hay perfil con nutriólogo que mostrar: al volver a vincularse se abre el inicio, no «Mi nutriólogo»
    LaunchedEffect(vinculo == null) { if (vinculo == null) enPerfil = false }
    // la racha: días seguidos con la meta de pasos del reloj o con una caminata o carrera guardada (al volver del Modo carrera se cuenta otra vez)
    // los días se reconstruyen desde la nube al abrir el inicio (sin esperar a que abra el historial) y `versionDias` avisa de que hay que volver a leerlos
    var versionDias by remember { mutableIntStateOf(0) }
    LaunchedEffect(perfil.uid) {
        sincronizarDiasRacha(contexto)
        versionDias++
    }
    val diasCarrera = remember(enCarrera, enRutina, enHistorial, reloj.resumen, versionDias) { AlmacenWidget.diasCarrera(contexto) }
    // un día de pasos ganado queda anotado para siempre: si el nutriólogo sube la meta después, la racha ya ganada no cambia. Solo con el vínculo
    // cargado: antes de eso la meta es la de por defecto y marcaría días de más.
    LaunchedEffect(reloj.resumen, vinculo?.metas?.pasos) {
        val meta = vinculo?.metas?.pasos ?: return@LaunchedEffect
        val ganados = diasDePasos(reloj.resumen?.pasosPorDia.orEmpty(), meta)
        if (ganados.isNotEmpty()) {
            AlmacenWidget.agregarDiasCarrera(contexto, ganados)
            versionDias++
        }
    }
    val rachaDeHoy = remember(reloj.resumen, diasCarrera, vinculo?.metas?.pasos) {
        racha(LocalDate.now(), reloj.resumen?.pasosPorDia.orEmpty(), diasCarrera, vinculo?.metas?.pasos ?: DatosWidget.META_PASOS)
    }
    // los números del encabezado y de las tarjetas. Score: el promedio de lo cumplido hoy de cada meta (pasos, kcal y minutos del reloj
    // si está conectado, agua, y si hubo una caminata, carrera o rutina). Avance: los días de la semana en que se cumplió algo.
    // «Pro» no se calcula: este inicio (con su encabezado) solo existe con un nutriólogo vinculado.
    val metas = vinculo?.metas ?: MetasDiarias()
    // las calorías gastadas (tarjeta «Calorías», su pantalla y «Actividades»): las activas del reloj y lo que guardó la app, sin duplicar
    // lo que ambos vieron; se leen al abrir y de nuevo al volver del Modo carrera o de una rutina, que acaban de guardar algo
    val estadoKcal = remember(reloj.fase, reloj.resumen, caloriasApp) {
        EstadoKcal(
            datos = DatosKcal(
                relojPorDia = reloj.resumen?.kcalPorDia.orEmpty(),
                relojPorHora = reloj.resumen?.actividad?.kcalPorHora.orEmpty(),
                app = caloriasApp.gastos,
            ),
            // sin Health Connect (o sin permiso) la fase deja de ser CARGANDO y solo cuenta lo de la app
            cargando = reloj.fase == FaseReloj.CARGANDO || caloriasApp.cargando,
            error = caloriasApp.error,
        )
    }
    var vinoDeGuardar by remember { mutableStateOf(false) }
    val enFlujoDeKcal = enCarrera || enRutina
    LaunchedEffect(enFlujoDeKcal, vinculo != null) {
        // sin nutriólogo no hay inicio con calorías que mostrar
        if (vinculo == null) return@LaunchedEffect
        if (enFlujoDeKcal) {
            vinoDeGuardar = true
        } else {
            caloriasVm.cargar(forzar = vinoDeGuardar)
            vinoDeGuardar = false
        }
    }
    val resumenHoy = remember(aguaMl, reloj.resumen, diasCarrera, metas, estadoKcal) {
        val hoy = LocalDate.now()
        val actividad = reloj.resumen?.actividad
        resumenDia.copy(
            calorias = kcalDeHoy(estadoKcal.datos, Instant.now(), ZoneId.systemDefault()),
            hidratacionMl = aguaMl.coerceAtMost(metas.aguaMl),
            score = scoreDelDia(
                listOfNotNull(
                    aguaMl to metas.aguaMl,
                    (if (hoy.toEpochDay() in diasCarrera) 1 else 0) to 1,
                    actividad?.let { it.pasos to metas.pasos },
                    actividad?.let { it.kcal to metas.kcal },
                    actividad?.let { it.minutos to metas.minutos },
                ),
            ),
            avance = avanceSemanal(hoy, reloj.resumen?.pasosPorDia.orEmpty(), diasCarrera, metas.pasos),
        )
    }
    // los anillos del widget «Resumen de hoy» se miden contra estas metas
    LaunchedEffect(vinculo?.metas) {
        vinculo?.metas?.let {
            AlmacenWidget.guardarMetas(contexto, it.pasos, it.kcal, it.minutos)
            actualizarWidgets(contexto)
        }
    }
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
    // el widget de saludo enseña el nombre y lo que le espera: los planes y los mensajes de su nutriólogo que aún no ha visto
    val planesNuevos = plan.planes.count { it.enviadoEn > visto }
    LaunchedEffect(perfil.uid, perfil.nombre, planesNuevos, chatPendientes) {
        AlmacenWidget.guardarPerfil(contexto, PerfilWidget(perfil.nombre, perfil.uid, planesNuevos, chatPendientes))
        actualizarWidgets(contexto)
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
    CompositionLocalProvider(LocalPasos provides pasosReloj, LocalCalorias provides estadoKcal) {
    when {
        estado.cargando -> PantallaCarga(conCabecera = true)
        // el modo carrera: la ruta en el mapa, el historial y los planes de caminata (el peso del expediente afina las calorías)
        enCarrera && vinculo != null -> {
            val expedienteVm: ExpedienteViewModel = viewModel(key = "expediente-${vinculo.pacienteId}-${Rol.PACIENTE.name}")
            val expediente by expedienteVm.estado.collectAsState()
            CarreraFlujo(pesoKg = expediente.expediente.pesoKg, onSalir = { enCarrera = false }, vm = carreraVm)
        }
        // el reloj: el pulso que su app manda a Health Connect
        enReloj -> RelojScreen(onVolver = { enReloj = false }, vm = relojVm)
        // «Entrenar ahora»: la rutina del día paso a paso
        enRutina && rutina != null -> EntrenarScreen(rutina, onSalir = { enRutina = false })
        // lo ya entrenado
        enHistorial -> HistorialEntrenamientosScreen(onVolver = { enHistorial = false })
        // lo que la app guardó y suma a las calorías
        enActividades -> ActividadesScreen(caloriasApp, onVolver = { enActividades = false })
        // lo que su nutriólogo le asignó en el plan más reciente
        enAsignados && asignados.isNotEmpty() -> EjerciciosAsignadosScreen(asignados, ejercicios.items, ejercicios.cargando, catalogo.fichas, catalogo.cargandoFichas, calendario, onVolver = { enAsignados = false })
        // el banco global de ejercicios de los entrenadores
        enEjercicios -> ExplorarEjerciciosScreen(
            abrirId = ejercicioInicial,
            onVolver = {
                enEjercicios = false
                ejercicioInicial = null
            },
        )
        // el banco global de platillos de los nutriólogos y entrenadores, con las alergias y la dieta del expediente
        enPlatillos && vinculo != null -> {
            val expedienteVm: ExpedienteViewModel = viewModel(key = "expediente-${vinculo.pacienteId}-${Rol.PACIENTE.name}")
            val expediente by expedienteVm.estado.collectAsState()
            // al volver tras cerrar el proceso nadie más lo ha leído: se pide aquí, porque sin las alergias no se muestra nada
            LaunchedEffect(vinculo.pacienteId) { expedienteVm.cargar(vinculo.pacienteId) }
            ExplorarPlatillosScreen(
                abrirId = platilloInicial,
                alergias = expediente.expediente.alergias,
                dieta = expediente.expediente.dieta,
                expedienteListo = !expediente.cargando && expediente.error == null,
                expedienteCargando = expediente.cargando,
                onActualizarExpediente = { expedienteVm.cargar(vinculo.pacienteId) },
                menuDeHoy = menuDeHoy,
                onVolver = {
                    enPlatillos = false
                    platilloInicial = null
                },
            )
        }
        enCitas -> {
            BackHandler { enCitas = false }
            AgendaScreen(
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
            otraId = vinculo.nutriologoId,
            onVolver = {
                enChat = false
                chatVersion++
                vm.refrescar()
            },
        )
        // las pantallas de «Métricas fitness»
        metricaAbierta != null && vinculo != null -> MetricaScreen(
            metrica = metricaAbierta,
            resumen = resumenHoy,
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
            resumen = resumenHoy,
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
                    vinculoVm = vm,
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
                        citasVm.cargar(Rol.PACIENTE)
                    },
                    proxima = citaProxima,
                    citaNueva = citaProxima?.let { it.origen == OrigenCita.NUTRIOLOGO && it.id != citaVista } == true,
                    onCitas = { enCitas = true },
                    chatPendientes = chatPendientes,
                    onChat = { enChat = true },
                    resumen = resumenHoy,
                    onMetrica = { metrica = it },
                    onVerTodasMetricas = { todasMetricas = true },
                    onActividades = { enActividades = true },
                    onCarrera = { enCarrera = true },
                    onReloj = { enReloj = true },
                    onEjercicios = { id ->
                        ejercicioInicial = id
                        enEjercicios = true
                    },
                    ejercicios = ejercicios,
                    rutina = rutina,
                    onRutina = { enRutina = true },
                    onHistorial = { enHistorial = true },
                    asignados = asignados,
                    menuDeHoy = menuDeHoy,
                    onAsignados = { enAsignados = true },
                    racha = rachaDeHoy,
                    onDieta = {
                        dietaInicio = it
                        dietaAbierta = true
                    },
                    platillos = platillos,
                    onPlatillos = { id ->
                        platilloInicial = id
                        enPlatillos = true
                    },
                )
            }
        }
    }
    }

    // las metas se ajustan en una hoja encima de la pantalla de métricas; al guardar se vuelve a leer el vínculo, que las trae
    if (hojaAvisos) HojaAvisos(avisos, onCerrar = { hojaAvisos = false })

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
    // la cita que le espera (la vigente más cercana) y si la agendó su nutriólogo y aún no la ha visto; tocarla abre «Mis citas»
    proxima: Cita?,
    citaNueva: Boolean,
    onCitas: () -> Unit,
    // cuántos mensajes de su nutriólogo no ha visto: la tarjeta del coach los cuenta y abre el chat
    chatPendientes: Int,
    onChat: () -> Unit,
    // los números de hoy (score, avance, agua…) que ven el encabezado y las tarjetas, y la apertura de la pantalla de cada métrica
    resumen: ResumenDia,
    onMetrica: (Metrica) -> Unit,
    onVerTodasMetricas: () -> Unit,
    // «Ver todas» de Actividades: la lista de lo que la app guardó
    onActividades: () -> Unit,
    // abre el modo carrera
    onCarrera: () -> Unit,
    // abre la pantalla del reloj
    onReloj: () -> Unit,
    // abre el banco global de ejercicios: la lista (null) o el ejercicio que se tocó
    onEjercicios: (String?) -> Unit,
    // el banco leído, la rutina que de él sale hoy y lo que abre su tarjeta («Entrenar ahora»)
    ejercicios: EjerciciosEstado,
    rutina: RutinaDelDia?,
    onRutina: () -> Unit,
    // abre el historial de entrenamientos
    onHistorial: () -> Unit,
    // los ejercicios que su nutriólogo le asignó (plan más reciente) y lo que abre su fila
    asignados: List<EjercicioAsignado>,
    // el menú que toca hoy en un plan con duración; null si no hay
    menuDeHoy: MenuDeHoy?,
    onAsignados: () -> Unit,
    // los días seguidos que lleva cumpliendo algo (se ven en el encabezado)
    racha: Racha,
    // abre Mis comidas (-1) o la comida que se tocó (su lugar en `platos`)
    onDieta: (Int) -> Unit,
    // los platillos publicados del banco global y lo que abre su fila: la lista (null) o el platillo que se tocó
    platillos: PlatillosEstado,
    onPlatillos: (String?) -> Unit,
    // misma clave que el flujo del expediente: la tarjeta refleja lo que se guarda ahí
    expedienteVm: ExpedienteViewModel = viewModel(key = "expediente-${vinculo.pacienteId}-${Rol.PACIENTE.name}"),
) {
    val colores = MaterialTheme.colorScheme
    // el estado del expediente cambia dos veces cada vez que se vuelve a pedir (`cargando`): se lee solo dentro de las piezas que lo
    // usan, porque leerlo aquí recompondría el inicio entero al jalar para actualizar
    val expediente = expedienteVm.estado.collectAsState()
    val incompleto by remember(expediente) { derivedStateOf { !expediente.value.expediente.completo } }
    LaunchedEffect(vinculo.pacienteId) { expedienteVm.cargar(vinculo.pacienteId) }

    val lista = rememberLazyListState()
    val densidad = LocalDensity.current
    // ponytail: umbral fijo; con la cabecera ya fuera de la pantalla los íconos de la barra vuelven a ser oscuros
    val sobreFoto by remember {
        derivedStateOf { lista.firstVisibleItemIndex == 0 && lista.firstVisibleItemScrollOffset < with(densidad) { 150.dp.toPx() } }
    }
    BarraEstadoClara(sobreFoto)

    // deslizar hacia abajo vuelve a pedir el expediente, el plan y el último mensaje, lo único del inicio que no es de muestra
    JalarParaActualizar(
        expediente = expediente,
        onActualizar = {
            expedienteVm.cargar(vinculo.pacienteId)
            onActualizarPlan()
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(colores.background),
            state = lista,
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item { CabeceraInicio(perfil.nombre, resumen, onPerfil, imagenAvatar = rememberFotoAvatar(perfil), imagenPortada = rememberFotoPortada(perfil), transicion = transicion, sinLeer = sinLeer, onNotificaciones = onNotificaciones, racha = racha) }
            // una vez completo, el expediente se consulta desde el perfil (y al volver a pedirlo no reaparece)
            if (incompleto) {
                item { TarjetaExpediente(expediente.value, onExpediente, Modifier.padding(horizontal = 16.dp)) }
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
            proxima?.let { item { ProximaCita(it, citaNueva, onCitas, Modifier.padding(horizontal = 16.dp)) } }
            item { MetricasFitness(resumen, onMetrica, onVerTodasMetricas) }
            item {
                val fase = RastreoRuta.global.estado.collectAsState().value.fase
                TarjetaCarrera(enCurso = fase == Fase.CORRIENDO || fase == Fase.PAUSADA, onAbrir = onCarrera)
            }
            item {
                FilaAjuste(
                    icono = R.drawable.ic_smartwatch,
                    titulo = stringResource(R.string.reloj_entrada_titulo),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    etiqueta = stringResource(R.string.reloj_entrada_etiqueta),
                    onClick = onReloj,
                )
            }
            item { RutinaDeHoy(rutina, ejercicios.cargando, onRutina, onHistorial, asignados = asignados.size, onAsignados = onAsignados) }
            item {
                // se esconden los platillos que nombran una alergia del expediente y se prefieren los de su dieta
                // y mientras el expediente no se haya leído no se muestra ninguno (se ven esqueletos o, si falló, la entrada a Explorar)
                val estado = expediente.value
                val expedienteListo = !estado.cargando && estado.error == null
                val visibles = remember(platillos.items, estado.expediente, expedienteListo) {
                    platillos.items.paraPaciente(estado.expediente.alergias, estado.expediente.dieta, expedienteListo)
                }
                DietaNutricion(platillos.copy(cargando = platillos.cargando || estado.cargando), visibles, onAbrir = onPlatillos, onMisComidas = { onDieta(-1) })
            }
            item { ActividadReciente(onVerTodas = onActividades) }
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
            item { BancoEjercicios(ejercicios, onEjercicios) }
        }
    }
}

// «Jalar para actualizar» del inicio. Lee `cargando` aquí, no en el inicio, para que al jalar se recomponga el indicador y no la pantalla entera.
@Composable
private fun JalarParaActualizar(expediente: State<ExpedienteEstado>, onActualizar: () -> Unit, contenido: @Composable () -> Unit) {
    ActualizarAlDeslizar(refrescando = expediente.value.cargando, onActualizar = onActualizar, modifier = Modifier.fillMaxSize(), contenido = contenido)
}
