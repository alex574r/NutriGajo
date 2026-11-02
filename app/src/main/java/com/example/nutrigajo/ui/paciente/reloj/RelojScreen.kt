package com.example.nutrigajo.ui.paciente.reloj

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.DrawableRes
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
import com.example.nutrigajo.domain.model.Barra
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
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
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

// ---- Los apartados de datos del reloj: cada uno con su encabezado, tres cifras arriba, su gráfica y datos de apoyo

// La tarjeta gris de un apartado: un icono naranja con el título y, debajo, lo que cada apartado muestra.
@Composable
private fun Apartado(@DrawableRes icono: Int, @StringRes titulo: Int, contenido: @Composable ColumnScope.() -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(colores.surfaceVariant).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(colores.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(20.dp), tint = colores.primary)
            }
            Text(stringResource(titulo), style = MaterialTheme.typography.titleLarge, color = colores.onBackground)
        }
        contenido()
    }
}

// Una cifra de una fila: el valor, su unidad pequeña y, debajo, qué es.
private class Dato(val valor: String, val unidad: String, @param:StringRes val etiqueta: Int)

@Composable
private fun FilaDatos(vararg datos: Dato, compacta: Boolean = false) {
    val colores = MaterialTheme.colorScheme
    val estilo = if (compacta) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        datos.forEach { d ->
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = d.valor,
                        style = estilo.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        color = colores.onBackground,
                        maxLines = 1,
                        softWrap = false,
                    )
                    if (d.unidad.isNotEmpty()) Text(d.unidad, Modifier.padding(bottom = 2.dp), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant, maxLines = 1)
                }
                Text(stringResource(d.etiqueta), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

// «7h 12m»
@Composable
private fun hm(minutos: Int): String = stringResource(R.string.reloj_hm, minutos / 60, minutos % 60)

private fun numero(n: Int) = String.format(Locale.getDefault(), "%,d", n)

// El pulso: el último, el promedio y el rango del periodo y su gráfica, por horas (Hoy) o por días (7 días).
@Composable
private fun TarjetaPulso(estado: RelojEstado, onPeriodo: (PeriodoPulso) -> Unit) {
    val colores = MaterialTheme.colorScheme
    val pulso = estado.pulso
    val rango = if (pulso?.minimo != null && pulso.maximo != null) "${pulso.minimo}-${pulso.maximo}" else "--"
    Apartado(R.drawable.ic_corazon, R.string.reloj_pulso) {
        FilaDatos(
            Dato(estado.resumen?.ultimoBpm?.toString() ?: "--", "bpm", R.string.reloj_ahora),
            Dato(pulso?.promedio?.toString() ?: "--", "bpm", R.string.reloj_promedio),
            Dato(rango, "", R.string.reloj_rango),
        )
        SelectorPestanas(listOf(R.string.reloj_hoy, R.string.reloj_semana), estado.periodo.ordinal, { onPeriodo(PeriodoPulso.entries[it]) })
        when {
            pulso == null -> Esqueleto(Modifier.fillMaxWidth().height(190.dp), RoundedCornerShape(22.dp))
            pulso.barras.all { it.valor == null } ->
                Text(stringResource(R.string.reloj_sin_pulso), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
            else -> GraficaBarras(pulso.barras, piso = 40f, techoMinimo = 100f, unidad = "bpm")
        }
    }
}

// Lo que lleva el día: pasos, calorías activas y kilómetros, los pasos hora por hora y, abajo, los minutos de entrenamiento y los
// pasos de ayer.
@Composable
private fun TarjetaActividad(a: ActividadHoy) {
    Apartado(R.drawable.ic_corredor, R.string.reloj_actividad) {
        FilaDatos(
            Dato(numero(a.pasos), "", R.string.reloj_pasos),
            Dato(a.kcal.toString(), "kcal", R.string.reloj_calorias_activas),
            Dato(String.format(Locale.getDefault(), "%.1f", a.km), "km", R.string.reloj_distancia),
        )
        if (a.pasosPorHora.any { it > 0 }) {
            val barras = a.pasosPorHora.mapIndexed { h, v -> Barra(if (h % 6 == 0) "$h" else "", String.format(Locale.US, "%02d:00", h), v.takeIf { it > 0 }) }
            GraficaBarras(barras, piso = 0f, techoMinimo = 100f, unidad = stringResource(R.string.reloj_pasos).lowercase())
        }
        FilaDatos(
            Dato(a.minutos.toString(), "min", R.string.reloj_minutos),
            Dato(a.pasosAyer?.let(::numero) ?: "--", "", R.string.reloj_pasos_ayer),
        )
    }
}

// El sueño: la última noche, el promedio y la mejor de la semana, las siete noches en barras y las etapas de la última.
@Composable
private fun TarjetaSueno(noches: List<NocheSueno>) {
    val contexto = LocalContext.current
    val ultima = noches.first()
    val hoy = LocalDate.now()
    val porDia = noches.associateBy { it.fecha }
    val barras = (6L downTo 0L).map { atras ->
        val dia = hoy.minusDays(atras)
        Barra(
            etiqueta = dia.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.forLanguageTag("es-MX")).uppercase(),
            detalle = DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es-MX")).format(dia),
            valor = porDia[dia]?.minutos,
        )
    }
    Apartado(R.drawable.ic_luna, R.string.reloj_sueno) {
        FilaDatos(
            Dato(hm(ultima.minutos), "", R.string.reloj_sueno_ultima),
            Dato(hm(noches.map { it.minutos }.average().toInt()), "", R.string.reloj_promedio),
            Dato(hm(noches.maxOf { it.minutos }), "", R.string.reloj_sueno_mejor),
        )
        GraficaBarras(barras, piso = 0f, techoMinimo = 480f, unidad = "", formato = { contexto.getString(R.string.reloj_hm, it / 60, it % 60) })
        // sin etapas (el reloj solo mandó la hora de dormir y de despertar) no hay nada que desglosar
        if (ultima.profundo + ultima.ligero + ultima.rem > 0) {
            FilaDatos(
                Dato(hm(ultima.profundo), "", R.string.reloj_etapa_profundo),
                Dato(hm(ultima.ligero), "", R.string.reloj_etapa_ligero),
                Dato(hm(ultima.rem), "", R.string.reloj_etapa_rem),
                Dato(hm(ultima.despierto), "", R.string.reloj_etapa_despierto),
                compacta = true,
            )
        }
    }
}

// El oxígeno en sangre: la última lectura, el promedio y el mínimo de 24 horas y el promedio de cada hora.
@Composable
private fun TarjetaOxigeno(o: Oxigeno) {
    val colores = MaterialTheme.colorScheme
    val ahora = remember(o) { Instant.now() }
    Apartado(R.drawable.ic_gota, R.string.reloj_oxigeno) {
        FilaDatos(
            Dato(o.porcentaje.toString(), "%", R.string.reloj_ultima),
            Dato(o.promedio.toString(), "%", R.string.reloj_promedio),
            Dato(o.minimo.toString(), "%", R.string.reloj_minimo),
        )
        if (o.porHora.any { it != null }) {
            val barras = o.porHora.mapIndexed { h, v -> Barra(if (h % 6 == 0) "$h" else "", String.format(Locale.US, "%02d:00", h), v) }
            GraficaBarras(barras, piso = 80f, techoMinimo = 100f, margen = 1f, unidad = "%")
        }
        Text(stringResource(R.string.reloj_oxigeno_lectura, haceCuanto(o.lectura, ahora)), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
    }
}

// Barras tocables: al tocar una se ve su hora (o día) y su valor; mientras no se toque ninguna se marca la más alta. `piso` es el
// valor del que arranca la altura y el techo es el máximo por `margen`, pero nunca menos de `techoMinimo`.
@Composable
private fun GraficaBarras(
    barras: List<Barra>,
    piso: Float,
    techoMinimo: Float,
    unidad: String,
    margen: Float = 1.1f,
    formato: (Int) -> String = { it.toString() },
) {
    val colores = MaterialTheme.colorScheme
    var tocada by remember(barras) { mutableStateOf<Int?>(null) }
    val activa = tocada ?: barras.indices.maxByOrNull { barras[it].valor ?: 0 } ?: 0
    val barra = barras[activa]
    val techo = maxOf(barras.maxOf { it.valor ?: 0 } * margen, techoMinimo)
    val color = colores.primary
    val gris = colores.onSurfaceVariant.copy(alpha = 0.28f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = barra.valor?.let { stringResource(R.string.reloj_barra, barra.detalle, formato(it), unidad).trim() } ?: barra.detalle,
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
                val alto = if (b.valor == null) 3.dp.toPx() else ((b.valor - piso) / (techo - piso)).coerceIn(0.04f, 1f) * size.height
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
