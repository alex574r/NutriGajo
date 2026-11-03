package com.example.nutrigajo.ui.paciente.rutina

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.remember
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Entrenamiento
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.nutriologo.plan.BarraSuperior
import com.example.nutrigajo.ui.shared.ejercicios.DatosEnColumnas
import com.example.nutrigajo.ui.shared.ejercicios.titulo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MARGEN = 16.dp

// Los entrenamientos terminados, el más reciente primero, con lo acumulado arriba. Lo que se ve son los últimos 50.
@Composable
fun HistorialEntrenamientosScreen(
    onVolver: () -> Unit,
    // la misma instancia que «Entrenar ahora»: lo recién guardado ya está en la lista
    vm: EntrenamientosViewModel = viewModel(key = "entrenamientos"),
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    val estado by vm.estado.collectAsState()
    LaunchedEffect(Unit) { vm.cargar() }

    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding()) {
        BarraSuperior(titulo = stringResource(R.string.historial_titulo), onVolver = onVolver)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = MARGEN, end = MARGEN, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            estado.error?.let { item { AlertaError(stringResource(it)) } }
            if (estado.items.isNotEmpty()) {
                item {
                    DatosEnColumnas(
                        listOf(
                            estado.items.size.toString() to stringResource(R.string.historial_sesiones),
                            stringResource(R.string.ejercicio_minutos, estado.items.sumOf { it.minutos }) to stringResource(R.string.ejercicio_tiempo),
                            stringResource(R.string.ejercicio_calorias, estado.items.sumOf { it.kcal }) to stringResource(R.string.ejercicio_calorias_nombre),
                        ),
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            when {
                estado.cargando && estado.items.isEmpty() -> items(3) { Esqueleto(Modifier.fillMaxWidth().height(86.dp), RoundedCornerShape(26.dp)) }
                estado.items.isEmpty() -> item {
                    Text(
                        text = stringResource(R.string.historial_vacio),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN, vertical = 40.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> items(estado.items, key = { it.id.ifEmpty { it.fecha.toString() } }) { FilaEntrenamiento(it) }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun FilaEntrenamiento(e: Entrenamiento) {
    val colores = MaterialTheme.colorScheme
    val fecha = remember(e.fecha) {
        Instant.ofEpochMilli(e.fecha).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(colores.surfaceVariant, RoundedCornerShape(26.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.size(44.dp).background(if (e.cuentaParaLaRacha) colores.primaryContainer else colores.background, CircleShape),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_llama),
                contentDescription = if (e.cuentaParaLaRacha) stringResource(R.string.historial_cuenta_racha) else null,
                modifier = Modifier.size(18.dp),
                tint = if (e.cuentaParaLaRacha) colores.primary else colores.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(e.enfoque.titulo()),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colores.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = fecha.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.ejercicio_minutos, e.minutos) + " · " + stringResource(R.string.ejercicio_calorias, e.kcal),
                style = MaterialTheme.typography.labelLarge,
                color = colores.onBackground,
            )
            Text(
                text = stringResource(R.string.historial_ejercicios_de, e.hechos, e.total),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
            )
        }
    }
}
