package com.example.nutrigajo.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.nutrigajo.R

// CampoTexto de contraseña: oculta el texto y trae el ojo para mostrarlo.
@Composable
fun CampoContrasena(
    etiqueta: String,
    valor: String,
    onValorChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    CampoTexto(
        etiqueta = etiqueta,
        valor = valor,
        onValorChange = onValorChange,
        icono = R.drawable.ic_lock,
        modifier = modifier,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        transformacion = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        extremo = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    painter = painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                    contentDescription = stringResource(if (visible) R.string.contrasena_ocultar else R.string.contrasena_mostrar),
                )
            }
        },
    )
}
