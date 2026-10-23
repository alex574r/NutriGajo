package com.example.nutrigajo.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

private val RADIO_CASILLA = 14.dp
private val ALTO_CASILLA = 60.dp
private val ANCHO_GUION = 12.dp
private val SEPARACION_CAPSULA = 14.dp
private val DESENFOQUE_GOO = 6.dp
private const val MS_SACUDIDA = 620f

// Las casillas se dibujan en dos capas con la misma geometría: el fondo (que se funde en el acierto) y el texto (que queda nítido encima).
private enum class Capa { FONDO, TEXTO }

// Casillas de código del diseño (OTP): una por carácter, con un guion entre grupos. La que toca escribir va en naranja
// con el anillo de foco. Quien lo usa le deja ANILLO_FOCO de margen menos para que el anillo no se recorte en los bordes.
// Efectos: el dígito sube con un resorte, la casilla se infla, una ola recorre el campo mientras se verifica, se sacude en el
// error y en el acierto las casillas se funden en una cápsula con el texto de `exito`. Compose ya omite las animaciones si el
// usuario las desactiva en el sistema.
// ponytail: un solo BasicTextField sin pintar recibe el teclado; las casillas solo dibujan su texto.
@Composable
fun CampoCodigo(
    valor: String,
    onValorChange: (String) -> Unit,
    largo: Int,
    descripcion: String,
    modifier: Modifier = Modifier,
    grupo: Int = 4,
    onListo: () -> Unit = {},
    error: Boolean = false,
    verificando: Boolean = false,
    exito: String? = null,
) {
    val colores = MaterialTheme.colorScheme
    val foco = remember { FocusRequester() }
    val teclado = LocalSoftwareKeyboardController.current
    var enfocado by remember { mutableStateOf(false) }
    var ancho by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { foco.requestFocus() }

    val sacudida = remember { Animatable(0f) }
    LaunchedEffect(error) {
        if (error) {
            sacudida.animateTo(1f, tween(MS_SACUDIDA.toInt(), easing = LinearEasing))
            sacudida.snapTo(0f)
        } else {
            sacudida.snapTo(0f)
        }
    }
    val fase = remember { Animatable(0f) }
    LaunchedEffect(verificando) {
        if (verificando) fase.animateTo(2f * PI.toFloat(), infiniteRepeatable(tween(900, easing = LinearEasing)))
        else fase.snapTo(0f)
    }
    val fusion by animateFloatAsState(
        targetValue = if (exito != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 120f),
        label = "fusion",
    )
    val activo = if (enfocado && exito == null) minOf(valor.length, largo - 1) else -1

    BasicTextField(
        value = valor,
        onValueChange = { onValorChange(it.filter(Char::isLetterOrDigit).uppercase().take(largo)) },
        modifier = modifier
            .focusRequester(foco)
            .onFocusChanged { enfocado = it.isFocused }
            .semantics { contentDescription = descripcion },
        readOnly = exito != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onListo() }),
        cursorBrush = SolidColor(Color.Transparent),
        decorationBox = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { ancho = it.width.toFloat() }
                    // seno amortiguado: sale de un lado al otro y se va apagando
                    .graphicsLayer {
                        val ms = sacudida.value * MS_SACUDIDA
                        translationX = 6.dp.toPx() * exp(-ms / 190f) * sin(ms * 2f * PI.toFloat() / 90f)
                    }
                    // tocar las casillas devuelve el foco y el teclado si se cerraron
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        foco.requestFocus()
                        teclado?.show()
                    },
                contentAlignment = Alignment.Center,
            ) {
                for (capa in Capa.entries) {
                    Fila(
                        capa = capa,
                        valor = valor,
                        largo = largo,
                        grupo = grupo,
                        activo = activo,
                        error = error,
                        fusion = fusion,
                        ancho = ancho,
                        ola = { i -> if (verificando) 1f + 0.05f * sin(fase.value - i * 0.7f) else 1f },
                    )
                }
                if (exito != null) {
                    Text(
                        text = exito,
                        modifier = Modifier.graphicsLayer { alpha = ((fusion - 0.55f) / 0.35f).coerceIn(0f, 1f) },
                        style = MaterialTheme.typography.titleMedium,
                        color = colores.background,
                    )
                }
            }
        },
    )
}

@Composable
private fun Fila(
    capa: Capa,
    valor: String,
    largo: Int,
    grupo: Int,
    activo: Int,
    error: Boolean,
    fusion: Float,
    ancho: Float,
    ola: (Int) -> Float,
) {
    val densidad = LocalDensity.current
    val guion = with(densidad) { ANCHO_GUION.toPx() }
    val separacion = with(densidad) { SEPARACION_CAPSULA.toPx() }
    val desenfoque = with(densidad) { DESENFOQUE_GOO.toPx() }
    val anchoCelda = (ancho - guion * ((largo - 1) / grupo)) / largo
    Row(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            // el desenfoque crece con la fusión para que los bordes finos no desaparezcan de golpe
            if (capa == Capa.FONDO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && fusion > 0.02f) {
                renderEffect = efectoGoo(desenfoque * min(1f, fusion * 3f))
            }
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(largo) { i ->
            if (i > 0 && i % grupo == 0) {
                val color = if (capa == Capa.FONDO) MaterialTheme.colorScheme.outline else Color.Transparent
                Box(Modifier.width(ANCHO_GUION).height(2.dp).background(color.copy(alpha = color.alpha * (1f - fusion.coerceIn(0f, 1f)))))
            }
            // cada casilla viaja de su sitio al que le toca en la cápsula, centrada y más estrecha
            val centro = anchoCelda * (i + 0.5f) + guion * (i / grupo)
            val destino = ancho / 2f + (i - (largo - 1) / 2f) * separacion
            Celda(
                capa = capa,
                caracter = valor.getOrNull(i),
                activa = i == activo,
                error = error,
                fusion = fusion,
                desplazamiento = (destino - centro) * fusion,
                ola = { ola(i) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Celda(
    capa: Capa,
    caracter: Char?,
    activa: Boolean,
    error: Boolean,
    fusion: Float,
    desplazamiento: Float,
    ola: () -> Float,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    // al llegar un carácter el dígito sube con un resorte y la casilla se infla un instante
    val entrada = remember(caracter) { Animatable(if (caracter == null) 1f else 0f) }
    LaunchedEffect(entrada) { entrada.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f)) }
    val f = fusion.coerceIn(0f, 1f)
    val posicion = modifier.graphicsLayer {
        translationX = desplazamiento
        val escala = (1f + 0.04f * sin(PI.toFloat() * entrada.value.coerceIn(0f, 1f))) * ola()
        scaleX = escala
        scaleY = escala
    }

    if (capa == Capa.FONDO) {
        val radio = lerp(RADIO_CASILLA, ALTO_CASILLA / 2, f)
        val forma = RoundedCornerShape(radio)
        val relleno by animateColorAsState(if (activa) colores.primary else colores.surfaceVariant, label = "relleno")
        val borde = if (error) colores.error else if (activa) colores.primary else colores.outline
        AnilloFoco(activo = activa, radio = radio, modifier = posicion) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ALTO_CASILLA)
                    .background(lerpColor(relleno, colores.onBackground, f), forma)
                    .border(1.dp, lerpColor(borde, colores.onBackground, f), forma),
            )
        }
    } else {
        Box(
            modifier = posicion.padding(ANILLO_FOCO).fillMaxWidth().height(ALTO_CASILLA),
            contentAlignment = Alignment.Center,
        ) {
            when {
                caracter != null -> Text(
                    text = caracter.toString(),
                    modifier = Modifier.graphicsLayer {
                        val p = entrada.value
                        translationY = (1f - p) * 16.dp.toPx()
                        scaleX = 0.6f + 0.4f * p
                        scaleY = scaleX
                        // los dígitos se apagan enseguida: el texto no debe pasar por el desenfoque
                        alpha = p.coerceIn(0f, 1f) * (1f - min(1f, f * 3f))
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    // la casilla naranja conserva el dígito blanco: en rojo sobre naranja no se lee
                    color = if (activa) colores.onPrimary else if (error) colores.error else colores.onSurfaceVariant,
                )
                activa -> {
                    val parpadeo by rememberInfiniteTransition(label = "cursor").animateFloat(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(durationMillis = 600), RepeatMode.Reverse),
                        label = "parpadeo",
                    )
                    val aparece = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { aparece.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 270f)) }
                    Box(
                        Modifier
                            .width(2.dp)
                            .height(26.dp)
                            .graphicsLayer {
                                scaleY = aparece.value
                                alpha = parpadeo
                            }
                            .background(colores.onPrimary),
                    )
                }
            }
        }
    }
}

// Efecto "goo": desenfoque seguido de un umbral de alfa (a' = 20a - 9), que funde las formas que se tocan. Android 12 en adelante;
// antes las casillas igual se juntan en la cápsula, solo que sin el estiramiento líquido.
@RequiresApi(Build.VERSION_CODES.S)
private fun efectoGoo(radio: Float): RenderEffect {
    val umbral = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 20f, -9f * 255f,
        ),
    )
    return android.graphics.RenderEffect.createChainEffect(
        android.graphics.RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(umbral)),
        android.graphics.RenderEffect.createBlurEffect(radio, radio, Shader.TileMode.DECAL),
    ).asComposeRenderEffect()
}
