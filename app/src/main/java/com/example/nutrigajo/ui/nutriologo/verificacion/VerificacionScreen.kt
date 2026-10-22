package com.example.nutrigajo.ui.nutriologo.verificacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.theme.NutriGajoTheme

private val MARGEN = 16.dp
private const val MIN_DIGITOS_CEDULA = 6
private const val MAX_DIGITOS_CEDULA = 8

// El nutriólogo envía su cédula profesional y su nombre tal como aparece en ella para que un ADMIN lo verifique.
@Composable
fun VerificacionScreen(
    nombreCuenta: String,
    estado: VerificacionEstado,
    onEnviar: (nombreCedula: String, cedula: String) -> Unit,
    onEditar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    var nombre by rememberSaveable { mutableStateOf(nombreCuenta) }
    var cedula by rememberSaveable { mutableStateOf("") }
    val completo = nombre.isNotBlank() && cedula.length >= MIN_DIGITOS_CEDULA

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colores.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.verificacion_titulo),
            modifier = Modifier.padding(horizontal = MARGEN),
            // el 100% de la escala no sirve para texto de varias líneas
            style = MaterialTheme.typography.headlineLarge.copy(lineHeight = 1.2.em),
            color = colores.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.verificacion_form_ayuda),
            modifier = Modifier.padding(horizontal = MARGEN),
            style = MaterialTheme.typography.bodyLarge,
            color = colores.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN - ANILLO_FOCO)) {
            CampoTexto(
                etiqueta = stringResource(R.string.verificacion_cedula),
                valor = cedula,
                onValorChange = { cedula = it.filter(Char::isDigit).take(MAX_DIGITOS_CEDULA); onEditar() },
                icono = R.drawable.ic_lock,
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(16.dp))
            CampoTexto(
                etiqueta = stringResource(R.string.verificacion_nombre),
                valor = nombre,
                onValorChange = { nombre = it; onEditar() },
                icono = R.drawable.ic_person,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            )
        }
        estado.error?.let {
            Spacer(Modifier.height(12.dp))
            AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN))
        }
        Spacer(Modifier.height(24.dp))
        BotonPrimario(
            texto = stringResource(R.string.verificacion_enviar),
            onClick = { onEnviar(nombre, cedula) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
            enabled = completo,
            colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
            cargando = estado.enviando,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onVolver, modifier = Modifier.padding(horizontal = MARGEN - 8.dp)) {
            Text(stringResource(R.string.verificacion_cancelar))
        }
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}

@Preview(showSystemUi = true)
@Composable
private fun VerificacionScreenPreview() {
    NutriGajoTheme {
        VerificacionScreen(
            nombreCuenta = "Ana López",
            estado = VerificacionEstado(cargando = false),
            onEnviar = { _, _ -> },
            onEditar = {},
            onVolver = {},
        )
    }
}
