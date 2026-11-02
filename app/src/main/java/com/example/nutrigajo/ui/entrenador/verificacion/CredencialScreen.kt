package com.example.nutrigajo.ui.entrenador.verificacion

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.nutriologo.verificacion.VerificacionEstado

private val MARGEN = 16.dp
private const val MIN_CREDENCIAL = 10
private const val MAX_CREDENCIAL = 200

// El entrenador escribe su certificación, la institución y el folio para que un ADMIN lo verifique (las reglas piden de 10 a 200 letras).
@Composable
fun CredencialScreen(
    estado: VerificacionEstado,
    onEnviar: (credencial: String) -> Unit,
    onEditar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    var credencial by rememberSaveable { mutableStateOf("") }

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
            style = MaterialTheme.typography.headlineLarge.copy(lineHeight = 1.2.em),
            color = colores.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.credencial_form_ayuda),
            modifier = Modifier.padding(horizontal = MARGEN),
            style = MaterialTheme.typography.bodyLarge,
            color = colores.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN - ANILLO_FOCO)) {
            CampoTexto(
                etiqueta = stringResource(R.string.credencial_campo),
                valor = credencial,
                onValorChange = { credencial = it.take(MAX_CREDENCIAL); onEditar() },
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                lineas = 4,
            )
        }
        estado.error?.let {
            Spacer(Modifier.height(12.dp))
            AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN))
        }
        Spacer(Modifier.height(24.dp))
        BotonPrimario(
            texto = stringResource(R.string.verificacion_enviar),
            onClick = { onEnviar(credencial) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
            enabled = credencial.trim().replace(Regex("\\s+"), " ").length >= MIN_CREDENCIAL,
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
