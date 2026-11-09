package com.example.nutrigajo.ui.paciente.citas

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.ui.permisos.rememberPermiso
import com.example.nutrigajo.domain.PermisoApp
import com.example.nutrigajo.domain.EstadoPermiso
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.Agenda
import com.example.nutrigajo.domain.model.Cita
import com.example.nutrigajo.domain.model.EstadoCita
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.shared.citas.diaCorto
import com.example.nutrigajo.ui.shared.citas.diaLargo
import com.example.nutrigajo.ui.shared.citas.diaYHora
import com.example.nutrigajo.ui.shared.citas.horaCorta
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val COLUMNAS_HORAS = 4

// Hoja para pedir una cita: el paciente elige uno de los próximos días y una hora libre del nutriólogo y manda la solicitud.
// Terminado el envío la misma hoja le dice que el nutriólogo la confirmará. Solo se puede tener una cita vigente: si ya hay una,
// la hoja la muestra para reagendarla (abre esta misma agenda, para pasarla a otra hora) o cancelarla.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgendarCitaSheet(
    estado: AgendaEstado,
    onDia: (LocalDate) -> Unit,
    onHora: (Long) -> Unit,
    onEnviar: () -> Unit,
    onReagendar: (Cita) -> Unit,
    onCancelarVigente: () -> Unit,
    onCerrar: () -> Unit,
) {
    if (estado.nutriologoId == null) return
    val nombre = estado.nutriologoNombre
    val reagendando = estado.reagendando
    val vigente = estado.citaVigente
    val colores = MaterialTheme.colorScheme
    // desde Android 13 los avisos piden permiso: se ofrece al solicitar la cita mientras falte (y Android todavía deje pedirlo) y la solicitud sigue
    // diga lo que diga la persona; con el permiso bloqueado no se insiste
    val avisos = rememberPermiso(PermisoApp.NOTIFICACIONES, alResponder = onEnviar)
    val enviar = { if (avisos.estado == EstadoPermiso.FALTA) avisos.pedir() else onEnviar() }
    val horarios = remember(estado.dia, estado.ocupados) {
        estado.dia?.let { Agenda.horarios(it, estado.ocupados, Instant.now(), ZoneId.systemDefault()) }.orEmpty()
    }

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
            if (estado.enviada) {
                Enviada(nombre, reagendada = reagendando != null, onCerrar)
            } else if (vigente != null) {
                YaTienesCita(vigente, estado.cancelando, estado.error, onReagendar = { onReagendar(vigente) }, onCancelar = onCancelarVigente)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(if (reagendando != null) R.string.agendar_reagendar_titulo else R.string.agendar_titulo),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colores.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.agendar_con, nombre),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.onSurfaceVariant,
                    )
                    reagendando?.let {
                        Text(
                            text = stringResource(R.string.agendar_reagendar_actual, diaYHora(it.inicio)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colores.onSurfaceVariant,
                        )
                    }
                }
                when {
                    estado.cargando -> Cargando()
                    estado.dias.isEmpty() -> Text(
                        text = stringResource(R.string.agendar_sin_horarios),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.onSurfaceVariant,
                    )
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
                    }
                }
                estado.error?.let { AlertaError(stringResource(it)) }
                estado.hora?.let { Text(diaYHora(it), style = MaterialTheme.typography.titleMedium, color = colores.onBackground) }
                BotonPrimario(
                    texto = stringResource(if (reagendando != null) R.string.agendar_reagendar_enviar else R.string.agendar_enviar),
                    onClick = enviar,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = estado.hora != null && !estado.cargando,
                    cargando = estado.enviando,
                )
                Text(
                    text = if (reagendando != null) stringResource(R.string.agendar_reagendar_ayuda, nombre) else stringResource(R.string.agendar_ayuda),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                )
            }
        }
    }
}

// internal: la hoja con la que el nutriólogo agenda (NuevaCitaSheet) usa los mismos chips
@Composable
internal fun Cargando() {
    val descripcion = stringResource(R.string.agendar_cargando)
    Column(Modifier.semantics { contentDescription = descripcion }, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) { Esqueleto(Modifier.size(width = 60.dp, height = 72.dp), RoundedCornerShape(19.dp)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { Esqueleto(Modifier.size(width = 72.dp, height = 44.dp), RoundedCornerShape(14.dp)) }
        }
    }
}

@Composable
internal fun DiaChip(dia: LocalDate, elegido: Boolean, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val descripcion = diaLargo(dia)
    Column(
        modifier = Modifier
            .width(60.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(if (elegido) colores.primary else colores.surfaceVariant)
            .selectable(selected = elegido, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = descripcion }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(diaCorto(dia), style = MaterialTheme.typography.labelLarge, color = if (elegido) colores.onPrimary else colores.onSurfaceVariant)
        Text(dia.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, color = if (elegido) colores.onPrimary else colores.onBackground)
    }
}

// Una hora ocupada o que ya pasó se ve tachada y no se puede elegir.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FlowRowScope.HoraChip(hora: String, elegida: Boolean, libre: Boolean, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(14.dp)
    Text(
        text = hora,
        modifier = Modifier
            .weight(1f)
            .clip(forma)
            .background(if (elegida) colores.primary else if (libre) colores.surfaceVariant else colores.background)
            .border(1.dp, if (libre) colores.surfaceVariant else colores.outlineVariant, forma)
            .selectable(selected = elegida, enabled = libre, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 11.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleSmall,
        color = if (elegida) colores.onPrimary else if (libre) colores.onBackground else colores.outline,
        textDecoration = if (libre) null else TextDecoration.LineThrough,
    )
}

@Composable
private fun Enviada(nombre: String, reagendada: Boolean, onCerrar: () -> Unit) {
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
            text = stringResource(if (reagendada) R.string.agendar_reagendada_titulo else R.string.agendar_enviada_titulo),
            style = MaterialTheme.typography.headlineMedium,
            color = colores.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(if (reagendada) R.string.agendar_reagendada_ayuda else R.string.agendar_enviada_ayuda, nombre),
            style = MaterialTheme.typography.bodyLarge,
            color = colores.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        BotonPrimario(texto = stringResource(R.string.agendar_listo), onClick = onCerrar, modifier = Modifier.fillMaxWidth())
    }
}

// Ya tiene una cita vigente y solo se puede una a la vez: se muestra para reagendarla (otra hora, con el mismo nutriólogo) o
// cancelarla (y entonces puede pedir otra, con quien quiera).
@Composable
private fun YaTienesCita(cita: Cita, cancelando: Boolean, @StringRes error: Int?, onReagendar: () -> Unit, onCancelar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    var confirmando by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(colores.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_calendario), contentDescription = null, modifier = Modifier.size(32.dp), tint = colores.onPrimaryContainer)
        }
        Text(
            text = stringResource(R.string.agendar_ya_titulo),
            style = MaterialTheme.typography.headlineMedium,
            color = colores.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.agendar_ya_ayuda),
            style = MaterialTheme.typography.bodyLarge,
            color = colores.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colores.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(cita.nutriologoNombre, style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
            Text(diaYHora(cita.inicio), style = MaterialTheme.typography.bodyMedium, color = colores.onBackground)
            Text(
                text = stringResource(if (cita.estado == EstadoCita.CONFIRMADA) R.string.cita_estado_confirmada else R.string.cita_estado_pendiente),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
            )
        }
        error?.let { AlertaError(stringResource(it)) }
        BotonPrimario(texto = stringResource(R.string.cita_reagendar), onClick = onReagendar, modifier = Modifier.fillMaxWidth(), enabled = !cancelando)
        TextButton(onClick = { confirmando = true }, enabled = !cancelando) {
            Text(stringResource(R.string.cita_cancelar), color = colores.error)
        }
    }
    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            containerColor = colores.background,
            title = { Text(stringResource(R.string.cita_dialogo_cancelar)) },
            text = { Text(stringResource(R.string.cita_dialogo_ayuda)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmando = false
                    onCancelar()
                }) { Text(stringResource(R.string.cita_dialogo_si_cancelar), color = colores.error) }
            },
            dismissButton = { TextButton(onClick = { confirmando = false }) { Text(stringResource(R.string.cita_dialogo_volver)) } },
        )
    }
}
