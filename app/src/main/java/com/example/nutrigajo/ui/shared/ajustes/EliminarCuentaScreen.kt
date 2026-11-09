package com.example.nutrigajo.ui.shared.ajustes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto

// «Eliminar mi cuenta» (Configuración): primero lo que se borra y lo que se conserva según el rol, luego el enlace que Firebase manda al correo
// de la cuenta (se abre desde el correo y, como la app aún no lo recibe sola, se copia y se pega aquí) y, ya confirmada la identidad, el botón
// rojo. Lo hace el Worker; al terminar, la sesión se cierra (`onEliminada`).
@Composable
fun EliminarCuentaScreen(
    perfil: Perfil,
    onEliminada: () -> Unit,
    onVolver: () -> Unit,
    espacioInferior: Dp = 0.dp,
    // clave propia: no se comparte con las demás pantallas de ajustes
    vm: EliminarCuentaViewModel = viewModel(key = "eliminar-cuenta-${perfil.uid}"),
) {
    val estado by vm.estado.collectAsState()
    val colores = MaterialTheme.colorScheme
    var pegado by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(perfil.rol) { if (perfil.rol == Rol.NUTRIOLOGO) vm.cargarPacientes() }
    LaunchedEffect(estado.eliminada) { if (estado.eliminada) onEliminada() }

    PantallaAjuste(stringResource(R.string.ajustes_eliminar_cuenta), onVolver, espacioInferior = espacioInferior) {
        when (estado.paso) {
            PasoEliminar.AVISO -> {
                Text(stringResource(R.string.eliminar_cuenta_irreversible), style = MaterialTheme.typography.titleMedium, color = colores.error)
                Text(
                    stringResource(
                        when (perfil.rol) {
                            Rol.NUTRIOLOGO -> R.string.eliminar_cuenta_aviso_nutriologo
                            Rol.ENTRENADOR -> R.string.eliminar_cuenta_aviso_entrenador
                            else -> R.string.eliminar_cuenta_aviso_paciente
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onBackground,
                )
                estado.pacientes?.takeIf { perfil.rol == Rol.NUTRIOLOGO && it > 0 }?.let {
                    Text(pluralStringResource(R.plurals.eliminar_cuenta_pacientes, it, it), style = MaterialTheme.typography.bodyMedium, color = colores.onBackground)
                }
                Text(stringResource(R.string.eliminar_cuenta_confirmar_correo, perfil.correo), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                estado.error?.let { AlertaError(stringResource(it), Modifier.fillMaxWidth()) }
                BotonPrimario(
                    texto = stringResource(R.string.eliminar_cuenta_enviar_enlace),
                    onClick = vm::enviarEnlace,
                    modifier = Modifier.fillMaxWidth(),
                    cargando = estado.trabajando,
                )
            }

            PasoEliminar.ENLACE -> {
                Text(stringResource(R.string.eliminar_cuenta_enlace_enviado, perfil.correo), style = MaterialTheme.typography.bodyMedium, color = colores.onBackground)
                CampoTexto(
                    etiqueta = stringResource(R.string.eliminar_cuenta_pegar_enlace),
                    valor = pegado,
                    onValorChange = {
                        pegado = it
                        vm.limpiarError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                estado.error?.let { AlertaError(stringResource(it), Modifier.fillMaxWidth()) }
                BotonPrimario(
                    texto = stringResource(R.string.eliminar_cuenta_confirmar),
                    onClick = { vm.confirmar(pegado) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = pegado.isNotBlank(),
                    cargando = estado.trabajando,
                )
                Text(
                    text = stringResource(R.string.eliminar_cuenta_otro_enlace),
                    modifier = Modifier.clickable(role = Role.Button, enabled = !estado.trabajando, onClick = vm::enviarEnlace).padding(vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = colores.primary,
                )
            }

            PasoEliminar.CONFIRMADO -> {
                Text(stringResource(R.string.eliminar_cuenta_confirmada), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
                Text(stringResource(R.string.eliminar_cuenta_cinco_minutos), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
                estado.error?.let { AlertaError(stringResource(it), Modifier.fillMaxWidth()) }
                BotonPrimario(
                    texto = stringResource(R.string.eliminar_cuenta_definitivamente),
                    onClick = vm::eliminar,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colores.error, contentColor = colores.onError),
                    cargando = estado.trabajando,
                )
            }
        }
    }
}
