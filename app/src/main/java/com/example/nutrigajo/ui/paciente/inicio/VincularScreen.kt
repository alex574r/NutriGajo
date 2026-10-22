package com.example.nutrigajo.ui.paciente.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.EncabezadoAuth
import com.example.nutrigajo.ui.theme.NutriGajoTheme

private val MARGEN = 16.dp
private const val LARGO_CODIGO = 8

// El paciente sin nutriólogo escribe el código que este le dio. El guion y los espacios no cuentan.
@Composable
fun VincularScreen(
    estado: VinculoEstado,
    onCanjear: (codigo: String) -> Unit,
    onEditar: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    var codigo by rememberSaveable { mutableStateOf("") }
    val completo = codigo.count { it.isLetterOrDigit() } == LARGO_CODIGO

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colores.background)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EncabezadoAuth(alto = 220.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.vincular_titulo),
            modifier = Modifier.padding(horizontal = MARGEN),
            style = MaterialTheme.typography.headlineLarge,
            color = colores.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.vincular_ayuda),
            modifier = Modifier.padding(horizontal = MARGEN),
            style = MaterialTheme.typography.bodyLarge,
            color = colores.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN - ANILLO_FOCO)) {
            CampoTexto(
                etiqueta = stringResource(R.string.vincular_codigo),
                valor = codigo,
                // un código con guion mide 9 caracteres
                onValorChange = { codigo = it.uppercase().take(LARGO_CODIGO + 1); onEditar() },
                icono = R.drawable.ic_lock,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
            )
        }
        estado.error?.let {
            Spacer(Modifier.height(12.dp))
            AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN))
        }
        Spacer(Modifier.height(24.dp))
        BotonPrimario(
            texto = stringResource(R.string.vincular_boton),
            onClick = { onCanjear(codigo) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
            enabled = completo,
            colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
            cargando = estado.canjeando,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCerrarSesion) { Text(stringResource(R.string.inicio_cerrar_sesion)) }
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}

@Preview(showSystemUi = true)
@Composable
private fun VincularScreenPreview() {
    NutriGajoTheme {
        VincularScreen(estado = VinculoEstado(cargando = false), onCanjear = {}, onEditar = {}, onCerrarSesion = {})
    }
}
