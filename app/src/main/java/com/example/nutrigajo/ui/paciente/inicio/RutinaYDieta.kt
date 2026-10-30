package com.example.nutrigajo.ui.paciente.inicio

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
import com.example.nutrigajo.ui.components.cristal

private val TEXTO_SOBRE_FOTO = Color(0xFFD0D0D0)
private val SOMBRA_TEXTO = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 3f), 6f)

// ponytail: los botones de reproducir y de la comida no abren nada todavía; el detalle llega con las rutinas y los planes.
@Composable
fun RutinaDeHoy(rutina: Rutina, modifier: Modifier = Modifier) {
    val tipos = MaterialTheme.typography
    Column(modifier) {
        EncabezadoSeccion(stringResource(R.string.inicio_rutinas), contador = "(25)", menu = true)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(234.dp)
                .clip(RoundedCornerShape(31.dp)),
        ) {
            Image(
                painter = painterResource(rutina.foto),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
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
            Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 26.dp)) {
                Text(rutina.titulo, style = tipos.titleLarge.copy(shadow = SOMBRA_TEXTO), color = Color.White)
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.rutina_series, rutina.series),
                        style = tipos.bodySmall.copy(shadow = SOMBRA_TEXTO),
                        color = Color.White,
                    )
                    Text(
                        text = rutina.intensidad,
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

@Composable
fun DietaNutricion(comidas: List<Comida>, modifier: Modifier = Modifier) {
    Column(modifier) {
        EncabezadoSeccion(stringResource(R.string.inicio_dieta), verTodas = true)
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(comidas) { TarjetaComida(it) }
        }
    }
}

@Composable
private fun TarjetaComida(comida: Comida) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    Box(Modifier.size(283.dp, 233.dp).clip(RoundedCornerShape(28.dp)).background(colores.surfaceVariant)) {
        // el plato sobresale por arriba y por la derecha: la tarjeta lo recorta
        Image(
            painter = painterResource(comida.foto),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopStart).offset(x = 73.dp, y = (-51).dp).size(284.dp),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(137.dp)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.7f)))),
        )
        Column(Modifier.padding(start = 23.dp, top = 26.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Macro(stringResource(R.string.dieta_gramos, comida.proteinaG), stringResource(R.string.dieta_proteina))
            Macro(stringResource(R.string.dieta_gramos, comida.grasasG), stringResource(R.string.dieta_grasas))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 16.dp)) {
            Text(comida.titulo, style = tipos.titleMedium, color = colores.tertiary, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(painterResource(R.drawable.ic_llama_solida), contentDescription = null, modifier = Modifier.size(16.dp), tint = colores.onSurfaceVariant)
                Text(stringResource(R.string.rutina_kcal, comida.kcal), style = tipos.bodyMedium, color = colores.onSurfaceVariant)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 13.dp, bottom = 22.dp)
                .size(55.dp, 56.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(colores.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_flecha),
                contentDescription = stringResource(R.string.dieta_ver),
                modifier = Modifier.size(20.dp),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun Macro(valor: String, nombre: String) {
    val tipos = MaterialTheme.typography
    Column(
        modifier = Modifier.size(52.dp, 47.dp).background(Color.White, RoundedCornerShape(10.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(valor, style = tipos.titleSmall, color = MaterialTheme.colorScheme.tertiary)
        Text(nombre, style = tipos.labelLarge.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.tertiary, maxLines = 1)
    }
}
