package com.example.nutrigajo.ui.nutriologo.inicio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.EstadoVerificacion
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.components.ActualizarAlDeslizar
import com.example.nutrigajo.ui.components.BarraInferior
import com.example.nutrigajo.ui.components.CambioConAvatar
import com.example.nutrigajo.ui.components.ItemBarra
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.nutriologo.pacientes.PacientesViewModel
import com.example.nutrigajo.ui.nutriologo.plan.PlanScreen
import com.example.nutrigajo.ui.nutriologo.ubicacion.UbicacionScreen
import com.example.nutrigajo.ui.nutriologo.verificacion.VerificacionScreen
import com.example.nutrigajo.ui.nutriologo.verificacion.VerificacionViewModel
import com.example.nutrigajo.ui.shared.agenda.AgendaScreen
import com.example.nutrigajo.ui.shared.citas.CitasViewModel
import com.example.nutrigajo.ui.shared.chat.ChatScreen
import com.example.nutrigajo.ui.shared.chat.chatVistoHasta
import com.example.nutrigajo.ui.shared.chat.sinLeer
import com.example.nutrigajo.ui.shared.expediente.ExpedienteFlujo
import com.example.nutrigajo.ui.shared.metas.MetasScreen
import com.example.nutrigajo.ui.shared.metas.MetasViewModel

private enum class Pestana { INICIO, PACIENTES, PERFIL }

// Inicio del nutriólogo: tres pestañas (inicio, pacientes y perfil), el mapa y el botón central de la barra, que abre
// la hoja con el código de vinculación. Las pantallas completas (formulario de cédula, mapa, expediente) tapan la barra.
// ponytail: se carga al abrir y con "Actualizar"; un listener en tiempo real cuando el expediente lo pida.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioNutriologoScreen(
    perfil: Perfil,
    onCerrarSesion: () -> Unit,
    vm: PacientesViewModel = viewModel(key = perfil.uid),
    // clave propia: dos ViewModels con la misma clave se pisarían
    verificacionVm: VerificacionViewModel = viewModel(key = "verificacion-${perfil.uid}"),
    citasVm: CitasViewModel = viewModel(key = "citas-${perfil.uid}"),
    // se tocó el aviso de un mensaje nuevo (el id del paciente de la conversación): se abre ese chat
    abrirChat: String? = null,
    onChatConsumido: () -> Unit = {},
    metasVm: MetasViewModel = viewModel(key = "metas-${perfil.uid}"),
) {
    val estado by vm.estado.collectAsState()
    val metasEstado by metasVm.estado.collectAsState()
    val citas by citasVm.estado.collectAsState()
    val fotosPacientes by vm.fotosPacientes.collectAsState()
    val pacientesConPlan by vm.pacientesConPlan.collectAsState()
    // las fotos de los pacientes se piden cada vez que la lista cambia (al abrir y al actualizar)
    val cache = LocalContext.current.cacheDir
    LaunchedEffect(estado.pacientes) { vm.cargarFotos(cache, estado.pacientes) }
    val verificacion by verificacionVm.estado.collectAsState()
    var pestana by rememberSaveable { mutableStateOf(Pestana.INICIO) }
    var enFormulario by rememberSaveable { mutableStateOf(false) }
    var enMapa by rememberSaveable { mutableStateOf(false) }
    var enCitas by rememberSaveable { mutableStateOf(false) }
    var codigoAbierto by rememberSaveable { mutableStateOf(false) }
    var pacienteAbierto by rememberSaveable { mutableStateOf<String?>(null) }
    // el panel del plan se abre desde el expediente del paciente abierto
    var planAbierto by rememberSaveable { mutableStateOf(false) }
    // el chat con el paciente abierto, también desde su expediente
    var chatAbierto by rememberSaveable { mutableStateOf(false) }
    // las metas diarias del paciente abierto, también desde su expediente
    var metasAbiertas by rememberSaveable { mutableStateOf(false) }
    // sube al volver del chat para que los puntos de «sin leer» se calculen de nuevo
    var chatVersion by remember { mutableIntStateOf(0) }
    val contexto = LocalContext.current
    // al cerrar al paciente, el panel y el chat también
    LaunchedEffect(pacienteAbierto) {
        if (pacienteAbierto == null) {
            planAbierto = false
            chatAbierto = false
            metasAbiertas = false
        }
    }
    // el aviso de un mensaje abre el chat de ese paciente en cuanto la lista de pacientes está cargada
    LaunchedEffect(abrirChat, estado.pacientes) {
        val id = abrirChat
        if (id != null && estado.pacientes.any { it.id == id }) {
            pacienteAbierto = id
            planAbierto = false
            chatAbierto = true
            onChatConsumido()
        }
    }
    // al abrir un expediente, y al volver del panel (tras enviar un plan), se mira si ese paciente ya tiene plan
    LaunchedEffect(pacienteAbierto, planAbierto) {
        pacienteAbierto?.takeIf { !planAbierto }?.let(vm::cargarPlan)
    }
    LaunchedEffect(Unit) {
        vm.cargar()
        verificacionVm.cargar()
        citasVm.cargar(Rol.NUTRIOLOGO)
    }
    // al enviar la cédula la solicitud queda pendiente: se vuelve al inicio
    LaunchedEffect(verificacion.verificacion?.estado) {
        if (verificacion.verificacion?.estado == EstadoVerificacion.PENDIENTE) enFormulario = false
    }

    // el mapa es de nutriólogos verificados: las reglas también lo exigen
    val aprobada = verificacion.verificacion?.estado == EstadoVerificacion.APROBADA
    val abierto = estado.pacientes.firstOrNull { it.id == pacienteAbierto }
    val verificar = {
        verificacionVm.limpiarError()
        enFormulario = true
    }
    val actualizar = {
        vm.cargar()
        verificacionVm.cargar()
        citasVm.cargar(Rol.NUTRIOLOGO)
    }
    // sin cuenta verificada el mapa no está disponible: se lleva al perfil, donde está el estado de la verificación
    val irAlMapa = { if (aprobada) enMapa = true else pestana = Pestana.PERFIL }

    when {
        enMapa -> UbicacionScreen(perfil = perfil, onVolver = { enMapa = false })
        enCitas -> {
            BackHandler { enCitas = false }
            AgendaScreen(
                rol = Rol.NUTRIOLOGO,
                estado = citas,
                onActualizar = { citasVm.cargar(Rol.NUTRIOLOGO) },
                onCambiar = citasVm::cambiarEstado,
                onVolver = { enCitas = false },
            )
        }
        // el chat con el paciente; al volver se leen otra vez los pacientes, que traen su último mensaje
        abierto != null && chatAbierto -> ChatScreen(
            pacienteId = abierto.id,
            nombre = abierto.nombre,
            subtitulo = stringResource(R.string.chat_subtitulo_paciente),
            otraId = abierto.usuarioId,
            onVolver = {
                chatAbierto = false
                chatVersion++
                vm.cargar()
            },
        )
        // las metas del paciente; al guardar se vuelve al expediente y se leen otra vez los pacientes, que las traen
        abierto != null && metasAbiertas -> MetasScreen(
            nombrePaciente = abierto.nombre,
            metas = abierto.metas,
            estado = metasEstado,
            onGuardar = { metasVm.guardar(abierto.id, it, Rol.NUTRIOLOGO) },
            onGuardado = {
                metasVm.limpiar()
                metasAbiertas = false
                vm.cargar()
            },
            onVolver = {
                metasVm.limpiar()
                metasAbiertas = false
            },
        )
        abierto != null && planAbierto -> PlanScreen(
            pacienteId = abierto.id,
            nombrePaciente = abierto.nombre,
            nombreNutriologo = perfil.nombre,
            onSalir = { planAbierto = false },
        )
        abierto != null -> ExpedienteFlujo(
            pacienteId = abierto.id,
            nombrePaciente = abierto.nombre,
            editor = Rol.NUTRIOLOGO,
            onSalir = { pacienteAbierto = null },
            onPlan = { planAbierto = true },
            planEnviado = abierto.id in pacientesConPlan,
            onChat = { chatAbierto = true },
            onMetas = {
                metasVm.limpiar()
                metasAbiertas = true
            },
            chatSinLeer = chatVersion.let { abierto.ultimoMensaje.sinLeer(perfil.uid, chatVistoHasta(contexto, perfil.uid, abierto.id)) },
        )
        enFormulario -> {
            BackHandler { enFormulario = false }
            VerificacionScreen(
                nombreCuenta = perfil.nombre,
                estado = verificacion,
                onEnviar = { nombreCedula, cedula -> verificacionVm.enviar(perfil, nombreCedula, cedula) },
                onEditar = verificacionVm::limpiarError,
                onVolver = { enFormulario = false },
            )
        }
        estado.cargando && estado.codigo == null && estado.pacientes.isEmpty() && estado.error == null -> PantallaCarga()
        else -> {
            BackHandler(enabled = pestana != Pestana.INICIO) { pestana = Pestana.INICIO }
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                // deslizar hacia abajo en cualquiera de las tres pestañas vuelve a cargar pacientes y verificación
                ActualizarAlDeslizar(
                    refrescando = estado.cargando || verificacion.cargando,
                    onActualizar = actualizar,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // al pasar de una pestaña a otra el avatar de la cabecera viaja al del perfil y viceversa
                    CambioConAvatar(pestana, Modifier.fillMaxSize()) { destino, transicion ->
                        when (destino) {
                            Pestana.INICIO -> TabInicio(
                                perfil = perfil,
                                estado = estado,
                                verificacion = verificacion,
                                aprobada = aprobada,
                                onPerfil = { pestana = Pestana.PERFIL },
                                onBuscar = { pestana = Pestana.PACIENTES },
                                onPacientes = { pestana = Pestana.PACIENTES },
                                onCodigo = { codigoAbierto = true },
                                onGenerar = { vm.generarCodigo(perfil.nombre) },
                                onMapa = irAlMapa,
                                citas = citas,
                                onCitas = { enCitas = true },
                                onVerificar = verificar,
                                onActualizar = actualizar,
                                onPaciente = { pacienteAbierto = it.id },
                                transicion = transicion,
                            )
                            Pestana.PACIENTES -> TabPacientes(
                                estado = estado,
                                onPaciente = { pacienteAbierto = it.id },
                                onActualizar = actualizar,
                                onCodigo = { codigoAbierto = true },
                                fotos = fotosPacientes,
                                sinLeer = { p -> chatVersion.let { p.ultimoMensaje.sinLeer(perfil.uid, chatVistoHasta(contexto, perfil.uid, p.id)) } },
                            )
                            Pestana.PERFIL -> TabPerfil(
                                perfil = perfil,
                                verificacion = verificacion,
                                aprobada = aprobada,
                                pacientes = estado.pacientes.size,
                                onVerificar = verificar,
                                onActualizar = actualizar,
                                onMapa = irAlMapa,
                                onCodigo = { codigoAbierto = true },
                                onCerrarSesion = onCerrarSesion,
                                transicion = transicion,
                            )
                        }
                    }
                }
                BarraInferior(
                    izquierda = listOf(
                        ItemBarra(R.drawable.ic_inicio, R.string.nutri_nav_inicio, pestana == Pestana.INICIO) { pestana = Pestana.INICIO },
                        ItemBarra(R.drawable.ic_pacientes, R.string.nutri_nav_pacientes, pestana == Pestana.PACIENTES) { pestana = Pestana.PACIENTES },
                    ),
                    derecha = listOf(
                        // el mapa abre una pantalla completa, por eso nunca queda marcado
                        ItemBarra(R.drawable.ic_ubicacion, R.string.nutri_nav_mapa, false, irAlMapa),
                        ItemBarra(R.drawable.ic_person, R.string.nutri_nav_perfil, pestana == Pestana.PERFIL) { pestana = Pestana.PERFIL },
                    ),
                    descripcionCentro = R.string.nutri_nav_nuevo,
                    iconoCentro = R.drawable.ic_agregar,
                    onCentro = { codigoAbierto = true },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            if (codigoAbierto) {
                ModalBottomSheet(
                    onDismissRequest = { codigoAbierto = false },
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    Column(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                        TarjetaCodigo(estado.codigo, estado.generando, onGenerar = { vm.generarCodigo(perfil.nombre) })
                    }
                }
            }
        }
    }
}
