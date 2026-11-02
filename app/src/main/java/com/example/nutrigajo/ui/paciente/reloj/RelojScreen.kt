package com.example.nutrigajo.ui.paciente.reloj

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.ActividadHoy
import com.example.nutrigajo.domain.model.BarraPulso
import com.example.nutrigajo.domain.model.NocheSueno
import com.example.nutrigajo.domain.model.Oxigeno
import com.example.nutrigajo.domain.model.PeriodoPulso
import com.example.nutrigajo.domain.model.haceCuanto
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.SelectorPestanas
import com.example.nutrigajo.ui.nutriologo.inicio.fechaMedia
import com.example.nutrigajo.ui.paciente.inicio.PantallaMetrica
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

private const val HEALTH_CONNECT = "com.google.android.apps.healthdata"

// El reloj: el pulso que su app manda a Health Connect, en una gráfica por horas o por días, y los entrenamientos recientes. Sin
// conexión se ve en qué falta avanzar (instalar Health Connect, dar el permiso) y qué botón sigue.
@Composable
fun RelojScreen(onVolver: () -> Unit, vm: RelojViewModel) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current
    val pedir = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { vm.cargar() }
    // al abrir y al volver de instalar Health Connect o de sus ajustes se vuelve a ver en qué punto está
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.cargar() }

    PantallaMetrica(stringResource(R.string.reloj_titulo), onVolver) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (estado.fase == FaseReloj.CARGANDO) {
                Esqueleto(Modifier.fillMaxWidth().height(150.dp), RoundedCornerShape(34.dp))
                Esqueleto(Modifier.fillMaxWidth().height(300.dp), RoundedCornerShape(34.dp))
            } else {
                TarjetaConexion(estado, onInstalar = { abrirTienda(contexto) }, onConectar = { pedir.launch(vm.permisos) }, onActualizar = vm::cargar)
                if (estado.fase == FaseReloj.CONECTADO) {
                    val resumen = estado.resumen
                    if (resumen?.faltanPermisos == true) PermisosFaltantes(onPermitir = { pedir.launch(vm.permisos) })
                    TarjetaPulso(estado, onPeriodo = vm::elegirPeriodo)
                    resumen?.actividad?.let { TarjetaActividad(it) }
                    if (resumen != null && resumen.sueno.isNotEmpty()) TarjetaSueno(resumen.sueno)
                    resumen?.oxigeno?.let { TarjetaOxigeno(it) }
                    Historial(estado)
                } else {
                    PasosConexion()
                }
            }
        }
    }
}

// La tarjeta oscura del reloj: de qué app vienen los datos, cuándo se sincronizaron y, si falta algo, el botón que sigue.
@Composable
private fun TarjetaConexion(estado: RelojEstado, onInstalar: () -> Unit, onConectar: () -> Unit, onActualizar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val tenue = colores.background.copy(alpha = 0.7f)
    val conectado = estado.fase == FaseReloj.CONECTADO
    val resumen = estado.resumen
    val ahora = remember(resumen) { Instant.now() }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(colores.onBackground).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(colores.primary), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_smartwatch), contentDescription = null, modifier = Modifier.size(28.dp), tint = colores.onPrimary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = (if (conectado) resumen?.origen else null) ?: stringResource(R.string.reloj_sin_conectar),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colores.background,
                )
                val estadoTexto = when (estado.fase) {
                    FaseReloj.INSTALAR -> stringResource(R.string.reloj_estado_instalar)
                    FaseReloj.PERMISO -> stringResource(R.string.reloj_estado_permiso)
                    FaseReloj.NO_DISPONIBLE -> stringResource(R.string.reloj_estado_no_disponible)
                    FaseReloj.ERROR -> stringResource(R.string.reloj_error)
                    FaseReloj.CONECTADO ->
                        resumen?.ultimaLectura?.let { stringResource(R.string.reloj_estado_sincronizado, haceCuanto(it, ahora)) }
                            ?: stringResource(R.string.reloj_estado_sin_datos)
                    FaseReloj.CARGANDO -> ""
                }
                if (conectado && resumen?.ultimaLectura != null) {
                    // el tag del diseño: fondo naranja claro y texto naranja
                    Text(
                        text = estadoTexto,
                        modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(colores.primaryContainer).padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = colores.primary,
                    )
                } else {
                    Text(estadoTexto, style = MaterialTheme.typography.bodyMedium, color = tenue)
                }
            }
        }
        when (estado.fase) {
            FaseReloj.INSTALAR -> BotonPrimario(stringResource(R.string.reloj_boton_instalar), onInstalar, Modifier.fillMaxWidth(), altura = 48.dp)
            FaseReloj.PERMISO -> BotonPrimario(stringResource(R.string.reloj_boton_conectar), onConectar, Modifier.fillMaxWidth(), altura = 48.dp)
            FaseReloj.ERROR -> BotonPrimario(stringResource(R.string.reloj_boton_reintentar), onActualizar, Modifier.fillMaxWidth(), altura = 48.dp)
            FaseReloj.CONECTADO ->
                TextButton(onClick = onActualizar, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.reloj_boton_actualizar), style = MaterialTheme.typography.titleMedium, color = tenue)
                }
            else -> Unit
        }
    }
}

// El último pulso y la gráfica: Hoy (una barra por hora) o 7 días (una por día). Al tocar una barra se ve su hora y su pulso.
@Composable
private fun TarjetaPulso(estado: RelojEstado, onPeriodo: (PeriodoPulso) -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(colores.surfaceVariant).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.reloj_pulso), style = MaterialTheme.typography.titleMedium, color = colores.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_corazon), contentDescription = null, modifier = Modifier.padding(bottom = 8.dp).size(28.dp), tint = colores.primary)
            Text(
                text = estado.resumen?.ultimoBpm?.toString() ?: "--",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                color = colores.onBackground,
            )
            Text(stringResource(R.string.reloj_unidad_bpm), Modifier.padding(bottom = 10.dp), style = MaterialTheme.typography.titleMedium, color = colores.onSurfaceVariant)
        }
        SelectorPestanas(listOf(R.string.reloj_hoy, R.string.reloj_semana), estado.periodo.ordinal, { onPeriodo(PeriodoPulso.entries[it]) })
        when {
            estado.barras.isEmpty() -> Esqueleto(Modifier.fillMaxWidth().height(190.dp), RoundedCornerShape(22.dp))
            estado.barras.all { it.bpm == null } ->
                Text(stringResource(R.string.reloj_sin_pulso), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
            else -> GraficaPulso(estado.barras)
        }
    }
}

@Composable
private fun GraficaPulso(barras: List<BarraPulso>) {
    val colores = MaterialTheme.colorScheme
    // la barra tocada; mientras no se toque ninguna, la más alta
    var tocada by remember(barras) { mutableStateOf<Int?>(null) }
    val activa = tocada ?: barras.indices.maxByOrNull { barras[it].bpm ?: 0 } ?: 0
    val barra = barras[activa]
    val maximo = barras.maxOf { it.bpm ?: 0 }
    // pulso en reposo ~40 como piso de la gráfica; el techo deja aire sobre la barra más alta
    val techo = maxOf(maximo + 10, 100).toFloat()
    val color = colores.primary
    val gris = colores.onSurfaceVariant.copy(alpha = 0.28f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = barra.bpm?.let { stringResource(R.string.reloj_barra, barra.detalle, it) } ?: barra.detalle,
            style = MaterialTheme.typography.labelLarge,
            color = colores.onBackground,
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .pointerInput(barras) { detectTapGestures { tocada = (it.x / size.width * barras.size).toInt().coerceIn(0, barras.size - 1) } },
        ) {
            val hueco = size.width / barras.size
            val ancho = hueco * 0.62f
            barras.forEachIndexed { i, b ->
                val alto = if (b.bpm == null) 3.dp.toPx() else ((b.bpm - 40f) / (techo - 40f)).coerceIn(0.04f, 1f) * size.height
                drawRoundRect(
                    color = if (i == activa) color else gris,
                    topLeft = Offset(i * hueco + (hueco - ancho) / 2, size.height - alto),
                    size = Size(ancho, alto),
                    cornerRadius = CornerRadius(minOf(ancho / 2, 6.dp.toPx())),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            barras.forEach { b ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (b.etiqueta.isNotEmpty()) {
                        Text(
                            text = b.etiqueta,
                            modifier = Modifier.wrapContentWidth(unbounded = true),
                            style = MaterialTheme.typography.bodySmall,
                            color = colores.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

// Cuando se dio el permiso del pulso pero no el de otras métricas (o se agregaron después): se vuelve a pedir todo junto.
@Composable
private fun PermisosFaltantes(onPermitir: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(colores.primaryContainer).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.reloj_permisos_titulo), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
        Text(stringResource(R.string.reloj_permisos_texto), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
        BotonPrimario(stringResource(R.string.reloj_boton_permitir), onPermitir, Modifier.fillMaxWidth(), altura = 48.dp)
    }
}

// La tarjeta gris con su título; las de actividad, sueño y oxígeno la comparten.
@Composable
private fun TarjetaReloj(@StringRes titulo: Int, contenido: @Composable ColumnScope.() -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(colores.surfaceVariant).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium, color = colores.onSurfaceVariant)
        contenido()
    }
}

// Lo que lleva el día: pasos y kilómetros, calorías activas y minutos de entrenamiento.
@Composable
private fun TarjetaActividad(a: ActividadHoy) {
    TarjetaReloj(R.string.reloj_actividad) {
        Row(Modifier.fillMaxWidth()) {
            DatoReloj(String.format(Locale.getDefault(), "%,d", a.pasos), "", R.string.reloj_pasos, Modifier.weight(1f))
            DatoReloj(String.format(Locale.getDefault(), "%.1f", a.km), "km", R.string.reloj_distancia, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            DatoReloj(a.kcal.toString(), "kcal", R.string.reloj_calorias_activas, Modifier.weight(1f))
            DatoReloj(a.minutos.toString(), "min", R.string.reloj_minutos, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DatoReloj(valor: String, unidad: String, @StringRes etiqueta: Int, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                color = colores.onBackground,
            )
            if (unidad.isNotEmpty()) Text(unidad, Modifier.padding(bottom = 3.dp), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
        }
        Text(stringResource(etiqueta), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
    }
}

// Lo dormido anoche y el promedio de las noches de la semana.
@Composable
private fun TarjetaSueno(noches: List<NocheSueno>) {
    val colores = MaterialTheme.colorScheme
    val ultima = noches.first()
    val promedio = noches.map { it.minutos }.average().toInt()
    val dia = if (ultima.fecha == LocalDate.now()) {
        stringResource(R.string.reloj_sueno_anoche)
    } else {
        fechaMedia(ultima.fecha.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }
    TarjetaReloj(R.string.reloj_sueno) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = horasMinutos(ultima.minutos),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                color = colores.onBackground,
            )
            Text(dia, style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
        }
        Text(stringResource(R.string.reloj_sueno_promedio, horasMinutos(promedio)), style = MaterialTheme.typography.bodyMedium, color = colores.onBackground)
    }
}

@Composable
private fun horasMinutos(minutos: Int): String = stringResource(R.string.reloj_horas_minutos, minutos / 60, minutos % 60)

// La última lectura de oxígeno en sangre y el promedio de 24 horas.
@Composable
private fun TarjetaOxigeno(o: Oxigeno) {
    val colores = MaterialTheme.colorScheme
    val ahora = remember(o) { Instant.now() }
    TarjetaReloj(R.string.reloj_oxigeno) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = o.porcentaje.toString(),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                color = colores.onBackground,
            )
            Text("%", Modifier.padding(bottom = 10.dp), style = MaterialTheme.typography.titleMedium, color = colores.onSurfaceVariant)
        }
        Text(stringResource(R.string.reloj_oxigeno_lectura, haceCuanto(o.lectura, ahora)), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
        Text(stringResource(R.string.reloj_oxigeno_promedio, o.promedio), style = MaterialTheme.typography.bodyMedium, color = colores.onBackground)
    }
}

@Composable
private fun Historial(estado: RelojEstado) {
    val colores = MaterialTheme.colorScheme
    val sesiones = estado.resumen?.sesiones.orEmpty()
    Text(stringResource(R.string.reloj_historial), style = MaterialTheme.typography.titleLarge, color = colores.onBackground)
    if (sesiones.isEmpty()) {
        Text(stringResource(R.string.reloj_historial_vacio), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
    } else {
        sesiones.forEach { s ->
            FilaAjuste(
                icono = R.drawable.ic_corazon,
                titulo = stringResource(R.string.reloj_sesion_titulo, s.titulo, s.minutos),
                etiqueta = fechaMedia(s.inicio),
                descripcion = if (s.promedio != null && s.maximo != null) stringResource(R.string.reloj_sesion_pulso, s.promedio, s.maximo) else null,
            )
        }
    }
}

// Lo que hay que hacer, en orden, mientras el reloj no está conectado.
@Composable
private fun PasosConexion() {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(colores.surfaceVariant).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.reloj_pasos_titulo), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
        for (paso in listOf(R.string.reloj_paso_1, R.string.reloj_paso_2, R.string.reloj_paso_3)) {
            Text(stringResource(paso), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
        }
    }
}

// Health Connect antes de Android 14 es una app de la tienda; si no hay tienda se abre su página web.
private fun abrirTienda(contexto: Context) {
    val tienda = Intent(Intent.ACTION_VIEW).apply {
        setPackage("com.android.vending")
        data = Uri.parse("market://details?id=$HEALTH_CONNECT&url=healthconnect%3A%2F%2Fonboarding")
        putExtra("overlay", true)
        putExtra("callerId", contexto.packageName)
    }
    try {
        contexto.startActivity(tienda)
    } catch (_: ActivityNotFoundException) {
        runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT"))) }
    }
}
