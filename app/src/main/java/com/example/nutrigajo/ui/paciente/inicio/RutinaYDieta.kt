package com.example.nutrigajo.ui.paciente.inicio

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

@Composable
fun DietaNutricion(comidas: List<Comida>, onVerTodas: () -> Unit, onComida: (Comida) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        EncabezadoSeccion(stringResource(R.string.inicio_dieta), verTodas = true, onVerTodas = onVerTodas)
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(comidas) { comida -> TarjetaComida(comida, onClick = { onComida(comida) }) }
        }
    }
}

// Toda la tarjeta abre la comida, no solo la flecha.
@Composable
private fun TarjetaComida(comida: Comida, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    Box(Modifier.size(283.dp, 233.dp).alPulsar(onClick, minimo = 0.97f).clip(RoundedCornerShape(28.dp)).background(colores.surfaceVariant)) {
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
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background.copy(alpha = 0f), MaterialTheme.colorScheme.background.copy(alpha = 0.7f)))),
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
        modifier = Modifier.size(52.dp, 47.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(valor, style = tipos.titleSmall, color = MaterialTheme.colorScheme.tertiary)
        Text(nombre, style = tipos.labelLarge.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.tertiary, maxLines = 1)
    }
}
