package com.example.nutrigajo.ui.admin.inicio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.SolicitudVerificacion
import com.example.nutrigajo.ui.admin.verificaciones.VerificacionesViewModel
import com.example.nutrigajo.ui.components.ActualizarAlDeslizar
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BarraInferior
import com.example.nutrigajo.ui.components.CambioConAvatar
import com.example.nutrigajo.ui.components.ItemBarra
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.shared.ejercicios.DetalleEjercicio
import com.example.nutrigajo.ui.shared.ejercicios.EjerciciosViewModel

// El registro de la SEP no ofrece API: el ADMIN lo consulta en su portal y resuelve aquí.
private const val CONSULTA_SEP = "https://cedulaprofesional.sep.gob.mx/"
private const val MAX_MOTIVO = 200

private enum class Pestana { SOLICITUDES, CONTENIDO, PERFIL }

// Inicio del ADMIN: la cola de verificaciones pendientes (de la más antigua a la más reciente), la de ejercicios por revisar y su
// perfil (se abre desde el avatar). El botón central de la barra vuelve a cargar las colas y el de la cabecera abre la consulta de la SEP.
@Composable
fun InicioAdminScreen(
    perfil: Perfil,
    onCerrarSesion: () -> Unit,
    // clave propia: el uid ya la usan otros ViewModels del mismo usuario
    vm: VerificacionesViewModel = viewModel(key = "admin-${perfil.uid}"),
    ejerciciosVm: EjerciciosViewModel = viewModel(key = "admin-ejercicios-${perfil.uid}"),
) {
    val estado by vm.estado.collectAsState()
    val ejercicios by ejerciciosVm.estado.collectAsState()
    val recargar = {
        vm.cargar()
        ejerciciosVm.cargarEnRevision()
    }
    LaunchedEffect(Unit) { recargar() }
    var pestana by rememberSaveable { mutableStateOf(Pestana.SOLICITUDES) }
    var rechazando by remember { mutableStateOf<SolicitudVerificacion?>(null) }
    var abiertoId by rememberSaveable { mutableStateOf<String?>(null) }
    var rechazandoEjercicio by remember { mutableStateOf<Ejercicio?>(null) }
    val abierto = ejercicios.items.firstOrNull { it.id == abiertoId }
    val contexto = LocalContext.current
    val abrirSep = { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CONSULTA_SEP))) }
    // el portal pide la cédula: se deja en el portapapeles para pegarla allí
    val revisar = { solicitud: SolicitudVerificacion ->
        val portapapeles = contexto.getSystemService(ClipboardManager::class.java)
        portapapeles.setPrimaryClip(ClipData.newPlainText(contexto.getString(R.string.admin_cedula_copiada), solicitud.cedula))
        // desde Android 13 el sistema avisa solo de la copia
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(contexto, R.string.admin_cedula_copiada, Toast.LENGTH_SHORT).show()
        }
        abrirSep()
    }

    if (abierto != null) {
        // el detalle del ejercicio que se revisa: se publica o se rechaza con motivo
        DetalleEjercicio(
            ejercicio = abierto,
            onVolver = { abiertoId = null },
            acciones = {
                ejercicios.error?.let { AlertaError(stringResource(it)) }
                val ocupado = ejercicios.trabajando != null
                val colores = MaterialTheme.colorScheme
                Button(
                    onClick = { ejerciciosVm.publicar(abierto) { abiertoId = null } },
                    enabled = !ocupado,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(21.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
                ) { Text(stringResource(R.string.admin_publicar)) }
                OutlinedButton(
                    onClick = { rechazandoEjercicio = abierto },
                    enabled = !ocupado,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(21.dp),
                ) { Text(stringResource(R.string.admin_rechazar)) }
            },
        )
    } else if (estado.cargando && estado.solicitudes.isEmpty() && estado.error == null) {
        PantallaCarga()
    } else {
        BackHandler(enabled = pestana != Pestana.SOLICITUDES) { pestana = Pestana.SOLICITUDES }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // deslizar hacia abajo en cualquiera de las pestañas vuelve a cargar las colas
            ActualizarAlDeslizar(
                refrescando = estado.cargando || ejercicios.cargando,
                onActualizar = { if (estado.trabajando == null) recargar() },
                modifier = Modifier.fillMaxSize(),
            ) {
                // al pasar de una pestaña a otra el avatar de la cabecera viaja al del perfil y viceversa
                CambioConAvatar(pestana, Modifier.fillMaxSize()) { destino, transicion ->
                    when (destino) {
                        Pestana.SOLICITUDES -> TabSolicitudes(
                            perfil = perfil,
                            estado = estado,
                            onRevisar = revisar,
                            onAprobar = vm::aprobar,
                            onRechazar = { rechazando = it },
                            onSep = { abrirSep() },
                            onPerfil = { pestana = Pestana.PERFIL },
                            transicion = transicion,
                        )
                        Pestana.CONTENIDO -> TabContenido(
                            perfil = perfil,
                            estado = ejercicios,
                            onEjercicio = { abiertoId = it.id },
                            onActualizar = { if (!ejercicios.cargando) ejerciciosVm.cargarEnRevision() },
                            onPerfil = { pestana = Pestana.PERFIL },
                            transicion = transicion,
                        )
                        Pestana.PERFIL -> TabPerfilAdmin(
                            perfil = perfil,
                            pendientes = estado.solicitudes.size,
                            onSolicitudes = { pestana = Pestana.SOLICITUDES },
                            onSep = { abrirSep() },
                            onCerrarSesion = onCerrarSesion,
                            transicion = transicion,
                        )
                    }
                }
            }
            BarraInferior(
                izquierda = listOf(
                    ItemBarra(R.drawable.ic_inicio, R.string.admin_nav_solicitudes, pestana == Pestana.SOLICITUDES) { pestana = Pestana.SOLICITUDES },
                ),
                // el perfil se abre desde el avatar de la cabecera
                derecha = listOf(
                    ItemBarra(R.drawable.ic_documento, R.string.admin_nav_contenido, pestana == Pestana.CONTENIDO) { pestana = Pestana.CONTENIDO },
                ),
                descripcionCentro = R.string.admin_actualizar,
                iconoCentro = R.drawable.ic_refrescar,
                onCentro = { if (!estado.cargando && estado.trabajando == null) recargar() },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    rechazandoEjercicio?.let { ejercicio ->
        DialogoRechazo(
            titulo = R.string.admin_rechazar_ejercicio,
            onConfirmar = { motivo ->
                ejerciciosVm.rechazar(ejercicio, motivo) { abiertoId = null }
                rechazandoEjercicio = null
            },
            onCancelar = { rechazandoEjercicio = null },
        )
    }

    rechazando?.let { solicitud ->
        DialogoRechazo(
            titulo = R.string.admin_rechazar_titulo,
            onConfirmar = { motivo ->
                vm.rechazar(solicitud, motivo)
                rechazando = null
            },
            onCancelar = { rechazando = null },
        )
    }
}

@Composable
private fun DialogoRechazo(@StringRes titulo: Int, onConfirmar: (motivo: String) -> Unit, onCancelar: () -> Unit) {
    var motivo by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(titulo)) },
        text = {
            OutlinedTextField(
                value = motivo,
                onValueChange = { motivo = it.take(MAX_MOTIVO) },
                label = { Text(stringResource(R.string.admin_motivo)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(motivo) }, enabled = motivo.isNotBlank()) {
                Text(stringResource(R.string.admin_rechazar))
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.verificacion_cancelar)) } },
    )
}
