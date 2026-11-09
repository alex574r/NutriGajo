package com.example.nutrigajo.ui.entrenador.inicio

import com.example.nutrigajo.ui.shared.platillos.MisPlatillosScreen
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.EstadoEjercicio
import com.example.nutrigajo.domain.model.EstadoVerificacion
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.duplicado
import com.example.nutrigajo.ui.components.ALTO_BARRA_INFERIOR
import com.example.nutrigajo.ui.components.AccionPerfil
import com.example.nutrigajo.ui.components.ActualizarAlDeslizar
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.AvisoInformativo
import com.example.nutrigajo.ui.components.BarraInferior
import com.example.nutrigajo.ui.components.CabeceraUsuario
import com.example.nutrigajo.ui.components.CambioConAvatar
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.ItemBarra
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.components.TransicionPerfil
import com.example.nutrigajo.ui.entrenador.ejercicios.EditorEjercicio
import com.example.nutrigajo.ui.entrenador.verificacion.CredencialScreen
import com.example.nutrigajo.ui.nutriologo.verificacion.TarjetaVerificacion
import com.example.nutrigajo.ui.nutriologo.verificacion.VerificacionEstado
import com.example.nutrigajo.ui.nutriologo.verificacion.VerificacionViewModel
import com.example.nutrigajo.ui.paciente.inicio.EncabezadoSeccion
import com.example.nutrigajo.ui.shared.ejercicios.DetalleEjercicio
import com.example.nutrigajo.ui.shared.ejercicios.EjerciciosEstado
import com.example.nutrigajo.ui.shared.ejercicios.EjerciciosViewModel
import com.example.nutrigajo.ui.shared.ejercicios.TarjetaEjercicio
import com.example.nutrigajo.ui.shared.perfil.PerfilScreen
import com.example.nutrigajo.ui.shared.perfil.altaCorta
import com.example.nutrigajo.ui.shared.perfil.datosBase
import com.example.nutrigajo.ui.shared.perfil.rememberFotoAvatar
import androidx.compose.ui.platform.LocalContext

private enum class Pestana { INICIO, PERFIL }

private val MARGEN = 20.dp

// Inicio del entrenador: sus ejercicios del banco global con el estado de cada uno, el estado de su verificación y su perfil. El
// botón central crea un ejercicio (solo con la cuenta verificada). Las pantallas completas (credencial, detalle, editor) tapan la barra.
// ponytail: se carga al abrir y al deslizar; sin listener en tiempo real.
@Composable
fun InicioEntrenadorScreen(
    perfil: Perfil,
    onCerrarSesion: () -> Unit,
    // claves propias: el uid ya lo usan otros ViewModels del mismo usuario
    verificacionVm: VerificacionViewModel = viewModel(key = "verificacion-${perfil.uid}"),
    vm: EjerciciosViewModel = viewModel(key = "ejercicios-${perfil.uid}"),
) {
    val verificacion by verificacionVm.estado.collectAsState()
    val ejercicios by vm.estado.collectAsState()
    var pestana by rememberSaveable { mutableStateOf(Pestana.INICIO) }
    var enFormulario by rememberSaveable { mutableStateOf(false) }
    var enPlatillos by rememberSaveable { mutableStateOf(false) }
    var abiertoId by rememberSaveable { mutableStateOf<String?>(null) }
    var editando by remember { mutableStateOf<Ejercicio?>(null) }
    val contexto = LocalContext.current

    LaunchedEffect(Unit) {
        verificacionVm.cargar()
        vm.cargarMios()
    }
    // al enviar la credencial la solicitud queda pendiente: se vuelve al inicio
    LaunchedEffect(verificacion.verificacion?.estado) {
        if (verificacion.verificacion?.estado == EstadoVerificacion.PENDIENTE) enFormulario = false
    }

    val aprobada = verificacion.verificacion?.estado == EstadoVerificacion.APROBADA
    val abierto = ejercicios.items.firstOrNull { it.id == abiertoId }
    val actualizar = {
        verificacionVm.cargar()
        vm.cargarMios()
    }
    val verificar = {
        verificacionVm.limpiarError()
        enFormulario = true
    }
    val nuevo = {
        if (aprobada) {
            vm.limpiarError()
            editando = Ejercicio()
        } else {
            // sin cuenta verificada se lleva al inicio, donde está el estado de la verificación
            pestana = Pestana.INICIO
        }
    }
    val editar = { ejercicio: Ejercicio ->
        vm.limpiarError()
        editando = ejercicio
    }

    val enEdicion = editando
    when {
        enPlatillos -> MisPlatillosScreen(perfil = perfil, puedeCrear = aprobada, onVolver = { enPlatillos = false })
        enEdicion != null -> EditorEjercicio(
            inicial = enEdicion,
            guardando = ejercicios.guardando,
            error = ejercicios.error,
            onGuardar = { ejercicio, enviar, foto, clip ->
                vm.guardar(ejercicio, perfil.nombre, enviar, foto, clip, contexto.contentResolver, contexto.cacheDir) { bien ->
                    if (bien) {
                        editando = null
                        abiertoId = null
                    }
                }
            },
            onVolver = {
                vm.limpiarError()
                editando = null
            },
        )
        enFormulario -> CredencialScreen(
            estado = verificacion,
            onEnviar = { verificacionVm.enviarCredencial(perfil, it) },
            onEditar = verificacionVm::limpiarError,
            onVolver = { enFormulario = false },
        )
        abierto != null -> DetalleEjercicio(
            ejercicio = abierto,
            onVolver = { abiertoId = null },
            acciones = {
                AccionesEjercicio(
                    ejercicio = abierto,
                    estado = ejercicios,
                    onEditar = { editar(abierto) },
                    onDuplicar = { editar(abierto.duplicado()) },
                    onRetirar = { vm.retirar(abierto) { vm.cargarMios(); abiertoId = null } },
                    onSacarDeRevision = { vm.sacarDeRevision(abierto) { vm.cargarMios(); abiertoId = null } },
                    onEliminar = { vm.eliminar(abierto) { abiertoId = null } },
                )
            },
        )
        verificacion.cargando && ejercicios.cargando && ejercicios.items.isEmpty() -> PantallaCarga()
        else -> {
            BackHandler(enabled = pestana != Pestana.INICIO) { pestana = Pestana.INICIO }
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                ActualizarAlDeslizar(
                    refrescando = ejercicios.cargando || verificacion.cargando,
                    onActualizar = actualizar,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // al pasar de una pestaña a otra el avatar de la cabecera viaja al del perfil y viceversa
                    CambioConAvatar(pestana, Modifier.fillMaxSize()) { destino, transicion ->
                        when (destino) {
                            Pestana.INICIO -> TabEjercicios(
                                perfil = perfil,
                                ejercicios = ejercicios,
                                verificacion = verificacion,
                                aprobada = aprobada,
                                onPerfil = { pestana = Pestana.PERFIL },
                                onNuevo = nuevo,
                                onVerificar = verificar,
                                onActualizar = actualizar,
                                onEjercicio = { abiertoId = it.id },
                                onPlatillos = { enPlatillos = true },
                                transicion = transicion,
                            )
                            Pestana.PERFIL -> TabPerfilEntrenador(
                                perfil = perfil,
                                verificacion = verificacion,
                                aprobada = aprobada,
                                publicados = ejercicios.items.count { it.estado == EstadoEjercicio.PUBLICADO },
                                onNuevo = nuevo,
                                onVerificar = verificar,
                                onActualizar = actualizar,
                                onCerrarSesion = onCerrarSesion,
                                transicion = transicion,
                            )
                        }
                    }
                }
                BarraInferior(
                    izquierda = listOf(
                        ItemBarra(R.drawable.ic_inicio, R.string.entrenador_nav_inicio, pestana == Pestana.INICIO) { pestana = Pestana.INICIO },
                    ),
                    derecha = listOf(
                        ItemBarra(R.drawable.ic_person, R.string.entrenador_nav_perfil, pestana == Pestana.PERFIL) { pestana = Pestana.PERFIL },
                    ),
                    descripcionCentro = R.string.entrenador_nav_nuevo,
                    iconoCentro = R.drawable.ic_agregar,
                    onCentro = nuevo,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun TabEjercicios(
    perfil: Perfil,
    ejercicios: EjerciciosEstado,
    verificacion: VerificacionEstado,
    aprobada: Boolean,
    onPerfil: () -> Unit,
    onNuevo: () -> Unit,
    onVerificar: () -> Unit,
    onActualizar: () -> Unit,
    onEjercicio: (Ejercicio) -> Unit,
    onPlatillos: () -> Unit,
    modifier: Modifier = Modifier,
    transicion: TransicionPerfil? = null,
) {
    val colores = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = ALTO_BARRA_INFERIOR + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            CabeceraUsuario(
                saludo = stringResource(R.string.entrenador_panel),
                nombre = perfil.nombre,
                verificado = aprobada,
                descripcionAvatar = stringResource(R.string.nutri_abrir_perfil),
                onAvatar = onPerfil,
                iconoAccion = R.drawable.ic_agregar,
                descripcionAccion = stringResource(R.string.entrenador_nav_nuevo),
                onAccion = onNuevo,
                imagenAvatar = rememberFotoAvatar(perfil),
                transicion = transicion,
            )
        }
        item { AvisoInformativo(stringResource(R.string.entrenador_aviso)) }
        if (!aprobada) {
            item { TarjetaVerificacion(verificacion, onVerificar, Modifier.padding(horizontal = MARGEN), onActualizar = onActualizar, entrenador = true) }
        }
        item {
            FilaAjuste(
                icono = R.drawable.ic_documento,
                titulo = stringResource(R.string.platillo_mis_platillos),
                modifier = Modifier.padding(horizontal = MARGEN),
                etiqueta = stringResource(R.string.platillo_entrada_etiqueta),
                onClick = onPlatillos,
            )
        }
        ejercicios.error?.let { item { AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN)) } }
        item { EncabezadoSeccion(stringResource(R.string.entrenador_mis_ejercicios), contador = "(${ejercicios.items.size})") }
        when {
            !aprobada -> item { Aviso(R.string.entrenador_sin_verificar) }
            ejercicios.cargando && ejercicios.items.isEmpty() -> items(3) {
                Esqueleto(Modifier.fillMaxWidth().padding(horizontal = MARGEN).height(104.dp), RoundedCornerShape(28.dp))
            }
            ejercicios.items.isEmpty() -> item { Aviso(R.string.entrenador_vacio) }
            else -> items(ejercicios.items, key = { it.id }) { ejercicio ->
                TarjetaEjercicio(ejercicio, onClick = { onEjercicio(ejercicio) }, modifier = Modifier.padding(horizontal = MARGEN), mostrarEstado = true)
            }
        }
    }
}

@Composable
private fun Aviso(texto: Int) {
    Text(
        text = stringResource(texto),
        modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

// Perfil del entrenador: sus datos con el estado de la verificación y la configuración de la cuenta.
@Composable
private fun TabPerfilEntrenador(
    perfil: Perfil,
    verificacion: VerificacionEstado,
    aprobada: Boolean,
    publicados: Int,
    onNuevo: () -> Unit,
    onVerificar: () -> Unit,
    onActualizar: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    transicion: TransicionPerfil? = null,
) {
    val estadoVerificacion = when (verificacion.verificacion?.estado) {
        EstadoVerificacion.APROBADA -> R.string.perfil_estado_aprobada
        EstadoVerificacion.PENDIENTE -> R.string.perfil_estado_pendiente
        EstadoVerificacion.RECHAZADA -> R.string.perfil_estado_rechazada
        null -> R.string.perfil_estado_sin
    }
    PerfilScreen(
        perfil = perfil,
        rol = R.string.rol_entrenador,
        verificado = aprobada,
        estadisticas = listOfNotNull(
            stringResource(R.string.entrenador_perfil_publicados, publicados),
            stringResource(estadoVerificacion),
            perfil.altaCorta()?.let { stringResource(R.string.perfil_desde, it) },
        ),
        principal = AccionPerfil(R.drawable.ic_agregar, R.string.entrenador_nav_nuevo, onNuevo),
        secundaria = null,
        datos = datosBase(perfil, R.string.rol_entrenador),
        onCerrarSesion = onCerrarSesion,
        modifier = modifier,
        espacioInferior = ALTO_BARRA_INFERIOR,
        transicion = transicion,
        datosExtra = { TarjetaVerificacion(verificacion, onVerificar, Modifier.padding(horizontal = 16.dp), onActualizar = onActualizar, entrenador = true) },
    )
}

// Los botones de un ejercicio según su estado: se edita lo que es borrador o rechazado, se saca de revisión lo que espera al ADMIN
// y lo publicado o retirado solo se duplica (un ejercicio publicado no cambia). Retirar y eliminar piden confirmación.
@Composable
private fun ColumnScope.AccionesEjercicio(
    ejercicio: Ejercicio,
    estado: EjerciciosEstado,
    onEditar: () -> Unit,
    onDuplicar: () -> Unit,
    onRetirar: () -> Unit,
    onSacarDeRevision: () -> Unit,
    onEliminar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    var confirmando by remember { mutableStateOf<Confirmacion?>(null) }
    val trabajando = estado.trabajando != null
    estado.error?.let { AlertaError(stringResource(it)) }

    when (ejercicio.estado) {
        EstadoEjercicio.BORRADOR, EstadoEjercicio.RECHAZADO -> {
            BotonAccion(R.string.ejercicio_editar, principal = true, enabled = !trabajando, onClick = onEditar)
            BotonAccion(R.string.ejercicio_eliminar, enabled = !trabajando) { confirmando = Confirmacion.ELIMINAR }
        }
        EstadoEjercicio.EN_REVISION -> BotonAccion(R.string.ejercicio_sacar_revision, principal = true, enabled = !trabajando, onClick = onSacarDeRevision)
        EstadoEjercicio.PUBLICADO -> {
            BotonAccion(R.string.ejercicio_duplicar, principal = true, enabled = !trabajando, onClick = onDuplicar)
            BotonAccion(R.string.ejercicio_retirar, enabled = !trabajando) { confirmando = Confirmacion.RETIRAR }
        }
        EstadoEjercicio.RETIRADO -> {
            BotonAccion(R.string.ejercicio_duplicar, principal = true, enabled = !trabajando, onClick = onDuplicar)
            BotonAccion(R.string.ejercicio_eliminar, enabled = !trabajando) { confirmando = Confirmacion.ELIMINAR }
        }
    }

    confirmando?.let { tipo ->
        val eliminar = tipo == Confirmacion.ELIMINAR
        AlertDialog(
            onDismissRequest = { confirmando = null },
            containerColor = colores.background,
            title = { Text(stringResource(if (eliminar) R.string.ejercicio_eliminar_titulo else R.string.ejercicio_retirar_titulo)) },
            text = { Text(stringResource(if (eliminar) R.string.ejercicio_eliminar_texto else R.string.ejercicio_retirar_texto, ejercicio.titulo)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmando = null
                    if (eliminar) onEliminar() else onRetirar()
                }) { Text(stringResource(if (eliminar) R.string.ejercicio_eliminar else R.string.ejercicio_retirar), color = colores.error) }
            },
            dismissButton = { TextButton(onClick = { confirmando = null }) { Text(stringResource(R.string.cita_dialogo_volver)) } },
        )
    }
}

private enum class Confirmacion { ELIMINAR, RETIRAR }

@Composable
private fun BotonAccion(texto: Int, principal: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val modificador = Modifier.fillMaxWidth().height(48.dp)
    if (principal) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modificador,
            shape = RoundedCornerShape(21.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
        ) { Text(stringResource(texto)) }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = modificador, shape = RoundedCornerShape(21.dp)) { Text(stringResource(texto)) }
    }
}
