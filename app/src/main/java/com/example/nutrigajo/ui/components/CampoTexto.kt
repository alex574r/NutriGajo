package com.example.nutrigajo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

private val RADIO_CAMPO = 19.dp

// Input de texto del diseño: etiqueta arriba, campo gris con icono; al enfocar, borde naranja y anillo.
// El anillo suma 4dp por lado: el contenedor debe descontarlos de su margen para alinear con el resto.
@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    onValorChange: (String) -> Unit,
    @DrawableRes icono: Int,
    modifier: Modifier = Modifier,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    transformacion: VisualTransformation = VisualTransformation.None,
    extremo: (@Composable () -> Unit)? = null,
) {
    val colores = MaterialTheme.colorScheme
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    val forma = RoundedCornerShape(RADIO_CAMPO)
    Column(modifier) {
        Text(
            text = etiqueta,
            modifier = Modifier.padding(start = ANILLO_FOCO),
            style = MaterialTheme.typography.titleSmall,
            color = colores.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        AnilloFoco(activo = enfocado, radio = RADIO_CAMPO) {
            TextField(
                value = valor,
                onValueChange = onValorChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (enfocado) colores.primary else Color.Transparent, forma),
                // sin alternativas contextuales: Inter dibuja "7x4" como "7×4" y una contraseña debe verse tal cual
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "calt 0"),
                leadingIcon = {
                    Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(24.dp), tint = colores.onBackground)
                },
                trailingIcon = extremo,
                visualTransformation = transformacion,
                keyboardOptions = teclado,
                singleLine = true,
                interactionSource = interaccion,
                shape = forma,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colores.surfaceVariant,
                    unfocusedContainerColor = colores.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = colores.primary,
                ),
            )
        }
    }
}
