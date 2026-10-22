package com.example.nutrigajo.ui.shared.login

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.PreguntaConEnlace
import com.example.nutrigajo.ui.theme.NutriGajoTheme

private val MARGEN = 16.dp

// Foto: "Modern Pilates Studio with Reformers", Paulina Vargas, Pexels (licencia Pexels: uso libre, sin atribución obligatoria).
// https://www.pexels.com/photo/modern-pilates-studio-with-reformers-36833353/
// Pantalla de inicio de sesión (tema claro) para quien ya vio la bienvenida.
@Composable
fun LoginScreen(
    onIngresar: () -> Unit,
    onRegistrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }
    // ponytail: solo valida el formato; Firebase Auth comprueba las credenciales cuando exista
    val valido = Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() && contrasena.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colores.background)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            Image(
                painter = painterResource(R.drawable.login_encabezado),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to colores.background)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(56.dp)
                    .background(colores.primary, RoundedCornerShape(21.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_logo_mono),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = colores.onPrimary,
                )
            }
        }
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
                onValorChange = { correo = it },
                icono = R.drawable.ic_mail,
                teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(12.dp))
            CampoTexto(
                etiqueta = stringResource(R.string.login_contrasena),
                valor = contrasena,
                onValorChange = { contrasena = it },
                icono = R.drawable.ic_lock,
                teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                transformacion = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                extremo = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            painter = painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                            contentDescription = stringResource(
                                if (visible) R.string.login_ocultar_contrasena else R.string.login_mostrar_contrasena,
                            ),
                        )
                    }
                },
            )
        }
        Spacer(Modifier.height(24.dp))
        BotonPrimario(
            texto = stringResource(R.string.login_ingresar),
            onClick = onIngresar,
            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
            enabled = valido,
            colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
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
    NutriGajoTheme { LoginScreen(onIngresar = {}, onRegistrar = {}) }
}
