package com.example.nutrigajo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val ANCHO_MARCA = 12.dp

// Regla horizontal con ajuste a cada marca: se desliza y el valor es el de la marca central (naranja).
// Las marcas altas llevan su número. `valor` solo fija dónde arranca; después manda el dedo.
@Composable
fun Regla(
    valor: Double,
    onValor: (Double) -> Unit,
    min: Double,
    max: Double,
    paso: Double,
    modifier: Modifier = Modifier,
    // una marca alta cada tantas marcas
    cadaAlta: Int = 2,
    // una marca alta con número cada tantas marcas altas
    etiquetaCada: Int = 5,
) {
    val colores = MaterialTheme.colorScheme
    val marcas = ((max - min) / paso).roundToInt() + 1
    val inicial = ((valor - min) / paso).roundToInt().coerceIn(0, marcas - 1)
    val estado = rememberLazyListState(initialFirstVisibleItemIndex = inicial)
    val anchoPx = with(LocalDensity.current) { ANCHO_MARCA.toPx() }

    // la marca central es la primera visible, más una si ya pasó de la mitad
    LaunchedEffect(estado, marcas) {
        snapshotFlow { estado.firstVisibleItemIndex + if (estado.firstVisibleItemScrollOffset > anchoPx / 2) 1 else 0 }
            .collect { onValor(min + it.coerceIn(0, marcas - 1) * paso) }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(96.dp)) {
        val relleno = maxWidth / 2 - ANCHO_MARCA / 2
        LazyRow(
            state = estado,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = relleno),
            flingBehavior = rememberSnapFlingBehavior(estado),
            verticalAlignment = Alignment.Top,
        ) {
            items(marcas) { i ->
                val alta = i % cadaAlta == 0
                Column(modifier = Modifier.width(ANCHO_MARCA), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .width(2.dp)
                            .height(if (alta) 40.dp else 24.dp)
                            .background(if (alta) colores.outline else colores.outlineVariant),
                    )
                    if (alta && (i / cadaAlta) % etiquetaCada == 0) {
                        // la aguja llega a 48dp: el número va debajo
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = ((min + i * paso).let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }),
                            modifier = Modifier.wrapContentWidth(unbounded = true),
                            style = MaterialTheme.typography.bodySmall,
                            color = colores.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        // aguja central
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .width(6.dp)
                .height(48.dp)
                .background(colores.primary, RoundedCornerShape(3.dp)),
        )
    }
}
