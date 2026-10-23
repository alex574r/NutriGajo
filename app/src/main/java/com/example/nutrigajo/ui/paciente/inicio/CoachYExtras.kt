package com.example.nutrigajo.ui.paciente.inicio

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.Etiqueta

// ponytail: los mensajes pendientes son de muestra y el botón no abre nada; el chat con el nutriólogo llega después.
// La foto del coach es la del diseño; se sustituye por la del nutriólogo cuando exista el almacenamiento de fotos.
@Composable
fun TarjetaCoach(nutriologo: String, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    Column(modifier) {
        EncabezadoSeccion(stringResource(R.string.inicio_coach), contador = nutriologo, menu = true)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(227.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(colores.primary),
        ) {
            Image(
                painter = painterResource(R.drawable.inicio_coach),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp).size(167.dp, 250.dp),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color(0xFFA64400).copy(alpha = 0.52f)),
                ),
            )
            Column(Modifier.padding(start = 16.dp, top = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Etiqueta(stringResource(R.string.coach_nueva_rutina))
                Etiqueta(stringResource(R.string.coach_nueva_dieta))
            }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp)) {
                Text("5+", style = tipos.headlineLarge, color = Color.White)
                Text(stringResource(R.string.coach_mensajes), style = tipos.titleLarge, color = Color.White)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 15.dp)
                    .size(55.dp, 56.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mensaje),
                    contentDescription = stringResource(R.string.coach_abrir_mensajes),
                    tint = colores.primary,
                )
            }
        }
    }
}

// ponytail: sin destino todavía; el catálogo de ejercicios llega con las rutinas.
@Composable
fun EjerciciosExtra(ejercicios: List<EjercicioExtra>, modifier: Modifier = Modifier) {
    Column(modifier) {
        EncabezadoSeccion(stringResource(R.string.inicio_extra))
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            ejercicios.forEach { FilaEjercicio(it) }
        }
    }
}

@Composable
private fun FilaEjercicio(ejercicio: EjercicioExtra) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(107.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(colores.surfaceVariant)
            .padding(horizontal = 19.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(ejercicio.foto),
            contentDescription = null,
            modifier = Modifier.size(79.dp, 80.dp).clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(ejercicio.titulo, style = tipos.titleMedium.copy(fontWeight = FontWeight.Bold), color = colores.onBackground, maxLines = 1)
            Text(ejercicio.subtitulo, style = tipos.bodySmall, color = colores.onSurfaceVariant, maxLines = 1)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Estadistica(R.drawable.ic_valoracion, ejercicio.valoracion)
                Estadistica(R.drawable.ic_corazon, ejercicio.favoritos.toString())
                Estadistica(R.drawable.ic_ojo, ejercicio.vistas)
            }
        }
        Icon(painterResource(R.drawable.ic_flecha), contentDescription = null, modifier = Modifier.size(22.dp), tint = colores.primary)
    }
}

@Composable
private fun Estadistica(@DrawableRes icono: Int, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        Text(texto, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}
