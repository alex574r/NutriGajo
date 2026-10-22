package com.example.nutrigajo.ui.admin.verificaciones

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.Preverificacion
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.SolicitudVerificacion
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.PantallaCarga

// El registro de la SEP no ofrece API: el ADMIN lo consulta en su portal y resuelve aquí.
private const val CONSULTA_SEP = "https://cedulaprofesional.sep.gob.mx/"
private const val MAX_MOTIVO = 200

// Inicio del ADMIN: cola de verificaciones pendientes, de la más antigua a la más reciente.
@Composable
fun VerificacionesScreen(
    perfil: Perfil,
    onCerrarSesion: () -> Unit,
    // clave propia: el uid ya la usan otros ViewModels del mismo usuario
    vm: VerificacionesViewModel = viewModel(key = "admin-${perfil.uid}"),
) {
    val estado by vm.estado.collectAsState()
    LaunchedEffect(Unit) { vm.cargar() }
    var rechazando by remember { mutableStateOf<SolicitudVerificacion?>(null) }

    if (estado.cargando && estado.solicitudes.isEmpty() && estado.error == null) {
        PantallaCarga()
    } else {
        VerificacionesContenido(
            perfil = perfil,
            estado = estado,
            onAprobar = vm::aprobar,
            onRechazar = { rechazando = it },
            onActualizar = vm::cargar,
            onCerrarSesion = onCerrarSesion,
        )
    }

    rechazando?.let { solicitud ->
        DialogoRechazo(
            onConfirmar = { motivo ->
                vm.rechazar(solicitud, motivo)
                rechazando = null
            },
            onCancelar = { rechazando = null },
        )
    }
}

@Composable
private fun VerificacionesContenido(
    perfil: Perfil,
    estado: VerificacionesEstado,
    onAprobar: (SolicitudVerificacion) -> Unit,
    onRechazar: (SolicitudVerificacion) -> Unit,
    onActualizar: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
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
        estado.error?.let { item { AlertaError(stringResource(it)) } }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.admin_titulo, estado.solicitudes.size),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = colores.onBackground,
                )
                TextButton(onClick = onActualizar, enabled = !estado.cargando && estado.trabajando == null) {
                    Text(stringResource(R.string.pacientes_actualizar))
                }
            }
        }
        if (estado.solicitudes.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.admin_vacio),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            items(estado.solicitudes, key = { it.nutriologoId }) { solicitud ->
                TarjetaSolicitud(
                    solicitud = solicitud,
                    trabajando = estado.trabajando != null,
                    onAprobar = { onAprobar(solicitud) },
                    onRechazar = { onRechazar(solicitud) },
                )
            }
        }
    }
}

@Composable
private fun TarjetaSolicitud(
    solicitud: SolicitudVerificacion,
    trabajando: Boolean,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    // el cliente ya filtra, pero el ADMIN no debe fiarse: se recalcula
    val nombreCoincide = Preverificacion.nombreCoincide(solicitud.nombreCuenta, solicitud.nombreCedula)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colores.surfaceVariant, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(solicitud.nombreCuenta, style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
        Text(solicitud.correo, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
        Text(
            text = stringResource(R.string.admin_cedula, solicitud.cedula),
            style = MaterialTheme.typography.titleLarge,
            color = colores.onBackground,
        )
        Text(
            text = stringResource(R.string.admin_nombre_cedula, solicitud.nombreCedula),
            style = MaterialTheme.typography.bodyMedium,
            color = colores.onBackground,
        )
        Text(
            text = stringResource(if (nombreCoincide) R.string.admin_nombre_coincide else R.string.admin_nombre_distinto),
            style = MaterialTheme.typography.bodySmall,
            color = if (nombreCoincide) colores.onSurfaceVariant else colores.error,
        )
        OutlinedButton(
            onClick = { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CONSULTA_SEP))) },
            shape = RoundedCornerShape(21.dp),
        ) { Text(stringResource(R.string.admin_abrir_sep)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAprobar,
                enabled = !trabajando,
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
            ) { Text(stringResource(R.string.admin_aprobar)) }
            OutlinedButton(onClick = onRechazar, enabled = !trabajando, shape = RoundedCornerShape(21.dp)) {
                Text(stringResource(R.string.admin_rechazar))
            }
        }
    }
}

@Composable
private fun DialogoRechazo(onConfirmar: (motivo: String) -> Unit, onCancelar: () -> Unit) {
    var motivo by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.admin_rechazar_titulo)) },
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
