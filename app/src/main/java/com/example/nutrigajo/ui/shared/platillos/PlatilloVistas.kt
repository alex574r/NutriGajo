package com.example.nutrigajo.ui.shared.platillos

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.PlatillosRepository
import com.example.nutrigajo.domain.model.EstadoPlatillo
import com.example.nutrigajo.domain.model.EtiquetaPlatillo
import com.example.nutrigajo.domain.model.Platillo
import com.example.nutrigajo.domain.model.ProblemaPlatillo
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.components.BarraEstadoClara
import com.example.nutrigajo.ui.components.Etiqueta
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.components.hundible
import com.example.nutrigajo.ui.nutriologo.plan.titulo
import com.example.nutrigajo.ui.shared.ejercicios.BotonVolverCristal
import com.example.nutrigajo.ui.shared.ejercicios.CabeceraOscura
import com.example.nutrigajo.ui.shared.ejercicios.DatosEnColumnas
import com.example.nutrigajo.ui.shared.ejercicios.Dato
import com.example.nutrigajo.ui.shared.ejercicios.FotoOMarcador
import com.example.nutrigajo.ui.shared.ejercicios.PasosPager
import com.example.nutrigajo.ui.shared.ejercicios.montadaSobreLaCabecera
import com.example.nutrigajo.ui.shared.ejercicios.rememberPasosEstado

private val MARGEN = 16.dp

// Las piezas de pantalla de un platillo, que usan «Mis platillos» del autor, la revisión del admin, la fila del inicio del paciente y
// Explorar. Son las de los ejercicios (EjercicioVistas.kt) con las cosas de una receta: macros, ingredientes y pasos de preparación.

// ---- textos de cada valor

@StringRes
fun EtiquetaPlatillo.titulo(): Int = when (this) {
    EtiquetaPlatillo.SIN_GLUTEN -> R.string.platillo_etiqueta_sin_gluten
    EtiquetaPlatillo.SIN_LACTOSA -> R.string.platillo_etiqueta_sin_lactosa
    EtiquetaPlatillo.VEGETARIANO -> R.string.platillo_etiqueta_vegetariano
    EtiquetaPlatillo.VEGANO -> R.string.platillo_etiqueta_vegano
    EtiquetaPlatillo.ALTO_PROTEINA -> R.string.platillo_etiqueta_alto_proteina
    EtiquetaPlatillo.BAJO_CARBOHIDRATOS -> R.string.platillo_etiqueta_bajo_carbohidratos
}

// Los estados dicen lo mismo que los de los ejercicios.
@StringRes
fun EstadoPlatillo.titulo(): Int = when (this) {
    EstadoPlatillo.BORRADOR -> R.string.ejercicio_estado_borrador
    EstadoPlatillo.EN_REVISION -> R.string.ejercicio_estado_en_revision
    EstadoPlatillo.PUBLICADO -> R.string.ejercicio_estado_publicado
    EstadoPlatillo.RECHAZADO -> R.string.ejercicio_estado_rechazado
    EstadoPlatillo.RETIRADO -> R.string.ejercicio_estado_retirado
}

@StringRes
fun ProblemaPlatillo.mensaje(): Int = when (this) {
    ProblemaPlatillo.NOMBRE -> R.string.platillo_problema_nombre
    ProblemaPlatillo.RESUMEN -> R.string.platillo_problema_resumen
    ProblemaPlatillo.INGREDIENTES -> R.string.platillo_problema_ingredientes
    ProblemaPlatillo.PASOS -> R.string.platillo_problema_pasos
    ProblemaPlatillo.MACROS -> R.string.platillo_problema_macros
    ProblemaPlatillo.KCAL -> R.string.platillo_problema_kcal
    ProblemaPlatillo.MINUTOS -> R.string.platillo_problema_minutos
    ProblemaPlatillo.ETIQUETAS -> R.string.platillo_problema_etiquetas
    ProblemaPlatillo.FOTO -> R.string.platillo_problema_foto
}

// «Receta de nutriólogo» o «Receta de entrenador»
@StringRes
private fun Rol.recetaDe(): Int = if (this == Rol.ENTRENADOR) R.string.platillo_receta_entrenador else R.string.platillo_receta_nutriologo

// ---- foto

// La foto del platillo, de la caché o del Worker; null mientras llega o si no tiene (entonces se ve el marcador gris).
@Composable
fun rememberFotoPlatillo(platillo: Platillo): ImageBitmap? {
    val cache = LocalContext.current.cacheDir
    val repo = remember { PlatillosRepository() }
    val imagen by produceState<ImageBitmap?>(null, platillo.id, platillo.fotoVersion) {
        value = runCatching { repo.foto(platillo, cache)?.asImageBitmap() }.getOrNull()
    }
    return imagen
}

// ---- piezas

@Composable
fun EtiquetaEstadoPlatillo(estado: EstadoPlatillo, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val (fondo, letra) = when (estado) {
        EstadoPlatillo.PUBLICADO -> colores.primaryContainer to colores.primary
        EstadoPlatillo.RECHAZADO -> colores.errorContainer to colores.error
        else -> colores.surfaceVariant to colores.onSurfaceVariant
    }
    Text(
        text = stringResource(estado.titulo()),
        modifier = modifier.background(fondo, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelLarge,
        color = letra,
        maxLines = 1,
    )
}

// La tarjeta del banco con el diseño de «Dieta & nutrición»: la foto como un plato que se sale por arriba y por la derecha, dos chips
// blancos con la proteína y las grasas, el nombre con las kcal abajo y el botón naranja con la flecha. Toda la tarjeta es tocable.
// Con `mostrarEstado` (las listas del autor y del admin) el estado ocupa el lugar de los chips de macros.
@Composable
fun TarjetaPlatillo(platillo: Platillo, onClick: () -> Unit, modifier: Modifier = Modifier, mostrarEstado: Boolean = false) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    val foto = rememberFotoPlatillo(platillo)
    Box(modifier.size(283.dp, 233.dp).alPulsar(onClick, minimo = 0.97f).clip(RoundedCornerShape(28.dp)).background(colores.surfaceVariant)) {
        FotoOMarcador(foto, Modifier.align(Alignment.TopEnd).offset(x = 48.dp, y = (-48.dp)).size(212.dp), CircleShape)
        Column(Modifier.align(Alignment.TopStart).padding(start = 22.dp, top = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (mostrarEstado) {
                EtiquetaEstadoPlatillo(platillo.estado)
            } else {
                ChipMacro(stringResource(R.string.platillo_gramos_corto, platillo.proteinaG), stringResource(R.string.platillo_proteina))
                ChipMacro(stringResource(R.string.platillo_gramos_corto, platillo.grasasG), stringResource(R.string.platillo_grasas))
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 22.dp, end = 90.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = platillo.nombre,
                style = tipos.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colores.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            DatoFila(R.drawable.ic_llama, stringResource(R.string.ejercicio_calorias, platillo.kcal))
        }
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp).size(58.dp).clip(RoundedCornerShape(20.dp)).background(colores.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_flecha), contentDescription = null, modifier = Modifier.size(24.dp), tint = colores.onPrimary)
        }
    }
}

// Un chip blanco con el valor y, debajo, qué mide («25g» «Proteína»).
@Composable
private fun ChipMacro(valor: String, nombre: String) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.background(colores.background, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(valor, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colores.onBackground, maxLines = 1)
        Text(nombre, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = colores.onSurfaceVariant, maxLines = 1)
    }
}

// La fila compacta de las listas: foto a la izquierda, nombre, momento y etiquetas, el tiempo y las kcal, y la flecha naranja.
@Composable
fun FilaPlatillo(platillo: Platillo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(34.dp)
    val foto = rememberFotoPlatillo(platillo)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(107.dp)
            .hundible(forma)
            .clip(forma)
            .background(colores.surfaceVariant)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 19.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FotoOMarcador(foto, Modifier.size(79.dp, 80.dp), RoundedCornerShape(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = platillo.nombre,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colores.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = (listOf(stringResource(platillo.momento.titulo())) + platillo.etiquetas.map { stringResource(it.titulo()) }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DatoFila(R.drawable.ic_reloj, stringResource(R.string.ejercicio_minutos, platillo.minutos))
                DatoFila(R.drawable.ic_llama, stringResource(R.string.ejercicio_calorias, platillo.kcal))
            }
        }
        Icon(painterResource(R.drawable.ic_flecha), contentDescription = null, modifier = Modifier.size(22.dp), tint = colores.primary)
    }
}

@Composable
private fun DatoFila(@DrawableRes icono: Int, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        Text(texto, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

// El platillo completo con el aspecto «Workout»: la cabecera oscura con su foto, el momento, el nombre y quién lo hizo, y debajo una hoja
// blanca con el resumen, el tiempo, las kcal y el momento, los macros, las etiquetas, los ingredientes y la preparación paso a paso.
// `acciones` son los botones fijos de abajo de quien lo gestiona (el autor y el ADMIN); `aviso` es el texto fijo del final.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetallePlatillo(
    platillo: Platillo,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    aviso: String? = null,
    acciones: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colores = MaterialTheme.colorScheme
    BackHandler(onBack = onVolver)
    BarraEstadoClara()
    val foto = rememberFotoPlatillo(platillo)
    val pasosEstado = rememberPasosEstado(platillo.pasos.size)
    Box(modifier.fillMaxSize().background(colores.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            CabeceraOscura(alto = 320.dp, foto = { FotoOMarcador(foto, Modifier.matchParentSize(), RectangleShape) }) {
                Etiqueta(stringResource(platillo.momento.titulo()).uppercase())
                Text(
                    text = platillo.nombre,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                val receta = stringResource(platillo.autorRol.recetaDe())
                Text(
                    text = if (platillo.autorNombre.isNotBlank()) stringResource(R.string.platillo_receta_de, receta, platillo.autorNombre) else receta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )
            }
            Column(
                modifier = Modifier
                    .montadaSobreLaCabecera()
                    .fillMaxWidth()
                    .background(colores.background, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .padding(start = MARGEN, end = MARGEN, top = 22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (platillo.resumen.isNotBlank()) {
                    Text(platillo.resumen, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 1.35.em), color = colores.onSurfaceVariant)
                }
                DatosEnColumnas(
                    listOf(
                        stringResource(R.string.ejercicio_minutos, platillo.minutos) to stringResource(R.string.platillo_tiempo),
                        stringResource(R.string.ejercicio_calorias, platillo.kcal) to stringResource(R.string.ejercicio_calorias_nombre),
                        stringResource(platillo.momento.titulo()) to stringResource(R.string.platillo_momento),
                    ),
                )
                DatosEnColumnas(
                    listOf(
                        stringResource(R.string.platillo_gramos, platillo.proteinaG) to stringResource(R.string.platillo_proteina),
                        stringResource(R.string.platillo_gramos, platillo.grasasG) to stringResource(R.string.platillo_grasas),
                        stringResource(R.string.platillo_gramos, platillo.carbosG) to stringResource(R.string.platillo_carbos),
                    ),
                )
                if (platillo.estado != EstadoPlatillo.PUBLICADO || platillo.etiquetas.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (platillo.estado != EstadoPlatillo.PUBLICADO) EtiquetaEstadoPlatillo(platillo.estado)
                        platillo.etiquetas.forEach { Dato(stringResource(it.titulo())) }
                    }
                }
                platillo.motivoRechazo?.let {
                    Text(stringResource(R.string.ejercicio_motivo_rechazo, it), style = MaterialTheme.typography.bodyMedium, color = colores.error)
                }
                if (platillo.ingredientes.isNotEmpty()) {
                    Text(stringResource(R.string.platillo_ingredientes), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        platillo.ingredientes.forEach { ingrediente ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                Box(Modifier.padding(top = 8.dp).size(6.dp).background(colores.primary, CircleShape))
                                Text(ingrediente, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.3.em), color = colores.onBackground)
                            }
                        }
                    }
                }
                if (platillo.pasos.isNotEmpty()) {
                    Text(stringResource(R.string.platillo_preparacion), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
                    PasosPager(platillo.pasos, pasosEstado)
                }
                aviso?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant) }
                // el pie deja libre la barra de botones fija y la de navegación
                Spacer(Modifier.navigationBarsPadding().height(if (acciones != null) 96.dp else 16.dp))
            }
        }
        BotonVolverCristal(onVolver, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp))
        if (acciones != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, colores.background, colores.background)))
                    .navigationBarsPadding()
                    .padding(start = MARGEN, end = MARGEN, top = 24.dp, bottom = MARGEN),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = acciones,
            )
        }
    }
}
