package com.example.nutrigajo.ui.actualizacion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.nutrigajo.data.actualizacion.InstaladorApk

// Lo que se ve de las actualizaciones, encima del inicio: la hoja de una opcional o la pantalla completa de una obligatoria. Al volver de la
// pantalla del sistema donde se da el permiso de instalar, sigue con la instalación.
@Composable
fun ActualizacionHost(vm: ActualizacionViewModel, onCerrarApp: () -> Unit) {
    val fase by vm.fase.collectAsState()
    val hoja by vm.hoja.collectAsState()
    val contexto = LocalContext.current
    val abrirConfiguracion = { contexto.startActivity(InstaladorApk.intentDePermiso(contexto)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.retomar() }
    // cuando falta el permiso se abre solo su pantalla, una vez; si vuelve sin darlo, el botón la abre otra vez (y girar el teléfono no)
    LaunchedEffect(Unit) { vm.pedirPermiso.collect { contexto.startActivity(InstaladorApk.intentDePermiso(contexto)) } }

    val actual = fase
    if (actual.actualizacion == null || !hoja) return
    if (actual.obligatoria) {
        ActualizacionObligatoria(
            fase = actual,
            onActualizar = vm::actualizar,
            onReintentar = vm::reintentar,
            onAbrirConfiguracion = abrirConfiguracion,
            onCerrarApp = onCerrarApp,
        )
    } else {
        ActualizacionHoja(
            fase = actual,
            onActualizar = vm::actualizar,
            onMasTarde = vm::masTarde,
            onReintentar = vm::reintentar,
            onAbrirConfiguracion = abrirConfiguracion,
            onCerrar = vm::cerrarHoja,
        )
    }
}
