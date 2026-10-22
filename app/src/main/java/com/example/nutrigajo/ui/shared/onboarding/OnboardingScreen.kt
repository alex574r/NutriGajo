package com.example.nutrigajo.ui.shared.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.BarraEstadoClara
import com.example.nutrigajo.ui.components.FondoFoto
import com.example.nutrigajo.ui.theme.NutriGajoTheme
import kotlinx.coroutines.launch

private class Pagina(
    @param:DrawableRes val imagen: Int,
    @param:StringRes val titulo: Int,
    @param:StringRes val descripcion: Int,
)

// Fotos (Pexels, licencia Pexels: uso libre, sin atribución obligatoria):
// planes: Анар Гумбатов, /photo/29027900 · medidas: Mikhail Nilov, /photo/8670203
// fotos: cottonbro studio, /photo/4754123 · alimentación: Jo Macabre, /photo/7719248
private val paginas = listOf(
    Pagina(R.drawable.onboarding_planes, R.string.onboarding_planes_titulo, R.string.onboarding_planes_desc),
    Pagina(R.drawable.onboarding_medidas, R.string.onboarding_medidas_titulo, R.string.onboarding_medidas_desc),
    Pagina(R.drawable.onboarding_fotos, R.string.onboarding_fotos_titulo, R.string.onboarding_fotos_desc),
    Pagina(R.drawable.onboarding_alimentacion, R.string.onboarding_alimentacion_titulo, R.string.onboarding_alimentacion_desc),
)

private val ALTURA_BOTON = 56.dp
private val MARGEN_BOTONES = 24.dp

@Composable
fun OnboardingScreen(
    onVolver: () -> Unit,
    onTerminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BarraEstadoClara()
    NutriGajoTheme(darkTheme = true) {
        val colores = MaterialTheme.colorScheme
        val estado = rememberPagerState(pageCount = { paginas.size })
        val scope = rememberCoroutineScope()
        val anterior = {
            if (estado.currentPage == 0) onVolver()
            else scope.launch { estado.animateScrollToPage(estado.currentPage - 1) }
            Unit
        }
        val siguiente = {
            if (estado.currentPage == paginas.lastIndex) onTerminar()
            else scope.launch { estado.animateScrollToPage(estado.currentPage + 1) }
            Unit
        }
        BackHandler(onBack = anterior)

        Box(modifier = modifier.fillMaxSize()) {
            HorizontalPager(state = estado, modifier = Modifier.fillMaxSize()) { indice ->
                val pagina = paginas[indice]
                FondoFoto(imagen = pagina.imagen) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            // deja libre la fila de botones fija
                            .padding(start = 24.dp, end = 24.dp, bottom = MARGEN_BOTONES + ALTURA_BOTON + 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(pagina.titulo),
                            style = MaterialTheme.typography.headlineLarge,
                            color = colores.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(pagina.descripcion),
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 1.4.em),
                            color = colores.onBackground,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            val avance by animateFloatAsState((estado.currentPage + 1f) / paginas.size, label = "avance")
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 64.dp, end = 64.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colores.scrim.copy(alpha = 0.32f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(avance)
                        .background(colores.onBackground),
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = MARGEN_BOTONES),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BotonNavegacion(R.drawable.ic_flecha_atras, R.string.onboarding_anterior, anterior, Modifier.weight(1f))
                BotonNavegacion(R.drawable.ic_flecha_adelante, R.string.onboarding_siguiente, siguiente, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BotonNavegacion(
    @DrawableRes icono: Int,
    @StringRes descripcion: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier.height(ALTURA_BOTON),
        shape = RoundedCornerShape(21.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
    ) {
        Icon(
            painter = painterResource(icono),
            contentDescription = stringResource(descripcion),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun OnboardingScreenPreview() {
    OnboardingScreen(onVolver = {}, onTerminar = {})
}
