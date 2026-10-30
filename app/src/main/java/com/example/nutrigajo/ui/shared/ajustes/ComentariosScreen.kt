package com.example.nutrigajo.ui.shared.ajustes

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.alPulsar
import kotlinx.coroutines.delay

// `codigo` es lo que se guarda (no cambia con el idioma); `etiqueta`, lo que se ve.
private class Area(val codigo: String, @param:StringRes val etiqueta: Int)

private val AREAS = listOf(
    Area("rendimiento", R.string.comentarios_area_rendimiento),
    Area("plan", R.string.comentarios_area_plan),
    Area("chat", R.string.comentarios_area_chat),
    Area("citas", R.string.comentarios_area_citas),
    Area("metricas", R.string.comentarios_area_metricas),
    Area("diseno", R.string.comentarios_area_diseno),
    Area("errores", R.string.comentarios_area_errores),
    Area("otro", R.string.comentarios_area_otro),
)

private const val MAX_MENSAJE = 1000

// Comentarios para quien hace la app: las partes que se quieren mejorar y, si se quiere, un texto. Se guardan en Firestore.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComentariosScreen(
    perfil: Perfil,
    versionApp: String,
    onVolver: () -> Unit,
    espacioInferior: Dp,
    vm: AjustesViewModel = viewModel(key = "ajustes-${perfil.uid}"),
) {
    val colores = MaterialTheme.colorScheme
    val estado by vm.comentario.collectAsState()
    var elegidas by rememberSaveable { mutableStateOf(listOf<String>()) }
    var mensaje by rememberSaveable { mutableStateOf("") }
    val salir = {
        vm.limpiar()
        onVolver()
    }
    // enviado: se deja ver el agradecimiento un momento y se regresa
    LaunchedEffect(estado.listo) {
        if (estado.listo) {
            delay(1400)
            salir()
        }
    }

    PantallaAjuste(stringResource(R.string.comentarios_titulo), onVolver = salir, espacioInferior = espacioInferior) {
        Text(stringResource(R.string.comentarios_pregunta), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AREAS.forEach { area ->
                val elegida = area.codigo in elegidas
                Text(
                    text = stringResource(area.etiqueta),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (elegida) colores.primary else colores.surfaceVariant)
                        .alPulsar({ elegidas = if (elegida) elegidas - area.codigo else elegidas + area.codigo }, minimo = 0.94f)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (elegida) Color.White else colores.onSurfaceVariant,
                )
            }
        }
        CampoTexto(
            etiqueta = stringResource(R.string.comentarios_mensaje),
            valor = mensaje,
            onValorChange = { mensaje = it.take(MAX_MENSAJE) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            lineas = 5,
        )
        estado.error?.let { AlertaError(stringResource(it)) }
        if (estado.listo) {
            Text(stringResource(R.string.comentarios_gracias), style = MaterialTheme.typography.bodyMedium, color = colores.primary)
        }
        BotonPrimario(
            texto = stringResource(R.string.comentarios_enviar),
            onClick = { vm.enviarComentario(perfil.rol, elegidas, mensaje.trim(), versionApp) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = (elegidas.isNotEmpty() || mensaje.isNotBlank()) && !estado.enviando && !estado.listo,
            cargando = estado.enviando,
        )
    }
}
