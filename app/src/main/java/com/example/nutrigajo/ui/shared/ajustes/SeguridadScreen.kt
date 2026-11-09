package com.example.nutrigajo.ui.shared.ajustes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.example.nutrigajo.R
import com.example.nutrigajo.data.local.AjustesApp
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.TarjetaAjustes

// La contraseña (el correo con el enlace sale del diálogo del perfil) y pedir la huella o el rostro al abrir la app.
@Composable
fun SeguridadScreen(onCambiarContrasena: () -> Unit, onVolver: () -> Unit, espacioInferior: Dp) {
    val contexto = LocalContext.current
    val bloqueo by AjustesApp.bloqueo
    PantallaAjuste(stringResource(R.string.seguridad_titulo), onVolver, espacioInferior = espacioInferior) {
        TarjetaAjustes {
            FilaAjuste(
                icono = R.drawable.ic_lock,
                titulo = stringResource(R.string.perfil_contrasena_titulo),
                descripcion = stringResource(R.string.seguridad_contrasena_desc),
                plana = true,
                onClick = onCambiarContrasena,
            )
            FilaAjuste(
                icono = R.drawable.ic_verificado,
                titulo = stringResource(R.string.ajustes_biometria),
                descripcion = stringResource(R.string.seguridad_biometria_desc),
                plana = true,
                activa = bloqueo,
                onActivaChange = { alternarBloqueo(contexto, it) },
            )
        }
    }
}
