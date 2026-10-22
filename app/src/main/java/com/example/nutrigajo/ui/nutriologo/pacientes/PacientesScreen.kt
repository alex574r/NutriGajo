package com.example.nutrigajo.ui.nutriologo.pacientes

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.CodigoVinculacion
import com.example.nutrigajo.domain.model.EstadoVerificacion
import com.example.nutrigajo.domain.model.PacienteResumen
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.nutriologo.ubicacion.TarjetaUbicacion
import com.example.nutrigajo.ui.nutriologo.ubicacion.UbicacionScreen
import com.example.nutrigajo.ui.nutriologo.verificacion.TarjetaVerificacion
import com.example.nutrigajo.ui.nutriologo.verificacion.VerificacionEstado
import com.example.nutrigajo.ui.nutriologo.verificacion.VerificacionScreen
import com.example.nutrigajo.ui.nutriologo.verificacion.VerificacionViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Inicio provisional del nutriólogo: su verificación, su código de vinculación y los pacientes que ya lo canjearon.
// ponytail: se carga al abrir y con "Actualizar"; un listener en tiempo real cuando el expediente lo pida.
@Composable
fun PacientesScreen(
    perfil: Perfil,
    onCerrarSesion: () -> Unit,
    vm: PacientesViewModel = viewModel(key = perfil.uid),
    // clave propia: dos ViewModels con la misma clave se pisarían
    verificacionVm: VerificacionViewModel = viewModel(key = "verificacion-${perfil.uid}"),
) {
    val estado by vm.estado.collectAsState()
    val verificacion by verificacionVm.estado.collectAsState()
    var enFormulario by rememberSaveable { mutableStateOf(false) }
    var enMapa by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        vm.cargar()
        verificacionVm.cargar()
    }
    // al enviar la cédula la solicitud queda pendiente: se vuelve al inicio
    LaunchedEffect(verificacion.verificacion?.estado) {
        if (verificacion.verificacion?.estado == EstadoVerificacion.PENDIENTE) enFormulario = false
    }

    when {
        enMapa -> UbicacionScreen(perfil = perfil, onVolver = { enMapa = false })
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
        else -> PacientesContenido(
            perfil = perfil,
            estado = estado,
            verificacion = verificacion,
            onGenerarCodigo = { vm.generarCodigo(perfil.nombre) },
            onVerificar = {
                verificacionVm.limpiarError()
                enFormulario = true
            },
            onUbicar = { enMapa = true },
            onActualizar = {
                vm.cargar()
                verificacionVm.cargar()
            },
            onCerrarSesion = onCerrarSesion,
        )
    }
}

@Composable
private fun PacientesContenido(
    perfil: Perfil,
    estado: PacientesEstado,
    verificacion: VerificacionEstado,
    onGenerarCodigo: () -> Unit,
    onVerificar: () -> Unit,
    onUbicar: () -> Unit,
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
        if (!verificacion.cargando) item { TarjetaVerificacion(verificacion, onVerificar) }
        // el mapa es de nutriólogos verificados: las reglas también lo exigen
        if (verificacion.verificacion?.estado == EstadoVerificacion.APROBADA) item { TarjetaUbicacion(onUbicar) }
        item { TarjetaCodigo(estado.codigo, estado.generando, onGenerarCodigo) }
        estado.error?.let { item { AlertaError(stringResource(it)) } }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.pacientes_lista_titulo, estado.pacientes.size),
                    style = MaterialTheme.typography.titleLarge,
                    color = colores.onBackground,
                )
                TextButton(onClick = onActualizar, enabled = !estado.cargando) {
                    Text(stringResource(R.string.pacientes_actualizar))
                }
            }
        }
        if (estado.pacientes.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.pacientes_vacio),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            items(estado.pacientes, key = { it.id }) { FilaPaciente(it) }
        }
    }
}

@Composable
private fun TarjetaCodigo(codigo: CodigoVinculacion?, generando: Boolean, onGenerar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    val negro = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colores.surfaceVariant, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.pacientes_codigo_titulo),
            style = MaterialTheme.typography.titleMedium,
            color = colores.onBackground,
        )
        if (codigo != null) {
            Text(
                text = codigo.formateado,
                style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 3.sp),
                color = colores.onBackground,
            )
            val vence = remember(codigo.expiraEn) {
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .format(Instant.ofEpochMilli(codigo.expiraEn).atZone(ZoneId.systemDefault()))
            }
            Text(
                text = stringResource(R.string.pacientes_codigo_vigencia, vence),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.pacientes_codigo_ayuda),
            style = MaterialTheme.typography.bodySmall,
            color = colores.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (codigo != null) {
                val texto = stringResource(R.string.pacientes_compartir_texto, codigo.formateado)
                Button(
                    onClick = {
                        val envio = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, texto)
                        }
                        contexto.startActivity(Intent.createChooser(envio, null))
                    },
                    shape = RoundedCornerShape(21.dp),
                    colors = negro,
                ) { Text(stringResource(R.string.pacientes_compartir)) }
                OutlinedButton(onClick = onGenerar, enabled = !generando, shape = RoundedCornerShape(21.dp)) {
                    Text(stringResource(R.string.pacientes_generar_nuevo))
                }
            } else {
                Button(onClick = onGenerar, enabled = !generando, shape = RoundedCornerShape(21.dp), colors = negro) {
                    Text(stringResource(R.string.pacientes_generar))
                }
            }
        }
    }
}

@Composable
private fun FilaPaciente(paciente: PacienteResumen) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colores.surfaceVariant, RoundedCornerShape(19.dp))
            .padding(16.dp),
    ) {
        Text(paciente.nombre, style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
        Text(paciente.correo, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
    }
}
