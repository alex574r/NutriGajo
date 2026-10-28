package com.example.nutrigajo.ui.shared.registro

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.nutrigajo.ui.components.aparece
import com.example.nutrigajo.ui.components.sacudida
import com.example.nutrigajo.ui.shared.auth.AuthEstado
import com.example.nutrigajo.ui.theme.NutriGajoTheme

private val MARGEN = 16.dp

// Alta de cuenta (tema claro), después de elegir el rol.
// ponytail: el aviso de privacidad es solo una casilla; el texto completo llega con su pantalla.
@Composable
fun RegistroScreen(
    rol: Rol,
    estado: AuthEstado,
    onRegistrar: (nombres: String, apellidos: String, correo: String, contrasena: String, confirmacion: String) -> Unit,
    onIniciarSesion: () -> Unit,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    var nombres by rememberSaveable { mutableStateOf("") }
    var apellidos by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var confirmacion by rememberSaveable { mutableStateOf("") }
    var aviso by rememberSaveable { mutableStateOf(false) }
    // cada pulsación de «registrarme»; con un error a la vista, el formulario se sacude aunque el error sea el mismo de antes
    var envios by rememberSaveable { mutableStateOf(0) }
    // el botón da un pequeño salto cuando el formulario queda completo, y la casilla rebota al aceptar el aviso
    val salto = remember { Animatable(1f) }
    val rebote = remember { Animatable(1f) }
    // el largo y la coincidencia de contraseñas los valida el ViewModel y se avisan en la alerta
    val completo = nombres.isNotBlank() && apellidos.isNotBlank() &&
        Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() &&
        contrasena.isNotEmpty() && confirmacion.isNotEmpty() && aviso
    val rolTexto = stringResource(if (rol == Rol.NUTRIOLOGO) R.string.rol_nutriologo else R.string.rol_paciente)
    LaunchedEffect(completo) {
        if (completo) {
            salto.animateTo(1.05f, tween(90))
            salto.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 420f))
        }
    }
    LaunchedEffect(aviso) {
        if (aviso) {
            rebote.snapTo(0.7f)
            rebote.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }

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
        // los campos llegan en cascada al abrir y se sacuden si el registro falla (la sacudida del código, de Bencho)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sacudida(if (estado.error != null) envios else null)
                .padding(horizontal = MARGEN - ANILLO_FOCO),
        ) {
            CampoTexto(
                etiqueta = stringResource(R.string.registro_nombre),
                valor = nombres,
                onValorChange = { nombres = it; onEditar() },
                modifier = Modifier.aparece(0),
                icono = R.drawable.ic_person,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(12.dp))
            CampoTexto(
                etiqueta = stringResource(R.string.registro_apellidos),
                valor = apellidos,
                onValorChange = { apellidos = it; onEditar() },
                modifier = Modifier.aparece(1),
                icono = R.drawable.ic_person,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(12.dp))
            CampoTexto(
                etiqueta = stringResource(R.string.login_correo),
                valor = correo,
                onValorChange = { correo = it; onEditar() },
                modifier = Modifier.aparece(2),
                icono = R.drawable.ic_mail,
                teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(12.dp))
            CampoContrasena(
                etiqueta = stringResource(R.string.login_contrasena),
                valor = contrasena,
                onValorChange = { contrasena = it; onEditar() },
                modifier = Modifier.aparece(3),
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(12.dp))
            CampoContrasena(
                etiqueta = stringResource(R.string.registro_confirmar),
                valor = confirmacion,
                onValorChange = { confirmacion = it; onEditar() },
                modifier = Modifier.aparece(4),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = aviso, role = Role.Checkbox, onValueChange = { aviso = it })
                .padding(horizontal = MARGEN, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = aviso,
                onCheckedChange = null,
                modifier = Modifier.graphicsLayer {
                    scaleX = rebote.value
                    scaleY = rebote.value
                },
            )
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
            onClick = {
                envios++
                onRegistrar(nombres, apellidos, correo, contrasena, confirmacion)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MARGEN)
                .graphicsLayer {
                    scaleX = salto.value
                    scaleY = salto.value
                },
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
            onRegistrar = { _, _, _, _, _ -> },
            onIniciarSesion = {},
            onVolver = {},
            onEditar = {},
        )
    }
}
