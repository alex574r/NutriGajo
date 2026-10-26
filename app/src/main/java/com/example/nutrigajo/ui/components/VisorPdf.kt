package com.example.nutrigajo.ui.components

import android.graphics.Bitmap
import android.graphics.Color as ColorAndroid
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Un PDF abierto con PdfRenderer. El renderizador solo deja una página abierta a la vez: todo pasa por un cerrojo.
private class DocumentoPdf(archivo: File) {
    private val renderizador = PdfRenderer(ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY))
    private val cerrojo = Any()
    val paginas = renderizador.pageCount
    // ancho / alto de cada página, para reservar su espacio antes de dibujarla
    val proporciones: List<Float> = (0 until paginas).map { i -> renderizador.openPage(i).use { it.width.toFloat() / it.height } }

    suspend fun pintar(indice: Int, anchoPx: Int): Bitmap = withContext(Dispatchers.IO) {
        synchronized(cerrojo) {
            renderizador.openPage(indice).use { pagina ->
                Bitmap.createBitmap(anchoPx, (anchoPx * pagina.height / pagina.width.toFloat()).toInt(), Bitmap.Config.ARGB_8888).also {
                    it.eraseColor(ColorAndroid.WHITE)
                    pagina.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }
    }

    fun cerrar() = synchronized(cerrojo) { renderizador.close() }
}

// Lee un PDF dentro de la app: las páginas ocupan el ancho de la pantalla y se recorren hacia abajo; con dos dedos (o con un
// doble toque) se amplía y entonces también se recorre hacia los lados.
// ponytail: sin selección de texto ni buscador; para eso, abrirlo en otra app (aún no hay botón).
@Composable
fun VisorPdf(archivo: File, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val documento by produceState<DocumentoPdf?>(null, archivo) {
        value = withContext(Dispatchers.IO) { runCatching { DocumentoPdf(archivo) }.getOrNull() }
        awaitDispose { value?.cerrar() }
    }
    var escala by rememberSaveable { mutableFloatStateOf(1f) }

    BoxWithConstraints(
        modifier = modifier
            .background(colores.surfaceVariant)
            // doble toque: amplía al doble o vuelve al ancho de la pantalla
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { escala = if (escala > 1.05f) 1f else 2f }) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        // con un dedo la lista se desplaza sola; con dos se amplía y el gesto no llega a la lista
                        val evento = awaitPointerEvent(PointerEventPass.Initial)
                        if (evento.changes.size > 1) {
                            val zoom = evento.calculateZoom()
                            if (zoom != 1f) escala = (escala * zoom).coerceIn(1f, 3f)
                            evento.changes.forEach { it.consume() }
                        }
                    } while (evento.changes.any { it.pressed })
                }
            },
    ) {
        val doc = documento
        val anchoPantalla = maxWidth
        if (doc == null) {
            Text(
                text = stringResource(R.string.visor_pdf_cargando),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onSurfaceVariant,
            )
        } else {
            // las páginas se pintan al doble del ancho de la pantalla: sobra resolución para ampliar sin que se vean borrosas
            val anchoPx = with(LocalDensity.current) { (anchoPantalla * 2).roundToPx() }
            Row(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                LazyColumn(
                    modifier = Modifier.width(anchoPantalla * escala).fillMaxHeight(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items((0 until doc.paginas).toList()) { PaginaPdf(doc, it, anchoPx) }
                }
            }
        }
    }
}

@Composable
private fun PaginaPdf(documento: DocumentoPdf, indice: Int, anchoPx: Int) {
    val imagen by produceState<ImageBitmap?>(null, documento, indice, anchoPx) {
        value = documento.pintar(indice, anchoPx).asImageBitmap()
    }
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxWidth().aspectRatio(documento.proporciones[indice]).background(Color.White),
    ) {
        imagen?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, filterQuality = FilterQuality.Medium) }
    }
}

// El visor en pantalla completa con su barra; `abajo` es el espacio de acciones bajo el PDF (por ejemplo, «enviar al paciente»).
@Composable
fun PantallaPdf(
    titulo: String,
    archivo: File,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    abajo: (@Composable () -> Unit)? = null,
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    Column(modifier.fillMaxSize().background(colores.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onVolver) { Text(stringResource(R.string.mapa_volver)) }
            Text(titulo, style = MaterialTheme.typography.titleLarge, color = colores.onBackground)
        }
        VisorPdf(archivo, Modifier.weight(1f).fillMaxWidth())
        if (abajo != null) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) { abajo() }
        } else {
            androidx.compose.foundation.layout.Spacer(Modifier.navigationBarsPadding())
        }
    }
}
