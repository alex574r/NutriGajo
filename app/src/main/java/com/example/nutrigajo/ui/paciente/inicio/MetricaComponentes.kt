package com.example.nutrigajo.ui.paciente.inicio

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.Agua
import com.example.nutrigajo.ui.components.alPulsar
import kotlin.math.PI
import kotlin.math.sin

// Marco de las pantallas de métricas: el retroceso del diseño (chevron gris en un círculo), el título al centro y debajo el
// contenido, que se desplaza si no cabe.
@Composable
internal fun PantallaMetrica(
    titulo: String?,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    // el engrane del diseño, a la derecha: abre las metas de esa métrica (solo donde las hay)
    onAjustes: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colores.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colores.surfaceVariant)
                    .alPulsar(onVolver),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_izquierda),
                    contentDescription = stringResource(R.string.mapa_volver),
                    tint = colores.onSurfaceVariant,
                )
            }
            if (titulo != null) Text(titulo, style = MaterialTheme.typography.titleMedium, color = colores.onBackground, maxLines = 1)
            if (onAjustes != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colores.surfaceVariant)
                        .alPulsar(onAjustes),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ajustes),
                        contentDescription = stringResource(R.string.metas_ajustes),
                        modifier = Modifier.size(20.dp),
                        tint = colores.onSurfaceVariant,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = contenido,
        )
    }
}

// La cifra de la métrica: su icono, el valor grande, la unidad y una frase debajo.
@Composable
internal fun CifraMetrica(
    @DrawableRes icono: Int,
    colorIcono: Color,
    valor: String,
    unidad: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    Column(modifier.padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(icono), contentDescription = null, modifier = Modifier.padding(bottom = 12.dp).size(28.dp), tint = colorIcono)
            Text(
                text = valor,
                style = tipos.headlineLarge.copy(fontSize = 48.sp, lineHeight = 54.sp, fontWeight = FontWeight.Bold),
                color = colores.onBackground,
                maxLines = 1,
            )
            Text(unidad, modifier = Modifier.padding(bottom = 10.dp), style = tipos.titleMedium, color = colores.onSurfaceVariant, maxLines = 1)
        }
        Text(subtitulo, style = tipos.bodyMedium, color = colores.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// La tarjeta gris que envuelve las gráficas, como la de Actividades.
@Composable
internal fun PanelMetrica(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(15.dp),
        content = contenido,
    )
}

// De 0 a 1 una sola vez al entrar la pantalla: las barras y los anillos crecen en vez de aparecer ya puestos.
@Composable
internal fun rememberCrecimiento(milisegundos: Int = 800): Float {
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progreso.animateTo(1f, tween(milisegundos, easing = FastOutSlowInEasing)) }
    return progreso.value
}

// Una barra vertical del diseño: la pista de color suave con su icono arriba y, desde abajo, el relleno hasta `fraccion` (0..1)
// con el texto que se le ponga dentro.
@Composable
internal fun BarraVertical(
    fraccion: Float,
    pista: Color,
    relleno: Color,
    @DrawableRes icono: Int,
    colorIcono: Color,
    modifier: Modifier = Modifier,
    textoRelleno: @Composable ColumnScope.() -> Unit,
) {
    val crecimiento = rememberCrecimiento()
    Box(modifier.clip(RoundedCornerShape(34.dp)).background(pista)) {
        Icon(
            painter = painterResource(icono),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp).size(20.dp),
            tint = colorIcono,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(fraccion * crecimiento)
                .padding(5.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(relleno),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, content = textoRelleno)
        }
    }
}

// El agua de la pantalla de hidratación: dos olas de `color` desde `nivel()` (0..1 de la altura) hacia abajo. La superficie se
// inclina con el teléfono y el oleaje crece con las sacudidas (ver `Agua`); la ola de atrás va a otro paso y más clara, para dar
// fondo. Se dibuja en todo el espacio que se le dé y lo recorta quien la contiene.
@Composable
internal fun OlasDeAgua(agua: Agua, nivel: () -> Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val altura = nivel()
        if (altura <= 0.001f) return@Canvas
        val ancho = size.width
        val alto = size.height
        val base = alto * (1f - altura)
        val amplitud = 2.dp.toPx() + 9.dp.toPx() * agua.energia
        // con la inclinación máxima la superficie cae unos 17°
        val pendiente = agua.inclinacion * 0.3f
        val paso = 6.dp.toPx()
        val vuelta = 2f * PI.toFloat()
        fun capa(desfase: Float, escala: Float, tinta: Color) {
            val camino = Path()
            var x = 0f
            while (true) {
                val xx = minOf(x, ancho)
                val onda = sin(xx / ancho * 1.5f * vuelta + agua.fase + desfase) + 0.5f * sin(xx / ancho * 2.5f * vuelta - 1.3f * agua.fase + desfase)
                val y = base - pendiente * (xx - ancho / 2f) + escala * amplitud * onda
                if (x == 0f) camino.moveTo(xx, y) else camino.lineTo(xx, y)
                if (x >= ancho) break
                x += paso
            }
            camino.lineTo(ancho, alto)
            camino.lineTo(0f, alto)
            camino.close()
            drawPath(camino, tinta)
        }
        capa(1.7f, 1.3f, color.copy(alpha = 0.4f))
        capa(0f, 1f, color)
    }
}

// Anillos concéntricos de exterior a interior; cada uno avanza `avances[i]` (0..1) desde arriba, con su pista detrás. `giro`
// (grados) los hace girar: los de dentro más que los de fuera, así que al deslizar se abren en abanico. Se lee al dibujar, no
// al componer.
@Composable
internal fun AnillosPasos(
    avances: List<Float>,
    colores: List<Color>,
    pista: Color,
    grosor: Dp,
    separacion: Dp,
    modifier: Modifier = Modifier,
    giro: () -> Float = { 0f },
) {
    val crecimiento = rememberCrecimiento(900)
    Canvas(modifier) {
        val trazo = grosor.toPx()
        val vuelta = giro()
        avances.forEachIndexed { i, avance ->
            val margen = i * (trazo + separacion.toPx()) + trazo / 2f
            val lado = size.minDimension - margen * 2f
            if (lado <= 0f) return@forEachIndexed
            val origen = Offset(margen, margen)
            val tamano = Size(lado, lado)
            drawArc(pista, 0f, 360f, useCenter = false, topLeft = origen, size = tamano, style = Stroke(trazo))
            drawArc(
                color = colores[i % colores.size],
                startAngle = -90f + vuelta * (1f + 0.35f * i),
                sweepAngle = 360f * avance.coerceIn(0f, 1f) * crecimiento,
                useCenter = false,
                topLeft = origen,
                size = tamano,
                style = Stroke(trazo, cap = StrokeCap.Round),
            )
        }
    }
}

// El color de una celda del mapa del IMC (ver `mapaImc`): sobrepeso en naranja más fuerte cuanto más alto, bajo peso en azul y normal
// en negro. `vacio` es el de «sin dato».
internal fun colorCeldaImc(nivel: Int, colores: ColorScheme, vacio: Color): Color = when (nivel) {
    1 -> colores.primary.copy(alpha = 0.25f)
    2 -> colores.primary.copy(alpha = 0.55f)
    3 -> colores.primary
    4 -> colores.secondary
    5 -> colores.onBackground
    else -> vacio
}
