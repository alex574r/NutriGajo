package com.example.nutrigajo.ui.paciente.inicio

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
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

@Composable
fun MetricasFitness(resumen: ResumenDia, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Column(modifier) {
        EncabezadoSeccion(stringResource(R.string.inicio_metricas), verTodas = true)
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                TarjetaMetrica(
                    fondo = colores.primary,
                    titulo = stringResource(R.string.metrica_score),
                    icono = R.drawable.ic_corredor,
                    valor = resumen.score.toString(),
                    unidad = "%",
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 60.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        val maxima = ALTURAS_SCORE.max()
                        ALTURAS_SCORE.forEach { altura ->
                            // las más altas son más claras
                            Box(
                                Modifier
                                    .size(7.dp, altura.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.4f + 0.6f * altura / maxima)),
                            )
                        }
                    }
                }
            }
            item {
                TarjetaMetrica(
                    fondo = colores.secondary,
                    titulo = stringResource(R.string.metrica_hidratacion),
                    icono = R.drawable.ic_botella,
                    valor = resumen.hidratacionMl.toString(),
                    unidad = "ml",
                ) {
                    Image(
                        painter = painterResource(R.drawable.ola_hidratacion),
                        contentDescription = null,
                        modifier = Modifier.offset(x = 4.5.dp, y = 48.5.dp).size(126.dp, 79.dp),
                    )
                }
            }
            item {
                TarjetaMetrica(
                    fondo = colores.onSurfaceVariant,
                    titulo = stringResource(R.string.metrica_calorias),
                    icono = null,
                    valor = String.format(Locale.getDefault(), "%,d", resumen.calorias),
                    unidad = "kcal",
                ) {
                    Column(Modifier.offset(x = 12.dp, y = 39.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        FILAS_CALORIAS.forEach { color ->
                            Box(Modifier.size(59.dp, 16.dp).clip(RoundedCornerShape(5.dp)).background(color))
                        }
                    }
                }
            }
        }
    }
}

// Tarjeta de 135x176: título y símbolo arriba, ilustración al centro y el valor grande abajo.
@Composable
private fun TarjetaMetrica(
    fondo: Color,
    titulo: String,
    @DrawableRes icono: Int?,
    valor: String,
    unidad: String,
    ilustracion: @Composable BoxScope.() -> Unit,
) {
    val tipos = MaterialTheme.typography
    Box(Modifier.size(135.dp, 176.dp).clip(RoundedCornerShape(34.dp)).background(fondo)) {
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
