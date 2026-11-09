package com.example.nutrigajo.ui.shared.ejercicios

import androidx.compose.ui.unit.em
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.BarraEstadoClara
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import com.example.nutrigajo.domain.model.EnfoqueDia
import com.example.nutrigajo.domain.model.FuenteEjercicio
import com.example.nutrigajo.data.repository.CatalogoRepository
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.EjerciciosRepository
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.EquipoEjercicio
import com.example.nutrigajo.domain.model.EstadoEjercicio
import com.example.nutrigajo.domain.model.NivelEjercicio
import com.example.nutrigajo.domain.model.ProblemaClip
import com.example.nutrigajo.domain.model.ProblemaEjercicio
import com.example.nutrigajo.domain.model.TipoEjercicio
import com.example.nutrigajo.domain.model.ZonaCuerpo
import com.example.nutrigajo.ui.components.DeslizaParaVer
import com.example.nutrigajo.ui.components.Etiqueta
import com.example.nutrigajo.ui.components.cristal
import com.example.nutrigajo.ui.components.hundible
import com.example.nutrigajo.ui.nutriologo.plan.BarraSuperior
import com.example.nutrigajo.ui.nutriologo.plan.OpcionChip

private val MARGEN = 16.dp

// ---- textos de cada valor

@StringRes
fun ZonaCuerpo.titulo(): Int = when (this) {
    ZonaCuerpo.PECHO -> R.string.zona_pecho
    ZonaCuerpo.ESPALDA -> R.string.zona_espalda
    ZonaCuerpo.PIERNAS -> R.string.zona_piernas
    ZonaCuerpo.BRAZOS -> R.string.zona_brazos
    ZonaCuerpo.HOMBROS -> R.string.zona_hombros
    ZonaCuerpo.ABDOMEN -> R.string.zona_abdomen
    ZonaCuerpo.CUERPO_COMPLETO -> R.string.zona_cuerpo_completo
}

@StringRes
fun TipoEjercicio.titulo(): Int = when (this) {
    TipoEjercicio.FUERZA -> R.string.tipo_fuerza
    TipoEjercicio.CARDIO -> R.string.tipo_cardio
    TipoEjercicio.FLEXIBILIDAD -> R.string.tipo_flexibilidad
    TipoEjercicio.YOGA -> R.string.tipo_yoga
}

@StringRes
fun NivelEjercicio.titulo(): Int = when (this) {
    NivelEjercicio.PRINCIPIANTE -> R.string.nivel_principiante
    NivelEjercicio.INTERMEDIO -> R.string.nivel_intermedio
    NivelEjercicio.AVANZADO -> R.string.nivel_avanzado
}

@StringRes
fun EquipoEjercicio.titulo(): Int = when (this) {
    EquipoEjercicio.SIN_EQUIPO -> R.string.equipo_sin_equipo
    EquipoEjercicio.MANCUERNAS -> R.string.equipo_mancuernas
    EquipoEjercicio.BANDAS -> R.string.equipo_bandas
    EquipoEjercicio.BARRA -> R.string.equipo_barra
    EquipoEjercicio.MAQUINA -> R.string.equipo_maquina
}

@StringRes
fun EstadoEjercicio.titulo(): Int = when (this) {
    EstadoEjercicio.BORRADOR -> R.string.ejercicio_estado_borrador
    EstadoEjercicio.EN_REVISION -> R.string.ejercicio_estado_en_revision
    EstadoEjercicio.PUBLICADO -> R.string.ejercicio_estado_publicado
    EstadoEjercicio.RECHAZADO -> R.string.ejercicio_estado_rechazado
    EstadoEjercicio.RETIRADO -> R.string.ejercicio_estado_retirado
}

@StringRes
fun EnfoqueDia.titulo(): Int = when (this) {
    EnfoqueDia.PIERNAS -> R.string.rutina_enfoque_piernas
    EnfoqueDia.PECHO_Y_BRAZOS -> R.string.rutina_enfoque_pecho_brazos
    EnfoqueDia.ESPALDA_Y_HOMBROS -> R.string.rutina_enfoque_espalda_hombros
    EnfoqueDia.ABDOMEN_Y_CARDIO -> R.string.rutina_enfoque_abdomen_cardio
    EnfoqueDia.CUERPO_COMPLETO -> R.string.rutina_enfoque_cuerpo_completo
    EnfoqueDia.CARDIO_ACTIVO -> R.string.rutina_enfoque_cardio
    EnfoqueDia.MOVILIDAD -> R.string.rutina_enfoque_movilidad
}

// El nivel de un ejercicio dicho como la intensidad de una rutina.
@StringRes
fun NivelEjercicio.intensidad(): Int = when (this) {
    NivelEjercicio.PRINCIPIANTE -> R.string.rutina_nivel_suave
    NivelEjercicio.INTERMEDIO -> R.string.rutina_nivel_moderado
    NivelEjercicio.AVANZADO -> R.string.rutina_nivel_intenso
}

@StringRes
fun ProblemaEjercicio.mensaje(): Int = when (this) {
    ProblemaEjercicio.TITULO -> R.string.ejercicio_problema_titulo
    ProblemaEjercicio.DESCRIPCION -> R.string.ejercicio_problema_descripcion
    ProblemaEjercicio.PASOS -> R.string.ejercicio_problema_pasos
    ProblemaEjercicio.DURACION -> R.string.ejercicio_problema_duracion
    ProblemaEjercicio.KCAL -> R.string.ejercicio_problema_kcal
    ProblemaEjercicio.TEXTO_LARGO -> R.string.ejercicio_problema_texto_largo
    ProblemaEjercicio.FOTO -> R.string.ejercicio_problema_foto
}

@StringRes
fun ProblemaClip.mensaje(): Int = when (this) {
    ProblemaClip.TIPO -> R.string.ejercicio_clip_tipo
    ProblemaClip.SIN_DURACION -> R.string.ejercicio_clip_sin_duracion
    ProblemaClip.MUY_LARGO -> R.string.ejercicio_clip_largo
    ProblemaClip.MUY_PESADO -> R.string.ejercicio_clip_pesado
}

// ---- foto

// La foto del ejercicio, de la caché o del Worker; null mientras llega o si no tiene.
@Composable
fun rememberFotoEjercicio(ejercicio: Ejercicio): ImageBitmap? {
    if (ejercicio.fuente == FuenteEjercicio.CATALOGO) return rememberFotoCatalogo(ejercicio.id).imagen
    val cache = LocalContext.current.cacheDir
    val repo = remember { EjerciciosRepository() }
    val imagen by produceState<ImageBitmap?>(null, ejercicio.id, ejercicio.fotoVersion) {
        value = runCatching { repo.foto(ejercicio, cache)?.asImageBitmap() }.getOrNull()
    }
    return imagen
}

// La foto de un ejercicio del catálogo: `intentada` dice que ya se pidió (con o sin éxito), para distinguir «en camino» de «no hay».
private class FotoCatalogo(val imagen: ImageBitmap?, val intentada: Boolean)

@Composable
private fun rememberFotoCatalogo(id: String): FotoCatalogo {
    val cache = LocalContext.current.cacheDir
    val repo = remember { CatalogoRepository(directorio = cache) }
    val foto by produceState(FotoCatalogo(null, false), id) {
        value = FotoCatalogo(runCatching { repo.foto(id)?.asImageBitmap() }.getOrNull(), true)
    }
    return foto
}

// La foto de cada tipo que se ve cuando el ejercicio no tiene la suya (el marcador gris queda para el editor).
@DrawableRes
fun TipoEjercicio.imagenPorDefecto(): Int = when (this) {
    TipoEjercicio.FUERZA -> R.drawable.ej_pesas
    TipoEjercicio.CARDIO -> R.drawable.ej_cardio
    TipoEjercicio.FLEXIBILIDAD, TipoEjercicio.YOGA -> R.drawable.ej_yoga
}

@Composable
fun FotoEjercicio(ejercicio: Ejercicio, modifier: Modifier = Modifier, forma: Shape = RoundedCornerShape(24.dp)) {
    if (ejercicio.fuente == FuenteEjercicio.CATALOGO) {
        // mientras llega, el marcador gris; si no llega (sin red, sin foto), la del tipo
        val foto = rememberFotoCatalogo(ejercicio.id)
        if (foto.imagen == null && foto.intentada) {
            Image(painterResource(ejercicio.tipo.imagenPorDefecto()), contentDescription = null, modifier = modifier.clip(forma), contentScale = ContentScale.Crop)
        } else {
            FotoOMarcador(foto.imagen, modifier, forma)
        }
        return
    }
    val imagen = rememberFotoEjercicio(ejercicio)
    // con foto propia pendiente de llegar se ve el marcador; sin ella, la del tipo
    if (imagen == null && ejercicio.fotoVersion == null) {
        Image(painterResource(ejercicio.tipo.imagenPorDefecto()), contentDescription = null, modifier = modifier.clip(forma), contentScale = ContentScale.Crop)
    } else {
        FotoOMarcador(imagen, modifier, forma)
    }
}

// La imagen recortada a la forma o, sin ella, un marcador gris con el icono de imagen.
@Composable
fun FotoOMarcador(imagen: ImageBitmap?, modifier: Modifier = Modifier, forma: Shape = RoundedCornerShape(24.dp)) {
    val colores = MaterialTheme.colorScheme
    Box(modifier.clip(forma).background(colores.surfaceVariant), contentAlignment = Alignment.Center) {
        if (imagen != null) {
            Image(bitmap = imagen, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(painterResource(R.drawable.ic_imagen), contentDescription = null, modifier = Modifier.size(28.dp), tint = colores.onSurfaceVariant)
        }
    }
}

// ---- piezas

// Un dato corto del ejercicio (zona, nivel…) en una píldora gris.
@Composable
fun Dato(texto: String, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Text(
        text = texto,
        modifier = modifier.background(colores.surfaceVariant, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = colores.onSurfaceVariant,
        maxLines = 1,
    )
}

@Composable
fun EtiquetaEstado(estado: EstadoEjercicio, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val (fondo, letra) = when (estado) {
        EstadoEjercicio.PUBLICADO -> colores.primaryContainer to colores.primary
        EstadoEjercicio.RECHAZADO -> colores.errorContainer to colores.error
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

// Un grupo de opciones en fila desplazable. Con `conTodos` se puede quitar la elección (el filtro de Explorar); sin él siempre hay una.
@Composable
fun <E : Enum<E>> GrupoOpciones(
    titulo: String?,
    opciones: List<E>,
    elegida: E?,
    nombre: (E) -> Int,
    onElegir: (E?) -> Unit,
    modifier: Modifier = Modifier,
    conTodos: Boolean = false,
) {
    val colores = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        titulo?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = colores.onBackground) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (conTodos) OpcionChip(stringResource(R.string.explorar_todos), elegida == null) { onElegir(null) }
            opciones.forEach { OpcionChip(stringResource(nombre(it)), elegida == it) { onElegir(it) } }
        }
    }
}

// La tarjeta del banco, con el diseño de «Exercise Process»: foto de fondo con velo oscuro, la zona arriba a la izquierda y el tipo en
// una píldora de cristal a la derecha, el nombre grande, la duración y las calorías en chips de cristal y abajo «Desliza para ver»
// con el botón «Ver». Con `mostrarEstado` (la lista del entrenador) la píldora dice el estado y el tipo pasa a la etiqueta de la izquierda.
@Composable
fun TarjetaEjercicio(
    ejercicio: Ejercicio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarEstado: Boolean = false,
) {
    val forma = RoundedCornerShape(29.dp)
    val foto = rememberFotoEjercicio(ejercicio)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .hundible(forma)
            .clip(forma)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
    ) {
        when {
            foto != null -> Image(bitmap = foto, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            // la foto propia está en camino: un fondo oscuro, no la del tipo, para que no parpadee al llegar
            ejercicio.fotoVersion != null || ejercicio.fuente == FuenteEjercicio.CATALOGO -> Box(Modifier.matchParentSize().background(Color(0xFF2A2A2A)))
            else -> Image(
                painter = painterResource(ejercicio.tipo.imagenPorDefecto()),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0f, -0.2f),
            )
        }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.82f)))))
        val zona = stringResource(ejercicio.zona.titulo())
        Text(
            text = zona + " · " + stringResource(if (mostrarEstado) ejercicio.tipo.titulo() else ejercicio.nivel.titulo()),
            modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 22.dp, end = 130.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Etiqueta(
            texto = stringResource(if (mostrarEstado) ejercicio.estado.titulo() else ejercicio.tipo.titulo()).uppercase(),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = ejercicio.titulo,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatoCristal(R.drawable.ic_reloj, stringResource(R.string.ejercicio_minutos, ejercicio.duracionMin))
                if (ejercicio.kcal > 0) DatoCristal(R.drawable.ic_llama, stringResource(R.string.ejercicio_calorias, ejercicio.kcal))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DeslizaParaVer(stringResource(R.string.ejercicio_desliza), onCompletar = onClick, modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .height(52.dp)
                        .cristal(CircleShape)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onClick)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(R.string.ejercicio_ver), style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Icon(painterResource(R.drawable.ic_flecha), contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                }
            }
        }
    }
}

// La fila compacta de «Más ejercicios extra» del inicio: foto a la izquierda, nombre, zona y tipo, la duración y las calorías, y la
// flecha naranja. La tarjeta grande de arriba es la de Explorar.
@Composable
fun FilaEjercicio(ejercicio: Ejercicio, onClick: () -> Unit, modifier: Modifier = Modifier, nuevo: Boolean = false) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(34.dp)
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
        Box {
            FotoEjercicio(ejercicio, Modifier.size(79.dp, 80.dp), RoundedCornerShape(24.dp))
            // lo publicado hace poco lleva la insignia, como «New» del diseño
            if (nuevo) {
                Text(
                    text = stringResource(R.string.ejercicio_nuevo),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp)
                        .background(colores.secondary, RoundedCornerShape(7.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                    color = colores.onSecondary,
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = ejercicio.titulo,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colores.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(ejercicio.zona.titulo()) + " · " + stringResource(ejercicio.tipo.titulo()),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DatoFila(R.drawable.ic_reloj, stringResource(R.string.ejercicio_minutos, ejercicio.duracionMin))
                if (ejercicio.kcal > 0) DatoFila(R.drawable.ic_llama, stringResource(R.string.ejercicio_calorias, ejercicio.kcal))
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

// Un dato de la tarjeta (duración, calorías) en un chip de cristal con su icono.
@Composable
private fun DatoCristal(@DrawableRes icono: Int, texto: String) {
    Row(
        modifier = Modifier.cristal(RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
        Text(texto, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Color.White, maxLines = 1)
    }
}

// El ejercicio completo con el aspecto «Workout»: la cabecera oscura con su foto, la zona, el nombre y el autor, y debajo una hoja blanca
// con la descripción, el tiempo, las calorías y el nivel, y las instrucciones paso a paso. Con `onEmpezar` (el paciente) cada paso abre la
// guía en ese paso y el botón fijo de abajo la abre desde el primero; `acciones` son, en su lugar, los botones de quien lo gestiona
// (el entrenador y el ADMIN).
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetalleEjercicio(
    ejercicio: Ejercicio,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    // texto fijo al final (el aviso a los pacientes)
    aviso: String? = null,
    onEmpezar: ((paso: Int) -> Unit)? = null,
    acciones: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colores = MaterialTheme.colorScheme
    BackHandler(onBack = onVolver)
    BarraEstadoClara()
    val barraAbajo = onEmpezar != null || acciones != null
    Box(modifier.fillMaxSize().background(colores.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            CabeceraOscura(alto = 320.dp, foto = { MediaEjercicio(ejercicio, Modifier.matchParentSize(), RectangleShape) }) {
                Etiqueta(stringResource(ejercicio.zona.titulo()).uppercase())
                Text(
                    text = ejercicio.titulo,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (ejercicio.autorNombre.isNotBlank()) {
                    Text(stringResource(R.string.ejercicio_por, ejercicio.autorNombre), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                }
            }
            Column(
                modifier = Modifier
                    .montadaSobreLaCabecera()
                    .fillMaxWidth()
                    .background(colores.background, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .padding(start = MARGEN, end = MARGEN, top = 22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (ejercicio.descripcion.isNotBlank()) {
                    Text(ejercicio.descripcion, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 1.35.em), color = colores.onSurfaceVariant)
                }
                DatosEnColumnas(
                    listOf(
                        stringResource(R.string.ejercicio_minutos, ejercicio.duracionMin) to stringResource(R.string.ejercicio_tiempo),
                        (if (ejercicio.kcal > 0) stringResource(R.string.ejercicio_calorias, ejercicio.kcal) else "—") to stringResource(R.string.ejercicio_calorias_nombre),
                        stringResource(ejercicio.nivel.titulo()) to stringResource(R.string.ejercicio_nivel),
                    ),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ejercicio.estado != EstadoEjercicio.PUBLICADO) EtiquetaEstado(ejercicio.estado)
                    Dato(stringResource(ejercicio.tipo.titulo()))
                    Dato(stringResource(ejercicio.equipo.titulo()))
                }
                ejercicio.motivoRechazo?.let {
                    Text(stringResource(R.string.ejercicio_motivo_rechazo, it), style = MaterialTheme.typography.bodyMedium, color = colores.error)
                }
                if (ejercicio.pasos.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.ejercicio_instrucciones), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = pluralStringResource(R.plurals.ejercicio_n_pasos, ejercicio.pasos.size, ejercicio.pasos.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = colores.onSurfaceVariant,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ejercicio.pasos.forEachIndexed { i, paso ->
                            val forma = RoundedCornerShape(22.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(forma)
                                    .background(colores.surfaceVariant)
                                    .clickable(enabled = onEmpezar != null, role = Role.Button) { onEmpezar?.invoke(i) }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(Modifier.size(44.dp).background(colores.primaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = colores.primary)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(stringResource(R.string.ejercicio_paso_n, i + 1), style = MaterialTheme.typography.labelLarge, color = colores.onSurfaceVariant)
                                    Text(paso, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.3.em), color = colores.onBackground, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                                if (onEmpezar != null) {
                                    Icon(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(20.dp), tint = colores.primary)
                                }
                            }
                        }
                    }
                }
                if (ejercicio.consejos.isNotBlank()) Parrafo(R.string.ejercicio_consejos, ejercicio.consejos)
                if (ejercicio.contraindicaciones.isNotBlank()) Parrafo(R.string.ejercicio_contraindicaciones, ejercicio.contraindicaciones)
                aviso?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant) }
                // el pie deja libre la barra de botones fija y la de navegación
                Spacer(Modifier.navigationBarsPadding().height(if (barraAbajo) 96.dp else 16.dp))
            }
        }
        BotonVolverCristal(onVolver, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp))
        if (barraAbajo) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, colores.background, colores.background)))
                    .navigationBarsPadding()
                    .padding(start = MARGEN, end = MARGEN, top = 24.dp, bottom = MARGEN),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (acciones != null) {
                    acciones()
                } else {
                    BotonPrimario(stringResource(R.string.ejercicio_empezar), onClick = { onEmpezar?.invoke(0) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
internal fun Parrafo(@StringRes titulo: Int, texto: String) {
    val colores = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(titulo), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
        Text(texto, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.3.em), color = colores.onSurfaceVariant)
    }
}
