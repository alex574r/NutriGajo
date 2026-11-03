package com.example.nutrigajo.ui.paciente.inicio

import com.example.nutrigajo.ui.shared.platillos.TarjetaPlatillo
import com.example.nutrigajo.ui.shared.platillos.PlatillosEstado
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.domain.model.Platillo
import com.example.nutrigajo.ui.shared.ejercicios.titulo
import com.example.nutrigajo.ui.shared.ejercicios.intensidad
import com.example.nutrigajo.ui.shared.ejercicios.FotoEjercicio
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.domain.model.RutinaDelDia
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.components.cristal

internal val TEXTO_SOBRE_FOTO = Color(0xFFD0D0D0)
internal val SOMBRA_TEXTO = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 3f), 6f)

// «Rutinas de hoy»: la rutina del día armada con el banco global (ver rutinaDelDia). Toda la tarjeta, y su botón de play, abren «Entrenar
// ahora» y «Historial» abre lo ya entrenado. Mientras se lee el banco se ve un esqueleto; sin ejercicios publicados no hay rutina y la sección no aparece.
// ponytail: el botón de la comida sigue sin abrir nada; el detalle llega con los planes.
@Composable
fun RutinaDeHoy(rutina: RutinaDelDia?, cargando: Boolean, onIniciar: () -> Unit, onHistorial: () -> Unit, modifier: Modifier = Modifier) {
    val tipos = MaterialTheme.typography
    if (rutina == null) {
        if (cargando) {
            Column(modifier) {
                EncabezadoSeccion(stringResource(R.string.inicio_rutinas))
                Spacer(Modifier.height(12.dp))
                Esqueleto(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(234.dp), RoundedCornerShape(31.dp))
            }
        }
        return
    }
    Column(modifier) {
        EncabezadoSeccion(
            titulo = stringResource(R.string.inicio_rutinas),
            contador = "(${rutina.ejercicios.size})",
            verTodas = true,
            etiquetaVerTodas = R.string.rutina_historial,
            onVerTodas = onHistorial,
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(234.dp)
                .clip(RoundedCornerShape(31.dp))
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.rutina_iniciar), onClick = onIniciar),
        ) {
            FotoEjercicio(rutina.ejercicios.first(), Modifier.matchParentSize(), RoundedCornerShape(0.dp))
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.3f), 0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.6f)),
                ),
            )
            Row(Modifier.padding(start = 15.dp, top = 19.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_reloj), contentDescription = null, tint = TEXTO_SOBRE_FOTO)
                Text(
                    text = stringResource(R.string.rutina_minutos, rutina.minutos),
                    modifier = Modifier.padding(start = 3.dp),
                    style = tipos.labelLarge,
                    color = TEXTO_SOBRE_FOTO,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_llama),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 14.dp).size(16.dp),
                    tint = TEXTO_SOBRE_FOTO,
                )
                Text(
                    text = stringResource(R.string.rutina_kcal, rutina.kcal),
                    modifier = Modifier.padding(start = 3.dp),
                    style = tipos.labelLarge,
                    color = TEXTO_SOBRE_FOTO,
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 26.dp, end = 70.dp)) {
                Text(stringResource(rutina.enfoque.titulo()), style = tipos.titleLarge.copy(shadow = SOMBRA_TEXTO), color = Color.White, maxLines = 2)
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = pluralStringResource(R.plurals.rutina_ejercicios, rutina.ejercicios.size, rutina.ejercicios.size),
                        style = tipos.bodySmall.copy(shadow = SOMBRA_TEXTO),
                        color = Color.White,
                    )
                    Text(
                        text = stringResource(rutina.nivel.intensidad()),
                        modifier = Modifier
                            .cristal(RoundedCornerShape(7.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        style = tipos.labelLarge,
                        color = Color.White,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 21.dp)
                    .size(36.dp)
                    // el botón de jugar es cristal teñido de naranja
                    .cristal(RoundedCornerShape(12.dp), velo = Color(0xFFFF974E))
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_play), contentDescription = stringResource(R.string.rutina_iniciar), tint = Color.White)
            }
        }
    }
}

// «Dieta & nutrición»: los platillos publicados del banco global (los que crean los nutriólogos y entrenadores verificados) en tarjetas;
// tocar una abre su receta y «Ver todas», el banco con búsqueda y filtros. Mientras cargan se ven esqueletos; sin platillos o sin red queda
// la entrada de siempre, para no perder el acceso. Debajo, «Mis comidas» abre el día comida por comida (de muestra por ahora).
@Composable
fun DietaNutricion(
    estado: PlatillosEstado,
    platillos: List<Platillo>,
    onAbrir: (String?) -> Unit,
    onMisComidas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        EncabezadoSeccion(stringResource(R.string.inicio_dieta), verTodas = platillos.isNotEmpty(), onVerTodas = { onAbrir(null) })
        Spacer(Modifier.height(12.dp))
        when {
            platillos.isNotEmpty() -> LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(platillos.take(PLATILLOS_RECIENTES), key = { it.id }) { platillo -> TarjetaPlatillo(platillo, onClick = { onAbrir(platillo.id) }) }
            }
            estado.cargando -> LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(2) { Esqueleto(Modifier.size(283.dp, 233.dp), RoundedCornerShape(28.dp)) }
            }
            else -> FilaAjuste(
                icono = R.drawable.ic_documento,
                titulo = stringResource(R.string.inicio_dieta_explorar),
                modifier = Modifier.padding(horizontal = 16.dp),
                etiqueta = stringResource(R.string.inicio_dieta_etiqueta),
                onClick = { onAbrir(null) },
            )
        }
        Spacer(Modifier.height(10.dp))
        FilaAjuste(
            icono = R.drawable.ic_calendario,
            titulo = stringResource(R.string.inicio_mis_comidas),
            modifier = Modifier.padding(horizontal = 16.dp),
            etiqueta = stringResource(R.string.inicio_mis_comidas_etiqueta),
            onClick = onMisComidas,
        )
    }
}

private const val PLATILLOS_RECIENTES = 6
