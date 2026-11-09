package com.example.nutrigajo.ui.paciente.inicio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.Esqueleto
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

// cabe una etiqueta de 4 dígitos («9000») con su hueco; las de 5 se abrevian (etiquetaEje)
private val MARGEN_EJE = 34.dp
private val ARRIBA = 6.dp
private val ALTO_PLOT = 119.dp

// «Actividades» del inicio: las calorías gastadas por rango (las del reloj y las de la app, ver CaloriasSerie.kt).
@Composable
fun ActividadReciente(onVerTodas: () -> Unit, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    val estado = LocalCalorias.current
    var rango by rememberSaveable { mutableStateOf(Rango.DIA) }
    val serie = remember(rango, estado.datos) { serieKcal(rango, estado.datos, Instant.now(), ZoneId.systemDefault()) }
    // al cambiar de rango, o al llegar los datos, la burbuja va al punto más reciente
    var seleccion by remember(rango, serie.valores.size) { mutableIntStateOf((serie.valores.size - 1).coerceAtLeast(0)) }

    Column(modifier) {
        EncabezadoSeccion(stringResource(R.string.inicio_actividades), verTodas = true, onVerTodas = onVerTodas)
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .padding(horizontal = 17.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(colores.surfaceVariant)
                .padding(15.dp),
        ) {
            SelectorRango(rango, onRango = { rango = it })
            Spacer(Modifier.height(14.dp))
            GraficaDelRango(serie, estado, seleccion, onSeleccion = { seleccion = it })
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (estado.cargando) {
                        Esqueleto(Modifier.size(width = 120.dp, height = 30.dp), RoundedCornerShape(8.dp))
                    } else {
                        Text(
                            text = stringResource(R.string.actividad_total, String.format(Locale.getDefault(), "%,d", serie.total)),
                            style = tipos.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = colores.onBackground,
                        )
                    }
                    // contra el periodo anterior; el año y «Todo» no tienen con qué compararse
                    if (!estado.cargando && serie.variacion != null) {
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_corredor), contentDescription = null, modifier = Modifier.size(16.dp), tint = colores.outline)
                            Text(
                                text = String.format(Locale.getDefault(), "%+d", serie.variacion),
                                style = tipos.bodyMedium,
                                color = colores.outline,
                            )
                        }
                    }
                }
                Image(painterResource(R.drawable.ic_naranja), contentDescription = null, modifier = Modifier.size(56.dp))
            }
        }
    }
}

// La gráfica de un rango con sus tres estados, del mismo alto para que nada salte: esqueleto mientras se lee, un aviso si el rango no
// tiene calorías (con la causa si la lectura de la app falló) y la línea cuando las hay.
@Composable
internal fun GraficaDelRango(serie: SerieKcal, estado: EstadoKcal, seleccion: Int, onSeleccion: (Int) -> Unit, modifier: Modifier = Modifier) {
    val alto = ARRIBA * 2 + ALTO_PLOT
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when {
            estado.cargando -> Esqueleto(Modifier.fillMaxWidth().height(alto), RoundedCornerShape(16.dp))
            serie.total <= 0 -> Box(Modifier.fillMaxWidth().height(alto), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.calorias_vacio),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            else -> GraficaKcal(serie.valores, serie.rotulos, seleccion, onSeleccion)
        }
        // la lectura de la app falló: lo que se ve puede estar incompleto
        if (!estado.cargando && estado.error != null) {
            Text(stringResource(estado.error), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

// El grupo de pestañas de rango (1d … Todo): la activa en negro sobre una pastilla blanca. Lo comparten Actividades y las
// pantallas de métricas.
@Composable
internal fun SelectorRango(rango: Rango, onRango: (Rango) -> Unit, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(39.dp)
            .background(colores.background, RoundedCornerShape(15.dp))
            .padding(4.dp),
    ) {
        Rango.entries.forEach { opcion ->
            val activa = opcion == rango
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (activa) colores.onBackground else Color.Transparent)
                    .selectable(selected = activa, role = Role.Tab, onClick = { onRango(opcion) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(opcion.etiqueta),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (activa) colores.background else colores.onSurfaceVariant,
                )
            }
        }
    }
}

// Línea suave por los valores, con la cuadrícula punteada (de 0 al máximo, según los datos) y una burbuja con el rótulo y el valor del
// punto elegido (toque o arrastre).
@Composable
internal fun GraficaKcal(valores: List<Int>, rotulos: List<String>, seleccion: Int, onSeleccion: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val medidor = rememberTextMeasurer()
    val estiloEje = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, color = colores.outline)
    val estiloBurbuja = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, color = colores.onPrimary)
    val eje = remember(valores) { ejeKcal(valores.maxOrNull() ?: 0) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ARRIBA * 2 + ALTO_PLOT)
            .pointerInput(valores) {
                val izquierda = MARGEN_EJE.toPx()
                detectTapGestures { onSeleccion(indiceMasCercano(it.x - izquierda, size.width - izquierda, valores.size)) }
            }
            .pointerInput(valores) {
                val izquierda = MARGEN_EJE.toPx()
                detectHorizontalDragGestures { cambio, _ ->
                    onSeleccion(indiceMasCercano(cambio.position.x - izquierda, size.width - izquierda, valores.size))
                }
            },
    ) {
        val izquierda = MARGEN_EJE.toPx()
        val arriba = ARRIBA.toPx()
        val alto = ALTO_PLOT.toPx()
        val ancho = size.width - izquierda
        val tope = eje.first().toFloat()
        val puntos = valores.mapIndexed { i, v ->
            val x = izquierda + if (valores.size > 1) ancho * i / (valores.size - 1) else 0f
            Offset(x, arriba + alto * (1f - v.coerceIn(0, eje.first()) / tope))
        }

        eje.forEachIndexed { i, valorEje ->
            val y = arriba + alto * i / (eje.size - 1)
            val texto = medidor.measure(etiquetaEje(valorEje), estiloEje)
            drawText(texto, topLeft = Offset(0f, y - texto.size.height / 2f))
            drawLine(
                color = colores.outline,
                start = Offset(izquierda, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
        }

        // Catmull-Rom convertida a curvas de Bézier: pasa por todos los puntos sin picos
        val linea = Path().apply {
            moveTo(puntos.first().x, puntos.first().y)
            for (i in 0 until puntos.size - 1) {
                val a = puntos[maxOf(i - 1, 0)]
                val b = puntos[i]
                val c = puntos[i + 1]
                val d = puntos[minOf(i + 2, puntos.size - 1)]
                // los puntos de control no salen de la cuadrícula: sin esto la curva se hunde bajo el 0 junto a un tramo plano
                cubicTo(
                    b.x + (c.x - a.x) / 6f, (b.y + (c.y - a.y) / 6f).coerceIn(arriba, arriba + alto),
                    c.x - (d.x - b.x) / 6f, (c.y - (d.y - b.y) / 6f).coerceIn(arriba, arriba + alto),
                    c.x, c.y,
                )
            }
        }
        val relleno = Path().apply {
            addPath(linea)
            lineTo(puntos.last().x, arriba + alto)
            lineTo(puntos.first().x, arriba + alto)
            close()
        }
        drawPath(relleno, Brush.verticalGradient(listOf(colores.primary.copy(alpha = 0.4f), Color.Transparent), startY = arriba, endY = arriba + alto))
        drawPath(linea, colores.primary, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

        val elegida = seleccion.coerceIn(0, puntos.lastIndex)
        val elegido = puntos[elegida]
        drawCircle(colores.primary, radius = 6.dp.toPx(), center = elegido)
        val burbuja = medidor.measure("${rotulos.getOrElse(elegida) { "" }} · ${valores[elegida]}", estiloBurbuja)
        val ladoX = 7.dp.toPx()
        val ladoY = 3.dp.toPx()
        val anchoB = burbuja.size.width + ladoX * 2
        val altoB = burbuja.size.height + ladoY * 2
        val izqB = (elegido.x - anchoB / 2).coerceIn(izquierda, maxOf(izquierda, size.width - anchoB))
        val arribaB = (elegido.y - 6.dp.toPx() - 5.dp.toPx() - altoB).coerceAtLeast(0f)
        drawRoundRect(colores.primary, Offset(izqB, arribaB), Size(anchoB, altoB), CornerRadius(7.dp.toPx()))
        drawText(burbuja, topLeft = Offset(izqB + ladoX, arribaB + ladoY))
    }
}

// Índice del punto más cercano a `x` (en px, medido desde el inicio del área de la línea).
internal fun indiceMasCercano(x: Float, ancho: Float, cantidad: Int): Int {
    if (cantidad <= 1 || ancho <= 0f) return 0
    val paso = ancho / (cantidad - 1)
    return (0 until cantidad).minBy { abs(it * paso - x) }
}
