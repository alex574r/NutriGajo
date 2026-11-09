package com.example.nutrigajo.ui.shared.ajustes

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.example.nutrigajo.domain.model.Perfil

// Las pantallas a las que lleva la pestaña Configuración del perfil.
enum class Ajuste { INFORMACION, NOTIFICACIONES, SEGURIDAD, PERMISOS, ACERCA, AYUDA, COMENTARIOS, ELIMINAR_CUENTA }

@Composable
fun AjustePantalla(
    ajuste: Ajuste,
    perfil: Perfil,
    versionApp: String,
    onCambiarContrasena: () -> Unit,
    onVolver: () -> Unit,
    espacioInferior: Dp,
    // la cuenta se borró: se cierra la sesión (el Worker ya borró la cuenta de Firebase)
    onCuentaEliminada: () -> Unit = {},
) {
    when (ajuste) {
        Ajuste.INFORMACION -> InformacionPersonalScreen(perfil, onVolver, espacioInferior)
        Ajuste.NOTIFICACIONES -> NotificacionesAjustesScreen(perfil.rol, onVolver, espacioInferior)
        Ajuste.SEGURIDAD -> SeguridadScreen(onCambiarContrasena, onVolver, espacioInferior)
        Ajuste.PERMISOS -> PermisosScreen(perfil, onVolver, espacioInferior)
        Ajuste.ACERCA -> AcercaDeScreen(versionApp, onVolver, espacioInferior)
        Ajuste.AYUDA -> AyudaScreen(onVolver, espacioInferior)
        Ajuste.COMENTARIOS -> ComentariosScreen(perfil, versionApp, onVolver, espacioInferior)
        Ajuste.ELIMINAR_CUENTA -> EliminarCuentaScreen(perfil, onCuentaEliminada, onVolver, espacioInferior)
    }
}
