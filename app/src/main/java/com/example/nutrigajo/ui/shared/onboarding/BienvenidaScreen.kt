package com.example.nutrigajo.ui.shared.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.BarraEstadoClara
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.FondoFoto
import com.example.nutrigajo.ui.components.PreguntaConEnlace
import com.example.nutrigajo.ui.theme.NutriGajoTheme

// Foto: "Grayscale Photo of a Woman Wearing a Boxing Gloves", Наталья Водопад, Pexels (licencia Pexels).
// https://www.pexels.com/photo/grayscale-photo-of-a-woman-wearing-a-boxing-gloves-8371676/
@Composable
fun BienvenidaScreen(
    onComenzar: () -> Unit,
    onIngresar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BarraEstadoClara()
    NutriGajoTheme(darkTheme = true) {
        val colores = MaterialTheme.colorScheme
        FondoFoto(imagen = R.drawable.onboarding_bienvenida, modifier = modifier, alturaDegradado = 560.dp) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.bienvenida_titulo),
                    style = MaterialTheme.typography.displaySmall,
                    color = colores.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colores.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                BotonPrimario(
                    texto = stringResource(R.string.bienvenida_comenzar),
                    onClick = onComenzar,
                    modifier = Modifier.fillMaxWidth(),
                    altura = 64.dp,
                )
                Spacer(Modifier.height(16.dp))
                PreguntaConEnlace(
                    pregunta = stringResource(R.string.bienvenida_ya_tienes_cuenta),
                    enlace = stringResource(R.string.bienvenida_ingresar),
                    onClick = onIngresar,
                )
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun BienvenidaScreenPreview() {
    BienvenidaScreen(onComenzar = {}, onIngresar = {})
}
