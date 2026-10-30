package com.example.nutrigajo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

// Contador rodante, traducido a Compose del bloque «Rolling counter» de Bencho (MIT). Un número entre un menos y un más: cada
// dígito es una tira de 0–9 detrás de una ventana, y al cambiar el número las tiras ruedan a su dígito nuevo como un odómetro.
//
// ── CADA COLUMNA CUENTA, NO MUESTRA ──────────────────────
// El destino de una columna no es su dígito sino cuántas decenas, centenas… enteras hay en el valor: 1284 es 1284 para la de las
// unidades, 128 para la de las decenas y 12 para la de las centenas. Así 9 → 10 es la de las unidades avanzando un paso más
// (9 → 0) y la de las decenas avanzando uno — nunca una tira que retrocede por ocho dígitos hasta llegar al cero. La tira es
// 0–9 y otro 0, y donde está parada es la cuenta tomada módulo diez.
//
// ── UN RESORTE POR COLUMNA, Y VAN POR TURNO ──────────────
// Un cambio llega primero a las unidades y a cada columna de la izquierda un poco después, así un acarreo se lee como acarreo,
// subiendo por el número, y no como toda la fila saltando.
//
// ── TRES FORMAS DE CAMBIARLO ─────────────────────────────
// Un botón da un paso; mantenido, se repite, más rápido y en pasos más grandes cuanto más se mantiene (`repetirAlMantener`). O
// se toma el número mismo y se arrastra hacia arriba o abajo, un paso cada pocos dp; si se suelta en movimiento sigue rodando y
// se va frenando.
//
// El valor lo manda quien llama: aquí solo se pide «n pasos» (`onPasos`) y el valor nuevo vuelve por `valor`.

// el resorte de las columnas, escrito por razón de amortiguamiento (ζ ≈ 0.44 con el rebote en 45, donde Bencho lo afinó) y no por
// los dos números sueltos: rigidez = k·60² con k = 0.08 + 0.16·0.45 por cuadro
private const val ZETA = 0.44f
private const val RIGIDEZ = 547f

// lo que se arrastra por paso, en dp
private val DP_POR_PASO = 12.dp

// ms entre que se mueve una columna y la siguiente a su izquierda
private const val RETRASO_COLUMNA_MS = 45L

// la tira va de 0 a 9 y repite el 0 para que el paso 9 → 0 sea hacia adelante
private val TIRA = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0)

private val POTENCIAS = intArrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000)

// `digitos` es cuántas columnas caben (las del valor máximo); las que el valor no usa se pliegan a ancho cero.
@Composable
fun ContadorRodante(
    valor: Int,
    onPasos: (Int) -> Unit,
    digitos: Int,
    modifier: Modifier = Modifier,
    estilo: TextStyle = MaterialTheme.typography.displayLarge,
    color: Color = MaterialTheme.colorScheme.onBackground,
    separadorDeMiles: Boolean = true,
) {
    val densidad = LocalDensity.current
    val medidor = rememberTextMeasurer()
    // dígitos del mismo ancho: si no, la columna del 1 sería más angosta y el número temblaría al rodar
    val tabular = remember(estilo) { estilo.copy(fontFeatureSettings = "tnum") }
    val anchoDigito = with(densidad) { medidor.measure("0", tabular).size.width.toDp() }
    val anchoComa = with(densidad) { medidor.measure(",", tabular).size.width.toDp() }
    // la ventana es más alta que la letra: los dígitos se desvanecen arriba y abajo mientras ruedan y en reposo no deben tocar eso
    val celda = with(densidad) {
        val alto = estilo.lineHeight.let { if (it.isSp) it.toDp() else (estilo.fontSize.value * (if (it.isEm) it.value else 1f)).sp.toDp() }
        alto * 1.3f
    }
    val mostrados = maxOf(1, valor.toString().length)
    val texto = if (separadorDeMiles) String.format(Locale.getDefault(), "%,d", valor) else valor.toString()

    val sobreElNumero = rememberUpdatedState(onPasos)
    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = texto }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // los dígitos que ruedan se desvanecen en el borde de la ventana
                drawRect(
                    // 12 % y no 18: la cola de la coma baja más que los dígitos y se veía cortada
                    Brush.verticalGradient(0f to Color.Transparent, 0.12f to Color.Black, 0.9f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn,
                )
            }
            .arrastrable(sobreElNumero),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (lugar in digitos - 1 downTo 0) {
            ColumnaRodante(lugar, valor, visible = lugar < mostrados, celda, anchoDigito, tabular, color)
            if (lugar == 3 && separadorDeMiles) {
                val ancho by animateDpAsState(if (mostrados > 3) anchoComa else 0.dp, spring(dampingRatio = 0.7f, stiffness = 380f), label = "coma")
                Box(Modifier.width(ancho).height(celda).clipToBounds().alpha(if (mostrados > 3) 1f else 0f), contentAlignment = Alignment.Center) {
                    Text(",", modifier = Modifier.wrapContentSize(Alignment.Center, unbounded = true), style = tabular, color = color, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun ColumnaRodante(lugar: Int, valor: Int, visible: Boolean, celda: Dp, ancho: Dp, estilo: TextStyle, color: Color) {
    val destino = valor / POTENCIAS[lugar]
    val posicion = remember { Animatable(destino.toFloat()) }
    LaunchedEffect(destino) {
        delay(lugar * RETRASO_COLUMNA_MS)
        // una columna lejos de casa salta casi todo el camino en vueltas enteras: se ve igual, y un manotazo no deja las
        // unidades girando un segundo
        val brecha = destino - posicion.value
        if (abs(brecha) > 24f) posicion.snapTo(posicion.value + ((brecha - sign(brecha) * 24f) / 10f).roundToInt() * 10f)
        posicion.animateTo(destino.toFloat(), spring(dampingRatio = ZETA, stiffness = RIGIDEZ, visibilityThreshold = 0.004f))
    }
    val anchoVisible by animateDpAsState(if (visible) ancho else 0.dp, spring(dampingRatio = 0.7f, stiffness = 380f), label = "columna")
    Box(Modifier.width(anchoVisible).height(celda).clipToBounds().alpha(if (visible) 1f else 0f)) {
        Column(
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .graphicsLayer {
                    val m = ((posicion.value % 10f) + 10f) % 10f
                    translationY = -m * celda.toPx()
                    // lo que gira se emborrona un poco en vertical, como una tira de verdad
                    val velocidad = abs(posicion.velocity) / 60f
                    val borroso = min(3f, velocidad * 2.4f) * density
                    renderEffect = if (borroso > 0.15f * density) BlurEffect(0.01f, borroso, TileMode.Decal) else null
                },
        ) {
            TIRA.forEach { d ->
                Box(Modifier.width(ancho).height(celda), contentAlignment = Alignment.Center) {
                    Text(d.toString(), modifier = Modifier.wrapContentSize(Alignment.Center, unbounded = true), style = estilo, color = color, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

// Tomar el número y arrastrarlo: arriba es más. Un paso cada DP_POR_PASO; si se suelta en movimiento sigue rodando y se frena
// (0.94 de la velocidad por cuadro, como en el original), y una mano que se detuvo antes de soltar no lanza nada.
private fun Modifier.arrastrable(onPasos: androidx.compose.runtime.State<(Int) -> Unit>): Modifier = pointerInput(Unit) {
    val porPaso = DP_POR_PASO.toPx()
    coroutineScope {
        var inercia: Job? = null
        // convierte lo acumulado en pasos enteros y devuelve lo que sobra
        fun pasos(acumulado: Float): Float {
            val n = if (acumulado > 0f) floor(acumulado / porPaso).toInt() else ceil(acumulado / porPaso).toInt()
            if (n != 0) onPasos.value(n)
            return acumulado - n * porPaso
        }
        awaitEachGesture {
            val abajo = awaitFirstDown(requireUnconsumed = false)
            inercia?.cancel()
            var acumulado = 0f
            var velocidad = 0f
            var y = abajo.position.y
            var ultimo = abajo.uptimeMillis
            while (true) {
                val evento = awaitPointerEvent()
                val cambio = evento.changes.firstOrNull { it.id == abajo.id } ?: break
                if (!cambio.pressed) {
                    // soltó: ¿venía moviéndose?
                    val enMovimiento = cambio.uptimeMillis - ultimo <= 90
                    var v = if (enMovimiento) velocidad * 16.67f else 0f
                    if (abs(v) >= 2f) {
                        var resto = acumulado
                        inercia = launch {
                            while (abs(v) > 0.4f) {
                                resto = pasos(resto + v)
                                v *= 0.94f
                                delay(16)
                            }
                        }
                    }
                    break
                }
                // arriba es más
                val dy = y - cambio.position.y
                y = cambio.position.y
                val dt = maxOf(1L, cambio.uptimeMillis - ultimo)
                ultimo = cambio.uptimeMillis
                velocidad = velocidad * 0.6f + (dy / dt) * 0.4f
                acumulado = pasos(acumulado + dy)
                cambio.consume()
            }
        }
    }
}

// Un botón de +1 / −1 que se repite al mantenerlo: tras 380 ms empieza, a un paso cada 90 ms; pasado 0.9 s cada 55 ms, y pasado
// 1.5 s de `aceleracion` en `aceleracion` (diez en el original, que contaba hasta 99 999 de uno en uno). Se encoge un poco mientras
// está pulsado. `direccion` es 1 o −1 y `onPasos` recibe los pasos con signo.
// ponytail: la aceleración la pone quien lo usa según los pasos que tenga su rango: con un rango de un centenar de pasos, diez a la
// vez lo cruza en medio segundo; 1 lo deja sin acelerar.
fun Modifier.repetirAlMantener(direccion: Int, aceleracion: Int = 10, onPasos: (Int) -> Unit): Modifier = composed {
    val actual by rememberUpdatedState(onPasos)
    var pulsado by remember { mutableStateOf(false) }
    val escala by animateFloatAsState(if (pulsado) 0.9f else 1f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "tecla")
    this
        .graphicsLayer {
            scaleX = escala
            scaleY = escala
        }
        .semantics {
            role = Role.Button
            onClick {
                actual(direccion)
                true
            }
        }
        .pointerInput(direccion) {
            coroutineScope {
                awaitEachGesture {
                    awaitFirstDown()
                    pulsado = true
                    val desde = System.currentTimeMillis()
                    val repite = launch {
                        actual(direccion)
                        delay(380)
                        while (true) {
                            val mantenido = System.currentTimeMillis() - desde
                            actual(direccion * if (mantenido > 1500) aceleracion else 1)
                            delay(if (mantenido > 900) 55 else 90)
                        }
                    }
                    waitForUpOrCancellation()
                    repite.cancel()
                    pulsado = false
                }
            }
        }
}

// El conjunto completo: el número rodante entre un menos y un más, en una placa. Para donde no hay una pantalla de cifra entera.
@Composable
fun ContadorConBotones(
    valor: Int,
    onPasos: (Int) -> Unit,
    digitos: Int,
    modifier: Modifier = Modifier,
    estilo: TextStyle = MaterialTheme.typography.headlineMedium,
    sufijo: String? = null,
    // cuántos pasos a la vez al mantener un botón más de 1.5 s; ver `repetirAlMantener`
    aceleracion: Int = 1,
) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colores.surfaceVariant)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TeclaContador("−", Modifier.repetirAlMantener(-1, aceleracion, onPasos))
        // el número se centra entre las dos teclas, y si la placa es más ancha que él, ocupa lo que sobra
        Row(Modifier.weight(1f).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            ContadorRodante(valor = valor, onPasos = onPasos, digitos = digitos, estilo = estilo)
            if (sufijo != null) Text(sufijo, modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
        }
        TeclaContador("+", Modifier.repetirAlMantener(1, aceleracion, onPasos))
    }
}

@Composable
private fun TeclaContador(signo: String, modifier: Modifier) {
    val colores = MaterialTheme.colorScheme
    Box(
        modifier = modifier.size(width = 44.dp, height = 44.dp).clip(RoundedCornerShape(12.dp)).background(colores.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(signo, style = MaterialTheme.typography.titleLarge, color = colores.onBackground)
    }
}
