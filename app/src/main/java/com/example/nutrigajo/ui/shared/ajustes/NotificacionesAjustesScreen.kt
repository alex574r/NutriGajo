package com.example.nutrigajo.ui.shared.ajustes

import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.nutrigajo.R
import com.example.nutrigajo.data.local.actividad
import com.example.nutrigajo.data.push.Avisos
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.components.FilaAjuste

private class TipoAviso(
    val canal: String,
    @param:DrawableRes val icono: Int,
    @param:StringRes val titulo: Int,
    @param:StringRes val descripcion: Int,
)

private val PLANES = TipoAviso(Avisos.CANAL_PLANES, R.drawable.ic_documento, R.string.aviso_canal_planes, R.string.notif_planes_desc)
private val MENSAJES = TipoAviso(Avisos.CANAL_MENSAJES, R.drawable.ic_mensaje, R.string.aviso_canal_mensajes, R.string.notif_mensajes_desc)
private val CITAS = TipoAviso(Avisos.CANAL_CITAS, R.drawable.ic_calendario, R.string.aviso_canal_citas, R.string.notif_citas_desc)

// Los tipos de aviso que le llegan a cada rol.
private fun tiposDe(rol: Rol) = when (rol) {
    Rol.PACIENTE -> listOf(PLANES, MENSAJES, CITAS)
    Rol.NUTRIOLOGO -> listOf(MENSAJES, CITAS)
    // ponytail: el entrenador aún no recibe avisos; el de «tu ejercicio se publicó» llega con el Worker de avisos
    Rol.ENTRENADOR, Rol.ADMIN -> emptyList()
}

// Android no deja que una app apague sus propios avisos: cada interruptor refleja cómo está ese tipo en el teléfono y tocarlo abre
// sus ajustes del sistema (sonido, vibración, silenciar). Al volver de allí se lee otra vez.
@Composable
fun NotificacionesAjustesScreen(rol: Rol, onVolver: () -> Unit, espacioInferior: Dp) {
    val contexto = LocalContext.current
    var lectura by remember { mutableIntStateOf(0) }
    val duenoCiclo = contexto.actividad()
    DisposableEffect(duenoCiclo) {
        val observador = LifecycleEventObserver { _, evento -> if (evento == Lifecycle.Event.ON_RESUME) lectura++ }
        duenoCiclo?.lifecycle?.addObserver(observador)
        onDispose { duenoCiclo?.lifecycle?.removeObserver(observador) }
    }
    val gestor = NotificationManagerCompat.from(contexto)
    // `lectura` fuerza leer de nuevo cada vez que la pantalla vuelve a primer plano
    val generales = remember(lectura) { gestor.areNotificationsEnabled() }
    val activos = remember(lectura) {
        tiposDe(rol).associate { it.canal to (gestor.getNotificationChannelCompat(it.canal)?.importance != NotificationManager.IMPORTANCE_NONE) }
    }

    PantallaAjuste(stringResource(R.string.notif_titulo), onVolver, espacioInferior = espacioInferior) {
        if (!generales) {
            FilaAjuste(
                icono = R.drawable.ic_warning,
                titulo = stringResource(R.string.notif_apagadas_titulo),
                descripcion = stringResource(R.string.notif_apagadas),
                onClick = {
                    contexto.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName),
                    )
                },
            )
        }
        GrupoAjustes(stringResource(R.string.notif_tipos))
        tiposDe(rol).forEach { tipo ->
            FilaAjuste(
                icono = tipo.icono,
                titulo = stringResource(tipo.titulo),
                descripcion = stringResource(tipo.descripcion),
                activa = generales && activos[tipo.canal] == true,
                onActivaChange = {
                    contexto.startActivity(
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, tipo.canal),
                    )
                },
            )
        }
        Text(
            stringResource(R.string.notif_ayuda),
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
