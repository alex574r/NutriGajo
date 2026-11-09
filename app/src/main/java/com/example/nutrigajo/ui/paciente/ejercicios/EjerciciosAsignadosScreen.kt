package com.example.nutrigajo.ui.paciente.ejercicios

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.CalendarioPlan
import com.example.nutrigajo.domain.model.Ejercicio
import java.time.format.DateTimeFormatter
import com.example.nutrigajo.domain.model.EjercicioAsignado
import com.example.nutrigajo.domain.model.FuenteEjercicio
import com.example.nutrigajo.domain.model.delDia
import com.example.nutrigajo.domain.model.dosis
import com.example.nutrigajo.domain.model.resolverDelBanco
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.nutriologo.plan.BarraSuperior
import com.example.nutrigajo.ui.shared.ejercicios.DetalleEjercicio
import com.example.nutrigajo.ui.shared.ejercicios.FilaEjercicio
import com.example.nutrigajo.ui.shared.ejercicios.GuiaEjercicioScreen

private val MARGEN = 16.dp

// Lo que el nutriólogo asignó al paciente en su plan más reciente, por día de lunes a domingo. Cada fila abre el detalle del ejercicio
// (con su animación) y de ahí la guía paso a paso; si el ejercicio ya no está publicado, la fila dice «Ya no está disponible» y no abre nada.
// ponytail: la dosis (series, repeticiones, nota) se ve en la fila y no dentro del detalle.
@Composable
fun EjerciciosAsignadosScreen(
    asignados: List<EjercicioAsignado>,
    banco: List<Ejercicio>,
    cargando: Boolean,
    // las fichas ya bajadas de lo asignado que viene del catálogo en la nube, por id, y si todavía se están bajando
    catalogo: Map<String, Ejercicio>,
    cargandoCatalogo: Boolean,
    // cómo se cuentan los días del plan: en uno con duración cada día lleva su fecha («Día 3 · 9 nov»); null o semanal, lunes a domingo
    calendario: CalendarioPlan?,
    onVolver: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val resolver = remember(banco, catalogo) { resolverDelBanco(banco, catalogo) }
    val semana = stringArrayResource(R.array.plan_dias).toList()
    val formatoFecha = remember { DateTimeFormatter.ofPattern("d MMM") }
    val nombres = if (calendario?.duracionDias != null) {
        List(calendario.dias) { i ->
            val dia = stringResource(R.string.plan_dia_n, i + 1)
            calendario.fechaDe(i + 1)?.let { "$dia · ${formatoFecha.format(it)}" } ?: dia
        }
    } else {
        semana
    }
    var abiertoId by rememberSaveable { mutableStateOf<String?>(null) }
    // el paso en que se abrió la guía del ejercicio abierto; null es el detalle
    var guiaPaso by rememberSaveable { mutableStateOf<Int?>(null) }

    val abierto = banco.firstOrNull { it.id == abiertoId } ?: catalogo[abiertoId]
    if (abierto != null) {
        val paso = guiaPaso
        if (paso != null) {
            GuiaEjercicioScreen(ejercicio = abierto, inicio = paso, onSalir = { guiaPaso = null })
        } else {
            DetalleEjercicio(ejercicio = abierto, onVolver = { abiertoId = null }, onEmpezar = { guiaPaso = it })
        }
        return
    }
    BackHandler(onBack = onVolver)
    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding()) {
        BarraSuperior(titulo = stringResource(R.string.asignados_titulo), onVolver = onVolver)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = MARGEN, end = MARGEN, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (cargando && banco.isEmpty()) {
                items(3) { Esqueleto(Modifier.fillMaxWidth().height(107.dp), RoundedCornerShape(34.dp)) }
            } else {
                nombres.forEachIndexed { i, nombre ->
                    val delDia = asignados.delDia(i + 1)
                    if (delDia.isEmpty()) return@forEachIndexed
                    item(key = "dia-$i") { Text(nombre, style = MaterialTheme.typography.titleMedium, color = colores.onBackground, modifier = Modifier.padding(top = 8.dp)) }
                    delDia.forEachIndexed { n, asignado ->
                        item(key = "asignado-$i-$n-${asignado.id}") {
                            val ejercicio = resolver(asignado)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (ejercicio != null) {
                                    FilaEjercicio(ejercicio, onClick = { abiertoId = ejercicio.id })
                                } else if (asignado.fuente == FuenteEjercicio.CATALOGO && cargandoCatalogo) {
                                    Esqueleto(Modifier.fillMaxWidth().height(107.dp), RoundedCornerShape(34.dp))
                                } else {
                                    Text(asignado.titulo, style = MaterialTheme.typography.titleMedium, color = colores.onSurfaceVariant)
                                    Text(stringResource(R.string.asignados_no_disponible), style = MaterialTheme.typography.bodySmall, color = colores.error)
                                }
                                Text(
                                    text = listOf(asignado.dosis(), asignado.nota).filter { it.isNotBlank() }.joinToString(" · "),
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colores.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            item { androidx.compose.foundation.layout.Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
