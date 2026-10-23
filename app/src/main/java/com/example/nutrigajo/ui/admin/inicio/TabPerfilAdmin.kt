package com.example.nutrigajo.ui.admin.inicio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.components.ALTO_BARRA_INFERIOR
import com.example.nutrigajo.ui.components.TransicionPerfil
import com.example.nutrigajo.ui.components.AccionPerfil
import com.example.nutrigajo.ui.shared.perfil.PerfilScreen
import com.example.nutrigajo.ui.shared.perfil.altaCorta
import com.example.nutrigajo.ui.shared.perfil.datosBase

// Cuenta del administrador con el molde del perfil de los demás roles: sus datos y la configuración de la cuenta.
// Los botones llevan a la cola de solicitudes y a la consulta de la SEP.
@Composable
fun TabPerfilAdmin(
    perfil: Perfil,
    pendientes: Int,
    onSolicitudes: () -> Unit,
    onSep: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    transicion: TransicionPerfil? = null,
) {
    PerfilScreen(
        perfil = perfil,
        rol = R.string.rol_admin,
        verificado = false,
        estadisticas = listOfNotNull(
            pluralStringResource(R.plurals.admin_pendientes, pendientes, pendientes),
            perfil.altaCorta()?.let { stringResource(R.string.perfil_desde, it) },
        ),
        principal = AccionPerfil(R.drawable.ic_inicio, R.string.admin_nav_solicitudes, onSolicitudes),
        secundaria = AccionPerfil(R.drawable.ic_buscar, R.string.perfil_sep, onSep),
        datos = datosBase(perfil, R.string.rol_admin),
        onCerrarSesion = onCerrarSesion,
        modifier = modifier,
        espacioInferior = ALTO_BARRA_INFERIOR,
        transicion = transicion,
    )
}
