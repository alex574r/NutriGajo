package com.example.nutrigajo.ui.permisos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.domain.EstadoPermiso
import com.example.nutrigajo.domain.PermisoApp

// La hoja que ofrece las notificaciones la primera vez que se abre el inicio del paciente: explica para qué sirven y deja elegir. «Dar permiso»
// muestra el cuadro del sistema; «Ahora no» (o cerrarla) no vuelve a insistir, el estado queda visible en «Permisos». `onCerrar` la quita.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaAvisos(permiso: PermisoUi, onCerrar: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            PanelPermiso(
                permiso = PermisoApp.NOTIFICACIONES,
                estado = permiso.estado,
                onAccion = {
                    onCerrar()
                    if (permiso.estado == EstadoPermiso.BLOQUEADO) permiso.abrirAjustes() else permiso.pedir()
                },
                onAhoraNo = onCerrar,
            )
        }
    }
}
