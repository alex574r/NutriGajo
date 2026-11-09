package com.example.nutrigajo.ui.shared.ejercicios

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.ui.nutriologo.plan.BarraSuperior
import kotlinx.coroutines.launch

private val MARGEN = 16.dp

// La guía de un ejercicio: sus instrucciones de una en una, cada paso en una tarjeta grande que se pasa con el dedo o con «Siguiente»,
// con una barra que muestra por dónde vas. Detrás de los pasos va una página con lo que conviene saber antes de terminar (consejos y
// contraindicaciones). `inicio` es el paso con que se abre: el que se tocó en el detalle.
@Composable
fun GuiaEjercicioScreen(ejercicio: Ejercicio, inicio: Int, onSalir: () -> Unit) {
    BackHandler(onBack = onSalir)
    val colores = MaterialTheme.colorScheme
    val ambito = rememberCoroutineScope()
    val hayFinal = ejercicio.consejos.isNotBlank() || ejercicio.contraindicaciones.isNotBlank()
    val estado = rememberPasosEstado(ejercicio.pasos.size, hayFinal, inicio)
    val ultima = estado.currentPage >= estado.pageCount - 1

    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding()) {
        BarraSuperior(titulo = ejercicio.titulo, onVolver = onSalir)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = MARGEN),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MediaEjercicio(ejercicio, Modifier.fillMaxWidth().aspectRatio(16f / 10f), RoundedCornerShape(28.dp))
            PasosPager(
                pasos = ejercicio.pasos,
                estado = estado,
                grande = true,
                final = if (hayFinal) {
                    {
                        Text(stringResource(R.string.ejercicio_antes_terminar), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
                        if (ejercicio.consejos.isNotBlank()) Parrafo(R.string.ejercicio_consejos, ejercicio.consejos)
                        if (ejercicio.contraindicaciones.isNotBlank()) Parrafo(R.string.ejercicio_contraindicaciones, ejercicio.contraindicaciones)
                    }
                } else {
                    null
                },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(MARGEN),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { ambito.launch { estado.animateScrollToPage(estado.currentPage - 1) } },
                enabled = estado.currentPage > 0,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(21.dp),
            ) { Text(stringResource(R.string.ejercicio_guia_anterior)) }
            Button(
                onClick = { if (ultima) onSalir() else ambito.launch { estado.animateScrollToPage(estado.currentPage + 1) } },
                modifier = Modifier.weight(1.3f).height(52.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colores.primary, contentColor = colores.onPrimary),
            ) { Text(stringResource(if (ultima) R.string.ejercicio_guia_terminar else R.string.ejercicio_guia_siguiente)) }
        }
    }
}
