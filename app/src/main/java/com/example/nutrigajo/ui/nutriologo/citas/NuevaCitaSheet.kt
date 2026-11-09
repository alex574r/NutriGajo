package com.example.nutrigajo.ui.nutriologo.citas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.Agenda
import com.example.nutrigajo.domain.model.PacienteResumen
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.paciente.citas.Cargando
import com.example.nutrigajo.ui.paciente.citas.DiaChip
import com.example.nutrigajo.ui.paciente.citas.HoraChip
import com.example.nutrigajo.ui.shared.citas.diaYHora
import com.example.nutrigajo.ui.shared.citas.horaCorta
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val COLUMNAS_HORAS = 4

// Hoja para agendar una cita a un paciente: se elige al paciente (si no viene elegido), el día, la hora libre y una nota opcional.
// Terminado el envío la misma hoja dice que la cita quedó y que al paciente se le avisó (o que no se pudo avisar).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaCitaSheet(
    estado: NuevaCitaEstado,
    pacientes: List<PacienteResumen>,
    onPaciente: (PacienteResumen) -> Unit,
    onDia: (LocalDate) -> Unit,
    onHora: (Long) -> Unit,
    onNota: (String) -> Unit,
    onAgendar: () -> Unit,
    onCerrar: () -> Unit,
) {
    if (!estado.abierta) return
    val colores = MaterialTheme.colorScheme
    val paciente = estado.paciente
    // mientras la cita se envía la hoja no se cierra (ni deslizándola ni con «atrás»): la respuesta tiene que llegar a esta hoja
    val enviando by rememberUpdatedState(estado.enviando)
    ModalBottomSheet(
        onDismissRequest = { if (!estado.enviando) onCerrar() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden || !enviando }),
        containerColor = colores.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                estado.agendada -> Agendada(paciente?.nombre.orEmpty(), estado.hora, estado.sinAviso, onCerrar)
                paciente == null -> ElegirPaciente(pacientes, onPaciente)
                else -> Formulario(estado, paciente, onDia, onHora, onNota, onAgendar)
            }
        }
    }
}

@Composable
private fun ElegirPaciente(pacientes: List<PacienteResumen>, onPaciente: (PacienteResumen) -> Unit) {
    val colores = MaterialTheme.colorScheme
    Text(stringResource(R.string.nueva_cita_titulo), style = MaterialTheme.typography.headlineMedium, color = colores.onBackground)
    if (pacientes.isEmpty()) {
        Text(stringResource(R.string.nueva_cita_sin_pacientes), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
    } else {
        Text(stringResource(R.string.nueva_cita_elegir_paciente), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
        pacientes.forEach { FilaAjuste(R.drawable.ic_pacientes, titulo = it.nombre, etiqueta = it.correo, onClick = { onPaciente(it) }) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Formulario(
    estado: NuevaCitaEstado,
    paciente: PacienteResumen,
    onDia: (LocalDate) -> Unit,
    onHora: (Long) -> Unit,
    onNota: (String) -> Unit,
    onAgendar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val horarios = remember(estado.dia, estado.ocupados) {
        estado.dia?.let { Agenda.horarios(it, estado.ocupados, Instant.now(), ZoneId.systemDefault()) }.orEmpty()
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(R.string.nueva_cita_titulo), style = MaterialTheme.typography.headlineMedium, color = colores.onBackground)
        Text(stringResource(R.string.agendar_con, paciente.nombre), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
    }
    estado.yaTiene?.let {
        Text(stringResource(R.string.nueva_cita_ya_tiene, diaYHora(it.inicio)), style = MaterialTheme.typography.bodyMedium, color = colores.primary)
    }
    when {
        estado.cargando -> Cargando()
        estado.dias.isEmpty() -> Text(stringResource(R.string.nueva_cita_sin_horarios), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
        else -> {
            Text(stringResource(R.string.agendar_dia), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(estado.dias, key = { it.toEpochDay() }) { dia -> DiaChip(dia, dia == estado.dia) { onDia(dia) } }
            }
            Text(stringResource(R.string.agendar_hora), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
            // cuadrícula de cuatro columnas iguales: los huecos del final mantienen el ancho de las últimas horas
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = COLUMNAS_HORAS,
            ) {
                horarios.forEach { HoraChip(horaCorta(it.inicio), it.inicio == estado.hora, it.libre) { onHora(it.inicio) } }
                repeat((COLUMNAS_HORAS - horarios.size % COLUMNAS_HORAS) % COLUMNAS_HORAS) { Spacer(Modifier.weight(1f)) }
            }
            CampoTexto(
                etiqueta = stringResource(R.string.nueva_cita_nota),
                valor = estado.nota,
                onValorChange = onNota,
                modifier = Modifier.fillMaxWidth(),
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
    }
    estado.error?.let { AlertaError(stringResource(it)) }
    estado.hora?.let { Text(diaYHora(it), style = MaterialTheme.typography.titleMedium, color = colores.onBackground) }
    BotonPrimario(
        texto = stringResource(R.string.nueva_cita_enviar),
        onClick = onAgendar,
        modifier = Modifier.fillMaxWidth(),
        enabled = estado.hora != null && !estado.cargando,
        cargando = estado.enviando,
    )
    Text(stringResource(R.string.nueva_cita_ayuda, paciente.nombre), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
}

@Composable
private fun Agendada(nombre: String, hora: Long?, sinAviso: Boolean, onCerrar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(colores.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_calendario), contentDescription = null, modifier = Modifier.size(32.dp), tint = colores.onPrimaryContainer)
        }
        Text(
            text = stringResource(R.string.nueva_cita_agendada_titulo),
            style = MaterialTheme.typography.headlineMedium,
            color = colores.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.nueva_cita_agendada_ayuda, nombre, hora?.let { diaYHora(it) }.orEmpty()),
            style = MaterialTheme.typography.bodyLarge,
            color = colores.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (sinAviso) {
            Text(
                text = stringResource(R.string.nueva_cita_sin_aviso),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.primary,
                textAlign = TextAlign.Center,
            )
        }
        BotonPrimario(texto = stringResource(R.string.agendar_listo), onClick = onCerrar, modifier = Modifier.fillMaxWidth())
    }
}
