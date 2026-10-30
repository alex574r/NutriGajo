package com.example.nutrigajo.ui.paciente.inicio

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.GrupoAlimento
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.theme.color
import com.example.nutrigajo.ui.theme.grisTarjeta
import com.example.nutrigajo.ui.theme.negro
import java.util.Locale

private val ALTURAS_SCORE = listOf(15, 26, 51, 74, 65, 28, 43)

// Barras de la tarjeta de calorías: color y opacidad como en el diseño
private val FILAS_CALORIAS = listOf(
    Color(0xFFF6F6F6).copy(alpha = 0.79f),
    Color(0xFF9C9C9C).copy(alpha = 0.79f),
    Color(0xFF515151).copy(alpha = 0.79f),
    Color(0xFFD9D9D9).copy(alpha = 0.24f),
    Color(0xFFD9D9D9).copy(alpha = 0.11f),
)

// las tarjetas, en el orden del carrusel y de «Ver todas» (las estadísticas cuelgan de las calorías y no tienen tarjeta)
private val TARJETAS = listOf(Metrica.SCORE, Metrica.HIDRATACION, Metrica.CALORIAS, Metrica.IMC, Metrica.PASOS)

private val ANCHO_TARJETA = 135.dp
private val ALTO_TARJETA = 176.dp

// La entrada de cada tarjeta es la de los anillos de pasos: `crecimiento` va de 0 a 1 en 900 ms al aparecer, y cada pieza (barra,
// celda) arranca un poco después de la anterior; `escalon` es su lugar, 0 la primera. Devuelve el avance de esa pieza (0..1).
private fun escalonado(crecimiento: Float, escalon: Int): Float = (crecimiento * 1.6f - escalon * 0.1f).coerceIn(0f, 1f)

@Composable
fun MetricasFitness(resumen: ResumenDia, onAbrir: (Metrica) -> Unit, onVerTodas: () -> Unit, modifier: Modifier = Modifier) {
    val lista = rememberLazyListState()
    // lo que avanza el carrusel de una tarjeta a la siguiente: su ancho y el hueco
    val pasoCarrusel = with(LocalDensity.current) { (ANCHO_TARJETA + 8.dp).toPx() }
    Column(modifier) {
        EncabezadoSeccion(stringResource(R.string.inicio_metricas), verTodas = true, onVerTodas = onVerTodas)
        Spacer(Modifier.height(12.dp))
        LazyRow(state = lista, contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TARJETAS) { metrica ->
                TarjetaDeMetrica(
                    metrica = metrica,
                    resumen = resumen,
                    onClick = { onAbrir(metrica) },
                    // al deslizar el carrusel los anillos de pasos giran con él, los de dentro más: 0° con el carrusel en su sitio
                    giro = { (lista.firstVisibleItemIndex * pasoCarrusel + lista.firstVisibleItemScrollOffset) * 0.2f },
                )
            }
        }
    }
}

// «Ver todas»: las mismas tarjetas del carrusel en dos columnas, cada una estirada al ancho de su celda; tocarla abre su pantalla.
@Composable
fun TodasLasMetricasScreen(resumen: ResumenDia, onAbrir: (Metrica) -> Unit, onVolver: () -> Unit) {
    PantallaMetrica(titulo = stringResource(R.string.inicio_metricas), onVolver = onVolver) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TARJETAS.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    fila.forEach { metrica ->
                        Box(Modifier.weight(1f)) {
                            CeldaEscalada { TarjetaDeMetrica(metrica, resumen, onClick = { onAbrir(metrica) }) }
                        }
                    }
                    // una fila con una sola tarjeta deja su mitad vacía, no la estira
                    if (fila.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// La tarjeta de 135x176 estirada al ancho de la celda sin tocar el diseño: se dibuja a su tamaño y se escala desde la esquina.
// La capa que escala llena la celda (y no la tarjeta) para que el toque caiga también en la parte que crece.
@Composable
private fun CeldaEscalada(tarjeta: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val escala = maxWidth / ANCHO_TARJETA
        Box(Modifier.fillMaxWidth().height(ALTO_TARJETA * escala)) {
            Box(Modifier.fillMaxSize().graphicsLayer { scaleX = escala; scaleY = escala; transformOrigin = TransformOrigin(0f, 0f) }) {
                Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)) { tarjeta() }
            }
        }
    }
}

@Composable
private fun TarjetaDeMetrica(metrica: Metrica, resumen: ResumenDia, onClick: () -> Unit, giro: () -> Float = { 0f }) {
    val colores = MaterialTheme.colorScheme
    when (metrica) {
        Metrica.SCORE -> TarjetaMetrica(
            fondo = colores.primary,
            titulo = stringResource(R.string.metrica_score),
            icono = R.drawable.ic_corredor,
            valor = resumen.score.toString(),
            unidad = "%",
            onClick = onClick,
        ) {
            // las barras crecen desde abajo, una tras otra
            val crecimiento = rememberCrecimiento(900)
            Row(
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 60.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                val maxima = ALTURAS_SCORE.max()
                ALTURAS_SCORE.forEachIndexed { i, altura ->
                    // las más altas son más claras
                    Box(
                        Modifier
                            .size(7.dp, altura.dp * escalonado(crecimiento, i))
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.4f + 0.6f * altura / maxima)),
                    )
                }
            }
        }
        Metrica.HIDRATACION -> TarjetaMetrica(
            fondo = colores.secondary,
            titulo = stringResource(R.string.metrica_hidratacion),
            icono = R.drawable.ic_botella,
            valor = resumen.hidratacionMl.toString(),
            unidad = "ml",
            onClick = onClick,
        ) {
            // la ola sube desde el fondo de la tarjeta; el chapoteo con el teléfono es solo de la pantalla de hidratación
            val crecimiento = rememberCrecimiento(900)
            Image(
                painter = painterResource(R.drawable.ola_hidratacion),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = 4.5.dp, y = 48.5.dp)
                    .size(126.dp, 79.dp)
                    .graphicsLayer {
                        translationY = (1f - crecimiento) * 70.dp.toPx()
                        alpha = crecimiento
                    },
            )
        }
        Metrica.CALORIAS -> TarjetaMetrica(
            fondo = colores.grisTarjeta,
            titulo = stringResource(R.string.metrica_calorias),
            icono = null,
            valor = String.format(Locale.getDefault(), "%,d", resumen.calorias),
            unidad = "kcal",
            onClick = onClick,
        ) {
            // las barras se llenan hacia la derecha, una tras otra
            val crecimiento = rememberCrecimiento(900)
            Column(Modifier.offset(x = 12.dp, y = 39.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                FILAS_CALORIAS.forEachIndexed { i, color ->
                    Box(Modifier.size(59.dp * escalonado(crecimiento, i), 16.dp).clip(RoundedCornerShape(5.dp)).background(color))
                }
            }
        }
        Metrica.IMC -> TarjetaMetrica(
            fondo = colores.negro,
            titulo = stringResource(R.string.metrica_imc),
            icono = null,
            valor = String.format(Locale.getDefault(), "%.1f", IMC_MUESTRA),
            unidad = "pt",
            onClick = onClick,
        ) {
            // un trozo del mapa de calor de la pantalla del IMC; las celdas se inflan en diagonal, de arriba a la izquierda
            val crecimiento = rememberCrecimiento(900)
            Column(Modifier.offset(x = 18.dp, y = 46.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                mapaImc.takeLast(16).chunked(4).forEachIndexed { y, fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        fila.forEachIndexed { x, nivel ->
                            val avance = escalonado(crecimiento, x + y)
                            Box(
                                Modifier
                                    .size(17.dp)
                                    .graphicsLayer {
                                        scaleX = avance
                                        scaleY = avance
                                    }
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(colorCeldaImc(if (nivel == 5) 3 else nivel, colores, Color.White.copy(alpha = 0.14f))),
                            )
                        }
                    }
                }
            }
        }
        Metrica.PASOS -> TarjetaMetrica(
            // el verde de las verduras del plan, el mismo de las estadísticas
            fondo = Color(GrupoAlimento.VERDURAS.color),
            titulo = stringResource(R.string.metrica_pasos),
            icono = null,
            valor = String.format(Locale.getDefault(), "%,d", pasosMuestra.total),
            unidad = "",
            onClick = onClick,
        ) {
            AnillosPasos(
                // de adorno: lo que lleva de cada meta se ve en la pantalla de pasos
                avances = listOf(0.78f, 0.58f, 0.42f),
                colores = listOf(Color.White, Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.5f)),
                pista = Color.White.copy(alpha = 0.16f),
                grosor = 8.dp,
                separacion = 4.dp,
                modifier = Modifier.offset(x = 18.dp, y = 42.dp).size(78.dp),
                giro = giro,
            )
        }
        Metrica.ESTADISTICAS -> Unit
    }
}

// Tarjeta de 135x176: título y símbolo arriba, ilustración al centro y el valor grande abajo; al tocarla abre su pantalla.
@Composable
private fun TarjetaMetrica(
    fondo: Color,
    titulo: String,
    @DrawableRes icono: Int?,
    valor: String,
    unidad: String,
    onClick: () -> Unit,
    ilustracion: @Composable BoxScope.() -> Unit,
) {
    val tipos = MaterialTheme.typography
    Box(Modifier.size(ANCHO_TARJETA, ALTO_TARJETA).alPulsar(onClick, minimo = 0.96f).clip(RoundedCornerShape(34.dp)).background(fondo)) {
        ilustracion()
        Text(
            text = titulo,
            modifier = Modifier.padding(start = 18.dp, top = 18.dp),
            style = tipos.labelLarge.copy(fontSize = 12.sp),
            color = Color.White,
        )
        if (icono != null) {
            Icon(
                painter = painterResource(icono),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 18.dp).size(14.dp),
                tint = Color.White,
            )
        }
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(valor, style = tipos.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
            Text(unidad, modifier = Modifier.padding(bottom = 3.dp), style = tipos.labelLarge, color = Color.White, maxLines = 1)
        }
    }
}
