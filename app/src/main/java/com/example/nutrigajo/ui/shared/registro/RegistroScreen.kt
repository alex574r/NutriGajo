package com.example.nutrigajo.ui.shared.registro

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Rol
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

// Alta de cuenta (tema claro), después de elegir el rol.
// ponytail: el aviso de privacidad es solo una casilla; el texto completo llega con su pantalla.
@Composable
fun RegistroScreen(
    rol: Rol,
    estado: AuthEstado,
    onRegistrar: (nombre: String, correo: String, contrasena: String, confirmacion: String) -> Unit,
    onIniciarSesion: () -> Unit,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var confirmacion by rememberSaveable { mutableStateOf("") }
    var aviso by rememberSaveable { mutableStateOf(false) }
    // el largo y la coincidencia de contraseñas los valida el ViewModel y se avisan en la alerta
    val completo = nombre.isNotBlank() &&
        Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() &&
        contrasena.isNotEmpty() && confirmacion.isNotEmpty() && aviso
    val rolTexto = stringResource(if (rol == Rol.NUTRIOLOGO) R.string.rol_nutriologo else R.string.rol_paciente)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colores.background)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EncabezadoAuth(alto = 200.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.registro_titulo),
            style = MaterialTheme.typography.headlineLarge,
            color = colores.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.registro_subtitulo, rolTexto.lowercase()),
            modifier = Modifier.padding(horizontal = MARGEN),
            style = MaterialTheme.typography.bodyLarge,
            color = colores.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN - ANILLO_FOCO)) {
            CampoTexto(
                etiqueta = stringResource(R.string.registro_nombre),
                valor = nombre,
                onValorChange = { nombre = it; onEditar() },
                icono = R.drawable.ic_person,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(12.dp))
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
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(12.dp))
            CampoContrasena(
                etiqueta = stringResource(R.string.registro_confirmar),
                valor = confirmacion,
                onValorChange = { confirmacion = it; onEditar() },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = aviso, role = Role.Checkbox, onValueChange = { aviso = it })
                .padding(horizontal = MARGEN, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = aviso, onCheckedChange = null)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.registro_aviso),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onBackground,
            )
        }
        estado.error?.let {
            Spacer(Modifier.height(8.dp))
            AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN))
        }
        Spacer(Modifier.height(16.dp))
        BotonPrimario(
            texto = stringResource(R.string.registro_registrarme),
            onClick = { onRegistrar(nombre, correo, contrasena, confirmacion) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
            enabled = completo,
            colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
            cargando = estado.cargando,
        )
        Spacer(Modifier.height(16.dp))
        PreguntaConEnlace(
            pregunta = stringResource(R.string.registro_con_cuenta),
            enlace = stringResource(R.string.registro_inicia_sesion),
            onClick = onIniciarSesion,
        )
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}

@Preview(showSystemUi = true)
@Composable
private fun RegistroScreenPreview() {
    NutriGajoTheme {
        RegistroScreen(
            rol = Rol.PACIENTE,
            estado = AuthEstado(sesion = false),
            onRegistrar = { _, _, _, _ -> },
            onIniciarSesion = {},
            onVolver = {},
            onEditar = {},
        )
    }
}
