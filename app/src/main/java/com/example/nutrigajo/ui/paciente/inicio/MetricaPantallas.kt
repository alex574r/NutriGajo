package com.example.nutrigajo.ui.paciente.inicio

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.GrupoAlimento
import com.example.nutrigajo.domain.model.MetasDiarias
import com.example.nutrigajo.ui.components.AccionAbanico
import com.example.nutrigajo.ui.components.BotonConAbanico
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.components.rememberAgua
import com.example.nutrigajo.ui.theme.color
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.atan2

// Lo que abre cada tarjeta de «Métricas fitness»; las estadísticas se abren desde las calorías.
enum class Metrica { SCORE, HIDRATACION, CALORIAS, ESTADISTICAS, IMC, PASOS }

// ponytail: las seis pantallas son de muestra (DatosMuestra.kt) salvo las metas, que son las del paciente (las fija su nutriólogo y
// él puede ajustarlas), y el agua, que sube con el + y vive en el inicio para que la tarjeta la refleje. Las mediciones reales
// reemplazan lo demás cuando existan.
@Composable
fun MetricaScreen(
    metrica: Metrica,
    resumen: ResumenDia,
    metas: MetasDiarias,
    // ml de agua que se suman (o restan, con signo) al valor de hoy
    onAgregarAgua: (Int) -> Unit,
    onAbrir: (Metrica) -> Unit,
    onAjustes: () -> Unit,
    onVolver: () -> Unit,
) {
    when (metrica) {
        Metrica.SCORE -> ScoreScreen(onVolver)
        Metrica.HIDRATACION -> HidratacionScreen(resumen.hidratacionMl, metas.aguaMl, onAgregarAgua, onAjustes, onVolver)
        Metrica.CALORIAS -> CaloriasScreen(resumen.calorias, metas.kcal, onEstadisticas = { onAbrir(Metrica.ESTADISTICAS) }, onAjustes = onAjustes, onVolver = onVolver)
        // las estadísticas cuelgan de las calorías: se vuelve a ellas, no al inicio
        Metrica.ESTADISTICAS -> EstadisticasScreen(onVolver = { onAbrir(Metrica.CALORIAS) })
        Metrica.IMC -> ImcScreen(onVolver)
        Metrica.PASOS -> PasosScreen(metas, onAjustes, onVolver)
    }
}

// ---- Hidratación: el vaso se llena con el +

// ml que suma cada acción del abanico del «+» (mantenerlo presionado); la última resta un vaso
private val CANTIDADES_ABANICO = intArrayOf(100, 500, -VASO_ML)

@Composable
private fun HidratacionScreen(actualMl: Int, metaMl: Int, onAgregar: (Int) -> Unit, onAjustes: () -> Unit, onVolver: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    val falta = (metaMl - actualMl).coerceAtLeast(0)
    val nivel by animateFloatAsState(
        targetValue = (actualMl / metaMl.toFloat()).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 120f),
        label = "agua",
    )
    // el agua chapotea con el teléfono, y cada vaso que se agrega la salpica
    val agua = rememberAgua()
    LaunchedEffect(actualMl) { agua.salpicar(0.9f) }
    val acciones = listOf(
        AccionAbanico("+100", stringResource(R.string.agua_sorbo)),
        AccionAbanico("+500", stringResource(R.string.agua_botella)),
        AccionAbanico("−$VASO_ML", stringResource(R.string.agua_quitar, VASO_ML)),
    )
    PantallaMetrica(stringResource(R.string.metrica_hidratacion), onVolver, onAjustes = onAjustes) {
        CifraMetrica(
            icono = R.drawable.ic_botella,
            colorIcono = colores.secondary,
            valor = actualMl.toString(),
            unidad = "ml",
            subtitulo = if (falta > 0) stringResource(R.string.agua_falta, falta) else stringResource(R.string.agua_meta_cumplida),
        )
        Box(Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(34.dp)).background(colores.surfaceVariant)) {
            Column(Modifier.padding(start = 22.dp, top = 20.dp)) {
                Text(stringResource(R.string.agua_meta), style = tipos.bodySmall, color = colores.onSurfaceVariant)
                Text("$metaMl ml", style = tipos.titleMedium.copy(fontWeight = FontWeight.Bold), color = colores.onBackground)
            }
            OlasDeAgua(agua, nivel = { nivel }, color = colores.secondary, modifier = Modifier.matchParentSize())
            // la etiqueta va a media profundidad: con el teléfono inclinado la superficie sube y baja decenas de dp en los lados
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(nivel)) {
                Column(Modifier.align(Alignment.CenterStart).padding(start = 22.dp)) {
                    Text(stringResource(R.string.agua_actual), style = tipos.bodySmall, color = Color.White.copy(alpha = 0.85f))
                    Text("$actualMl ml", style = tipos.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }
            // el «+» suma un vaso; mantenido, abre el abanico de cantidades (sorbo, botella y quitar un vaso) desde su esquina
            BotonConAbanico(
                acciones = acciones,
                onAccion = { onAgregar(CANTIDADES_ABANICO[it]) },
                onClick = { onAgregar(VASO_ML) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
                habilitado = actualMl < metaMl,
                etiquetaMantener = stringResource(R.string.agua_mas_cantidades),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_agregar),
                    contentDescription = stringResource(R.string.agua_agregar),
                    modifier = Modifier.size(32.dp),
                    tint = colores.secondary,
                )
            }
        }
    }
}

// ---- Calorías y sus estadísticas

@Composable
private fun CaloriasScreen(calorias: Int, metaKcal: Int, onEstadisticas: () -> Unit, onAjustes: () -> Unit, onVolver: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    var rango by rememberSaveable { mutableStateOf(Rango.DIA) }
    val serie = seriesActividad.getValue(rango)
    // al cambiar de rango la selección vuelve a un punto intermedio
    var seleccion by remember(rango) { mutableIntStateOf((serie.valores.size * 0.7f).toInt()) }
    val faltan = (metaKcal - calorias).coerceAtLeast(0)
    PantallaMetrica(stringResource(R.string.metrica_calorias), onVolver, onAjustes = onAjustes) {
        CifraMetrica(
            icono = R.drawable.ic_llama_solida,
            colorIcono = colores.primary,
            valor = String.format(Locale.getDefault(), "%,d", calorias),
            unidad = "kcal",
            subtitulo = if (faltan > 0) stringResource(R.string.calorias_restantes, faltan) else stringResource(R.string.calorias_meta_cumplida),
        )
        PanelMetrica {
            GraficaKcal(serie.valores, seleccion, onSeleccion = { seleccion = it })
            Spacer(Modifier.height(16.dp))
            SelectorRango(rango, onRango = { rango = it })
        }
        Spacer(Modifier.height(12.dp))
        FilaAjuste(icono = R.drawable.ic_llama, titulo = stringResource(R.string.calorias_estadisticas), onClick = onEstadisticas)
    }
}

@Composable
private fun EstadisticasScreen(onVolver: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    // el verde es el de las verduras del plan: la paleta de la app no tiene otro
    val tonos = listOf(colores.onBackground, colores.primary, colores.secondary, Color(GrupoAlimento.VERDURAS.color))
    val mes = remember { LocalDate.now().month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar { it.uppercase() } }
    PantallaMetrica(stringResource(R.string.calorias_estadisticas), onVolver) {
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.estadisticas_macros), style = tipos.titleLarge.copy(fontWeight = FontWeight.Bold), color = colores.onBackground)
            Text(
                text = mes,
                modifier = Modifier.background(colores.surfaceVariant, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
                style = tipos.labelLarge,
                color = colores.onSurfaceVariant,
            )
        }
        Row(Modifier.fillMaxWidth().height(300.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            macros.forEachIndexed { i, macro ->
                BarraVertical(
                    // el 40 % llena casi toda la barra; el resto, en proporción
                    fraccion = macro.porcentaje / 40f * 0.78f,
                    pista = tonos[i].copy(alpha = 0.12f),
                    relleno = tonos[i],
                    icono = macro.icono,
                    colorIcono = tonos[i].copy(alpha = 0.55f),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    Text("${macro.porcentaje}%", style = tipos.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = Color.White)
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
            macros.forEachIndexed { i, macro ->
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(tonos[i]))
                    Text(stringResource(macro.nombre), Modifier.weight(1f), style = tipos.bodyMedium, color = colores.onBackground)
                    Text("${macro.gramos} g", style = tipos.titleMedium, color = colores.onBackground)
                }
                if (i < macros.lastIndex) HorizontalDivider(color = colores.outlineVariant)
            }
        }
    }
}

// ---- IMC: el mapa de calor de las últimas semanas

@Composable
private fun ImcScreen(onVolver: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    var rango by rememberSaveable { mutableStateOf(Rango.MES) }
    val dias = remember { DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()).uppercase() } }
    // el rango decide cuántas semanas se ven: del día de hoy (la semana en curso) al mes entero
    val semanas = when (rango) {
        Rango.DIA -> 1
        Rango.SEMANA -> 2
        else -> mapaImc.size / 7
    }
    PantallaMetrica(stringResource(R.string.imc_titulo), onVolver) {
        CifraMetrica(
            icono = R.drawable.ic_corredor,
            colorIcono = colores.onBackground,
            valor = String.format(Locale.getDefault(), "%.1f", IMC_MUESTRA),
            unidad = "pt",
            subtitulo = stringResource(R.string.imc_estado_sobrepeso),
        )
        PanelMetrica {
            SelectorRango(rango, onRango = { rango = it })
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                dias.forEach { dia ->
                    Text(dia, Modifier.weight(1f), style = tipos.labelLarge, color = colores.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                mapaImc.takeLast(semanas * 7).chunked(7).forEach { semana ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        semana.forEach { nivel ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colorCeldaImc(nivel, colores, Color.White)),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LeyendaImc(colores.primary, R.string.imc_leyenda_sobrepeso)
                LeyendaImc(colores.secondary, R.string.imc_leyenda_bajo)
                LeyendaImc(colores.onBackground, R.string.imc_leyenda_normal)
            }
        }
    }
}

@Composable
private fun LeyendaImc(color: Color, @StringRes texto: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(stringResource(texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---- Pasos: los anillos y sus tres datos

// los metros que da un paso, para la meta de kilómetros que sale de la de pasos
private const val KM_POR_PASO = 0.00075f

@Composable
private fun PasosScreen(metas: MetasDiarias, onAjustes: () -> Unit, onVolver: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    val pasos = LocalPasos.current
    val metaKm = metas.pasos * KM_POR_PASO
    // cada anillo (de fuera a dentro: pasos, calorías, movimiento y distancia) es lo que lleva de su meta
    val avances = listOf(pasos.total / metas.pasos.toFloat(), pasos.kcal / metas.kcal.toFloat(), pasos.minutos / metas.minutos.toFloat(), pasos.km / metaKm)
    // los anillos giran como un dial bajo el dedo (los de dentro más) y al soltarlos vuelven a su sitio con un resorte que se pasa
    val giro = remember { Animatable(0f) }
    val alcance = rememberCoroutineScope()
    PantallaMetrica(stringResource(R.string.metrica_pasos), onVolver, onAjustes = onAjustes) {
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(260.dp)
                .pointerInput(Unit) {
                    var anterior = 0f
                    val angulo = { p: Offset -> Math.toDegrees(atan2((p.y - size.height / 2f).toDouble(), (p.x - size.width / 2f).toDouble())).toFloat() }
                    val suelta = { alcance.launch { giro.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 90f)) }; Unit }
                    detectDragGestures(
                        onDragStart = {
                            anterior = angulo(it)
                            alcance.launch { giro.stop() }
                        },
                        onDragEnd = { suelta() },
                        onDragCancel = { suelta() },
                        onDrag = { cambio, _ ->
                            val actual = angulo(cambio.position)
                            var delta = actual - anterior
                            if (delta > 180f) delta -= 360f else if (delta < -180f) delta += 360f
                            anterior = actual
                            alcance.launch { giro.snapTo(giro.value + delta) }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            AnillosPasos(
                avances = avances,
                colores = listOf(colores.primary, colores.onBackground, colores.secondary, colores.onSurfaceVariant),
                pista = colores.surfaceVariant,
                grosor = 20.dp,
                separacion = 5.dp,
                modifier = Modifier.fillMaxSize(),
                giro = { giro.value },
            )
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(colores.secondary), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_corredor), contentDescription = null, modifier = Modifier.size(26.dp), tint = Color.White)
            }
        }
        Text(
            text = String.format(Locale.getDefault(), "%,d", pasos.total),
            modifier = Modifier.padding(top = 24.dp),
            style = tipos.headlineLarge.copy(fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold),
            color = colores.onBackground,
        )
        Text(stringResource(R.string.pasos_total), style = tipos.bodyMedium, color = colores.onSurfaceVariant)
        Text(
            text = stringResource(R.string.pasos_meta_total, String.format(Locale.getDefault(), "%,d", metas.pasos)),
            style = tipos.bodySmall,
            color = colores.outline,
        )
        Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DatoPasos(R.drawable.ic_llama_solida, colores.primary, pasos.kcal.toString(), R.string.pasos_kcal, String.format(Locale.getDefault(), "%,d", metas.kcal), Modifier.weight(1f))
            DatoPasos(R.drawable.ic_ubicacion, colores.onSurfaceVariant, String.format(Locale.getDefault(), "%.1f", pasos.km), R.string.pasos_km, String.format(Locale.getDefault(), "%.1f", metaKm), Modifier.weight(1f))
            DatoPasos(R.drawable.ic_reloj, colores.secondary, pasos.minutos.toString(), R.string.pasos_minutos, metas.minutos.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun DatoPasos(@DrawableRes icono: Int, fondo: Color, valor: String, @StringRes unidad: Int, meta: String, modifier: Modifier = Modifier) {
    val tipos = MaterialTheme.typography
    Column(
        modifier = modifier.clip(RoundedCornerShape(24.dp)).background(fondo).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
        Text(valor, style = tipos.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
        Text(stringResource(unidad), style = tipos.bodySmall, color = Color.White.copy(alpha = 0.9f), maxLines = 1)
        Text(stringResource(R.string.pasos_de_meta, meta), style = tipos.bodySmall.copy(fontSize = 11.sp), color = Color.White.copy(alpha = 0.7f), maxLines = 1)
    }
}

// ---- Score: lo que dejó la sesión

@Composable
private fun ScoreScreen(onVolver: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    val sesion = sesionMuestra
    PantallaMetrica(titulo = null, onVolver = onVolver) {
        Text(
            text = stringResource(R.string.score_titulo),
            modifier = Modifier.padding(top = 16.dp),
            style = tipos.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colores.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.score_subtitulo, sesion.scoreSumado),
            modifier = Modifier.padding(top = 8.dp),
            style = tipos.bodyMedium,
            color = colores.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp).height(300.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DatoSesion(0.32f, colores.surfaceVariant, colores.outlineVariant, R.drawable.ic_reloj, colores.onSurfaceVariant, sesion.minutos.toString(), "min", colores.onBackground, Modifier.weight(1f).fillMaxHeight())
            DatoSesion(0.52f, colores.secondaryContainer, colores.secondary, R.drawable.ic_llama_solida, colores.secondary, sesion.kcal.toString(), "kcal", Color.White, Modifier.weight(1f).fillMaxHeight())
            DatoSesion(0.76f, colores.primaryContainer, colores.primary, R.drawable.ic_montana, colores.primary, sesion.kg.toString(), "kg", Color.White, Modifier.weight(1f).fillMaxHeight())
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colores.surfaceVariant).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.score_sugerencia_titulo), style = tipos.titleMedium.copy(fontWeight = FontWeight.Bold), color = colores.onBackground)
                Text(stringResource(R.string.score_sugerencia_texto, sesion.sugerencias), style = tipos.bodySmall, color = colores.onSurfaceVariant)
            }
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(17.dp)).background(colores.primary), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_bombilla), contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.White)
            }
        }
        BotonPrimario(
            texto = stringResource(R.string.score_gracias),
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
        )
    }
}

@Composable
private fun DatoSesion(
    fraccion: Float,
    pista: Color,
    relleno: Color,
    @DrawableRes icono: Int,
    colorIcono: Color,
    valor: String,
    unidad: String,
    colorTexto: Color,
    modifier: Modifier = Modifier,
) {
    val tipos = MaterialTheme.typography
    BarraVertical(fraccion, pista, relleno, icono, colorIcono, modifier) {
        Text(valor, style = tipos.titleLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold), color = colorTexto, maxLines = 1)
        Text(unidad, style = tipos.bodySmall, color = colorTexto, maxLines = 1)
    }
}
