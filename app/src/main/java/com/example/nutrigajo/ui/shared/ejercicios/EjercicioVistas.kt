package com.example.nutrigajo.ui.shared.ejercicios

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.EjerciciosRepository
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.EquipoEjercicio
import com.example.nutrigajo.domain.model.EstadoEjercicio
import com.example.nutrigajo.domain.model.NivelEjercicio
import com.example.nutrigajo.domain.model.ProblemaEjercicio
import com.example.nutrigajo.domain.model.TipoEjercicio
import com.example.nutrigajo.domain.model.ZonaCuerpo
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

@Composable
fun FotoEjercicio(ejercicio: Ejercicio, modifier: Modifier = Modifier, forma: Shape = RoundedCornerShape(24.dp)) {
    FotoOMarcador(rememberFotoEjercicio(ejercicio), modifier, forma)
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

// Una fila de la lista: foto, nombre, zona y nivel, duración y calorías. Con `mostrarEstado` lleva su estado (la lista del entrenador).
@Composable
fun TarjetaEjercicio(
    ejercicio: Ejercicio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarEstado: Boolean = false,
) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .hundible(forma)
            .clip(forma)
            .background(colores.surfaceVariant)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FotoEjercicio(ejercicio, Modifier.size(76.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = ejercicio.titulo,
                style = MaterialTheme.typography.titleMedium,
                color = colores.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(ejercicio.zona.titulo()) + " · " + stringResource(ejercicio.nivel.titulo()),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Estadistica(R.drawable.ic_reloj, stringResource(R.string.ejercicio_minutos, ejercicio.duracionMin))
                if (ejercicio.kcal > 0) Estadistica(R.drawable.ic_llama, stringResource(R.string.ejercicio_calorias, ejercicio.kcal))
            }
            if (mostrarEstado) EtiquetaEstado(ejercicio.estado, Modifier.padding(top = 4.dp))
        }
        Icon(painterResource(R.drawable.ic_flecha), contentDescription = null, modifier = Modifier.size(22.dp), tint = colores.primary)
    }
}

@Composable
private fun Estadistica(icono: Int, texto: String) {
    val colores = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(14.dp), tint = colores.primary)
        Text(texto, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant, maxLines = 1)
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
