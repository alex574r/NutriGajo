package com.example.nutrigajo.ui.nutriologo.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.EjercicioAsignado
import com.example.nutrigajo.domain.model.PlanContenido
import com.example.nutrigajo.domain.model.delDia
import com.example.nutrigajo.domain.model.dosis
import com.example.nutrigajo.domain.model.valido
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto

// Lo que escribió el nutriólogo en los campos de la hoja, como asignado: null si no pasa las reglas (día entre 1 y 7, series o minutos
// dentro de su rango; las series o los minutos son obligatorios, solo las repeticiones no bastan).
internal fun EjercicioAsignado.Companion.desde(
    ejercicio: Ejercicio,
    dia: Int,
    series: String,
    repeticiones: String,
    minutos: String,
    nota: String,
    // el último día que admite el plan: 7 de la semana o su duración
    maxDia: Int = 7,
): EjercicioAsignado? {
    val s = series.trim().toIntOrNull()
    val m = minutos.trim().toIntOrNull()
    if (s == null && m == null) return null
    return EjercicioAsignado(
        dia = dia,
        fuente = ejercicio.fuente,
        id = ejercicio.id,
        titulo = ejercicio.titulo,
        series = s,
        repeticiones = repeticiones.trim().toIntOrNull(),
        minutos = m,
        nota = nota.trim(),
    ).takeIf { it.valido(maxDia) }
}

// Hay lugar en ese día (hasta 10) y en el plan (hasta 35).
internal fun PlanContenido.puedeAsignar(dia: Int): Boolean =
    ejerciciosAsignados.size < maxAsignados && ejerciciosAsignados.delDia(dia).size < PlanContenido.MAX_ASIGNADOS_DIA

// La hoja para asignar un ejercicio del banco: series y repeticiones o minutos, una nota opcional y uno o varios días. Los días sin lugar o
// donde ya está ese ejercicio no se pueden elegir.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AsignarEjercicioSheet(
    ejercicio: Ejercicio,
    plan: PlanContenido,
    onAsignar: (List<EjercicioAsignado>) -> Unit,
    onCerrar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    var series by remember { mutableStateOf("3") }
    var repeticiones by remember { mutableStateOf("10") }
    var minutos by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    val dias = remember { mutableStateListOf<Int>() }
    // los días de la semana o, en un plan con duración, los números de sus días
    val semana = stringArrayResource(R.array.plan_dias)
    val nombres = plan.duracionDias?.let { n -> List(n) { (it + 1).toString() } } ?: semana.toList()
    val nuevos = dias.sorted().mapNotNull { EjercicioAsignado.desde(ejercicio, it, series, repeticiones, minutos, nota, maxDia = nombres.size) }
    // elegir días de más que ya no caben en el plan deja el botón apagado en vez de recortar en silencio
    val cabe = plan.ejerciciosAsignados.size + dias.size <= plan.maxAsignados

    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(ejercicio.titulo, style = MaterialTheme.typography.titleLarge, color = colores.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto(
                    etiqueta = stringResource(R.string.plan_asignar_series),
                    valor = series,
                    onValorChange = { series = it.filter(Char::isDigit).take(2) },
                    modifier = Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                CampoTexto(
                    etiqueta = stringResource(R.string.plan_asignar_repeticiones),
                    valor = repeticiones,
                    onValorChange = { repeticiones = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                CampoTexto(
                    etiqueta = stringResource(R.string.plan_asignar_minutos),
                    valor = minutos,
                    onValorChange = { minutos = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.weight(1f),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            Text(
                stringResource(if (plan.duracionDias != null) R.string.plan_asignar_dia_plan else R.string.plan_asignar_dias),
                style = MaterialTheme.typography.titleSmall,
                color = colores.onBackground,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                nombres.forEachIndexed { i, nombre ->
                    val dia = i + 1
                    val elegido = dia in dias
                    val libre = plan.puedeAsignar(dia) && plan.ejerciciosAsignados.none { it.dia == dia && it.id == ejercicio.id }
                    Text(
                        text = nombre,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (elegido) colores.primary else colores.surfaceVariant)
                            .selectable(selected = elegido, enabled = libre, role = Role.Checkbox, onClick = { if (elegido) dias.remove(dia) else dias.add(dia) })
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            !libre -> colores.outline
                            elegido -> colores.onPrimary
                            else -> colores.onBackground
                        },
                    )
                }
            }
            CampoTexto(
                etiqueta = stringResource(R.string.plan_asignar_nota),
                valor = nota,
                onValorChange = { nota = it.take(EjercicioAsignado.MAX_NOTA) },
                modifier = Modifier.fillMaxWidth(),
                lineas = 2,
            )
            if (!cabe) {
                Text(stringResource(R.string.plan_asignar_lleno), style = MaterialTheme.typography.bodySmall, color = colores.error)
            }
            BotonPrimario(
                texto = stringResource(R.string.plan_asignar_confirmar),
                onClick = { onAsignar(nuevos) },
                modifier = Modifier.fillMaxWidth(),
                enabled = dias.isNotEmpty() && nuevos.size == dias.size && cabe,
            )
        }
    }
}

// Lo asignado, por día de lunes a domingo: cada fila con el título, su dosis y un botón para quitarlo.
@Composable
internal fun ListaAsignados(asignados: List<EjercicioAsignado>, nombres: List<String>, onQuitar: (EjercicioAsignado) -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        nombres.forEachIndexed { i, nombre ->
            val delDia = asignados.delDia(i + 1)
            if (delDia.isNotEmpty()) {
                Text(nombre, style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
                delDia.forEach { a ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(colores.surfaceVariant, RoundedCornerShape(18.dp)).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(a.titulo, style = MaterialTheme.typography.titleSmall, color = colores.onBackground, maxLines = 1)
                            Text(a.dosis(), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                        }
                        TextButton(onClick = { onQuitar(a) }) { Text(stringResource(R.string.plan_quitar), color = colores.error) }
                    }
                }
            }
        }
    }
}
