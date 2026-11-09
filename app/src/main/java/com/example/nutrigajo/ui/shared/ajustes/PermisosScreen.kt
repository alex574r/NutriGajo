package com.example.nutrigajo.ui.shared.ajustes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.data.actualizacion.InstaladorApk
import com.example.nutrigajo.data.local.AjustesApp
import com.example.nutrigajo.domain.EstadoPermiso
import com.example.nutrigajo.domain.PermisoApp
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.permisosDelRol
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.TarjetaAjustes
import com.example.nutrigajo.ui.paciente.reloj.abrirAjustesDeHealthConnect
import com.example.nutrigajo.ui.paciente.reloj.abrirTienda
import com.example.nutrigajo.ui.permisos.rememberPermiso
import com.example.nutrigajo.ui.permisos.textoNivel
import com.example.nutrigajo.ui.permisos.texto
import com.example.nutrigajo.ui.permisos.titulo

// «Permisos» (Configuración): una fila por cada permiso que le toca al rol, con qué tan necesario es, en qué estado está y, tocándola, lo que lo
// arregla (cuadro del sistema, ajustes, tienda o la pantalla de instalar apps). Se recalcula al volver a la app, así que refleja lo que se
// cambió en los ajustes del teléfono.
@Composable
fun PermisosScreen(
    perfil: Perfil,
    onVolver: () -> Unit,
    espacioInferior: Dp = 0.dp,
    // clave propia: no se comparte con las demás pantallas de ajustes
    vm: PermisosViewModel = viewModel(key = "permisos-${perfil.uid}"),
) {
    val extra by vm.estado.collectAsState()
    val contexto = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.cargar() }
    val pedirReloj = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        AjustesApp.marcarPedido(contexto, PermisoApp.RELOJ.grupo)
        vm.cargar()
    }

    PantallaAjuste(stringResource(R.string.ajustes_permisos), onVolver, espacioInferior = espacioInferior) {
        Text(stringResource(R.string.permisos_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TarjetaAjustes {
        permisosDelRol(perfil.rol).forEach { permiso ->
            key(permiso) {
                when (permiso) {
                    PermisoApp.RELOJ -> {
                        val estado = extra.reloj
                        if (estado == null) {
                            Esqueleto(Modifier.fillMaxWidth().height(60.dp), RoundedCornerShape(18.dp))
                        } else {
                            Fila(
                                permiso,
                                estado,
                                when (estado) {
                                    EstadoPermiso.FALTA -> ({ pedirReloj.launch(vm.permisosReloj) })
                                    EstadoPermiso.BLOQUEADO -> ({ abrirAjustesDeHealthConnect(contexto) })
                                    EstadoPermiso.NO_DISPONIBLE -> if (extra.relojInstalable) ({ abrirTienda(contexto) }) else null
                                    EstadoPermiso.CONCEDIDO -> null
                                },
                            )
                        }
                    }
                    PermisoApp.ACTUALIZACIONES -> Fila(
                        permiso,
                        extra.actualizaciones,
                        if (extra.actualizaciones == EstadoPermiso.FALTA) ({ contexto.startActivity(InstaladorApk.intentDePermiso(contexto)) }) else null,
                    )
                    else -> {
                        val ejecucion = rememberPermiso(permiso)
                        Fila(
                            permiso,
                            ejecucion.estado,
                            when (ejecucion.estado) {
                                EstadoPermiso.FALTA -> ejecucion.pedir
                                EstadoPermiso.BLOQUEADO -> ejecucion.abrirAjustes
                                else -> null
                            },
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun Fila(permiso: PermisoApp, estado: EstadoPermiso, accion: (() -> Unit)?) {
    FilaAjuste(
        icono = icono(permiso),
        titulo = stringResource(permiso.titulo()),
        descripcion = stringResource(permiso.textoNivel()) + " · " + stringResource(estado.texto()),
        plana = true,
        onClick = accion,
    )
}

@DrawableRes
private fun icono(permiso: PermisoApp): Int = when (permiso) {
    PermisoApp.UBICACION_CARRERA, PermisoApp.UBICACION_MAPA -> R.drawable.ic_ubicacion
    PermisoApp.NOTIFICACIONES -> R.drawable.ic_campana
    PermisoApp.RELOJ -> R.drawable.ic_smartwatch
    PermisoApp.ACTUALIZACIONES -> R.drawable.ic_informacion
}
