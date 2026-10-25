package com.example.nutrigajo.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

private val ALTO_PESTANA = 40.dp

// Pestañas del diseño: contenedor gris y la elegida en negro con anillo de foco negro al 25%.
// Microinteracciones: la píldora negra (con su anillo) viaja de una pestaña a otra con un resorte, se estira un poco en el
// camino y se pasa antes de asentarse; el color de cada título sigue a la píldora al pasar; al pulsar, el título se encoge y
// el cambio de pestaña da un toque háptico suave. Compose omite las animaciones si se desactivan en el sistema.
@Composable
fun SelectorPestanas(
    @StringRes titulos: List<Int>,
    seleccionada: Int,
    onSeleccionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val densidad = LocalDensity.current
    val haptico = LocalHapticFeedback.current
    var ancho by remember { mutableFloatStateOf(0f) }
    val anchoPestana = ancho / titulos.size
    // posición de la píldora medida en pestañas (0 = la primera)
    val posicion = remember { Animatable(seleccionada.toFloat()) }
    LaunchedEffect(seleccionada) {
        posicion.animateTo(seleccionada.toFloat(), spring(dampingRatio = 0.62f, stiffness = 380f))
    }

    Box(
        modifier
            .fillMaxWidth()
            .background(colores.surfaceVariant, RoundedCornerShape(28.dp))
            .onSizeChanged { ancho = it.width.toFloat() },
    ) {
        if (ancho > 0f) {
            AnilloFoco(
                activo = true,
                radio = 20.dp,
                modifier = Modifier
                    .width(with(densidad) { anchoPestana.toDp() })
                    .graphicsLayer {
                        translationX = posicion.value * anchoPestana
                        // se estira a mitad de camino y recupera su ancho al llegar
                        scaleX = 1f + 0.07f * sin(PI.toFloat() * (posicion.value - floor(posicion.value)).coerceIn(0f, 1f))
                    },
                anillo = colores.onBackground.copy(alpha = 0.25f),
            ) {
                Box(Modifier.fillMaxWidth().height(ALTO_PESTANA).clip(RoundedCornerShape(20.dp)).background(colores.onBackground))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            titulos.forEachIndexed { indice, titulo ->
                val activa = indice == seleccionada
                val interaccion = remember { MutableInteractionSource() }
                val pulsada by interaccion.collectIsPressedAsState()
                val escala by animateFloatAsState(
                    targetValue = if (pulsada) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
                    label = "pulsacion",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(ANILLO_FOCO)
                        .height(ALTO_PESTANA)
                        .clip(RoundedCornerShape(20.dp))
                        .selectable(
                            selected = activa,
                            interactionSource = interaccion,
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                if (!activa) haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSeleccionar(indice)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // 1 cuando la píldora está justo debajo del título, 0 cuando está a una pestaña o más
                    val cerca = 1f - abs(posicion.value - indice).coerceIn(0f, 1f)
                    Text(
                        text = stringResource(titulo),
                        modifier = Modifier.graphicsLayer {
                            scaleX = escala
                            scaleY = escala
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = lerp(colores.onSurfaceVariant, colores.background, cerca),
                    )
                }
            }
        }
    }
}
