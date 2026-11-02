package com.example.nutrigajo.ui.paciente.carrera

import android.annotation.SuppressLint
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.nutrigajo.R
import com.example.nutrigajo.data.carrera.Fase
import com.example.nutrigajo.data.carrera.RastreoEstado
import com.example.nutrigajo.domain.model.Carrera
import com.example.nutrigajo.domain.model.PuntoRuta
import com.example.nutrigajo.domain.model.Ruta
import com.example.nutrigajo.domain.model.TipoActividad
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CENTRO_POR_DEFECTO
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.components.ultimaUbicacion
import com.example.nutrigajo.ui.nutriologo.inicio.fechaMedia
import com.example.nutrigajo.ui.paciente.inicio.PantallaMetrica
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val ZOOM_RUTA = 17f
private const val ZOOM_MAXIMO_RESUMEN = 18f

// ---- Carrera en curso: el mapa con la ruta dibujándose y, abajo, el tiempo, la distancia, el ritmo y las calorías

@Composable
internal fun CarreraEnCurso(
    estado: RastreoEstado,
    pesoKg: Double,
    onPausar: () -> Unit,
    onReanudar: () -> Unit,
    onTerminar: () -> Unit,
    onDescartar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val densidad = LocalDensity.current
    var confirmar by rememberSaveable { mutableStateOf(false) }
    BackHandler { confirmar = true }
    MantenerPantallaEncendida()
    // el reloj de la pantalla late cada medio segundo; la duración real sale de RastreoEstado
    val reloj by produceState(SystemClock.elapsedRealtime()) {
        while (true) {
            value = SystemClock.elapsedRealtime()
            delay(500)
        }
    }
    val segundos = estado.duracionMs(reloj) / 1000
    val pausada = estado.fase == Fase.PAUSADA
    var altoPanel by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().background(colores.background)) {
        MapaRuta(estado.puntos, siguiendo = true, relleno = with(densidad) { altoPanel.toDp() }, modifier = Modifier.fillMaxSize())
        val aviso = when {
            pausada -> R.string.carrera_en_pausa
            estado.puntos.isEmpty() -> R.string.carrera_buscando_gps
            else -> null
        }
        aviso?.let {
            Text(
                text = stringResource(it),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colores.background.copy(alpha = 0.94f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = colores.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { altoPanel = it.height }
                .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .background(colores.background)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = duracion(segundos),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    color = colores.onBackground,
                )
                Text(stringResource(R.string.carrera_tiempo), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Cifra(kilometros(estado.distanciaM), stringResource(R.string.carrera_unidad_km), stringResource(R.string.carrera_distancia))
                Cifra(
                    ritmo(Ruta.ritmoSegPorKm(estado.distanciaM, segundos.toInt())),
                    stringResource(R.string.carrera_unidad_ritmo),
                    stringResource(R.string.carrera_ritmo),
                )
                Cifra(
                    Ruta.kcal(estado.tipo, pesoKg, segundos.toInt(), estado.distanciaM).toString(),
                    stringResource(R.string.carrera_unidad_kcal),
                    stringResource(R.string.carrera_calorias),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(colores.onBackground)
                        .alPulsar(if (pausada) onReanudar else onPausar),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (pausada) R.drawable.ic_reanudar else R.drawable.ic_pausa),
                        contentDescription = stringResource(if (pausada) R.string.carrera_reanudar else R.string.carrera_pausar),
                        modifier = Modifier.size(32.dp),
                        tint = colores.background,
                    )
                }
                BotonPrimario(stringResource(R.string.carrera_terminar), onTerminar, Modifier.weight(1f))
            }
        }
    }
    if (confirmar) {
        DialogoConfirmar(
            titulo = stringResource(R.string.carrera_salir_titulo),
            texto = stringResource(R.string.carrera_salir_texto),
            confirmar = stringResource(R.string.carrera_descartar),
            onConfirmar = onDescartar,
            onCancelar = { confirmar = false },
        )
    }
}

// ---- Antes de salir: el mapa con tu ubicación y, cuando ya se ve y hay señal del GPS, la cuenta de 5 a 1

@Composable
internal fun PrepararCarrera(preparacion: Preparacion, onListo: () -> Unit, onCancelar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val densidad = LocalDensity.current
    BackHandler(onBack = onCancelar)
    MantenerPantallaEncendida()
    // con la pantalla apagada o la app en segundo plano el servicio del GPS ya no puede arrancar: se cancela
    LifecycleEventEffect(Lifecycle.Event.ON_STOP, onEvent = onCancelar)
    val fix = rememberFixGps()
    var mapaListo by remember { mutableStateOf(false) }
    var espera by remember { mutableStateOf(false) }
    var altoPanel by remember { mutableIntStateOf(0) }
    val listo = mapaListo && fix != null
    LaunchedEffect(listo) { if (listo) onListo() }
    LaunchedEffect(Unit) {
        delay(15_000)
        espera = true
    }
    val puntos = remember(fix) { listOfNotNull(fix?.let { PuntoRuta(it.latitude, it.longitude) }) }
    val relleno = with(densidad) { altoPanel.toDp() }

    Box(Modifier.fillMaxSize().background(colores.background)) {
        MapaRuta(puntos, siguiendo = true, relleno = relleno, modifier = Modifier.fillMaxSize(), onCargado = { mapaListo = true })
        // arriba, para no tapar el punto de tu ubicación, que la cámara deja en el centro del mapa
        preparacion.cuenta?.let { Cuenta(it, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 56.dp)) }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { altoPanel = it.height }
                .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .background(colores.background)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    when {
                        preparacion.cuenta != null -> R.string.carrera_preparando_listo
                        !mapaListo -> R.string.carrera_preparando_mapa
                        else -> R.string.carrera_preparando_gps
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                color = colores.onBackground,
                textAlign = TextAlign.Center,
            )
            if (preparacion.cuenta == null && espera) {
                Text(
                    text = stringResource(if (mapaListo) R.string.carrera_preparando_gps_ayuda else R.string.carrera_preparando_mapa_ayuda),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            TextButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.carrera_cancelar), style = MaterialTheme.typography.titleMedium, color = colores.onSurfaceVariant)
            }
        }
    }
}

// El número de la cuenta: un círculo naranja que entra un poco grande y se asienta con cada segundo.
@Composable
private fun Cuenta(numero: Int, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val escala = remember(numero) { Animatable(1.35f) }
    LaunchedEffect(numero) { escala.animateTo(1f, tween(450)) }
    Box(
        modifier = modifier
            .size(168.dp)
            .graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
            }
            .clip(CircleShape)
            .background(colores.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = numero.toString(),
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            color = colores.onPrimary,
        )
    }
}

// El primer punto del GPS con la precisión que pide la ruta (la de Ruta), pedido solo mientras la preparación está a la vista.
@SuppressLint("MissingPermission") // quien abre la preparación ya comprobó el permiso (ver `PestanaActividad`)
@Composable
private fun rememberFixGps(): LatLng? {
    val contexto = LocalContext.current
    var fix by remember { mutableStateOf<LatLng?>(null) }
    DisposableEffect(contexto) {
        val gestor = contexto.getSystemService(LocationManager::class.java)
        val escucha = LocationListener { ubicacion ->
            if (!ubicacion.hasAccuracy() || ubicacion.accuracy <= Ruta.PRECISION_MAX_M) fix = LatLng(ubicacion.latitude, ubicacion.longitude)
        }
        runCatching { gestor.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, escucha, Looper.getMainLooper()) }
        onDispose { gestor.removeUpdates(escucha) }
    }
    return fix
}

// ---- Resumen al terminar y detalle de una actividad guardada: el mismo diseño, con otras acciones debajo

@Composable
internal fun ResumenCarrera(estado: RastreoEstado, pesoKg: Double, guardando: Boolean, error: Int?, onGuardar: () -> Unit, onDescartar: () -> Unit) {
    val segundos = (estado.duracionMs(0) / 1000).toInt()
    val carrera = remember(estado) {
        Carrera(
            tipo = estado.tipo,
            inicio = estado.inicio,
            duracionSeg = segundos,
            distanciaM = estado.distanciaM.roundToInt(),
            kcal = Ruta.kcal(estado.tipo, pesoKg, segundos, estado.distanciaM),
            ruta = estado.puntos,
        )
    }
    var confirmar by rememberSaveable { mutableStateOf(false) }
    PantallaRuta(
        titulo = stringResource(if (estado.tipo == TipoActividad.CORRER) R.string.carrera_completada_correr else R.string.carrera_completada_caminar),
        onVolver = { confirmar = true },
        carrera = carrera,
    ) {
        error?.let { AlertaError(stringResource(it)) }
        if (segundos < 10) {
            Text(stringResource(R.string.carrera_muy_corta), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BotonPrimario(
            texto = stringResource(R.string.carrera_guardar),
            onClick = onGuardar,
            modifier = Modifier.fillMaxWidth(),
            enabled = segundos >= 10,
            cargando = guardando,
        )
        TextButton(onClick = { confirmar = true }, modifier = Modifier.fillMaxWidth(), enabled = !guardando) {
            Text(stringResource(R.string.carrera_descartar), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (confirmar) {
        DialogoConfirmar(
            titulo = stringResource(R.string.carrera_descartar_titulo),
            texto = stringResource(R.string.carrera_descartar_texto),
            confirmar = stringResource(R.string.carrera_descartar),
            onConfirmar = onDescartar,
            onCancelar = { confirmar = false },
        )
    }
}

@Composable
internal fun DetalleCarrera(carrera: Carrera, onVolver: () -> Unit, onEliminar: () -> Unit) {
    var confirmar by rememberSaveable { mutableStateOf(false) }
    val tipo = stringResource(if (carrera.tipo == TipoActividad.CORRER) R.string.carrera_correr else R.string.carrera_caminar)
    PantallaRuta(titulo = "$tipo · ${fechaMedia(carrera.inicio)}", onVolver = onVolver, carrera = carrera) {
        TextButton(onClick = { confirmar = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.carrera_eliminar), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirmar) {
        DialogoConfirmar(
            titulo = stringResource(R.string.carrera_eliminar_titulo),
            texto = stringResource(R.string.carrera_eliminar_texto),
            confirmar = stringResource(R.string.carrera_eliminar),
            onConfirmar = onEliminar,
            onCancelar = { confirmar = false },
        )
    }
}

@Composable
private fun PantallaRuta(titulo: String, onVolver: () -> Unit, carrera: Carrera, acciones: @Composable ColumnScope.() -> Unit) {
    val colores = MaterialTheme.colorScheme
    PantallaMetrica(titulo, onVolver) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val forma = RoundedCornerShape(34.dp)
            if (carrera.ruta.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(280.dp).clip(forma).background(colores.surfaceVariant), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.carrera_sin_ruta),
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                MapaRuta(carrera.ruta, siguiendo = false, modifier = Modifier.fillMaxWidth().height(300.dp).clip(forma))
            }
            Column(
                modifier = Modifier.fillMaxWidth().clip(forma).background(colores.surfaceVariant).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Cifra(kilometros(carrera.distanciaM.toDouble()), stringResource(R.string.carrera_unidad_km), stringResource(R.string.carrera_distancia), Modifier.weight(1f))
                    Cifra(duracion(carrera.duracionSeg.toLong()), "", stringResource(R.string.carrera_tiempo), Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth()) {
                    Cifra(ritmo(carrera.ritmoSegKm), stringResource(R.string.carrera_unidad_ritmo), stringResource(R.string.carrera_ritmo), Modifier.weight(1f))
                    Cifra(carrera.kcal.toString(), stringResource(R.string.carrera_unidad_kcal), stringResource(R.string.carrera_calorias), Modifier.weight(1f))
                }
            }
            acciones()
        }
    }
}

// ---- Piezas

// Un número grande con su unidad y, debajo, qué mide.
@Composable
internal fun Cifra(valor: String, unidad: String, etiqueta: String, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                color = colores.onBackground,
            )
            if (unidad.isNotEmpty()) {
                Text(unidad, modifier = Modifier.padding(bottom = 3.dp), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
            }
        }
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
    }
}

@Composable
private fun DialogoConfirmar(titulo: String, texto: String, confirmar: String, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onConfirmar) { Text(confirmar, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.carrera_seguir)) } },
    )
}

// Mientras se corre la pantalla no se apaga sola: apagarla a mano sí es posible y el servicio sigue registrando.
@Composable
private fun MantenerPantallaEncendida() {
    val vista = LocalView.current
    DisposableEffect(vista) {
        vista.keepScreenOn = true
        onDispose { vista.keepScreenOn = false }
    }
}

// El mapa con la ruta en naranja. Con `siguiendo` (carrera en curso) la cámara va tras el último punto y se puede mover el mapa;
// sin él (resumen y detalle) es una vista fija que encuadra toda la ruta. `relleno` deja libre abajo lo que tapa el panel.
@Composable
private fun MapaRuta(ruta: List<PuntoRuta>, siguiendo: Boolean, modifier: Modifier = Modifier, relleno: Dp = 0.dp, onCargado: () -> Unit = {}) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    val densidad = LocalDensity.current
    val puntos = remember(ruta) { ruta.map { LatLng(it.lat, it.lng) } }
    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(puntos.lastOrNull() ?: ultimaUbicacion(contexto) ?: CENTRO_POR_DEFECTO, ZOOM_RUTA)
    }
    if (siguiendo) {
        LaunchedEffect(puntos.lastOrNull()) {
            puntos.lastOrNull()?.let { camara.animate(CameraUpdateFactory.newLatLng(it), 700) }
        }
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = camara,
        contentPadding = PaddingValues(bottom = relleno),
        properties = MapProperties(isMyLocationEnabled = siguiendo),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false,
            scrollGesturesEnabled = siguiendo,
            zoomGesturesEnabled = siguiendo,
        ),
        onMapLoaded = {
            onCargado()
            if (!siguiendo && puntos.size > 1) {
                val limites = LatLngBounds.Builder().apply { puntos.forEach { include(it) } }.build()
                camara.move(CameraUpdateFactory.newLatLngBounds(limites, with(densidad) { 40.dp.roundToPx() }))
                // una ruta muy corta no debe acercarse hasta la calle
                if (camara.position.zoom > ZOOM_MAXIMO_RESUMEN) camara.move(CameraUpdateFactory.zoomTo(ZOOM_MAXIMO_RESUMEN))
            }
        },
    ) {
        if (puntos.size > 1) {
            Polyline(points = puntos, color = colores.primary, width = 18f, startCap = RoundCap(), endCap = RoundCap())
        }
        // salida: círculo vacío; llegada (solo en el resumen): círculo lleno
        puntos.firstOrNull()?.let {
            Circle(center = it, radius = 6.0, fillColor = colores.background, strokeColor = colores.primary, strokeWidth = 8f, zIndex = 1f)
        }
        if (!siguiendo && puntos.size > 1) {
            Circle(center = puntos.last(), radius = 6.0, fillColor = colores.primary, strokeColor = colores.background, strokeWidth = 8f, zIndex = 1f)
        }
    }
}
