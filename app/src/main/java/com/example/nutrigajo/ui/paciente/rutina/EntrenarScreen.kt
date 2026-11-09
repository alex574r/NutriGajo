package com.example.nutrigajo.ui.paciente.rutina

import com.example.nutrigajo.domain.model.Entrenamiento
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.RutinaDelDia
import com.example.nutrigajo.ui.components.BarraEstadoClara
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.Etiqueta
import com.example.nutrigajo.ui.shared.ejercicios.BotonVolverCristal
import com.example.nutrigajo.ui.shared.ejercicios.CabeceraOscura
import com.example.nutrigajo.ui.shared.ejercicios.DatosEnColumnas
import com.example.nutrigajo.ui.shared.ejercicios.FotoEjercicio
import com.example.nutrigajo.ui.shared.ejercicios.MediaEjercicio
import com.example.nutrigajo.ui.shared.ejercicios.PasosPager
import com.example.nutrigajo.ui.shared.ejercicios.Parrafo
import com.example.nutrigajo.ui.shared.ejercicios.intensidad
import com.example.nutrigajo.ui.shared.ejercicios.montadaSobreLaCabecera
import com.example.nutrigajo.ui.shared.ejercicios.rememberPasosEstado
import com.example.nutrigajo.ui.shared.ejercicios.titulo
import kotlinx.coroutines.delay
import java.util.Locale

private val MARGEN = 16.dp

private enum class Fase { INTRO, EJERCICIO, FIN }

// «Entrenar ahora» con el aspecto «Workout»: primero la cabecera con el enfoque del día, tiempo, calorías y nivel, y la lista de ejercicios;
// luego cada ejercicio con su cronómetro y sus instrucciones de una en una (pausa, anterior y siguiente; al llegar a cero pasa solo al
// que sigue) y al final lo que se hizo, que se guarda en el historial y, si se hizo al menos la mitad, suma el día a la racha. La pantalla
// no se apaga mientras dura. Las calorías son la suma de lo estimado de cada ejercicio que se terminó.
@Composable
fun EntrenarScreen(
    rutina: RutinaDelDia,
    onSalir: () -> Unit,
    // la misma instancia que el historial: al terminar se guarda ahí
    vm: EntrenamientosViewModel = viewModel(key = "entrenamientos"),
) {
    BackHandler(onBack = onSalir)
    val guardado by vm.estado.collectAsState()
    val colores = MaterialTheme.colorScheme
    val vista = LocalView.current
    DisposableEffect(Unit) {
        vista.keepScreenOn = true
        onDispose { vista.keepScreenOn = false }
    }

    val ejercicios = rutina.ejercicios
    val total = ejercicios.size
    var fase by rememberSaveable { mutableStateOf(Fase.INTRO) }
    var indice by rememberSaveable { mutableIntStateOf(0) }
    var restante by rememberSaveable { mutableIntStateOf(0) }
    var pausa by rememberSaveable { mutableStateOf(false) }
    var inicioMs by rememberSaveable { mutableLongStateOf(0L) }
    var terminoMs by rememberSaveable { mutableLongStateOf(0L) }
    var hechos by rememberSaveable { mutableIntStateOf(0) }

    fun irA(i: Int) {
        indice = i
        restante = ejercicios[i].duracionMin * 60
        pausa = false
    }

    fun siguiente() {
        hechos = maxOf(hechos, indice + 1)
        if (indice < total - 1) {
            irA(indice + 1)
        } else {
            terminoMs = System.currentTimeMillis()
            fase = Fase.FIN
        }
    }

    // el cronómetro: un segundo a la vez mientras no esté en pausa; a cero pasa al siguiente
    LaunchedEffect(fase, indice, pausa) {
        if (fase == Fase.EJERCICIO && !pausa) {
            while (restante > 0) {
                delay(1000)
                restante--
            }
            siguiente()
        }
    }
    BarraEstadoClara(sobreFoto = fase != Fase.FIN)

    Box(Modifier.fillMaxSize().background(colores.background)) {
        when (fase) {
            Fase.INTRO -> {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    CabeceraOscura(alto = 300.dp, foto = { MediaEjercicio(ejercicios.first(), Modifier.matchParentSize(), RectangleShape) }) {
                        Etiqueta(pluralStringResource(R.plurals.rutina_ejercicios, total, total).uppercase())
                        Text(
                            text = stringResource(rutina.enfoque.titulo()),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Text(stringResource(R.string.entrenar_titulo), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                    }
                    Column(
                        modifier = Modifier
                            .montadaSobreLaCabecera()
                            .fillMaxWidth()
                            .background(colores.background, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .padding(start = MARGEN, end = MARGEN, top = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        DatosEnColumnas(
                            listOf(
                                stringResource(R.string.ejercicio_minutos, rutina.minutos) to stringResource(R.string.ejercicio_tiempo),
                                stringResource(R.string.ejercicio_calorias, rutina.kcal) to stringResource(R.string.ejercicio_calorias_nombre),
                                stringResource(rutina.nivel.intensidad()) to stringResource(R.string.ejercicio_nivel),
                            ),
                        )
                        ejercicios.forEachIndexed { i, ejercicio -> FilaRutina(i, ejercicio) }
                        Text(stringResource(R.string.entrenar_aviso), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                        Spacer(Modifier.navigationBarsPadding().height(96.dp))
                    }
                }
                BotonVolverCristal(onSalir, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp))
                BarraInferior {
                    BotonPrimario(
                        texto = stringResource(R.string.entrenar_comenzar),
                        onClick = {
                            inicioMs = System.currentTimeMillis()
                            hechos = 0
                            vm.reiniciarGuardado()
                            irA(0)
                            fase = Fase.EJERCICIO
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Fase.EJERCICIO -> {
                val actual = ejercicios[indice]
                val duracion = actual.duracionMin * 60
                // cada ejercicio empieza en su primer paso
                key(indice) {
                    val pasos = rememberPasosEstado(actual.pasos.size)
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        CabeceraOscura(alto = 300.dp, foto = { MediaEjercicio(actual, Modifier.matchParentSize(), RectangleShape) }) {
                            Etiqueta(stringResource(R.string.entrenar_ejercicio_n, indice + 1, total).uppercase())
                            Text(
                                text = actual.titulo,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", restante / 60, restante % 60),
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp, fontFeatureSettings = "tnum"),
                                color = Color.White,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .montadaSobreLaCabecera()
                                .fillMaxWidth()
                                .background(colores.background, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                                .padding(start = MARGEN, end = MARGEN, top = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            LinearProgressIndicator(
                                progress = { if (duracion == 0) 1f else 1f - restante / duracion.toFloat() },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = colores.primary,
                                trackColor = colores.surfaceVariant,
                            )
                            if (actual.pasos.isNotEmpty()) PasosPager(actual.pasos, pasos)
                            if (actual.consejos.isNotBlank()) Parrafo(R.string.ejercicio_consejos, actual.consejos)
                            Spacer(Modifier.navigationBarsPadding().height(150.dp))
                        }
                    }
                }
                BotonVolverCristal(onSalir, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp))
                BarraInferior {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { irA(indice - 1) },
                            enabled = indice > 0,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(21.dp),
                        ) { Text(stringResource(R.string.entrenar_anterior)) }
                        OutlinedButton(
                            onClick = { pausa = !pausa },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(21.dp),
                        ) { Text(stringResource(if (pausa) R.string.entrenar_reanudar else R.string.entrenar_pausa)) }
                    }
                    BotonPrimario(
                        texto = stringResource(if (indice == total - 1) R.string.entrenar_terminar else R.string.entrenar_siguiente),
                        onClick = { siguiente() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Fase.FIN -> {
                val minutos = ((terminoMs - inicioMs) / 60_000).toInt().coerceIn(1, Entrenamiento.MAX_MINUTOS)
                val kcal = ejercicios.take(hechos).sumOf { it.kcal }.coerceAtMost(Entrenamiento.MAX_KCAL)
                val terminado = Entrenamiento(fecha = terminoMs, enfoque = rutina.enfoque, minutos = minutos, kcal = kcal, hechos = hechos, total = total)
                // al llegar aquí se guarda una vez (con ningún ejercicio hecho no hay entrenamiento que guardar)
                LaunchedEffect(Unit) { if (hechos >= 1) vm.guardar(terminado) }
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = MARGEN),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    ) {
                        Icon(painterResource(R.drawable.ic_verificado), contentDescription = null, modifier = Modifier.size(72.dp), tint = colores.primary)
                        Text(stringResource(R.string.entrenar_completada), style = MaterialTheme.typography.headlineLarge, color = colores.onBackground, textAlign = TextAlign.Center)
                        DatosEnColumnas(
                            listOf(
                                stringResource(R.string.ejercicio_minutos, minutos) to stringResource(R.string.entrenar_resumen_tiempo),
                                stringResource(R.string.ejercicio_calorias, kcal) to stringResource(R.string.entrenar_resumen_kcal),
                                hechos.toString() to pluralStringResource(R.plurals.entrenar_resumen_ejercicios, hechos),
                            ),
                        )
                        if (terminado.cuentaParaLaRacha) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(painterResource(R.drawable.ic_llama), contentDescription = null, modifier = Modifier.size(18.dp), tint = colores.primary)
                                Text(stringResource(R.string.entrenar_racha_hoy), style = MaterialTheme.typography.titleSmall, color = colores.primary)
                            }
                        }
                        val estadoGuardado = when {
                            guardado.errorGuardar != null -> stringResource(guardado.errorGuardar!!)
                            guardado.guardado -> stringResource(R.string.entrenar_guardado)
                            guardado.guardando -> stringResource(R.string.entrenar_guardando)
                            else -> null
                        }
                        estadoGuardado?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (guardado.errorGuardar != null) colores.error else colores.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(MARGEN)) {
                        BotonPrimario(texto = stringResource(R.string.entrenar_terminar), onClick = onSalir, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

// La barra de botones fija al pie, con un degradado que la separa del contenido que pasa por debajo.
@Composable
private fun androidx.compose.foundation.layout.BoxScope.BarraInferior(contenido: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val fondo = MaterialTheme.colorScheme.background
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, fondo, fondo)))
            .navigationBarsPadding()
            .padding(start = MARGEN, end = MARGEN, top = 24.dp, bottom = MARGEN),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = contenido,
    )
}

// Un ejercicio de la lista de la rutina, como los del diseño: su foto con un play, «Ejercicio 1», el nombre y cuánto dura.
@Composable
private fun FilaRutina(indice: Int, ejercicio: Ejercicio) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().background(colores.surfaceVariant, RoundedCornerShape(26.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            FotoEjercicio(ejercicio, Modifier.size(68.dp), RoundedCornerShape(18.dp))
            Box(Modifier.size(30.dp).background(colores.primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(16.dp), tint = colores.onPrimary)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.entrenar_ejercicio_tag, indice + 1),
                modifier = Modifier.background(colores.background, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                color = colores.onSurfaceVariant,
            )
            Text(
                text = ejercicio.titulo,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colores.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(painterResource(R.drawable.ic_reloj), contentDescription = null, modifier = Modifier.size(14.dp), tint = colores.onSurfaceVariant)
                Text(stringResource(R.string.ejercicio_minutos, ejercicio.duracionMin), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
            }
        }
    }
}
