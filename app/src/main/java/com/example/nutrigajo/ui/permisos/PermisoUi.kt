package com.example.nutrigajo.ui.permisos

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.nutrigajo.data.local.AjustesApp
import com.example.nutrigajo.domain.EstadoPermiso
import com.example.nutrigajo.domain.PermisoApp
import com.example.nutrigajo.domain.estadoDePermiso
import com.example.nutrigajo.domain.notificacionesPidenPermiso

// El estado de un permiso de ejecución y lo que se puede hacer con él: `pedir` muestra el cuadro del sistema y `abrirAjustes` lleva a la
// pantalla de la app en los ajustes (donde se arregla un permiso bloqueado).
class PermisoUi(val estado: EstadoPermiso, val pedir: () -> Unit, val abrirAjustes: () -> Unit)

// Los permisos del sistema que hay detrás de cada permiso de la app que se pide con el cuadro de Android.
private fun permisosDelSistema(permiso: PermisoApp): Array<String> = when (permiso) {
    PermisoApp.UBICACION_CARRERA, PermisoApp.UBICACION_MAPA -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    PermisoApp.NOTIFICACIONES -> if (notificacionesPidenPermiso(Build.VERSION.SDK_INT)) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
    else -> error("$permiso no se pide con el cuadro del sistema")
}

private fun otorgado(contexto: Context, permiso: String) = ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED

// Modo carrera exige la ubicación precisa; el mapa se conforma con la aproximada; las notificaciones no piden nada antes de Android 13.
private fun concedido(contexto: Context, permiso: PermisoApp): Boolean = when (permiso) {
    PermisoApp.UBICACION_CARRERA -> otorgado(contexto, Manifest.permission.ACCESS_FINE_LOCATION)
    PermisoApp.UBICACION_MAPA -> otorgado(contexto, Manifest.permission.ACCESS_FINE_LOCATION) || otorgado(contexto, Manifest.permission.ACCESS_COARSE_LOCATION)
    PermisoApp.NOTIFICACIONES -> !notificacionesPidenPermiso(Build.VERSION.SDK_INT) || otorgado(contexto, Manifest.permission.POST_NOTIFICATIONS)
    else -> error("$permiso no se pide con el cuadro del sistema")
}

private tailrec fun Context.actividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.actividad()
    else -> null
}

// El estado de un permiso de ejecución (ubicación o notificaciones) y cómo pedirlo. Se calcula de nuevo cada vez que la app vuelve al frente
// (así se entera de que se arregló en los ajustes) y cuando el cuadro del sistema contesta. `alConcederse` se llama si, tras pedirlo, la persona
// lo concede; `alResponder` se llama cuando el cuadro contesta, se conceda o no (para lo que sigue pase lo que pase, como pedir una cita). Marca el
// permiso como «ya pedido» (y «ya ofrecido») al contestar el cuadro.
@Composable
fun rememberPermiso(permiso: PermisoApp, alConcederse: () -> Unit = {}, alResponder: () -> Unit = {}): PermisoUi {
    val contexto = LocalContext.current
    val alConcederseActual by rememberUpdatedState(alConcederse)
    val alResponderActual by rememberUpdatedState(alResponder)
    // sube cada vez que algo pudo cambiar el permiso, para calcular el estado de nuevo
    var version by remember { mutableIntStateOf(0) }
    val sistema = remember(permiso) { permisosDelSistema(permiso) }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        AjustesApp.marcarPedido(contexto, permiso.grupo)
        AjustesApp.marcarOfrecido(contexto, permiso.grupo)
        version++
        if (concedido(contexto, permiso)) alConcederseActual()
        alResponderActual()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { version++ }

    val estado = remember(version, permiso) {
        val actividad = contexto.actividad()
        estadoDePermiso(
            concedido = concedido(contexto, permiso),
            disponible = true,
            yaSePidio = AjustesApp.yaSePidio(contexto, permiso.grupo),
            elSistemaTodaviaExplica = actividad != null && sistema.any { ActivityCompat.shouldShowRequestPermissionRationale(actividad, it) },
        )
    }
    return PermisoUi(
        estado = estado,
        pedir = {
            if (sistema.isEmpty()) {
                alConcederseActual()
                alResponderActual()
            } else {
                // el resultado llega aunque el sistema ya no muestre el cuadro (permiso bloqueado): ahí se marca como pedido
                lanzador.launch(sistema)
            }
        },
        abrirAjustes = { abrirAjustesDeLaApp(contexto) },
    )
}

// La pantalla de la app en los ajustes del sistema, donde se activa un permiso que Android ya no pide.
fun abrirAjustesDeLaApp(contexto: Context) {
    contexto.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${contexto.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
