package com.example.nutrigajo.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Abanico de acciones, traducido a Compose del bloque «Action node» de Bencho (MIT). Un botón con sus acciones plegadas en la
// esquina hasta que se piden: aquí, al mantenerlo presionado.
//
// ── SALEN DE LA ESQUINA, NO LLEGAN A ELLA ────────────────
// El reflejo es hacer aparecer una barra de herramientas con un fundido, y es un error fácil de no ver: un control que aparece
// con un fundido no tiene relación con lo que controla; podría ser de cualquier cosa. Estos se dibujan sobre un camino que parte
// de la esquina del propio botón, nacen EN esa esquina, sin tamaño, y viajan hacia afuera por él: el botón es visiblemente de
// donde salieron y la esquina es visiblemente a lo que están unidos. Por eso crecen mientras viajan: lo que sale de algún sitio
// es pequeño mientras sigue casi dentro.
//
// ── UN RESORTE CADA UNO, Y EL RETRASO ES DEL DESTINO ─────
// Un resorte por botón, siempre construidos: el número de acciones decide cuántos se dibujan, no cuántos existen. Cada uno abre
// un instante después del anterior y el resorte hace el resto: la cascada son objetos con su propia física y no una animación
// con desfases, que es la diferencia entre un abanico que se abre y una lista que aparece en orden. Al cerrar va al revés,
// el último sale primero, para que el abanico se pliegue en la esquina y no se desplome desde dentro.
//
// ── EL AIRE ENTRE EL BOTÓN Y EL ABANICO ES UN HUECO ──────
// Los botones cuelgan fuera del botón; si el contenedor fuera solo el botón, los dedos que van de uno a otro caerían por el
// hueco. Aquí el contenedor mide lo que mide el abanico (más un margen), y la zona de toque es suya.
//
// ── EN EL TELÉFONO NO HAY «HOVER» ────────────────────────
// En la web el abanico sale al pasar el puntero. Aquí se mantiene presionado el botón: sale con una vibración, y se puede
// DESLIZAR el dedo hasta una acción (crece 12 % al pasar sobre ella) y soltar para elegirla. Si se suelta sin haberse movido,
// el abanico se queda abierto para tocar una acción; un toque en el botón, atrás, o 5 s sin hacer nada lo pliegan.

class AccionAbanico(val texto: String, val descripcion: String)

// medidas en dp. El botón de cada acción es de 44: es lo que cabe un dedo, y todo lo demás se mide desde él (la separación, el
// escondite, el margen), así que los botones crecen cambiando este número y ninguno más.
private const val DOT = 44f
private const val R = DOT / 2f

// de centro a centro a lo largo del camino: una distancia real y no un ángulo, así diez dp de aire entre dos botones son diez
// dp pase lo que pase en la esquina
private const val SPACING = DOT + 10f

// el aire entre el botón y el abanico (Bencho: 20..40 de «Reach» son 11 a 16 px; aquí, el punto medio)
private const val AIRE = 12f

// lo que se ensancha la zona de toque del abanico más allá de sus botones
private const val MARGEN = 6f

// el compás entre un botón y el siguiente (90 ms es lo más largo que puede ser una cascada antes de que el último se lea como
// tarde y no como siguiente) por el 55 % de «Stagger» con que se afinó
private const val COMPAS_MS = 90L * 55 / 100

// el resorte, escrito por razón de amortiguamiento (ζ ≈ 0.62 con el rebote en 20) y rigidez k·60² con k = 0.08 + 0.16·0.2
private const val ZETA = 0.62f
private const val RIGIDEZ = 403f

// un punto del camino y hacia dónde sale de la tarjeta ahí
private class Punto(val x: Float, val y: Float)

// ── cabalgan sobre la esquina PROPIA, desplazada ──────────
// El camino es el contorno del botón empujado hacia afuera una distancia constante: recto por el borde de arriba, redondo por la
// esquina con un arco concéntrico con la suya, y recto hacia abajo por el borde derecho. Los botones se colocan por cuánto han
// avanzado por él. NO por un círculo trazado desde un punto: eso solo se veía bien con un radio de esquina (con otro, el material
// del botón salía al encuentro de un camino que no se había movido) y no puede dar más que el cuarto entre los dos bordes: más
// allá los botones siguen girando alejándose de un borde que ya no se curva. Un camino desplazado no tiene ninguno de los dos
// problemas porque es la forma del propio botón: cada punto está exactamente a `off` de la superficie más cercana.
//
// `s` es la longitud de arco medida desde donde el arco de la esquina toca el borde de arriba, así que la esquina ocupa 0..len y
// lo que queda fuera ya corrió a un tramo recto. El origen es la esquina superior derecha del botón; x a la derecha, y abajo.
private fun cabalga(s: Float, esquina: Float, off: Float): Punto {
    // el arco desplazado es concéntrico con el del botón, así que su radio es el del botón más el desplazamiento, con un suelo justo
    // por encima de cero para un botón cuadrado, donde la esquina es un punto y el arco es un cuarto de vuelta en torno a él
    val arco = max(0.001f, esquina + off)
    val largo = arco * PI.toFloat() / 2f
    if (s <= 0f) return Punto(-esquina + s, -off)
    if (s >= largo) return Punto(off, esquina + (s - largo))
    val th = PI.toFloat() / 2f - s / arco
    return Punto(-esquina + arco * cos(th), esquina - arco * sin(th))
}

// ── dónde se esconden, y tiene que ser DEMOSTRABLEMENTE dentro ──
// Un punto de la diagonal del botón, tan adentro que un botón entero cabe detrás con nada asomando por la esquina. No puede ser
// «hacia atrás por la normal», que es lo que fue: el que está en la esquina se escondía hacia una esquina que, con esquina 0, no
// tiene material en la diagonal. Dos casos, porque una esquina redonda está mordida de lo mismo que una cuadrada tiene:
//   · esquina >= R + 2: el centro de la propia esquina. Está a `esquina` de los dos bordes y del arco, así que un botón ahí queda
//     al menos R por dentro de todo, con dos dp de sobra. El margen no es adorno: a justo R el botón es tangente al borde y asoma
//     un pelo.
//   · menos que eso: el arco se comió el sitio; se sigue más allá del disco de la esquina hasta que los dos bordes quedan libres.
// La distancia va por la diagonal, así que es un solo número. Nadie ve la posición; lo que se ve es que no asoma nada plegado.
private fun escondite(esquina: Float): Float =
    if (esquina >= R + 2f) esquina * sqrt(2f) else max(R * sqrt(2f) + 2f, esquina * sqrt(2f) + esquina + 2f)

// El botón que lleva el abanico. `modifier` lo coloca (alinear y separar del borde); el contenedor mide botón + abanico, con el
// botón abajo al centro, así que lo que se alinee abajo al centro queda con el botón donde lo quiere. El abanico no debe asomar por
// debajo del botón: con tres acciones no lo hace.
@Composable
fun BotonConAbanico(
    acciones: List<AccionAbanico>,
    onAccion: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tamano: Dp = 76.dp,
    esquina: Dp = 24.dp,
    // el toque corto; mantenerlo presionado abre el abanico aunque aquí sea falso
    habilitado: Boolean = true,
    // lo que dice el lector de pantalla para abrir el abanico (mantener presionado)
    etiquetaMantener: String = "Más opciones",
    fondo: Color = Color.White,
    tinta: Color = MaterialTheme.colorScheme.secondary,
    contenido: @Composable BoxScope.() -> Unit,
) {
    val n = acciones.size.coerceIn(2, 4)
    val haptico = LocalHapticFeedback.current
    val holgura = LocalViewConfiguration.current.touchSlop
    val tiempoLargo = LocalViewConfiguration.current.longPressTimeoutMillis
    val t = tamano.value
    val c = esquina.value
    val off = AIRE + R

    // el abanico se centra en la diagonal del botón y se reparte por longitud de arco: simétrico con cuantas acciones haya
    val puntos = remember(n, t, c) {
        val medio = (c + off) * PI.toFloat() / 4f
        List(n) { i -> cabalga(medio + (i - (n - 1) / 2f) * SPACING, c, off) }
    }
    val hondo = escondite(c) / sqrt(2f)
    val escondido = Punto(-hondo, hondo)

    // el cuadro que ocupan los botones, para que el contenedor incluya el aire: origen en la esquina de arriba a la izquierda del
    // botón, a lo ancho simétrico para que el botón quede al centro
    val medidas = remember(puntos, t) {
        var izq = 0f
        var der = t
        var arriba = 0f
        var abajo = t
        puntos.forEach {
            izq = min(izq, t + it.x - R - MARGEN)
            der = max(der, t + it.x + R + MARGEN)
            arriba = min(arriba, it.y - R - MARGEN)
            abajo = max(abajo, it.y + R + MARGEN)
        }
        val extra = max(-izq, der - t)
        floatArrayOf(extra, -arriba, t + 2 * extra, abajo - arriba)
    }
    val ox = medidas[0]
    val oy = medidas[1]

    var abierto by remember { mutableStateOf(false) }
    // abierto sin haberse deslizado: se queda para tocar una acción
    var fijo by remember { mutableStateOf(false) }
    var sobre by remember { mutableIntStateOf(-1) }
    var pulsado by remember { mutableStateOf(false) }
    val progreso = remember(n) { List(n) { Animatable(0f) } }
    val cerrar = { abierto = false; fijo = false; sobre = -1 }

    LaunchedEffect(abierto) {
        progreso.forEachIndexed { i, p ->
            launch {
                // abrir va del primero al último; cerrar, del último al primero
                val lugar = if (abierto) i else n - 1 - i
                delay(COMPAS_MS * lugar)
                p.animateTo(if (abierto) 1f else 0f, spring(dampingRatio = ZETA, stiffness = RIGIDEZ, visibilityThreshold = 0.001f))
            }
        }
    }
    BackHandler(enabled = abierto, onBack = cerrar)
    LaunchedEffect(abierto, fijo) {
        if (abierto && fijo) {
            delay(5000)
            cerrar()
        }
    }

    val alAccion by rememberUpdatedState(onAccion)
    val alToque by rememberUpdatedState(onClick)
    val activo by rememberUpdatedState(habilitado)
    val escala by animateFloatAsState(if (pulsado) 0.92f else 1f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "boton")

    Box(modifier.size(medidas[2].dp, medidas[3].dp)) {
        // primero los botones del abanico, y el botón PINTADO ENCIMA: por eso salen de la esquina y no del medio
        acciones.take(n).forEachIndexed { i, accion ->
            BotonDelAbanico(
                accion = accion,
                avance = progreso[i],
                destino = puntos[i],
                origen = escondido,
                anclaX = ox + t,
                anclaY = oy,
                encima = sobre == i,
                fondo = fondo,
                tinta = tinta,
                activo = abierto,
                onClick = {
                    alAccion(i)
                    cerrar()
                },
            )
        }
        Box(
            modifier = Modifier
                .offset(ox.dp, oy.dp)
                .size(tamano)
                .graphicsLayer {
                    scaleX = escala
                    scaleY = escala
                }
                .clip(RoundedCornerShape(esquina))
                .background(fondo)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    onClick {
                        alToque()
                        true
                    }
                    onLongClick(etiquetaMantener) {
                        abierto = true
                        fijo = true
                        true
                    }
                }
                .pointerInput(n, t) {
                    val lado = t * density
                    // el centro de cada acción en coordenadas del botón: su esquina superior derecha es (lado, 0)
                    val centros = puntos.map { androidx.compose.ui.geometry.Offset(lado + it.x * density, it.y * density) }
                    val alcance = (R + 8f) * density
                    awaitEachGesture {
                        val abajo = awaitFirstDown(requireUnconsumed = false)
                        pulsado = true
                        var largo = false
                        val subida = try {
                            withTimeout(tiempoLargo) { waitForUpOrCancellation() }
                        } catch (e: PointerEventTimeoutCancellationException) {
                            largo = true
                            null
                        }
                        if (!largo) {
                            pulsado = false
                            // un toque: abre o cierra el abanico si está abierto, y si no, hace lo suyo
                            if (subida != null) {
                                if (abierto) cerrar() else if (activo) alToque()
                            }
                            return@awaitEachGesture
                        }
                        // mantenido: sale el abanico, con una vibración
                        haptico.performHapticFeedback(HapticFeedbackType.LongPress)
                        abierto = true
                        fijo = false
                        var viajo = false
                        while (true) {
                            val evento = awaitPointerEvent()
                            val cambio = evento.changes.firstOrNull { it.id == abajo.id } ?: break
                            if (!cambio.pressed) {
                                // soltó: sobre una acción la elige; tras haberse deslizado a ningún sitio, pliega; si no se movió, queda abierto
                                val elegida = sobre
                                if (elegida >= 0) {
                                    alAccion(elegida)
                                    cerrar()
                                } else if (viajo) {
                                    cerrar()
                                } else {
                                    fijo = true
                                }
                                break
                            }
                            val encima = centros.indexOfFirst { (it - cambio.position).getDistance() <= alcance }
                            if (encima != sobre) {
                                sobre = encima
                                if (encima >= 0) haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            if ((cambio.position - abajo.position).getDistance() > holgura * 2) viajo = true
                            cambio.consume()
                        }
                        pulsado = false
                    }
                },
            contentAlignment = Alignment.Center,
            content = contenido,
        )
    }
}

@Composable
private fun BotonDelAbanico(
    accion: AccionAbanico,
    avance: Animatable<Float, AnimationVector1D>,
    destino: Punto,
    origen: Punto,
    anclaX: Float,
    anclaY: Float,
    encima: Boolean,
    fondo: Color,
    tinta: Color,
    activo: Boolean,
    onClick: () -> Unit,
) {
    // sin fundido y casi sin escala: un botón escondido DEBAJO del botón no necesita ser transparente para estar escondido, y un
    // fundido en algo que sale de un cajón es la única señal de que nunca estuvo ahí. El 0.82 que queda no es ocultación, es peso:
    // lo que sale empujado llega a su tamaño un instante después de llegar a su sitio. El crecimiento al pasar el dedo es aparte
    // porque dos escritores no pueden compartir una propiedad: el resorte manda cuánto ha salido y el dedo, si se le apunta.
    val zoom by animateFloatAsState(if (encima) 1.12f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "zoom")
    Box(
        modifier = Modifier
            .offset((anclaX - R).dp, (anclaY - R).dp)
            .size(DOT.dp)
            .graphicsLayer {
                // sin acotar: el sobrepaso del resorte lleva a cada botón un poco más allá de su sitio y de vuelta, no se aplana al final
                val u = avance.value.coerceIn(0f, 1.4f)
                translationX = (origen.x + (destino.x - origen.x) * u).dp.toPx()
                translationY = (origen.y + (destino.y - origen.y) * u).dp.toPx()
                val s = (0.82f + 0.18f * min(u, 1f)) * zoom
                scaleX = s
                scaleY = s
            }
            .clip(CircleShape)
            .background(fondo)
            .semantics { contentDescription = accion.descripcion }
            .clickable(enabled = activo, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(accion.texto, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = tinta, maxLines = 1)
    }
}
