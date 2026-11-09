package com.example.nutrigajo.ui.shared.ejercicios

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.CatalogoRepository
import com.example.nutrigajo.data.repository.EjerciciosRepository
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.FuenteEjercicio
import java.io.File

// El clip animado de un ejercicio, en bucle y sin sonido; un toque lo pausa (y entonces se ve `pausa`, la foto fija) y otro lo reanuda,
// como el «tap to pause» de openGym. Solo corre con la pantalla en primer plano. El reproductor se libera al salir de la composición.
@Composable
fun ReproductorBucle(
    fuente: Uri,
    modifier: Modifier = Modifier,
    forma: Shape = RoundedCornerShape(24.dp),
    // true muestra el clip entero (con franjas si no coincide la proporción); false lo recorta para llenar el espacio
    ajustar: Boolean = false,
    pausa: @Composable () -> Unit,
) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current.lifecycle
    var reproduciendo by remember { mutableStateOf(true) }
    val reproductor = remember(fuente) {
        ExoPlayer.Builder(contexto).build().apply {
            setMediaItem(MediaItem.fromUri(fuente))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            prepare()
        }
    }
    // sigue lo que pide el usuario mientras la pantalla está visible; en segundo plano siempre pausado
    DisposableEffect(reproductor, reproduciendo, ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> reproductor.playWhenReady = reproduciendo
                Lifecycle.Event.ON_PAUSE -> reproductor.playWhenReady = false
                else -> Unit
            }
        }
        ciclo.addObserver(observador)
        reproductor.playWhenReady = reproduciendo && ciclo.currentState.isAtLeast(Lifecycle.State.RESUMED)
        onDispose { ciclo.removeObserver(observador) }
    }
    DisposableEffect(reproductor) { onDispose { reproductor.release() } }

    Box(
        modifier = modifier
            .clip(forma)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) { reproduciendo = !reproduciendo },
    ) {
        AndroidView(
            factory = { c ->
                PlayerView(c).apply {
                    useController = false
                    resizeMode = if (ajustar) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    player = reproductor
                }
            },
            update = { it.player = reproductor },
            modifier = Modifier.fillMaxSize(),
        )
        if (!reproduciendo) Box(Modifier.fillMaxSize()) { pausa() }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(if (reproduciendo) R.drawable.ic_pausa else R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
            Text(
                text = stringResource(if (reproduciendo) R.string.ejercicio_toca_pausar else R.string.ejercicio_toca_reproducir),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                color = Color.White,
            )
        }
    }
}

// La ilustración de un ejercicio: su clip animado si lo tiene y ya se bajó; mientras baja, o si no se pudo bajar (sin red, sin clip en el
// Worker), su foto de siempre. Sin clip es exactamente `FotoEjercicio`.
@Composable
fun MediaEjercicio(ejercicio: Ejercicio, modifier: Modifier = Modifier, forma: Shape = RoundedCornerShape(24.dp)) {
    val delCatalogo = ejercicio.fuente == FuenteEjercicio.CATALOGO
    // los del catálogo no llevan versión: todos tienen clip, y se pide por su id
    if (ejercicio.clipVersion == null && !delCatalogo) {
        FotoEjercicio(ejercicio, modifier, forma)
        return
    }
    val cache = LocalContext.current.cacheDir
    val repo = remember { EjerciciosRepository() }
    val catalogo = remember { CatalogoRepository(directorio = cache) }
    val archivo by produceState<File?>(null, ejercicio.id, ejercicio.clipVersion, delCatalogo) {
        value = runCatching { if (delCatalogo) catalogo.clip(ejercicio.id) else repo.clip(ejercicio, cache) }.getOrNull()
    }
    val clip = archivo
    if (clip == null) {
        FotoEjercicio(ejercicio, modifier, forma)
        return
    }
    val fuente = Uri.fromFile(clip)
    var ampliado by remember { mutableStateOf(false) }
    Box(modifier) {
        // este reproductor no se destruye al abrir el visor (el visor lleva el suyo y lo cubre): recrearlo al cerrar dejaba su superficie en
        // negro, porque Android ya había reciclado la anterior
        ReproductorBucle(fuente, Modifier.fillMaxSize(), forma, pausa = { FotoEjercicio(ejercicio, Modifier.fillMaxSize(), forma) })
        // arriba a la derecha, bajo la barra de estado si el clip llega hasta ahí; abajo lo tapa la hoja en el detalle y la guía
        BotonRedondo(
            icono = R.drawable.ic_ampliar,
            descripcion = stringResource(R.string.ejercicio_ampliar),
            onClick = { ampliado = true },
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp),
        )
    }
    if (ampliado) VisorClip(fuente, ejercicio.titulo) { ampliado = false }
}

// Un botón circular oscuro y translúcido con un icono blanco; 44 dp para que se acierte con el dedo.
@Composable
private fun BotonRedondo(@DrawableRes icono: Int, descripcion: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icono), contentDescription = descripcion, modifier = Modifier.size(22.dp), tint = Color.White)
    }
}

// El clip a pantalla completa sobre negro, entero y sin recortar; un toque lo pausa y la X (o «atrás») lo cierra.
@Composable
private fun VisorClip(fuente: Uri, titulo: String, onCerrar: () -> Unit) {
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        // el negro va de fondo de la VENTANA, no de un Box: el video se dibuja en una capa detrás de la ventana y un fondo opaco de Compose
        // encima lo taparía por completo
        val ventana = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { ventana?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK)) }
        Box(Modifier.fillMaxSize()) {
            // al pausar se queda el cuadro que estaba, no la foto: aquí se quiere ver bien la postura
            ReproductorBucle(fuente, Modifier.fillMaxSize().systemBarsPadding(), RectangleShape, ajustar = true, pausa = {})
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 12.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(titulo, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 2)
                BotonRedondo(R.drawable.ic_cerrar, stringResource(R.string.ejercicio_cerrar_visor), onCerrar)
            }
        }
    }
}
