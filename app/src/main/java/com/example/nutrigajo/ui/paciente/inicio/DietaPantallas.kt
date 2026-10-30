package com.example.nutrigajo.ui.paciente.inicio

import android.graphics.ImageDecoder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.GrupoAlimento
import com.example.nutrigajo.ui.components.BarraEstadoClara
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.SelectorPestanas
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.components.cristal
import com.example.nutrigajo.ui.theme.color
import com.example.nutrigajo.ui.theme.negro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

// Las pantallas de «Dieta & nutrición»: Mis comidas (por día y tipo), la comida con sus flechas y su detalle con la receta, agregar
// una comida a mano y el escáner con IA con su análisis. Todo es de muestra (DatosMuestra.kt): el plan de comidas es el mismo
// cada día, lo que se agrega a mano vive mientras la app esté abierta y el escáner no abre la cámara ni analiza nada; «reconoce»
// siempre la misma fruta.
private enum class Paso { MIS_COMIDAS, VISTA, DETALLE, AGREGAR, ESCANER, ANALISIS, NUTRIENTES }

private val TIPOS = TipoComida.entries.map { it.etiqueta }

// Maneja sus propias pantallas con una pila (guardada como texto para sobrevivir a un giro): atrás saca la de arriba y, con
// una sola, sale. `inicio` es la comida (índice en `platos`) que se abre desde la tarjeta del inicio, o -1 para Mis comidas.
@Composable
fun DietaFlujo(inicio: Int, agregadas: List<Comida>, onAgregar: (Comida) -> Unit, onSalir: () -> Unit) {
    var pila by rememberSaveable { mutableStateOf(if (inicio < 0) Paso.MIS_COMIDAS.name else Paso.VISTA.name) }
    var indice by rememberSaveable { mutableIntStateOf(inicio.coerceAtLeast(0)) }
    var diaElegido by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    // la pestaña de Mis comidas vive aquí para que, al agregar una comida, se vuelva a la de su tipo y se vea
    var tipo by rememberSaveable { mutableIntStateOf(0) }
    var aviso by rememberSaveable { mutableStateOf(false) }
    val pasos = pila.split(",").map { Paso.valueOf(it) }
    fun ir(paso: Paso) {
        pila = "$pila,${paso.name}"
    }
    fun volver() {
        if (pasos.size == 1) onSalir() else pila = pasos.dropLast(1).joinToString(",") { it.name }
    }
    BackHandler { volver() }

    val dia = LocalDate.ofEpochDay(diaElegido)
    val todas = platos + agregadas
    // agregar una comida la deja en el día elegido y avisa; al cerrar el aviso se vuelve a Mis comidas
    fun agregar(comida: Comida) {
        onAgregar(comida.copy(dia = dia))
        tipo = comida.tipo.ordinal
        aviso = true
    }
    when (pasos.last()) {
        Paso.MIS_COMIDAS -> MisComidas(
            comidas = todas,
            dia = dia,
            tipo = tipo,
            onTipo = { tipo = it },
            onDia = { diaElegido = it.toEpochDay() },
            onComida = {
                indice = todas.indexOf(it)
                ir(Paso.VISTA)
            },
            onAgregar = { ir(Paso.AGREGAR) },
            onVolver = { volver() },
        )
        Paso.VISTA -> VistaComida(todas, indice.coerceIn(todas.indices), onCambiar = { indice = it }, onDetalles = { ir(Paso.DETALLE) }, onVolver = { volver() })
        Paso.DETALLE -> DetalleComida(todas[indice.coerceIn(todas.indices)], onAgregar = ::agregar, onVolver = { volver() })
        Paso.AGREGAR -> AgregarComida(dia, onContinuar = ::agregar, onEscaner = { ir(Paso.ESCANER) }, onVolver = { volver() })
        Paso.ESCANER -> Escaner(onFoto = { ir(Paso.ANALISIS) }, onVolver = { volver() })
        Paso.ANALISIS -> Analisis(onNutrientes = { ir(Paso.NUTRIENTES) }, onVolver = { volver() })
        Paso.NUTRIENTES -> Nutrientes(onAgregar = { agregar(comidaEscaneada) }, onVolver = { volver() })
    }
    if (aviso) {
        ComidaAgregada(
            onCerrar = {
                aviso = false
                pila = Paso.MIS_COMIDAS.name
            },
        )
    }
}

// ---- Mis comidas

@Composable
private fun MisComidas(
    comidas: List<Comida>,
    dia: LocalDate,
    tipo: Int,
    onTipo: (Int) -> Unit,
    onDia: (LocalDate) -> Unit,
    onComida: (Comida) -> Unit,
    onAgregar: () -> Unit,
    onVolver: () -> Unit,
) {
    BarraEstadoClara(true)
    val colores = MaterialTheme.colorScheme
    val hoy = remember { LocalDate.now() }
    val dias = remember(hoy) { (-3L..3L).map { hoy.plusDays(it) } }
    // el plan se ve todos los días; lo agregado a mano, solo el día en que se agregó
    val lista = comidas.filter { it.tipo.ordinal == tipo && (it.dia == null || it.dia == dia) }
    Box(Modifier.fillMaxSize().background(colores.background)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(colores.negro)
                    .statusBarsPadding()
                    .padding(bottom = 20.dp),
            ) {
                CabeceraOscura(stringResource(R.string.dieta_mis_comidas), onVolver)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dias.forEach { DiaPildora(it, seleccionado = it == dia, modifier = Modifier.weight(1f), onClick = { onDia(it) }) }
                }
            }
            SelectorPestanas(TIPOS, tipo, onTipo, Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.dieta_todas), style = MaterialTheme.typography.titleMedium, color = colores.tertiary)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.dieta_cuantas, lista.size), style = MaterialTheme.typography.labelLarge, color = colores.outline)
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (lista.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.dieta_vacio),
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colores.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                items(lista) { FilaComida(it) { onComida(it) } }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .size(60.dp)
                .clip(CircleShape)
                .background(colores.primary)
                .alPulsar(onAgregar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_agregar), contentDescription = stringResource(R.string.dieta_agregar), modifier = Modifier.size(26.dp), tint = Color.White)
        }
    }
}

@Composable
private fun CabeceraOscura(titulo: String, onVolver: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        BotonAtras(onVolver, Modifier.align(Alignment.CenterStart), sobreFoto = true)
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun DiaPildora(dia: LocalDate, seleccionado: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .height(66.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (seleccionado) colores.primary else Color.White.copy(alpha = 0.12f))
            .alPulsar(onClick, minimo = 0.94f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            dia.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
            color = Color.White.copy(alpha = if (seleccionado) 1f else 0.6f),
            maxLines = 1,
        )
        Text(dia.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

// La foto de la comida: la que se eligió al agregarla o la de muestra, recortada en cuadro.
@Composable
private fun FotoComida(comida: Comida, modifier: Modifier = Modifier) {
    Box(modifier.background(Color.White)) {
        if (comida.miniatura != null) {
            Image(comida.miniatura, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Image(painterResource(comida.foto), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

// «P 25g · C 12g · G 17g»: las iniciales de proteína, carbos y grasas
@Composable
private fun resumenMacros(comida: Comida): String {
    val p = stringResource(R.string.dieta_proteina).first()
    val c = stringResource(R.string.dieta_carbos).first()
    val g = stringResource(R.string.dieta_grasas).first()
    return "$p ${comida.proteinaG}g · $c ${comida.carbosG}g · $g ${comida.grasasG}g"
}

@Composable
private fun FilaComida(comida: Comida, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alPulsar(onClick, minimo = 0.97f)
            .clip(RoundedCornerShape(24.dp))
            .background(colores.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FotoComida(comida, Modifier.size(88.dp).clip(RoundedCornerShape(18.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(comida.titulo, style = tipos.titleMedium, color = colores.tertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DatoIcono(R.drawable.ic_llama_solida, stringResource(R.string.rutina_kcal, comida.kcal))
                if (comida.minutos > 0) DatoIcono(R.drawable.ic_reloj, stringResource(R.string.dieta_minutos, comida.minutos))
            }
            Text(resumenMacros(comida), style = tipos.bodySmall, color = colores.onSurfaceVariant, maxLines = 1)
        }
        Flecha(derecha = true, tinta = colores.onSurfaceVariant)
    }
}

@Composable
private fun DatoIcono(icono: Int, texto: String) {
    val colores = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(15.dp), tint = colores.onSurfaceVariant)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant, maxLines = 1)
    }
}

// el chevron del diseño apunta a la izquierda; a la derecha es el mismo girado
@Composable
private fun Flecha(derecha: Boolean, tinta: Color, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_izquierda),
        contentDescription = null,
        modifier = modifier.graphicsLayer { rotationZ = if (derecha) 180f else 0f },
        tint = tinta,
    )
}

@Composable
private fun BotonAtras(onVolver: () -> Unit, modifier: Modifier = Modifier, sobreFoto: Boolean = false) {
    val colores = MaterialTheme.colorScheme
    val fondo = if (sobreFoto) Modifier.cristal(CircleShape) else Modifier.background(colores.surfaceVariant, CircleShape)
    Box(modifier.size(40.dp).then(fondo).clip(CircleShape).alPulsar(onVolver), contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(R.drawable.ic_chevron_izquierda),
            contentDescription = stringResource(R.string.mapa_volver),
            tint = if (sobreFoto) Color.White else colores.onSurfaceVariant,
        )
    }
}

// ---- La comida, con las flechas para pasar a la anterior y la siguiente

@Composable
private fun VistaComida(comidas: List<Comida>, indice: Int, onCambiar: (Int) -> Unit, onDetalles: () -> Unit, onVolver: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    val n = comidas.size
    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        BotonAtras(onVolver)
        Box(Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp)) {
            AnimatedContent(
                targetState = indice,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) },
                modifier = Modifier.fillMaxSize(),
                label = "comida",
            ) { i ->
                val comida = comidas[i]
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    FotoComida(comida, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(36.dp)))
                    Spacer(Modifier.height(20.dp))
                    EtiquetaTipo(stringResource(comida.tipo.etiqueta))
                    Spacer(Modifier.height(10.dp))
                    Text(comida.titulo, style = tipos.headlineMedium, color = colores.onBackground, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(resumenMacros(comida), style = tipos.bodyMedium, color = colores.onSurfaceVariant)
                }
            }
            // las flechas van sobre la foto, a los lados; al llegar al final se da la vuelta
            Box(
                Modifier.align(Alignment.TopStart).padding(top = 150.dp, start = 8.dp).size(40.dp).clip(CircleShape).cristal(CircleShape).alPulsar(onClick = { onCambiar((indice - 1 + n) % n) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_chevron_izquierda), stringResource(R.string.dieta_anterior), tint = colores.onBackground)
            }
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 150.dp, end = 8.dp).size(40.dp).clip(CircleShape).cristal(CircleShape).alPulsar(onClick = { onCambiar((indice + 1) % n) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_chevron_izquierda),
                    stringResource(R.string.dieta_siguiente),
                    modifier = Modifier.graphicsLayer { rotationZ = 180f },
                    tint = colores.onBackground,
                )
            }
        }
        BotonPrimario(stringResource(R.string.dieta_ver_detalles), onDetalles, Modifier.fillMaxWidth())
    }
}

@Composable
private fun EtiquetaTipo(texto: String) {
    val colores = MaterialTheme.colorScheme
    Text(
        texto,
        modifier = Modifier.background(colores.primaryContainer, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
        color = colores.primary,
    )
}

// ---- El detalle: resumen, ingredientes y nutrientes, o la receta paso a paso

@Composable
private fun DetalleComida(comida: Comida, onAgregar: (Comida) -> Unit, onVolver: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(colores.background)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(260.dp).background(colores.primaryContainer)) {
                FotoComida(comida, Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).size(190.dp).clip(CircleShape))
                BotonAtras(onVolver, Modifier.statusBarsPadding().padding(start = 16.dp, top = 10.dp))
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                EtiquetaTipo(stringResource(comida.tipo.etiqueta))
                Text(comida.titulo, style = tipos.headlineMedium, color = colores.onBackground)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (comida.minutos > 0) DatoIcono(R.drawable.ic_reloj, stringResource(R.string.dieta_minutos, comida.minutos))
                    DatoIcono(R.drawable.ic_llama_solida, stringResource(R.string.rutina_kcal, comida.kcal))
                }
                SelectorPestanas(listOf(R.string.dieta_detalles, R.string.dieta_receta), pestana, { pestana = it })
                if (pestana == 0) {
                    if (comida.resumen.isNotBlank()) {
                        Seccion(R.string.dieta_resumen) { Text(comida.resumen, style = tipos.bodyMedium, color = colores.onSurfaceVariant) }
                    }
                    if (comida.ingredientes.isNotEmpty()) {
                        Seccion(R.string.dieta_ingredientes) {
                            comida.ingredientes.forEach { ingrediente ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(painterResource(R.drawable.ic_verificado), contentDescription = null, modifier = Modifier.size(18.dp), tint = colores.secondary)
                                    Text(ingrediente, style = tipos.bodyMedium, color = colores.tertiary)
                                }
                            }
                        }
                    }
                    Seccion(R.string.dieta_nutrientes) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Nutriente(stringResource(R.string.dieta_calorias), comida.kcal.toString(), Modifier.weight(1f))
                            Nutriente(stringResource(R.string.dieta_proteina), "${comida.proteinaG}g", Modifier.weight(1f))
                            Nutriente(stringResource(R.string.dieta_carbos), "${comida.carbosG}g", Modifier.weight(1f))
                            Nutriente(stringResource(R.string.dieta_grasas), "${comida.grasasG}g", Modifier.weight(1f))
                        }
                    }
                } else {
                    Seccion(R.string.dieta_preparacion) {
                        comida.pasos.forEachIndexed { i, (titulo, texto) -> PasoReceta(i + 1, titulo, texto) }
                    }
                }
            }
        }
        BotonPrimario(
            stringResource(R.string.dieta_agregar),
            { onAgregar(comida) },
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun Seccion(titulo: Int, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        contenido()
    }
}

@Composable
private fun Nutriente(nombre: String, valor: String, modifier: Modifier) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(colores.surfaceVariant).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(valor, style = MaterialTheme.typography.titleMedium, color = colores.onBackground, maxLines = 1)
        Text(nombre, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant, maxLines = 1)
    }
}

// Un paso de la receta: se marca al tocarlo, con la casilla naranja del diseño.
@Composable
private fun PasoReceta(numero: Int, titulo: String, texto: String) {
    val colores = MaterialTheme.colorScheme
    var hecho by rememberSaveable(titulo) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().alPulsar({ hecho = !hecho }, minimo = 0.98f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (hecho) colores.primary else colores.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (hecho) {
                Icon(painterResource(R.drawable.ic_verificado), contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
            } else {
                Text(numero.toString(), style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp), color = colores.primary)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
        }
    }
}

// ---- Agregar una comida a mano

@Composable
private fun AgregarComida(dia: LocalDate, onContinuar: (Comida) -> Unit, onEscaner: () -> Unit, onVolver: () -> Unit) {
    BarraEstadoClara(true)
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var nombre by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableIntStateOf(0) }
    var proteina by rememberSaveable { mutableFloatStateOf(20f) }
    var carbos by rememberSaveable { mutableFloatStateOf(40f) }
    var kcal by rememberSaveable { mutableFloatStateOf(400f) }
    var notas by rememberSaveable { mutableStateOf("") }
    var sugerir by rememberSaveable { mutableStateOf(true) }
    var miniatura by remember { mutableStateOf<ImageBitmap?>(null) }
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            alcance.launch {
                miniatura = withContext(Dispatchers.IO) {
                    try {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contexto.contentResolver, uri)) { decoder, info, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            // con 600 px de lado sobra para una miniatura
                            decoder.setTargetSampleSize(max(1, max(info.size.width, info.size.height) / 600))
                        }.asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().background(colores.background)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(colores.negro)
                .statusBarsPadding()
                .padding(bottom = 18.dp),
        ) {
            CabeceraOscura(stringResource(R.string.dieta_agregar), onVolver)
            // «Escáner IA» abre el escáner; al volver sigue en «Manual»
            SelectorPestanas(listOf(R.string.dieta_manual, R.string.dieta_escaner), 0, { if (it == 1) onEscaner() }, Modifier.padding(horizontal = 16.dp))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CampoTexto(stringResource(R.string.dieta_nombre), nombre, { nombre = it }, modifier = Modifier.fillMaxWidth())
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.dieta_tipo), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
                SelectorPestanas(TIPOS, tipo, { tipo = it })
            }
            Deslizador(R.string.dieta_proteina_total, proteina, 0f..100f, "g") { proteina = it }
            Deslizador(R.string.dieta_carbos_total, carbos, 0f..200f, "g") { carbos = it }
            Deslizador(R.string.dieta_calorias_total, kcal, 0f..1500f, "kcal") { kcal = it }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.dieta_miniatura), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
                val trazo = colores.outline
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .drawBehind {
                            drawRoundRect(
                                color = trazo,
                                cornerRadius = CornerRadius(20.dp.toPx()),
                                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))),
                            )
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .alPulsar({ elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, minimo = 0.98f),
                    contentAlignment = Alignment.Center,
                ) {
                    val foto = miniatura
                    if (foto != null) {
                        Image(foto, contentDescription = stringResource(R.string.dieta_miniatura), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(painterResource(R.drawable.ic_agregar), contentDescription = stringResource(R.string.dieta_miniatura_agregar), modifier = Modifier.size(28.dp), tint = colores.outline)
                    }
                }
            }
            CampoTexto(stringResource(R.string.dieta_notas), notas, { notas = it }, modifier = Modifier.fillMaxWidth(), lineas = 3)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.dieta_sugerir), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
                Switch(
                    checked = sugerir,
                    onCheckedChange = { sugerir = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = colores.primary, checkedThumbColor = Color.White),
                )
            }
        }
        BotonPrimario(
            texto = stringResource(R.string.dieta_continuar),
            onClick = {
                val p = proteina.roundToInt()
                val c = carbos.roundToInt()
                val k = kcal.roundToInt()
                // el resto de las calorías son grasa (9 kcal por gramo)
                onContinuar(
                    Comida(
                        foto = R.drawable.inicio_comida,
                        titulo = nombre.trim(),
                        proteinaG = p,
                        grasasG = ((k - 4 * p - 4 * c) / 9).coerceAtLeast(0),
                        kcal = k,
                        carbosG = c,
                        tipo = TipoComida.entries[tipo],
                        resumen = notas.trim(),
                        miniatura = miniatura,
                        dia = dia,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            enabled = nombre.isNotBlank(),
        )
    }
}

@Composable
private fun Deslizador(titulo: Int, valor: Float, rango: ClosedFloatingPointRange<Float>, unidad: String, onCambio: (Float) -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(titulo), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
            Text("${valor.roundToInt()} $unidad", style = MaterialTheme.typography.titleSmall, color = colores.primary)
        }
        Slider(
            value = valor,
            onValueChange = onCambio,
            valueRange = rango,
            colors = SliderDefaults.colors(
                thumbColor = colores.primary,
                activeTrackColor = colores.primary,
                inactiveTrackColor = colores.surfaceVariant,
            ),
        )
        Row {
            Text("${rango.start.roundToInt()}", style = MaterialTheme.typography.bodySmall, color = colores.outline)
            Spacer(Modifier.weight(1f))
            Text("${rango.endInclusive.roundToInt()}", style = MaterialTheme.typography.bodySmall, color = colores.outline)
        }
    }
}

// ---- El escáner con IA (de muestra: no abre la cámara) y su análisis

@Composable
private fun Escaner(onFoto: () -> Unit, onVolver: () -> Unit) {
    BarraEstadoClara(true)
    val colores = MaterialTheme.colorScheme
    var analizando by remember { mutableStateOf(false) }
    LaunchedEffect(analizando) {
        if (analizando) {
            delay(1400)
            onFoto()
        }
    }
    // la línea de escaneo sube y baja dentro del marco
    val linea = rememberInfiniteTransition(label = "escaner").animateFloat(
        initialValue = 0.04f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "linea",
    )
    val tinta = colores.primary
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Image(painterResource(comidaEscaneada.foto), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(270.dp, 330.dp)
                .drawBehind {
                    val trazo = 4.dp.toPx()
                    val largo = 38.dp.toPx()
                    // las cuatro esquinas del marco
                    fun esquina(x: Float, y: Float, dx: Float, dy: Float) {
                        drawLine(Color.White, Offset(x, y + dy * largo), Offset(x, y), trazo, StrokeCap.Round)
                        drawLine(Color.White, Offset(x, y), Offset(x + dx * largo, y), trazo, StrokeCap.Round)
                    }
                    esquina(0f, 0f, 1f, 1f)
                    esquina(size.width, 0f, -1f, 1f)
                    esquina(0f, size.height, 1f, -1f)
                    esquina(size.width, size.height, -1f, -1f)
                    val y = size.height * linea.value
                    drawLine(
                        brush = Brush.horizontalGradient(listOf(Color.Transparent, tinta, Color.Transparent)),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 3.dp.toPx(),
                    )
                },
        )
        BotonAtras(onVolver, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 16.dp, top = 10.dp), sobreFoto = true)
        Text(
            stringResource(if (analizando) R.string.dieta_analizando else R.string.dieta_escaneando),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp).cristal(RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 36.dp).fillMaxWidth().padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotonCristal(R.drawable.ic_imagen, stringResource(R.string.dieta_galeria)) { analizando = true }
            // el obturador: un anillo blanco con el botón dentro
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
                    .padding(7.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .alPulsar({ analizando = true }, minimo = 0.9f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_naranja), stringResource(R.string.dieta_tomar_foto), Modifier.size(30.dp), tint = colores.primary)
            }
            BotonCristal(R.drawable.ic_bombilla, stringResource(R.string.dieta_destello)) {}
        }
    }
}

@Composable
private fun BotonCristal(icono: Int, descripcion: String, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).cristal(CircleShape).clip(CircleShape).alPulsar(onClick), contentAlignment = Alignment.Center) {
        Icon(painterResource(icono), contentDescription = descripcion, modifier = Modifier.size(22.dp), tint = Color.White)
    }
}

@Composable
private fun Analisis(onNutrientes: () -> Unit, onVolver: () -> Unit) {
    BarraEstadoClara(true)
    val colores = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Image(painterResource(comidaEscaneada.foto), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.45f)))))
        BotonAtras(onVolver, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 16.dp, top = 10.dp), sobreFoto = true)
        Text(
            stringResource(R.string.dieta_analisis),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 20.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        // lo que «encontró» en la foto: cada etiqueta salta con un resorte, una tras otra
        EtiquetaFlotante(stringResource(R.string.dieta_etiqueta_vitamina_c), 0, Modifier.align(Alignment.TopStart).padding(start = 40.dp, top = 220.dp))
        EtiquetaFlotante(stringResource(R.string.dieta_proteina), 1, Modifier.align(Alignment.CenterStart).padding(start = 56.dp, top = 40.dp))
        EtiquetaFlotante(stringResource(R.string.dieta_etiqueta_potasio), 2, Modifier.align(Alignment.CenterEnd).padding(end = 44.dp, top = 120.dp))
        EtiquetaFlotante(stringResource(R.string.dieta_etiqueta_fibra), 3, Modifier.align(Alignment.CenterEnd).padding(end = 70.dp, bottom = 120.dp))
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(colores.background)
                .alPulsar(onNutrientes, minimo = 0.97f)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FotoComida(comidaEscaneada, Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f)) {
                Text(comidaEscaneada.titulo, style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
                Text(stringResource(R.string.rutina_kcal, comidaEscaneada.kcal), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
            }
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colores.primary), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_flecha), contentDescription = stringResource(R.string.dieta_ver), modifier = Modifier.size(20.dp), tint = Color.White)
            }
        }
    }
}

@Composable
private fun EtiquetaFlotante(texto: String, indice: Int, modifier: Modifier) {
    val colores = MaterialTheme.colorScheme
    val escala = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(250L + indice * 170L)
        escala.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
    }
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
            }
            .cristal(RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(colores.primary))
        Text(texto, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Color.White)
    }
}

@Composable
private fun Nutrientes(onAgregar: () -> Unit, onVolver: () -> Unit) {
    BarraEstadoClara(true)
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    Column(Modifier.fillMaxSize().background(colores.background)) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Image(painterResource(comidaEscaneada.foto), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            BotonAtras(onVolver, Modifier.statusBarsPadding().padding(start = 16.dp, top = 10.dp), sobreFoto = true)
        }
        Column(
            modifier = Modifier
                .offset(y = (-28).dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(colores.background)
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EtiquetaTipo(stringResource(R.string.dieta_fruta))
                EtiquetaTipo("185 g")
            }
            Column {
                Text(comidaEscaneada.titulo, style = tipos.headlineMedium.copy(fontWeight = FontWeight.Bold), color = colores.onBackground)
                Text(stringResource(R.string.dieta_buena), style = tipos.bodyMedium, color = colores.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BloqueMacro(R.drawable.ic_corredor, stringResource(R.string.dieta_proteina), "${comidaEscaneada.proteinaG}g", colores.negro, Color.White, Modifier.weight(1f))
                BloqueMacro(R.drawable.ic_llama_solida, stringResource(R.string.dieta_carbos), "${comidaEscaneada.carbosG}g", colores.primary, Color.White, Modifier.weight(1f))
                BloqueMacro(R.drawable.ic_corazon, stringResource(R.string.dieta_grasas), "${comidaEscaneada.grasasG}g", colores.surfaceVariant, colores.onBackground, Modifier.weight(1f))
            }
            BotonPrimario(stringResource(R.string.dieta_agregar), onAgregar, Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 10.dp))
        }
    }
}

@Composable
private fun BloqueMacro(icono: Int, nombre: String, valor: String, fondo: Color, tinta: Color, modifier: Modifier) {
    Column(
        modifier = modifier.height(120.dp).clip(RoundedCornerShape(22.dp)).background(fondo).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(20.dp), tint = tinta)
        Column {
            Text(valor, style = MaterialTheme.typography.titleLarge, color = tinta, maxLines = 1)
            Text(nombre, style = MaterialTheme.typography.bodySmall, color = tinta.copy(alpha = 0.75f), maxLines = 1)
        }
    }
}

// ---- El aviso de que la comida quedó agregada

@Composable
private fun ComidaAgregada(onCerrar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val verde = Color(GrupoAlimento.VERDURAS.color)
    Dialog(onDismissRequest = onCerrar) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(colores.background).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(verde.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_verificado), contentDescription = null, modifier = Modifier.size(28.dp), tint = verde)
            }
            Text(stringResource(R.string.dieta_agregada), style = MaterialTheme.typography.titleLarge, color = colores.onBackground, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.dieta_agregada_texto),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(18.dp)).background(colores.negro).alPulsar(onCerrar),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.dieta_gracias), style = MaterialTheme.typography.titleSmall, color = Color.White)
            }
        }
    }
}
