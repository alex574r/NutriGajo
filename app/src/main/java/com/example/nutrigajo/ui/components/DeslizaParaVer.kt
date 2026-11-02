package com.example.nutrigajo.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ALTO = 52.dp
private val PULGAR = 44.dp
private val MARGEN = 4.dp

// «Desliza para empezar» del diseño: una píldora de cristal con un círculo blanco que se arrastra hasta el final. Con el 80 % del
// recorrido llama a `onCompletar` y el círculo vuelve solo al inicio; si se suelta antes, vuelve con resorte. Va sobre fotos.
@Composable
fun DeslizaParaVer(texto: String, onCompletar: () -> Unit, modifier: Modifier = Modifier) {
    val densidad = LocalDensity.current
    val ambito = rememberCoroutineScope()
    var arrastre by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(modifier.height(ALTO).cristal(CircleShape).clip(CircleShape)) {
        val recorrido = with(densidad) { (maxWidth - PULGAR - MARGEN * 2).toPx() }.coerceAtLeast(1f)
        Text(
            text = texto,
            modifier = Modifier.align(Alignment.Center).padding(start = PULGAR).alpha(1f - arrastre / recorrido),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(arrastre.roundToInt(), 0) }
                .padding(MARGEN)
                .size(PULGAR)
                .background(Color.White, CircleShape)
                .pointerInput(recorrido) {
                    val volver = {
                        ambito.launch { animate(arrastre, 0f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)) { v, _ -> arrastre = v } }
                    }
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (arrastre >= recorrido * 0.8f) onCompletar()
                            volver()
                        },
                        onDragCancel = { volver() },
                    ) { cambio, delta ->
                        cambio.consume()
                        arrastre = (arrastre + delta).coerceIn(0f, recorrido)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_flecha), contentDescription = null, modifier = Modifier.size(22.dp), tint = Color(0xFF111111))
        }
    }
}
