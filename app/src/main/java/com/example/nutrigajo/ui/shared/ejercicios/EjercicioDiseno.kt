package com.example.nutrigajo.ui.shared.ejercicios

import androidx.compose.ui.unit.em
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.cristal

// Las piezas del aspecto «Workout» de los ejercicios: la cabecera oscura con foto, el botón de volver de cristal, las tres columnas
// de datos y los pasos en tarjetas que se pasan con el dedo. Las usan el detalle, la guía de pasos, Explorar y «Entrenar ahora».

// La cabecera con foto a todo lo ancho, un velo oscuro que la deja leer y el contenido centrado abajo. Deja libres 28 dp al pie para
// que la hoja blanca de abajo se monte encima con las esquinas redondeadas.
@Composable
fun CabeceraOscura(
    modifier: Modifier = Modifier,
    alto: Dp = 300.dp,
    foto: @Composable BoxScope.() -> Unit,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth().height(alto)) {
        foto()
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.55f), 0.4f to Color.Black.copy(alpha = 0.2f), 1f to Color.Black.copy(alpha = 0.88f)),
            ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = contenido,
        )
    }
}

// Monta el elemento sobre lo que tiene arriba: se dibuja `alto` más arriba y solo ocupa lo que queda debajo, así la hoja blanca de
// esquinas redondeadas cubre el pie de la cabecera sin dejar un hueco en la lista.
fun Modifier.montadaSobreLaCabecera(alto: Dp = 28.dp): Modifier = layout { medible, restricciones ->
    val colocable = medible.measure(restricciones)
    val subida = alto.roundToPx()
    layout(colocable.width, (colocable.height - subida).coerceAtLeast(0)) { colocable.place(0, -subida) }
}

// El botón de volver de las pantallas con cabecera de foto: un cuadrado de cristal con la flecha.
@Composable
fun BotonVolverCristal(onVolver: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier.size(44.dp).cristal(forma).clip(forma).clickable(role = Role.Button, onClick = onVolver),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.ejercicio_volver), tint = Color.White)
    }
}

// Tres (o las que se pasen) columnas de «valor y nombre» separadas por una línea fina: tiempo, calorías y nivel.
@Composable
fun DatosEnColumnas(datos: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        datos.forEachIndexed { i, (valor, nombre) ->
            if (i > 0) Box(Modifier.width(1.dp).height(36.dp).background(colores.outlineVariant))
            Column(Modifier.weight(1f).padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(valor, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colores.onBackground, maxLines = 1)
                Text(nombre, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

// El estado de un pager de pasos: un paso por página y, si hay algo que decir al final, una página más.
@Composable
fun rememberPasosEstado(pasos: Int, conFinal: Boolean = false, inicio: Int = 0): PagerState {
    val paginas = pasos + if (conFinal) 1 else 0
    return rememberPagerState(initialPage = inicio.coerceIn(0, (paginas - 1).coerceAtLeast(0)), pageCount = { paginas.coerceAtLeast(1) })
}

// Una barra con un tramo por página que se llena hasta la que se ve, como las historias.
@Composable
fun IndicadorPasos(estado: PagerState, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(estado.pageCount) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i <= estado.currentPage) colores.primary else colores.surfaceVariant),
            )
        }
    }
}

// Los pasos de un ejercicio de uno en uno: cada uno en su tarjeta con su número y «Paso 2 de 4», y se pasan con el dedo. Con `final` la
// última página es aparte (lo que conviene saber antes de terminar). `grande` pone el texto grande, para la guía a pantalla completa.
@Composable
fun PasosPager(
    pasos: List<String>,
    estado: PagerState,
    modifier: Modifier = Modifier,
    grande: Boolean = false,
    final: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colores = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndicadorPasos(estado)
        HorizontalPager(state = estado, pageSpacing = 12.dp, verticalAlignment = Alignment.Top) { i ->
            Column(
                modifier = Modifier.fillMaxWidth().background(colores.surfaceVariant, RoundedCornerShape(26.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (i < pasos.size) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(36.dp).background(colores.primary, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.titleSmall, color = colores.onPrimary)
                        }
                        Text(stringResource(R.string.ejercicio_paso_de, i + 1, pasos.size), style = MaterialTheme.typography.titleSmall, color = colores.onSurfaceVariant)
                    }
                    Text(
                        text = pasos[i],
                        style = (if (grande) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge).copy(lineHeight = 1.35.em),
                        color = colores.onBackground,
                    )
                } else {
                    final?.invoke(this)
                }
            }
        }
    }
}
