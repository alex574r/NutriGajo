package com.example.nutrigajo.ui.shared.ajustes

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.alPulsar

private class Pregunta(@param:StringRes val pregunta: Int, @param:StringRes val respuesta: Int)

private val PREGUNTAS = listOf(
    Pregunta(R.string.faq_1_p, R.string.faq_1_r),
    Pregunta(R.string.faq_2_p, R.string.faq_2_r),
    Pregunta(R.string.faq_3_p, R.string.faq_3_r),
    Pregunta(R.string.faq_4_p, R.string.faq_4_r),
    Pregunta(R.string.faq_5_p, R.string.faq_5_r),
    Pregunta(R.string.faq_6_p, R.string.faq_6_r),
    Pregunta(R.string.faq_7_p, R.string.faq_7_r),
    Pregunta(R.string.faq_8_p, R.string.faq_8_r),
)

// Las preguntas frecuentes con buscador: se abre una a la vez y la búsqueda mira la pregunta y la respuesta.
@Composable
fun AyudaScreen(onVolver: () -> Unit, espacioInferior: Dp) {
    val colores = MaterialTheme.colorScheme
    var busqueda by rememberSaveable { mutableStateOf("") }
    // el índice en PREGUNTAS de la abierta; -1 ninguna
    var abierta by rememberSaveable { mutableIntStateOf(0) }
    val textos = PREGUNTAS.map { stringResource(it.pregunta) to stringResource(it.respuesta) }
    val consulta = busqueda.trim()
    val visibles = PREGUNTAS.indices.filter { i ->
        consulta.isEmpty() || textos[i].first.contains(consulta, ignoreCase = true) || textos[i].second.contains(consulta, ignoreCase = true)
    }

    PantallaAjuste(stringResource(R.string.ayuda_titulo), onVolver, espacioInferior = espacioInferior) {
        CampoTexto(stringResource(R.string.ayuda_buscar), busqueda, { busqueda = it.take(60) }, icono = R.drawable.ic_buscar, modifier = Modifier.fillMaxWidth())
        if (visibles.isEmpty()) {
            Text(
                stringResource(R.string.ayuda_sin_resultados),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        visibles.forEach { i ->
            FilaPregunta(
                pregunta = textos[i].first,
                respuesta = textos[i].second,
                abierta = abierta == i,
                onClick = { abierta = if (abierta == i) -1 else i },
            )
        }
    }
}

// Cerrada es una fila gris; abierta se rodea de naranja, la flecha gira y se ve la respuesta.
@Composable
private fun FilaPregunta(pregunta: String, respuesta: String, abierta: Boolean, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(colores.surfaceVariant)
            .border(1.dp, if (abierta) colores.primary else colores.surfaceVariant, forma)
            .alPulsar(onClick, minimo = 0.98f)
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(pregunta, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(if (abierta) colores.primary else colores.background),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = if (abierta) 90f else 0f },
                    tint = if (abierta) androidx.compose.ui.graphics.Color.White else colores.onBackground,
                )
            }
        }
        if (abierta) {
            Text(respuesta, modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
        }
    }
}
