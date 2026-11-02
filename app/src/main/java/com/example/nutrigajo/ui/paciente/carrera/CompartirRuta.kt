package com.example.nutrigajo.ui.paciente.carrera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Carrera
import com.example.nutrigajo.domain.model.PuntoRuta
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.alPulsar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.max

private const val DISENOS = 4
private const val GALERIA = -1
private const val LADO_MAX_PX = 1800

// Los fondos que se ofrecen además de una foto de la galería: dos lisos y tres fotos (`fondo_*`, las eligió el usuario). Sobre el naranja la ruta va en
// blanco (en naranja no se vería).
private enum class Fondo(@DrawableRes val foto: Int?, val degradado: List<Color> = emptyList(), val rutaBlanca: Boolean = false) {
    OSCURO(null, listOf(Color(0xFF2A2C31), Color(0xFF111214))),
    NARANJA(null, listOf(Color(0xFFFB923C), Color(0xFFC2410C)), rutaBlanca = true),
    PISTA(R.drawable.fondo_pista),
    ZANCADAS(R.drawable.fondo_zancadas),
    NOCHE(R.drawable.fondo_noche),
}

// El editor de la imagen para compartir, como las historias de Strava: una imagen vertical con la distancia, el ritmo, el tiempo y la
// forma de la ruta sobre una foto (de la galería o de la app) o un fondo liso. Cada toque sobre la imagen cambia el diseño y la
// posición de las cifras; «compartir» la guarda como PNG en la caché y abre el menú de compartir del sistema.
@Composable
internal fun EditorCompartir(carrera: Carrera, onCerrar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val tenue = colores.background.copy(alpha = 0.7f)
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val capa = rememberGraphicsLayer()
    BackHandler(onBack = onCerrar)
    var elegido by rememberSaveable { mutableIntStateOf(0) }
    var diseno by rememberSaveable { mutableIntStateOf(0) }
    var galeria by rememberSaveable { mutableStateOf<Uri?>(null) }
    var preparando by remember { mutableStateOf(false) }
    var fallo by remember { mutableStateOf(false) }
    val foto by produceState<ImageBitmap?>(null, galeria) { value = galeria?.let { leerFoto(contexto, it) } }
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            galeria = uri
            elegido = GALERIA
        }
    }
    val fondo = Fondo.entries.getOrNull(elegido)

    fun compartir() {
        preparando = true
        fallo = false
        alcance.launch {
            val uri = runCatching {
                val bitmap = capa.toImageBitmap().asAndroidBitmap()
                withContext(Dispatchers.IO) {
                    val carpeta = File(contexto.cacheDir, "compartir").apply { mkdirs() }
                    carpeta.listFiles()?.forEach { it.delete() }
                    val archivo = File(carpeta, "ruta-${System.currentTimeMillis()}.png")
                    archivo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
                }
            }.getOrNull()
            preparando = false
            if (uri == null) {
                fallo = true
                return@launch
            }
            val envio = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            contexto.startActivity(Intent.createChooser(envio, null))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.onBackground)
            // este editor tapa la pantalla de abajo: sus toques no deben pasar a los botones de ahí
            .pointerInput(Unit) { detectTapGestures { } }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(colores.background.copy(alpha = 0.16f)).alPulsar(onCerrar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cerrar),
                    contentDescription = stringResource(R.string.carrera_compartir_cerrar),
                    modifier = Modifier.size(20.dp),
                    tint = colores.background,
                )
            }
            Text(
                text = stringResource(R.string.carrera_compartir),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = colores.background,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(40.dp))
        }
        // la imagen que se comparte: 9:16, se captura tal cual se ve aquí (sin las esquinas redondeadas)
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(24.dp))
                .alPulsar({ diseno = (diseno + 1) % DISENOS }, minimo = 0.99f)
                .drawWithContent {
                    capa.record { this@drawWithContent.drawContent() }
                    drawLayer(capa)
                },
        ) {
            FondoImagen(fondo, foto, Modifier.fillMaxSize())
            // con una foto detrás el texto necesita un velo para leerse
            if (fondo?.foto != null || (fondo == null && foto != null)) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.55f)))))
            }
            Cifras(carrera, diseno, if (fondo?.rutaBlanca == true) Color.White else colores.primary)
        }
        Text(stringResource(R.string.carrera_compartir_ayuda), style = MaterialTheme.typography.bodySmall, color = tenue)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Miniatura(
                seleccionada = elegido == GALERIA,
                onClick = {
                    if (galeria != null && elegido != GALERIA) elegido = GALERIA
                    else elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            ) {
                val miniatura = foto
                if (miniatura != null) {
                    Image(miniatura, contentDescription = stringResource(R.string.carrera_compartir_galeria), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().background(colores.background.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_imagen),
                            contentDescription = stringResource(R.string.carrera_compartir_galeria),
                            modifier = Modifier.size(22.dp),
                            tint = colores.background,
                        )
                    }
                }
            }
            Fondo.entries.forEachIndexed { i, f ->
                Miniatura(seleccionada = elegido == i, onClick = { elegido = i }) { FondoImagen(f, null, Modifier.fillMaxSize()) }
            }
        }
        if (fallo) AlertaError(stringResource(R.string.carrera_compartir_error))
        BotonPrimario(stringResource(R.string.carrera_compartir_enviar), ::compartir, Modifier.fillMaxWidth(), cargando = preparando)
    }
}

// La foto elegida de la galería, reducida a un tamaño que cabe en memoria; sin ella (o si falla) se queda el fondo liso.
private suspend fun leerFoto(contexto: Context, uri: Uri): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contexto.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(max(1, max(info.size.width, info.size.height) / LADO_MAX_PX))
        }.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

// Una foto de la galería (`fondo` null), una foto de la app o un degradado liso; la galería sin cargar se ve como el fondo oscuro.
@Composable
private fun FondoImagen(fondo: Fondo?, foto: ImageBitmap?, modifier: Modifier = Modifier) {
    when {
        fondo == null && foto != null -> Image(foto, contentDescription = null, modifier, contentScale = ContentScale.Crop)
        fondo?.foto != null -> Image(painterResource(fondo.foto), contentDescription = null, modifier, contentScale = ContentScale.Crop)
        else -> Box(modifier.background(Brush.verticalGradient((fondo ?: Fondo.OSCURO).degradado)))
    }
}

@Composable
private fun Miniatura(seleccionada: Boolean, onClick: () -> Unit, contenido: @Composable BoxScope.() -> Unit) {
    val forma = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(forma)
            .border(2.dp, if (seleccionada) MaterialTheme.colorScheme.primary else Color.Transparent, forma)
            .alPulsar(onClick, role = Role.RadioButton),
        content = contenido,
    )
}

// ---- Los cuatro diseños de las cifras: cada toque pasa al siguiente

@Composable
private fun Cifras(carrera: Carrera, diseno: Int, colorRuta: Color) {
    val distancia = "${kilometros(carrera.distanciaM.toDouble())} km"
    val paso = "${ritmo(carrera.ritmoSegKm)} /km"
    val tiempo = tiempoCorto(carrera.duracionSeg)
    val etiquetaDistancia = stringResource(R.string.carrera_distancia)
    val etiquetaRitmo = stringResource(R.string.carrera_ritmo)
    val etiquetaTiempo = stringResource(R.string.carrera_tiempo)
    val tipos = MaterialTheme.typography
    val hayRuta = carrera.ruta.size > 1
    Box(Modifier.fillMaxSize().padding(24.dp)) {
        when (diseno % DISENOS) {
            // todo centrado y un poco arriba, con la ruta y la marca debajo
            0 -> Column(
                modifier = Modifier.align(Alignment.Center).padding(bottom = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DatoCompartido(etiquetaDistancia, distancia, tipos.headlineLarge, Alignment.CenterHorizontally)
                DatoCompartido(etiquetaRitmo, paso, tipos.headlineLarge, Alignment.CenterHorizontally)
                DatoCompartido(etiquetaTiempo, tiempo, tipos.headlineLarge, Alignment.CenterHorizontally)
                if (hayRuta) FormaRuta(carrera.ruta, colorRuta, Modifier.size(120.dp))
                Marca()
            }
            // las tres cifras en una fila abajo, con la marca y la ruta encima
            1 -> Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (hayRuta) FormaRuta(carrera.ruta, colorRuta, Modifier.size(96.dp))
                Marca()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DatoCompartido(etiquetaDistancia, distancia, tipos.headlineSmall, Alignment.Start)
                    DatoCompartido(etiquetaRitmo, paso, tipos.headlineSmall, Alignment.CenterHorizontally)
                    DatoCompartido(etiquetaTiempo, tiempo, tipos.headlineSmall, Alignment.End)
                }
            }
            // la distancia enorme abajo a la izquierda y la ruta arriba a la derecha
            2 -> {
                if (hayRuta) FormaRuta(carrera.ruta, colorRuta, Modifier.align(Alignment.TopEnd).size(132.dp))
                Marca(Modifier.align(Alignment.TopStart))
                Column(Modifier.align(Alignment.BottomStart).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = distancia,
                        style = tipos.displayMedium.copy(fontSize = 64.sp, lineHeight = 68.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        color = Color.White,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        DatoCompartido(etiquetaRitmo, paso, tipos.headlineSmall, Alignment.Start)
                        DatoCompartido(etiquetaTiempo, tiempo, tipos.headlineSmall, Alignment.Start)
                    }
                }
            }
            // la ruta grande al centro y las cifras abajo
            else -> {
                Marca(Modifier.align(Alignment.TopStart))
                if (hayRuta) {
                    FormaRuta(carrera.ruta, colorRuta, Modifier.align(Alignment.Center).padding(bottom = 48.dp).fillMaxWidth(0.8f).aspectRatio(1f), 6.dp)
                } else {
                    Text(
                        text = distancia,
                        modifier = Modifier.align(Alignment.Center),
                        style = tipos.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        color = Color.White,
                    )
                }
                Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DatoCompartido(etiquetaDistancia, distancia, tipos.headlineSmall, Alignment.Start)
                    DatoCompartido(etiquetaRitmo, paso, tipos.headlineSmall, Alignment.CenterHorizontally)
                    DatoCompartido(etiquetaTiempo, tiempo, tipos.headlineSmall, Alignment.End)
                }
            }
        }
    }
}

// Una cifra: su nombre pequeño arriba y el valor debajo, en blanco (la imagen es la misma con cualquier tema de la app).
@Composable
private fun DatoCompartido(etiqueta: String, valor: String, estilo: TextStyle, alineacion: Alignment.Horizontal) {
    Column(horizontalAlignment = alineacion) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
        Text(valor, style = estilo.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = Color.White, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun Marca(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(R.drawable.ic_logo_mono), contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.White)
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = Color.White,
        )
    }
}

// El trazo de la ruta solo, sin mapa: la forma que dibujaron tus pasos, centrada y a escala dentro del espacio que tenga.
@Composable
private fun FormaRuta(ruta: List<PuntoRuta>, color: Color, modifier: Modifier = Modifier, grosor: Dp = 4.dp) {
    Canvas(modifier) {
        val minLat = ruta.minOf { it.lat }
        val maxLat = ruta.maxOf { it.lat }
        val minLng = ruta.minOf { it.lng }
        val maxLng = ruta.maxOf { it.lng }
        // un grado de longitud mide menos cuanto más lejos del ecuador: sin esto la forma saldría ancha
        val cosLat = cos(Math.toRadians((minLat + maxLat) / 2))
        val ancho = ((maxLng - minLng) * cosLat).coerceAtLeast(1e-9)
        val alto = (maxLat - minLat).coerceAtLeast(1e-9)
        val escala = minOf(size.width / ancho, size.height / alto)
        val izquierda = (size.width - ancho * escala) / 2
        val arriba = (size.height - alto * escala) / 2
        val trazo = Path()
        ruta.forEachIndexed { i, p ->
            val x = (izquierda + (p.lng - minLng) * cosLat * escala).toFloat()
            val y = (arriba + (maxLat - p.lat) * escala).toFloat()
            if (i == 0) trazo.moveTo(x, y) else trazo.lineTo(x, y)
        }
        drawPath(trazo, color, style = Stroke(grosor.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// 34m 37s, o 1h 5m cuando pasa de la hora
internal fun tiempoCorto(segundos: Int): String =
    if (segundos >= 3600) "${segundos / 3600}h ${segundos % 3600 / 60}m" else "${segundos / 60}m ${segundos % 60}s"
