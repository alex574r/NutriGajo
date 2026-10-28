package com.example.nutrigajo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

// Microinteracciones compartidas, traducidas a Compose de los bloques Carousel y Generate de Bencho (MIT). Lo que tienen en
// común: todo lo que se asienta lo hace con un resorte y no con una curva fija; ninguna lleva librería; y cada efecto vive
// en un solo lugar, para que dos pantallas no lo copien a mano. Compose omite las animaciones si el usuario las desactiva
// en el sistema, así que ninguna necesita su propio «movimiento reducido».

// El resorte con el que se asienta el tacto: poco amortiguado, con un rebote que se ve y nada más. 0.41 es el valor del
// ajuste «50» de Bencho (ahí se explica por qué no 0.15: con ese todo rebotaba un sesenta por ciento).
private fun resorte(): SpringSpec<Float> = spring(dampingRatio = 0.41f, stiffness = 380f)

// ── Carousel: la tarjeta se hunde bajo el dedo ────────────────────────────────────────────────────────────────────────
// Se HUNDE, no se levanta: el punto que tocas se va hacia atrás y el lado opuesto sube, así la tarjeta se siente tocada y
// no expuesta. Son tres lecturas de un mismo número, la posición del dedo (-1..1 en cada eje) con resorte: la inclinación,
// una sombra donde está el dedo y un destello tenue en el borde contrario. La sombra se queda quieta en Bencho (parpadeaba
// con varias tarjetas); aquí tampoco se mueve ninguna sombra de elevación.
// No consume el toque (mira en la pasada Initial), así que el clic de la tarjeta sigue funcionando. Un dedo que se arrastra
// más allá de la holgura está desplazando la lista, no pulsando la tarjeta: la suelta, como el carrusel con una tarjeta
// «llevada» en lugar de «pulsada».
// `forma` recorta la sombra y el destello a la silueta de la tarjeta; ponlo antes de clip y background.
fun Modifier.hundible(forma: Shape, profundidad: Float = 0.5f): Modifier = composed {
    val alcance = rememberCoroutineScope()
    val densidad = LocalDensity.current
    val holgura = LocalViewConfiguration.current.touchSlop
    val x = remember { Animatable(0f) }
    val y = remember { Animatable(0f) }
    val luz = remember { Animatable(0f) }
    var tam by remember { mutableStateOf(IntSize.Zero) }
    val hondo = profundidad.coerceIn(0f, 1f)

    // null: soltada, vuelve al reposo
    fun posar(p: Offset?) {
        alcance.launch { luz.animateTo(if (p == null) 0f else 1f, resorte()) }
        val nx = if (p == null || tam.width == 0) 0f else (p.x / tam.width * 2f - 1f).coerceIn(-1f, 1f)
        val ny = if (p == null || tam.height == 0) 0f else (p.y / tam.height * 2f - 1f).coerceIn(-1f, 1f)
        alcance.launch { x.animateTo(nx, resorte()) }
        alcance.launch { y.animateTo(ny, resorte()) }
    }

    this
        .onSizeChanged { tam = it }
        .pointerInput(Unit) {
            awaitEachGesture {
                val abajo = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                posar(abajo.position)
                while (true) {
                    val evento = awaitPointerEvent(PointerEventPass.Initial)
                    val toque = evento.changes.firstOrNull { it.id == abajo.id } ?: break
                    if (!toque.pressed || (toque.position - abajo.position).getDistance() > holgura) break
                    posar(toque.position)
                }
                posar(null)
            }
        }
        .graphicsLayer {
            // el retroceso de Bencho es un translateZ; sin tercera dimensión, es un poco de escala
            val e = 1f - 0.05f * hondo * luz.value
            scaleX = e
            scaleY = e
            rotationX = -y.value * 13f * hondo
            rotationY = x.value * 13f * hondo
            cameraDistance = 14f * densidad.density
        }
        .drawWithContent {
            drawContent()
            val l = luz.value
            if (l > 0.01f) {
                val centro = Offset((x.value + 1f) / 2f * size.width, (y.value + 1f) / 2f * size.height)
                val silueta = Path().apply { addOutline(forma.createOutline(size, layoutDirection, this@drawWithContent)) }
                clipPath(silueta) {
                    // la hendidura: oscura donde está el dedo, que es donde la superficie está más honda
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color(0xFF090E1C).copy(alpha = 0.5f * hondo * l), Color.Transparent),
                            center = centro,
                            radius = size.minDimension * 0.7f,
                        ),
                    )
                    // y el borde contrario, que subió y recoge la luz; una insinuación, no un brillo
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.16f * hondo * l), Color.Transparent),
                            center = Offset(size.width - centro.x, size.height - centro.y),
                            radius = size.minDimension * 0.8f,
                        ),
                    )
                }
            }
        }
}

// El botón pequeño que se encoge al pulsarlo, con resorte. Sin onda: el encogerse es la respuesta (como en las pestañas).
fun Modifier.alPulsar(
    onClick: () -> Unit,
    role: Role = Role.Button,
    enabled: Boolean = true,
    minimo: Float = 0.92f,
): Modifier = composed {
    val interaccion = remember { MutableInteractionSource() }
    val pulsado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pulsado) minimo else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "pulsacion",
    )
    this
        .graphicsLayer {
            scaleX = escala
            scaleY = escala
        }
        .clickable(interactionSource = interaccion, indication = null, enabled = enabled, role = role, onClick = onClick)
}

// Aparece subiendo 10 dp, con un retraso por posición para que lleguen en cascada. `animar` en falso la deja puesta (para no
// repetir la entrada cada vez que se vuelve a la pantalla).
fun Modifier.aparece(indice: Int, animar: Boolean = true): Modifier = composed {
    val progreso = remember { Animatable(if (animar) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animar) progreso.animateTo(1f, tween(260, delayMillis = 20 + indice * 40, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        alpha = progreso.value
        translationY = (1f - progreso.value) * 10.dp.toPx()
    }
}

// ── Carousel: la deriva en reposo ─────────────────────────────────────────────────────────────────────────────────────
// Sube y baja unos dp y se mece un grado, sin que nadie la toque. El periodo se da por tarjeta y no debe dividirse entre el de
// sus vecinas (Bencho usa 4.7, 5.9, 6.7, 5.3, 7.1 y 6.1 s); así nunca respiran al unísono. Ida y vuelta con suavizado.
fun Modifier.flota(periodoSegundos: Float): Modifier = composed {
    val fase by rememberInfiniteTransition(label = "flota").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween((periodoSegundos * 1000).toInt(), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fase",
    )
    graphicsLayer {
        translationY = fase * 3.dp.toPx()
        rotationZ = fase * 1.2f
    }
}

val PERIODOS_FLOTA = listOf(4.7f, 5.9f, 6.7f, 5.3f, 7.1f, 6.1f)

// ── Generate: la imagen se forma ──────────────────────────────────────────────────────────────────────────────────────
// Lo grueso primero y lo fino al final: llega desenfocada y un poco grande, y el desenfoque cae rápido al principio y se arrastra
// por los radios pequeños, que es donde vive el detalle. `clave` la vuelve a formar cuando cambia lo que se muestra. El
// desenfoque necesita Android 12; antes de eso queda el resto (opacidad y escala).
fun Modifier.formandose(clave: Any? = Unit, milisegundos: Int = 700): Modifier = composed {
    val avance = remember(clave) { Animatable(0f) }
    LaunchedEffect(clave) { avance.animateTo(1f, tween(milisegundos, easing = FastOutSlowInEasing)) }
    graphicsLayer {
        val borroso = 18.dp.toPx() * (1f - avance.value).pow(2.2f)
        renderEffect = if (borroso > 0.5f) BlurEffect(borroso, borroso, TileMode.Decal) else null
        alpha = 0.6f + 0.4f * avance.value
        val e = 1.04f - 0.04f * avance.value
        scaleX = e
        scaleY = e
    }
}

// ── La sacudida del error ─────────────────────────────────────────────────────────────────────────────────────────────
// Seno amortiguado: sale de un lado al otro y se va apagando (los mismos números que las casillas del código). Se dispara
// cada vez que `clave` cambia a algo que no es null.
private const val MS_SACUDIDA = 620f

fun Modifier.sacudida(clave: Any?): Modifier = composed {
    val t = remember { Animatable(0f) }
    LaunchedEffect(clave) {
        if (clave != null) {
            t.snapTo(0f)
            t.animateTo(1f, tween(MS_SACUDIDA.toInt(), easing = LinearEasing))
            t.snapTo(0f)
        }
    }
    graphicsLayer {
        val ms = t.value * MS_SACUDIDA
        translationX = 6.dp.toPx() * exp(-ms / 190f) * sin(ms * 2f * PI.toFloat() / 90f)
    }
}
