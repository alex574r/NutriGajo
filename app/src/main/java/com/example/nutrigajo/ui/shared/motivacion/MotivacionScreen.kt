package com.example.nutrigajo.ui.shared.motivacion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.BarraEstadoClara
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.FondoFoto
import com.example.nutrigajo.ui.theme.NutriGajoTheme
import com.example.nutrigajo.ui.theme.TextLgMedium

// Foto: "Side View of a Man", Thomas Ronveaux, Pexels (licencia Pexels: uso libre, sin atribución obligatoria).
// https://www.pexels.com/photo/side-view-of-a-man-13951188/
// Pantalla de bienvenida: siempre en tema oscuro. Avanza con el botón "siguiente" o con un toque; no se cierra sola.
@Composable
fun MotivacionScreen(onContinuar: () -> Unit, modifier: Modifier = Modifier) {
    BarraEstadoClara()
    NutriGajoTheme(darkTheme = true) {
        val colores = MaterialTheme.colorScheme
        FondoFoto(
            imagen = R.drawable.motivacion_fondo,
            modifier = modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onContinuar,
            ),
            // el diseño usa fotografía en blanco y negro
            blancoYNegro = true,
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(colores.primary, RoundedCornerShape(21.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_quote),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = colores.onPrimary,
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.motivacion_frase),
                    // lineHeight explícito: el 100% de la escala no sirve para texto de varias líneas
                    style = TextLgMedium.copy(lineHeight = 1.4.em),
                    color = colores.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.motivacion_autor),
                    style = MaterialTheme.typography.labelMedium,
                    color = colores.onBackground,
                )
                Spacer(Modifier.height(40.dp))
                BotonPrimario(
                    texto = stringResource(R.string.motivacion_siguiente),
                    onClick = onContinuar,
                    modifier = Modifier.fillMaxWidth(),
                    altura = 64.dp,
                )
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun MotivacionScreenPreview() {
    MotivacionScreen(onContinuar = {})
}
