package com.example.nutrigajo.ui.shared.chat

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.data.push.Avisos
import com.example.nutrigajo.domain.model.Mensaje
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.AvatarInicial
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.nutriologo.inicio.fechaMedia
import kotlinx.coroutines.CancellationException
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MARGEN = 16.dp
private val RADIO = 20.dp
private val PICO = 6.dp

// El chat entre el nutriólogo y su paciente, el mismo para los dos: `nombre` es el de la otra persona, `subtitulo` su papel y
// `otraId` su cuenta, con la que se encuentra su foto de perfil (sin ella o sin foto, se ve su inicial).
// Los mensajes llegan en tiempo real; lo que escribes se ve al instante y se confirma después.
@Composable
fun ChatScreen(
    pacienteId: String,
    nombre: String,
    subtitulo: String,
    otraId: String,
    onVolver: () -> Unit,
    vm: ChatViewModel = viewModel(key = "chat-$pacienteId"),
) {
    val colores = MaterialTheme.colorScheme
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current
    val cache = contexto.cacheDir
    val yo = vm.yo
    BackHandler(onBack = onVolver)

    // con el chat a la vista se escucha la conversación y no se avisa de lo que llega a ese mismo chat
    DisposableEffect(pacienteId) {
        vm.abrir(pacienteId)
        Avisos.chatAbierto = pacienteId
        onDispose {
            Avisos.chatAbierto = null
            vm.cerrar()
        }
    }
    // lo que llega mientras el chat está a la vista cuenta como visto
    val masNuevo = estado.mensajes.firstOrNull()?.enviadoEn
    LaunchedEffect(masNuevo) { if (masNuevo != null) marcarChatVisto(contexto, yo, pacienteId, masNuevo) }

    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.elegirFoto(contexto.contentResolver, uri)
    }
    var fotoAbierta by remember { mutableStateOf<Bitmap?>(null) }
    // la foto de perfil de la otra persona; mientras llega (o si no tiene) se ve su inicial
    val fotoDeLaOtra by produceState<ImageBitmap?>(null, otraId) {
        value = if (otraId.isEmpty()) {
            null
        } else {
            try {
                vm.fotoDeLaOtra(otraId, cache)?.asImageBitmap()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }

    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding().imePadding()) {
        CabeceraChat(nombre, subtitulo, fotoDeLaOtra, onVolver)
        HorizontalDivider(color = colores.outlineVariant)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                estado.cargando -> EsqueletoChat()
                estado.mensajes.isEmpty() -> ChatVacio(nombre)
                else -> ListaMensajes(
                    mensajes = estado.mensajes,
                    yo = yo,
                    cargarFoto = { id -> vm.cargarFoto(pacienteId, id, cache) },
                    onFoto = { fotoAbierta = it },
                )
            }
        }
        estado.error?.let { AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN, vertical = 8.dp)) }
        BarraEscribir(
            borrador = estado.borrador,
            foto = estado.foto,
            enviando = estado.enviando,
            onCambio = vm::escribir,
            onElegirFoto = { elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onQuitarFoto = vm::quitarFoto,
            onEnviar = { vm.enviar(pacienteId, cache) },
        )
    }
    fotoAbierta?.let { VisorFoto(it) { fotoAbierta = null } }
}

// ---- cabecera

@Composable
private fun CabeceraChat(nombre: String, subtitulo: String, foto: ImageBitmap?, onVolver: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colores.surfaceVariant)
                .alPulsar(onVolver),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.mapa_volver), modifier = Modifier.size(22.dp), tint = colores.onBackground)
        }
        AvatarInicial(nombre, 44.dp, MaterialTheme.typography.titleMedium, imagen = foto)
        Column(Modifier.weight(1f)) {
            Text(nombre, style = MaterialTheme.typography.titleMedium, color = colores.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant, maxLines = 1)
        }
    }
}

// ---- lista

private sealed interface Fila {
    val clave: String

    class Dia(val etiqueta: String, override val clave: String) : Fila

    // `mostrarHora`: es el último de una racha del mismo autor en el mismo minuto; `separado`: cambia de autor respecto al de abajo
    class Burbuja(val mensaje: Mensaje, val mostrarHora: Boolean, val separado: Boolean) : Fila {
        override val clave: String get() = mensaje.id
    }
}

// `mensajes` viene del más nuevo al más viejo (la lista se pinta al revés): la fila de un día va después de su mensaje más viejo,
// que es arriba en la pantalla.
private fun filasDe(mensajes: List<Mensaje>, hoy: String, ayer: String): List<Fila> {
    val zona = ZoneId.systemDefault()
    val dia = { m: Mensaje -> Instant.ofEpochMilli(m.enviadoEn).atZone(zona).toLocalDate() }
    val hoyFecha = LocalDate.now(zona)
    val filas = ArrayList<Fila>(mensajes.size + 4)
    mensajes.forEachIndexed { i, m ->
        val posterior = mensajes.getOrNull(i - 1)
        val anterior = mensajes.getOrNull(i + 1)
        val mismoAutor = posterior != null && posterior.autorId == m.autorId
        filas += Fila.Burbuja(
            mensaje = m,
            mostrarHora = posterior == null || !mismoAutor || posterior.enviadoEn / 60_000 != m.enviadoEn / 60_000,
            separado = posterior != null && !mismoAutor,
        )
        if (anterior == null || dia(anterior) != dia(m)) {
            val fecha = dia(m)
            val etiqueta = when (fecha) {
                hoyFecha -> hoy
                hoyFecha.minusDays(1) -> ayer
                else -> fechaMedia(m.enviadoEn)
            }
            filas += Fila.Dia(etiqueta, "dia-$fecha")
        }
    }
    return filas
}

@Composable
private fun ListaMensajes(mensajes: List<Mensaje>, yo: String, cargarFoto: suspend (String) -> Bitmap, onFoto: (Bitmap) -> Unit) {
    val hoy = stringResource(R.string.chat_hoy)
    val ayer = stringResource(R.string.chat_ayer)
    val filas = remember(mensajes, hoy, ayer) { filasDe(mensajes, hoy, ayer) }
    val lista = rememberLazyListState()
    // al llegar un mensaje (o al enviar uno) se baja hasta él, salvo que se esté leyendo algo más arriba
    val nuevo = mensajes.first()
    LaunchedEffect(nuevo.id) {
        if (nuevo.autorId == yo || lista.firstVisibleItemIndex <= 2) lista.animateScrollToItem(0)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lista,
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = MARGEN, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items(filas, key = { it.clave }) { fila ->
            when (fila) {
                is Fila.Dia -> SeparadorDia(fila.etiqueta, Modifier.animateItem())
                is Fila.Burbuja -> BurbujaMensaje(
                    fila = fila,
                    mio = fila.mensaje.autorId == yo,
                    cargarFoto = { cargarFoto(fila.mensaje.id) },
                    onFoto = onFoto,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun SeparadorDia(etiqueta: String, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(
            text = etiqueta,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(colores.surfaceVariant).padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = colores.onSurfaceVariant,
        )
    }
}

private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-MX"))

// Lo mío en negro y lo de la otra persona en gris; el pico más cerrado queda del lado de quien lo dice.
@Composable
private fun BurbujaMensaje(
    fila: Fila.Burbuja,
    mio: Boolean,
    cargarFoto: suspend () -> Bitmap,
    onFoto: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val m = fila.mensaje
    val forma = RoundedCornerShape(
        topStart = RADIO,
        topEnd = RADIO,
        bottomStart = if (mio) RADIO else PICO,
        bottomEnd = if (mio) PICO else RADIO,
    )
    val hora = remember(m.enviadoEn) { Instant.ofEpochMilli(m.enviadoEn).atZone(ZoneId.systemDefault()).format(FORMATO_HORA) }
    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = if (fila.separado) 8.dp else 0.dp),
        horizontalAlignment = if (mio) Alignment.End else Alignment.Start,
    ) {
        // con foto la burbuja mide lo que la foto (240 dp más su marco), y el pie de foto se ajusta a ese ancho
        Column(
            Modifier
                .widthIn(max = if (m.foto) 248.dp else 300.dp)
                .clip(forma)
                .background(if (mio) colores.onBackground else colores.surfaceVariant)
                .padding(if (m.foto) 4.dp else 0.dp),
        ) {
            if (m.foto) FotoMensaje(cargarFoto, onFoto)
            if (m.texto.isNotEmpty()) {
                Text(
                    text = m.texto,
                    modifier = Modifier.padding(horizontal = if (m.foto) 10.dp else 16.dp, vertical = if (m.foto) 8.dp else 11.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (mio) colores.background else colores.onBackground,
                )
            }
        }
        if (fila.mostrarHora || m.pendiente) {
            Text(
                text = if (m.pendiente) stringResource(R.string.chat_enviando) else hora,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
            )
        }
    }
}

private sealed interface EstadoFoto {
    data object Cargando : EstadoFoto

    class Lista(val bitmap: Bitmap) : EstadoFoto

    data object Fallo : EstadoFoto
}

// La foto de un mensaje: un esqueleto mientras llega, y tocarla la abre grande. Si no llega, tocar vuelve a intentarlo.
@Composable
private fun FotoMensaje(cargar: suspend () -> Bitmap, onAbrir: (Bitmap) -> Unit) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(16.dp)
    var intento by remember { mutableIntStateOf(0) }
    val estado by produceState<EstadoFoto>(EstadoFoto.Cargando, intento) {
        value = EstadoFoto.Cargando
        value = try {
            EstadoFoto.Lista(cargar())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            EstadoFoto.Fallo
        }
    }
    when (val e = estado) {
        EstadoFoto.Cargando -> Esqueleto(Modifier.size(width = 220.dp, height = 160.dp), forma)
        is EstadoFoto.Lista -> {
            // proporción de la foto, sin pasar de muy alta ni de muy ancha
            val proporcion = (e.bitmap.width.toFloat() / e.bitmap.height).coerceIn(0.6f, 1.8f)
            Image(
                bitmap = remember(e.bitmap) { e.bitmap.asImageBitmap() },
                contentDescription = stringResource(R.string.chat_foto),
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .fillMaxWidth()
                    .height(240.dp / proporcion)
                    .clip(forma)
                    .clickable(role = Role.Button) { onAbrir(e.bitmap) },
                contentScale = ContentScale.Crop,
            )
        }
        EstadoFoto.Fallo -> Box(
            modifier = Modifier
                .size(width = 220.dp, height = 110.dp)
                .clip(forma)
                .background(colores.outlineVariant.copy(alpha = 0.4f))
                .clickable(role = Role.Button) { intento++ },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.chat_foto_error),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// La foto a pantalla completa; tocar en cualquier parte la cierra.
@Composable
private fun VisorFoto(foto: Bitmap, onCerrar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(colores.onBackground).clickable(onClick = onCerrar)) {
            Image(
                bitmap = remember(foto) { foto.asImageBitmap() },
                contentDescription = stringResource(R.string.chat_foto),
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentScale = ContentScale.Fit,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colores.background.copy(alpha = 0.16f))
                    .alPulsar(onCerrar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_cerrar), contentDescription = stringResource(R.string.chat_cerrar_foto), modifier = Modifier.size(22.dp), tint = colores.background)
            }
        }
    }
}

// ---- estados vacíos

@Composable
private fun EsqueletoChat() {
    val forma = RoundedCornerShape(RADIO)
    Column(Modifier.fillMaxSize().padding(MARGEN), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom)) {
        Esqueleto(Modifier.size(width = 220.dp, height = 52.dp), forma)
        Esqueleto(Modifier.align(Alignment.End).size(width = 180.dp, height = 44.dp), forma)
        Esqueleto(Modifier.size(width = 260.dp, height = 64.dp), forma)
        Esqueleto(Modifier.align(Alignment.End).size(width = 140.dp, height = 44.dp), forma)
    }
}

@Composable
private fun ChatVacio(nombre: String) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(colores.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_mensaje), contentDescription = null, modifier = Modifier.size(28.dp), tint = colores.onPrimaryContainer)
        }
        Text(stringResource(R.string.chat_vacio_titulo), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
        Text(
            text = stringResource(R.string.chat_vacio_texto, nombre),
            style = MaterialTheme.typography.bodyMedium,
            color = colores.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ---- escribir

@Composable
private fun BarraEscribir(
    borrador: String,
    foto: FotoElegida?,
    enviando: Boolean,
    onCambio: (String) -> Unit,
    onElegirFoto: () -> Unit,
    onQuitarFoto: () -> Unit,
    onEnviar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val puede = (borrador.isNotBlank() || foto != null) && !enviando
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        if (foto != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    bitmap = remember(foto) { foto.previa.asImageBitmap() },
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
                Text(stringResource(R.string.chat_foto_lista), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colores.onBackground)
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(colores.surfaceVariant).alPulsar(onQuitarFoto, enabled = !enviando),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_cerrar), contentDescription = stringResource(R.string.chat_quitar_foto), modifier = Modifier.size(18.dp), tint = colores.onBackground)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(colores.surfaceVariant).alPulsar(onElegirFoto, enabled = !enviando),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_imagen), contentDescription = stringResource(R.string.chat_adjuntar), modifier = Modifier.size(24.dp), tint = colores.onBackground)
            }
            BasicTextField(
                value = borrador,
                onValueChange = onCambio,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colores.surfaceVariant)
                    .border(1.dp, if (enfocado) colores.primary else colores.surfaceVariant, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colores.onBackground),
                cursorBrush = SolidColor(colores.primary),
                maxLines = 5,
                interactionSource = interaccion,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                decorationBox = { campo ->
                    Box {
                        if (borrador.isEmpty()) {
                            Text(stringResource(R.string.chat_escribe), style = MaterialTheme.typography.bodyLarge, color = colores.onSurfaceVariant)
                        }
                        campo()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (puede || enviando) colores.primary else colores.surfaceVariant)
                    .alPulsar(onEnviar, enabled = puede),
                contentAlignment = Alignment.Center,
            ) {
                if (enviando) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = colores.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = stringResource(R.string.chat_enviar),
                        modifier = Modifier.size(24.dp),
                        tint = if (puede) colores.onPrimary else colores.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
