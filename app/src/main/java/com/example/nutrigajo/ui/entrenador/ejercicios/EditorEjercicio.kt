package com.example.nutrigajo.ui.entrenador.ejercicios

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.EquipoEjercicio
import com.example.nutrigajo.domain.model.NivelEjercicio
import com.example.nutrigajo.domain.model.TipoEjercicio
import com.example.nutrigajo.domain.model.ZonaCuerpo
import com.example.nutrigajo.domain.model.problemaParaRevision
import com.example.nutrigajo.domain.model.puedeGuardarseComoBorrador
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.nutriologo.plan.BarraSuperior
import com.example.nutrigajo.ui.shared.ejercicios.FotoEjercicio
import com.example.nutrigajo.ui.shared.ejercicios.FotoOMarcador
import com.example.nutrigajo.ui.shared.ejercicios.GrupoOpciones
import com.example.nutrigajo.ui.shared.ejercicios.mensaje
import com.example.nutrigajo.ui.shared.ejercicios.titulo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val MARGEN = 16.dp
private const val DURACION_POR_DEFECTO = 10
private const val LADO_VISTA_PREVIA = 800f

// Crear (id vacío) o editar un ejercicio propio. Un borrador solo pide el nombre; para mandarlo a revisión el resto tiene que estar
// completo y con foto. La foto elegida se sube al guardar.
@Composable
fun EditorEjercicio(
    inicial: Ejercicio,
    guardando: Boolean,
    @StringRes error: Int?,
    onGuardar: (ejercicio: Ejercicio, enviarARevision: Boolean, foto: Uri?) -> Unit,
    onVolver: () -> Unit,
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    var titulo by remember { mutableStateOf(inicial.titulo) }
    var descripcion by remember { mutableStateOf(inicial.descripcion) }
    var zona by remember { mutableStateOf(inicial.zona) }
    var tipo by remember { mutableStateOf(inicial.tipo) }
    var nivel by remember { mutableStateOf(inicial.nivel) }
    var equipo by remember { mutableStateOf(inicial.equipo) }
    var duracion by remember { mutableStateOf(inicial.duracionMin.toString()) }
    var kcal by remember { mutableStateOf(if (inicial.kcal > 0) inicial.kcal.toString() else "") }
    val pasos = remember { mutableStateListOf<String>().apply { addAll(inicial.pasos.ifEmpty { listOf("") }) } }
    var consejos by remember { mutableStateOf(inicial.consejos) }
    var contraindicaciones by remember { mutableStateOf(inicial.contraindicaciones) }
    var foto by remember { mutableStateOf<Uri?>(null) }
    // qué botón se tocó, para que el indicador de progreso salga en ese
    var enviando by remember { mutableStateOf(false) }
    val nuevo = inicial.id.isEmpty()

    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) foto = uri }
    val vistaPrevia = rememberVistaPrevia(foto)

    // lo escrito tal cual, para comprobar si ya se puede mandar a revisión (la foto elegida cuenta aunque aún no se haya subido)
    val escrito = inicial.copy(
        titulo = titulo,
        descripcion = descripcion,
        zona = zona,
        tipo = tipo,
        nivel = nivel,
        equipo = equipo,
        duracionMin = duracion.toIntOrNull() ?: 0,
        pasos = pasos.toList(),
        consejos = consejos,
        contraindicaciones = contraindicaciones,
        kcal = kcal.toIntOrNull() ?: 0,
        fotoVersion = inicial.fotoVersion ?: if (foto != null) 0L else null,
    )
    val problema = escrito.problemaParaRevision()

    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding().imePadding()) {
        BarraSuperior(
            titulo = stringResource(if (nuevo) R.string.ejercicio_form_nuevo else R.string.ejercicio_form_editar),
            onVolver = onVolver,
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = MARGEN - ANILLO_FOCO),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.padding(horizontal = ANILLO_FOCO), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.ejercicio_foto), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
                val forma = RoundedCornerShape(24.dp)
                when {
                    vistaPrevia != null -> FotoOMarcador(vistaPrevia, Modifier.fillMaxWidth().aspectRatio(16f / 10f), forma)
                    inicial.fotoVersion != null -> FotoEjercicio(inicial, Modifier.fillMaxWidth().aspectRatio(16f / 10f), forma)
                    else -> FotoOMarcador(null, Modifier.fillMaxWidth().aspectRatio(16f / 10f), forma)
                }
                OutlinedButton(
                    onClick = { elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    shape = RoundedCornerShape(21.dp),
                ) {
                    Text(stringResource(if (foto != null || inicial.fotoVersion != null) R.string.ejercicio_cambiar_foto else R.string.ejercicio_elegir_foto))
                }
            }
            CampoTexto(
                etiqueta = stringResource(R.string.ejercicio_titulo),
                valor = titulo,
                onValorChange = { titulo = it.take(Ejercicio.MAX_TITULO) },
                modifier = Modifier.fillMaxWidth(),
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            CampoTexto(
                etiqueta = stringResource(R.string.ejercicio_descripcion),
                valor = descripcion,
                onValorChange = { descripcion = it.take(Ejercicio.MAX_DESCRIPCION) },
                modifier = Modifier.fillMaxWidth(),
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                lineas = 4,
            )
            GrupoOpciones(stringResource(R.string.ejercicio_zona), ZonaCuerpo.entries, zona, ZonaCuerpo::titulo, { zona = it ?: zona }, Modifier.padding(horizontal = ANILLO_FOCO))
            GrupoOpciones(stringResource(R.string.ejercicio_tipo), TipoEjercicio.entries, tipo, TipoEjercicio::titulo, { tipo = it ?: tipo }, Modifier.padding(horizontal = ANILLO_FOCO))
            GrupoOpciones(stringResource(R.string.ejercicio_nivel), NivelEjercicio.entries, nivel, NivelEjercicio::titulo, { nivel = it ?: nivel }, Modifier.padding(horizontal = ANILLO_FOCO))
            GrupoOpciones(stringResource(R.string.ejercicio_equipo), EquipoEjercicio.entries, equipo, EquipoEjercicio::titulo, { equipo = it ?: equipo }, Modifier.padding(horizontal = ANILLO_FOCO))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto(
                    etiqueta = stringResource(R.string.ejercicio_duracion),
                    valor = duracion,
                    onValorChange = { duracion = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                CampoTexto(
                    etiqueta = stringResource(R.string.ejercicio_kcal),
                    valor = kcal,
                    onValorChange = { kcal = it.filter(Char::isDigit).take(4) },
                    modifier = Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            Text(
                text = stringResource(R.string.ejercicio_pasos),
                modifier = Modifier.padding(horizontal = ANILLO_FOCO),
                style = MaterialTheme.typography.titleSmall,
                color = colores.onBackground,
            )
            pasos.forEachIndexed { i, paso ->
                CampoTexto(
                    etiqueta = stringResource(R.string.ejercicio_paso_n, i + 1),
                    valor = paso,
                    onValorChange = { pasos[i] = it.take(Ejercicio.MAX_PASO) },
                    modifier = Modifier.fillMaxWidth(),
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    extremo = {
                        if (pasos.size > 1) {
                            IconButton(onClick = { pasos.removeAt(i) }) {
                                Icon(painterResource(R.drawable.ic_cerrar), stringResource(R.string.ejercicio_quitar_paso), tint = colores.onSurfaceVariant)
                            }
                        }
                    },
                    lineas = 2,
                )
            }
            if (pasos.size < Ejercicio.MAX_PASOS) {
                TextButton(onClick = { pasos.add("") }, Modifier.padding(horizontal = ANILLO_FOCO)) {
                    Text(stringResource(R.string.ejercicio_agregar_paso))
                }
            }
            CampoTexto(
                etiqueta = stringResource(R.string.ejercicio_consejos),
                valor = consejos,
                onValorChange = { consejos = it.take(Ejercicio.MAX_TEXTO) },
                modifier = Modifier.fillMaxWidth(),
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                lineas = 3,
            )
            CampoTexto(
                etiqueta = stringResource(R.string.ejercicio_contraindicaciones),
                valor = contraindicaciones,
                onValorChange = { contraindicaciones = it.take(Ejercicio.MAX_TEXTO) },
                modifier = Modifier.fillMaxWidth(),
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                lineas = 3,
            )
            error?.let { AlertaError(stringResource(it), Modifier.padding(horizontal = ANILLO_FOCO)) }
        }
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(MARGEN),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            problema?.let { Text(stringResource(it.mensaje()), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant) }
            BotonPrimario(
                texto = stringResource(R.string.ejercicio_enviar_revision),
                onClick = {
                    enviando = true
                    onGuardar(escrito.paraGuardar(), true, foto)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = problema == null && !guardando,
                cargando = guardando && enviando,
            )
            OutlinedButton(
                onClick = {
                    enviando = false
                    onGuardar(escrito.paraGuardar(), false, foto)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = escrito.puedeGuardarseComoBorrador() && !guardando,
                shape = RoundedCornerShape(21.dp),
            ) { Text(stringResource(R.string.ejercicio_guardar_borrador)) }
        }
    }
}

// Un borrador no tiene que estar completo, pero las reglas piden la duración y las calorías dentro de su rango.
private fun Ejercicio.paraGuardar(): Ejercicio = copy(
    duracionMin = if (duracionMin in Ejercicio.MIN_DURACION..Ejercicio.MAX_DURACION) duracionMin else if (duracionMin > 0) Ejercicio.MAX_DURACION else DURACION_POR_DEFECTO,
    kcal = kcal.coerceIn(0, Ejercicio.MAX_KCAL),
)

// La foto elegida, reducida para verla antes de subirla; null mientras se lee o si no se pudo. La usa también el editor de platillos.
@Composable
internal fun rememberVistaPrevia(uri: Uri?): ImageBitmap? {
    val resolver = LocalContext.current.contentResolver
    val imagen by produceState<ImageBitmap?>(null, uri) {
        value = if (uri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        val escala = LADO_VISTA_PREVIA / maxOf(info.size.width, info.size.height)
                        if (escala < 1f) decoder.setTargetSize((info.size.width * escala).roundToInt(), (info.size.height * escala).roundToInt())
                    }.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return imagen
}
