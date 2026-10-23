package com.example.nutrigajo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import kotlin.math.roundToInt

// Los dos ámbitos que hacen falta para que un elemento viaje de una pantalla a otra: el de la capa compartida y el del
// contenido que entra o sale. Los pasa quien alberga el cambio de pantalla; `null` es una pantalla sin transición.
@OptIn(ExperimentalSharedTransitionApi::class)
class TransicionPerfil(val compartida: SharedTransitionScope, val visibilidad: AnimatedVisibilityScope)

// clave con la que el avatar del inicio y el del perfil se reconocen como el mismo elemento
const val CLAVE_AVATAR = "avatar"

private const val PORCENTAJE_CIRCULO = 50f
private const val PORCENTAJE_CUADRADO_REDONDEADO = 32f

// El elemento con esa clave vuela de su sitio en una pantalla al de la otra, con un resorte suave y por encima del resto.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.compartido(transicion: TransicionPerfil?, clave: String): Modifier {
    if (transicion == null) return this
    return with(transicion.compartida) {
        this@compartido.sharedElement(
            sharedContentState = rememberSharedContentState(clave),
            animatedVisibilityScope = transicion.visibilidad,
            boundsTransform = { _, _ -> spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow) },
        )
    }
}

// Forma del avatar: círculo en el inicio y cuadrado redondeado en el perfil. Durante el viaje pasa de una a la otra, así el
// avatar se transforma en lugar de cambiar de golpe. Sin transición se queda con la suya.
@Composable
fun formaAvatar(transicion: TransicionPerfil?, circulo: Boolean): Shape {
    val propio = if (circulo) PORCENTAJE_CIRCULO else PORCENTAJE_CUADRADO_REDONDEADO
    if (transicion == null) return RoundedCornerShape(percent = propio.roundToInt())
    val ajeno = if (circulo) PORCENTAJE_CUADRADO_REDONDEADO else PORCENTAJE_CIRCULO
    val porcentaje by transicion.visibilidad.transition.animateFloat(transitionSpec = { tween(450) }, label = "forma-avatar") {
        if (it == EnterExitState.Visible) propio else ajeno
    }
    return RoundedCornerShape(percent = porcentaje.roundToInt())
}

// Cambia entre dos pantallas (el inicio y el perfil, o las pestañas de uno) con un desvanecido corto: el protagonista es el
// avatar, que viaja de una a la otra. `contenido` recibe el destino que le toca pintar (el que sale también se pinta mientras
// se va) y la transición que debe pasar a quien lleve el avatar.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <T> CambioConAvatar(
    destino: T,
    modifier: Modifier = Modifier,
    contenido: @Composable (destino: T, transicion: TransicionPerfil) -> Unit,
) {
    SharedTransitionLayout(modifier) {
        AnimatedContent(
            targetState = destino,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(300, delayMillis = 100)) togetherWith fadeOut(tween(200)) },
            label = "cambio-con-avatar",
        ) { actual ->
            contenido(actual, TransicionPerfil(this@SharedTransitionLayout, this))
        }
    }
}
