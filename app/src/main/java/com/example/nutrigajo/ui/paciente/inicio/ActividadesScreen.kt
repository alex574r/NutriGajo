package com.example.nutrigajo.ui.paciente.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.shared.ejercicios.titulo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// «Ver todas» de Actividades: lo que la app guardó y suma a las calorías (caminatas, carreras y rutinas), por día. Los entrenamientos del
// reloj no se listan: Health Connect no da las calorías de cada uno, solo el total del día.
// ponytail: sin detalle al tocar una fila (la ruta y los ejercicios se ven en el Modo carrera y en «Mis entrenamientos»); solo las últimas
// 100 de cada una (el límite de `CaloriasViewModel`).
@Composable
fun ActividadesScreen(estado: CaloriasAppEstado, onVolver: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val zona = remember { ZoneId.systemDefault() }
    val dias = remember(estado.actividades) { actividadesPorDia(estado.actividades, zona) }
    PantallaMetrica(stringResource(R.string.inicio_actividades), onVolver) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            estado.error?.let { AlertaError(stringResource(it)) }
            when {
                estado.cargando && dias.isEmpty() -> repeat(3) { Esqueleto(Modifier.fillMaxWidth().height(86.dp), RoundedCornerShape(26.dp)) }
                dias.isEmpty() -> Text(
                    text = stringResource(R.string.actividades_vacio),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                else -> {
                    val hoy = remember { LocalDate.now(zona) }
                    dias.forEach { (dia, filas) ->
                        Text(
                            text = rotuloDelDia(dia, hoy),
                            modifier = Modifier.padding(top = 10.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = colores.onBackground,
                        )
                        filas.forEach { FilaActividad(it, zona) }
                    }
                    Text(stringResource(R.string.actividades_nota), style = MaterialTheme.typography.bodySmall, color = colores.outline)
                }
            }
        }
    }
}

@Composable
private fun rotuloDelDia(dia: LocalDate, hoy: LocalDate): String = when (dia) {
    hoy -> stringResource(R.string.actividades_hoy)
    hoy.minusDays(1) -> stringResource(R.string.actividades_ayer)
    else -> DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()).format(dia).replaceFirstChar { it.uppercase() }
}

@Composable
private fun FilaActividad(f: ActividadFila, zona: ZoneId) {
    val colores = MaterialTheme.colorScheme
    val hora = remember(f.instante) { DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(f.instante).atZone(zona)) }
    val icono = when (f.clase) {
        ClaseActividad.CAMINATA -> R.drawable.ic_caminar
        ClaseActividad.CARRERA -> R.drawable.ic_correr
        ClaseActividad.RUTINA -> R.drawable.ic_llama
    }
    val titulo = when (f.clase) {
        ClaseActividad.CAMINATA -> stringResource(R.string.actividad_caminata)
        ClaseActividad.CARRERA -> stringResource(R.string.actividad_carrera)
        ClaseActividad.RUTINA -> f.enfoque?.let { stringResource(it.titulo()) } ?: stringResource(R.string.actividad_rutina)
    }
    // la hora y, si la hubo, la distancia
    val detalle = if (f.distanciaM > 0) {
        hora + " · " + String.format(Locale.getDefault(), "%.1f", f.distanciaM / 1000.0) + " " + stringResource(R.string.carrera_unidad_km)
    } else {
        hora
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(colores.surfaceVariant, RoundedCornerShape(26.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.size(44.dp).background(colores.primaryContainer, CircleShape),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(20.dp), tint = colores.primary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colores.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = detalle, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.ejercicio_calorias, f.kcal), style = MaterialTheme.typography.labelLarge, color = colores.onBackground)
            Text(stringResource(R.string.ejercicio_minutos, f.minutos), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
        }
    }
}
