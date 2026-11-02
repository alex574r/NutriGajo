package com.example.nutrigajo.ui.shared.ejercicios

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
fun ProblemaEjercicio.mensaje(): Int = when (this) {
    ProblemaEjercicio.TITULO -> R.string.ejercicio_problema_titulo
    ProblemaEjercicio.DESCRIPCION -> R.string.ejercicio_problema_descripcion
    ProblemaEjercicio.PASOS -> R.string.ejercicio_problema_pasos
    ProblemaEjercicio.DURACION -> R.string.ejercicio_problema_duracion
    ProblemaEjercicio.KCAL -> R.string.ejercicio_problema_kcal
    ProblemaEjercicio.TEXTO_LARGO -> R.string.ejercicio_problema_texto_largo
    ProblemaEjercicio.FOTO -> R.string.ejercicio_problema_foto
}

// ---- foto

// La foto del ejercicio, de la caché o del Worker; null mientras llega o si no tiene.
@Composable
fun rememberFotoEjercicio(ejercicio: Ejercicio): ImageBitmap? {
    val cache = LocalContext.current.cacheDir
    val repo = remember { EjerciciosRepository() }
    val imagen by produceState<ImageBitmap?>(null, ejercicio.id, ejercicio.fotoVersion) {
        value = runCatching { repo.foto(ejercicio, cache)?.asImageBitmap() }.getOrNull()
    }
    return imagen
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
            ejercicio.fotoVersion != null -> Box(Modifier.matchParentSize().background(Color(0xFF2A2A2A)))
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

// El ejercicio completo, para el entrenador, el ADMIN que lo revisa y el paciente. `acciones` son los botones de abajo, según quién lo ve.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetalleEjercicio(
    ejercicio: Ejercicio,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    // texto fijo al final (el aviso a los pacientes)
    aviso: String? = null,
    acciones: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colores = MaterialTheme.colorScheme
    BackHandler(onBack = onVolver)
    Column(modifier.fillMaxSize().background(colores.background).statusBarsPadding()) {
        BarraSuperior(titulo = ejercicio.titulo, onVolver = onVolver)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = MARGEN),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FotoEjercicio(ejercicio, Modifier.fillMaxWidth().aspectRatio(16f / 10f), RoundedCornerShape(28.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ejercicio.estado != EstadoEjercicio.PUBLICADO) EtiquetaEstado(ejercicio.estado)
                Dato(stringResource(ejercicio.zona.titulo()))
                Dato(stringResource(ejercicio.tipo.titulo()))
                Dato(stringResource(ejercicio.nivel.titulo()))
                Dato(stringResource(ejercicio.equipo.titulo()))
                Dato(stringResource(R.string.ejercicio_minutos, ejercicio.duracionMin))
                if (ejercicio.kcal > 0) Dato(stringResource(R.string.ejercicio_calorias, ejercicio.kcal))
            }
            ejercicio.motivoRechazo?.let {
                Text(stringResource(R.string.ejercicio_motivo_rechazo, it), style = MaterialTheme.typography.bodyMedium, color = colores.error)
            }
            if (ejercicio.descripcion.isNotBlank()) {
                Text(ejercicio.descripcion, style = MaterialTheme.typography.bodyLarge, color = colores.onBackground)
            }
            if (ejercicio.pasos.isNotEmpty()) {
                Text(stringResource(R.string.ejercicio_como_hacerlo), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ejercicio.pasos.forEachIndexed { i, paso ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(28.dp).background(colores.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = colores.primary)
                            }
                            Text(paso, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colores.onBackground)
                        }
                    }
                }
            }
            if (ejercicio.consejos.isNotBlank()) Parrafo(R.string.ejercicio_consejos, ejercicio.consejos)
            if (ejercicio.contraindicaciones.isNotBlank()) Parrafo(R.string.ejercicio_contraindicaciones, ejercicio.contraindicaciones)
            if (ejercicio.autorNombre.isNotBlank()) {
                Text(stringResource(R.string.ejercicio_por, ejercicio.autorNombre), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
            }
            aviso?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant) }
            if (acciones == null) Box(Modifier.navigationBarsPadding().padding(bottom = MARGEN))
        }
        if (acciones != null) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(MARGEN), verticalArrangement = Arrangement.spacedBy(8.dp)) { acciones() }
        }
    }
}

@Composable
private fun Parrafo(@StringRes titulo: Int, texto: String) {
    val colores = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(titulo), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
    }
}
