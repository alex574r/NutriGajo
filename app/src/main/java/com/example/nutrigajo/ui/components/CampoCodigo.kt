package com.example.nutrigajo.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val RADIO_CASILLA = 14.dp

// Casillas de código del diseño (OTP): una por carácter, con un guion entre grupos. La que toca escribir va en naranja
// con el anillo de foco. Quien lo usa le deja ANILLO_FOCO de margen menos para que el anillo no se recorte en los bordes.
// ponytail: un solo BasicTextField sin pintar recibe el teclado; las casillas solo dibujan su texto.
@Composable
fun CampoCodigo(
    valor: String,
    onValorChange: (String) -> Unit,
    largo: Int,
    descripcion: String,
    modifier: Modifier = Modifier,
    grupo: Int = 4,
    onListo: () -> Unit = {},
) {
    val colores = MaterialTheme.colorScheme
    val foco = remember { FocusRequester() }
    val teclado = LocalSoftwareKeyboardController.current
    var enfocado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { foco.requestFocus() }

    BasicTextField(
        value = valor,
        onValueChange = { onValorChange(it.filter(Char::isLetterOrDigit).uppercase().take(largo)) },
        modifier = modifier
            .focusRequester(foco)
            .onFocusChanged { enfocado = it.isFocused }
            .semantics { contentDescription = descripcion },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onListo() }),
        cursorBrush = SolidColor(Color.Transparent),
        decorationBox = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // tocar las casillas devuelve el foco y el teclado si se cerraron
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        foco.requestFocus()
                        teclado?.show()
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(largo) { indice ->
                    if (indice > 0 && indice % grupo == 0) {
                        Box(Modifier.width(12.dp).height(2.dp).background(colores.outline))
                    }
                    Casilla(
                        caracter = valor.getOrNull(indice),
                        activa = enfocado && indice == minOf(valor.length, largo - 1),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    )
}

@Composable
private fun Casilla(caracter: Char?, activa: Boolean, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(RADIO_CASILLA)
    AnilloFoco(activo = activa, radio = RADIO_CASILLA, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(if (activa) colores.primary else colores.surfaceVariant, forma)
                .border(1.dp, if (activa) colores.primary else colores.outline, forma),
            contentAlignment = Alignment.Center,
        ) {
            when {
                caracter != null -> Text(
                    text = caracter.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (activa) colores.onPrimary else colores.onSurfaceVariant,
                )
                activa -> {
                    val parpadeo by rememberInfiniteTransition(label = "cursor").animateFloat(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(durationMillis = 600), RepeatMode.Reverse),
                        label = "parpadeo",
                    )
                    Box(Modifier.width(2.dp).height(26.dp).alpha(parpadeo).background(colores.onPrimary))
                }
            }
        }
    }
}
