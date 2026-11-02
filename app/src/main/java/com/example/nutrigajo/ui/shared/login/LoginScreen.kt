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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoContrasena
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.EncabezadoAuth
import com.example.nutrigajo.ui.components.PreguntaConEnlace
import com.example.nutrigajo.ui.shared.auth.AuthEstado
import com.example.nutrigajo.ui.shared.perfil.PerfilEstado
import com.example.nutrigajo.ui.shared.perfil.PerfilViewModel
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
    // el envío del correo para elegir una contraseña nueva es el mismo que usa el perfil
    restablecerVm: PerfilViewModel = viewModel(key = "restablecer-login"),
) {
    val colores = MaterialTheme.colorScheme
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var olvido by rememberSaveable { mutableStateOf(false) }
    val restablecer by restablecerVm.estado.collectAsState()
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
            TextButton(onClick = { olvido = true }, modifier = Modifier.align(Alignment.End).padding(end = ANILLO_FOCO)) {
                Text(stringResource(R.string.login_olvidaste))
            }
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

    if (olvido) {
        DialogoOlvido(
            correoInicial = correo.trim(),
            estado = restablecer,
            onEnviar = { restablecerVm.restablecerContrasena(it.trim().lowercase()) },
            onCerrar = {
                olvido = false
                restablecerVm.reiniciar()
            },
        )
    }
}

// Pide el correo y manda el enlace de Firebase para elegir una contraseña nueva. Sin cuenta contesta lo mismo que con cuenta.
// ponytail: es un enlace en el correo, no un código; un código pediría mandar correos desde nuestro propio servidor.
@Composable
private fun DialogoOlvido(correoInicial: String, estado: PerfilEstado, onEnviar: (String) -> Unit, onCerrar: () -> Unit) {
    var correo by rememberSaveable { mutableStateOf(correoInicial) }
    val valido = Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(stringResource(R.string.login_restablecer_titulo)) },
        text = {
            if (estado.enviado) {
                Text(stringResource(R.string.login_restablecer_enviado, correo.trim()))
            } else {
                Column {
                    Text(stringResource(R.string.login_restablecer_ayuda))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.login_correo)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    )
                    estado.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (estado.enviado) {
                TextButton(onClick = onCerrar) { Text(stringResource(R.string.perfil_listo)) }
            } else {
                TextButton(onClick = { onEnviar(correo) }, enabled = valido && !estado.enviando) {
                    Text(stringResource(R.string.perfil_contrasena_enviar))
                }
            }
        },
        dismissButton = {
            if (!estado.enviado) TextButton(onClick = onCerrar) { Text(stringResource(R.string.verificacion_cancelar)) }
        },
    )
}

@Preview(showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    NutriGajoTheme {
        LoginScreen(estado = AuthEstado(sesion = false), onIngresar = { _, _ -> }, onRegistrar = {}, onEditar = {})
    }
}
