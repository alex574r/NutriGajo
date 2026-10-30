package com.example.nutrigajo.ui.shared.ajustes

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.example.nutrigajo.domain.model.Perfil

// Las pantallas a las que lleva la pestaña Configuración del perfil.
enum class Ajuste { INFORMACION, NOTIFICACIONES, SEGURIDAD, ACERCA, AYUDA, COMENTARIOS }

@Composable
fun AjustePantalla(
    ajuste: Ajuste,
    perfil: Perfil,
    versionApp: String,
    onCambiarContrasena: () -> Unit,
    onVolver: () -> Unit,
    espacioInferior: Dp,
) {
    when (ajuste) {
        Ajuste.INFORMACION -> InformacionPersonalScreen(perfil, onVolver, espacioInferior)
        Ajuste.NOTIFICACIONES -> NotificacionesAjustesScreen(perfil.rol, onVolver, espacioInferior)
        Ajuste.SEGURIDAD -> SeguridadScreen(onCambiarContrasena, onVolver, espacioInferior)
        Ajuste.ACERCA -> AcercaDeScreen(versionApp, onVolver, espacioInferior)
        Ajuste.AYUDA -> AyudaScreen(onVolver, espacioInferior)
        Ajuste.COMENTARIOS -> ComentariosScreen(perfil, versionApp, onVolver, espacioInferior)
    }
}
