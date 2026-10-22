package com.example.nutrigajo.ui.shared.login

import android.util.Patterns
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoContrasena
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.EncabezadoAuth
import com.example.nutrigajo.ui.components.PreguntaConEnlace
import com.example.nutrigajo.ui.shared.auth.AuthEstado
import com.example.nutrigajo.ui.theme.NutriGajoTheme

private val MARGEN = 16.dp

// Pantalla de inicio de sesión (tema claro) para quien ya vio la bienvenida.
@Composable
fun LoginScreen(
    estado: AuthEstado,
    onIngresar: (correo: String, contrasena: String) -> Unit,
    onRegistrar: () -> Unit,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    // solo valida el formato; las credenciales las comprueba Firebase Auth
    val valido = Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() && contrasena.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colores.background)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EncabezadoAuth()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.login_titulo),
            style = MaterialTheme.typography.headlineLarge,
            color = colores.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.app_tagline),
            modifier = Modifier.padding(horizontal = MARGEN),
            style = MaterialTheme.typography.bodyLarge,
            color = colores.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN - ANILLO_FOCO)) {
            CampoTexto(
                etiqueta = stringResource(R.string.login_correo),
                valor = correo,
                onValorChange = { correo = it; onEditar() },
                icono = R.drawable.ic_mail,
                teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(12.dp))
            CampoContrasena(
                etiqueta = stringResource(R.string.login_contrasena),
                valor = contrasena,
                onValorChange = { contrasena = it; onEditar() },
            )
        }
        estado.error?.let {
            Spacer(Modifier.height(12.dp))
            AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN))
        }
        Spacer(Modifier.height(24.dp))
        BotonPrimario(
            texto = stringResource(R.string.login_ingresar),
            onClick = { onIngresar(correo, contrasena) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
            enabled = valido,
            colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
            cargando = estado.cargando,
        )
        Spacer(Modifier.height(24.dp))
        PreguntaConEnlace(
            pregunta = stringResource(R.string.login_sin_cuenta),
            enlace = stringResource(R.string.login_registrate),
            onClick = onRegistrar,
        )
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}

@Preview(showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    NutriGajoTheme {
        LoginScreen(estado = AuthEstado(sesion = false), onIngresar = { _, _ -> }, onRegistrar = {}, onEditar = {})
    }
}
