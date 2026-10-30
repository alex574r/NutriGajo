package com.example.nutrigajo.ui.shared.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Cita
import com.example.nutrigajo.domain.model.EstadoCita
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.components.ActualizarAlDeslizar
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.shared.citas.EtiquetaEstado
import com.example.nutrigajo.ui.shared.citas.diaLargo
import com.example.nutrigajo.ui.shared.citas.horaCorta
import com.example.nutrigajo.ui.shared.citas.inicialDia
import com.example.nutrigajo.ui.shared.citas.mesYAnio
import com.example.nutrigajo.ui.shared.citas.rangoSemana
import com.example.nutrigajo.ui.theme.negro
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

// Cómo se ve una cita en la agenda: confirmada (naranja suave), por confirmar (contorno naranja discontinuo) o cerrada (gris:
// ya pasó, se rechazó o se canceló). Es el mismo criterio de colores de las etiquetas de la lista.
internal enum class TipoCita { CONFIRMADA, PENDIENTE, CERRADA }

internal fun Cita.tipo(ahora: Long): TipoCita = when {
    vigente(ahora) && estado == EstadoCita.CONFIRMADA -> TipoCita.CONFIRMADA
    vigente(ahora) && estado == EstadoCita.PENDIENTE -> TipoCita.PENDIENTE
    else -> TipoCita.CERRADA
}

// la consulta es de una hora y va de las 9:00 a las 17:00 (ver `Agenda`); el calendario enseña de las 9:00 a las 18:00
private val HORAS = 9..17
private val ALTO_HORA = 80.dp
private val MARGEN = 16.dp
private const val UNA_HORA_MS = 3_600_000L

// ---- Mes: el calendario y, debajo, las citas del día elegido

@Composable
internal fun VistaMes(
    rol: Rol,
    dia: LocalDate,
    hoy: LocalDate,
    porDia: Map<LocalDate, List<Cita>>,
    ahora: Long,
    refrescando: Boolean,
    onActualizar: () -> Unit,
    onDia: (LocalDate) -> Unit,
    onCita: (Cita) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val mes = YearMonth.from(dia)
    ActualizarAlDeslizar(refrescando, onActualizar, modifier) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MARGEN)
                .padding(bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Cabecera(mesYAnio(dia), onAnterior = { onDia(dia.minusMonths(1)) }, onSiguiente = { onDia(dia.plusMonths(1)) })
            Row(Modifier.fillMaxWidth()) {
                val lunes = dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                repeat(7) { i ->
                    Text(
                        text = inicialDia(lunes.plusDays(i.toLong())),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        color = colores.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            val hueco = mes.atDay(1).dayOfWeek.value - 1
            val filas = (hueco + mes.lengthOfMonth() + 6) / 7
            repeat(filas) { fila ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { columna ->
                        val numero = fila * 7 + columna - hueco + 1
                        Box(Modifier.weight(1f).height(52.dp), contentAlignment = Alignment.TopCenter) {
                            if (numero in 1..mes.lengthOfMonth()) {
                                val d = mes.atDay(numero)
                                CeldaDia(d, seleccionado = d == dia, esHoy = d == hoy, citas = porDia[d].orEmpty(), ahora = ahora, onClick = { onDia(d) })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            val delDia = porDia[dia].orEmpty()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.agenda_citas_dia), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
                    Text(diaLargo(dia), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                }
                if (delDia.isNotEmpty()) {
                    Text(
                        text = delDia.size.toString(),
                        modifier = Modifier.clip(CircleShape).background(colores.negro).padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
            if (delDia.isEmpty()) {
                Text(stringResource(R.string.agenda_sin_citas), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
            }
            delDia.forEach { cita -> TarjetaAgenda(cita, cita.tipo(ahora), rol, onClick = { onCita(cita) }) }
        }
    }
}

// El título del mes (o de la semana) con sus flechas.
@Composable
private fun Cabecera(titulo: String, onAnterior: () -> Unit, onSiguiente: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = colores.onBackground)
        BotonFlecha(derecha = false, descripcion = stringResource(R.string.agenda_anterior), onClick = onAnterior)
        Spacer(Modifier.width(8.dp))
        BotonFlecha(derecha = true, descripcion = stringResource(R.string.agenda_siguiente), onClick = onSiguiente)
    }
}

@Composable
private fun BotonFlecha(derecha: Boolean, descripcion: String, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).background(colores.surfaceVariant).alPulsar(onClick),
        contentAlignment = Alignment.Center,
    ) {
        // el chevron del diseño apunta a la izquierda; a la derecha es el mismo girado
        Icon(
            painter = painterResource(R.drawable.ic_chevron_izquierda),
            contentDescription = descripcion,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = if (derecha) 180f else 0f },
            tint = colores.onSurfaceVariant,
        )
    }
}

// Un día: su número (en un círculo negro si es el elegido, naranja si es hoy) y, debajo, un punto por clase de cita que tiene.
@Composable
private fun CeldaDia(dia: LocalDate, seleccionado: Boolean, esHoy: Boolean, citas: List<Cita>, ahora: Long, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val domingo = dia.dayOfWeek == DayOfWeek.SUNDAY
    Column(
        modifier = Modifier.fillMaxWidth().alPulsar(onClick, minimo = 0.92f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(if (seleccionado) colores.negro else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = dia.dayOfMonth.toString(),
                style = if (esHoy || seleccionado) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                color = when {
                    seleccionado -> Color.White
                    esHoy -> colores.primary
                    // no hay consulta los domingos
                    domingo -> colores.outline
                    else -> colores.onBackground
                },
            )
        }
        Puntos(citas, ahora)
    }
}

@Composable
private fun Puntos(citas: List<Cita>, ahora: Long) {
    val colores = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
        citas.map { it.tipo(ahora) }.distinct().sorted().forEach { tipo ->
            val relleno = when (tipo) {
                TipoCita.CONFIRMADA -> colores.primary
                TipoCita.PENDIENTE -> colores.primary.copy(alpha = 0.35f)
                TipoCita.CERRADA -> colores.outline
            }
            Box(Modifier.size(6.dp).clip(CircleShape).background(relleno))
        }
    }
}

// ---- Semana: los siete días arriba y, debajo, las horas del día elegido con su cita

@Composable
internal fun VistaSemana(
    rol: Rol,
    dia: LocalDate,
    hoy: LocalDate,
    porDia: Map<LocalDate, List<Cita>>,
    ahora: Long,
    refrescando: Boolean,
    onActualizar: () -> Unit,
    onDia: (LocalDate) -> Unit,
    onCita: (Cita) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val lunes = dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val delDia = porDia[dia].orEmpty()
    ActualizarAlDeslizar(refrescando, onActualizar, modifier) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.padding(horizontal = MARGEN), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Cabecera(rangoSemana(lunes), onAnterior = { onDia(dia.minusWeeks(1)) }, onSiguiente = { onDia(dia.plusWeeks(1)) })
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { i ->
                        val d = lunes.plusDays(i.toLong())
                        DiaDeLaSemana(d, seleccionado = d == dia, esHoy = d == hoy, citas = porDia[d].orEmpty(), ahora = ahora, onClick = { onDia(d) }, modifier = Modifier.weight(1f))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colores.outlineVariant))
            Box(Modifier.padding(horizontal = MARGEN)) {
                Column {
                    HORAS.forEach { hora ->
                        val deLaHora = delDia.filter { Instant.ofEpochMilli(it.inicio).atZone(ZoneId.systemDefault()).hour == hora }
                        val cita = deLaHora.firstOrNull { it.vigente(ahora) } ?: deLaHora.lastOrNull()
                        FilaHora(rol, hora, dia, cita, ahora, onCita)
                    }
                }
                // la línea de «ahora» cruza las horas del día de hoy
                if (dia == hoy) {
                    val ya = LocalTime.now()
                    val fraccion = (ya.hour - HORAS.first) + ya.minute / 60f
                    if (fraccion >= 0f && fraccion <= HORAS.count().toFloat()) {
                        Row(Modifier.fillMaxWidth().offset(y = ALTO_HORA * fraccion), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(colores.primary))
                            Box(Modifier.weight(1f).height(1.5.dp).background(colores.primary))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaDeLaSemana(dia: LocalDate, seleccionado: Boolean, esHoy: Boolean, citas: List<Cita>, ahora: Long, onClick: () -> Unit, modifier: Modifier) {
    val colores = MaterialTheme.colorScheme
    val domingo = dia.dayOfWeek == DayOfWeek.SUNDAY
    Column(
        modifier = modifier.alPulsar(onClick, minimo = 0.94f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(inicialDia(dia), style = MaterialTheme.typography.labelLarge, color = colores.onSurfaceVariant)
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (seleccionado) colores.negro else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = dia.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    seleccionado -> Color.White
                    esHoy -> colores.primary
                    domingo -> colores.outline
                    else -> colores.onBackground
                },
            )
        }
        Puntos(citas, ahora)
    }
}

// Una hora del día: su etiqueta a la izquierda y, a la derecha, la cita que empieza a esa hora o, en la agenda del nutriólogo, un
// hueco «Libre» si aún se puede pedir (el paciente solo ve sus citas). Los domingos no hay consulta.
@Composable
private fun FilaHora(rol: Rol, hora: Int, dia: LocalDate, cita: Cita?, ahora: Long, onCita: (Cita) -> Unit) {
    val colores = MaterialTheme.colorScheme
    val inicio = dia.atTime(hora, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val hayConsulta = dia.dayOfWeek != DayOfWeek.SUNDAY
    Row(Modifier.fillMaxWidth().height(ALTO_HORA)) {
        Text(
            text = horaCorta(inicio),
            modifier = Modifier.width(52.dp).padding(top = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = colores.onSurfaceVariant,
        )
        Box(Modifier.weight(1f).padding(vertical = 4.dp)) {
            when {
                cita != null -> TarjetaAgenda(cita, cita.tipo(ahora), rol, onClick = { onCita(cita) }, modifier = Modifier.height(ALTO_HORA - 8.dp))
                rol == Rol.NUTRIOLOGO && hayConsulta && inicio > ahora -> {
                    val trazo = colores.outlineVariant
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(ALTO_HORA - 8.dp)
                            .drawBehind {
                                val grosor = 1.dp.toPx()
                                drawRoundRect(
                                    color = trazo,
                                    topLeft = Offset(grosor / 2, grosor / 2),
                                    size = Size(size.width - grosor, size.height - grosor),
                                    cornerRadius = CornerRadius(18.dp.toPx()),
                                    style = Stroke(grosor, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))),
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.agenda_libre), style = MaterialTheme.typography.bodySmall, color = colores.outline)
                    }
                }
            }
        }
    }
}

// ---- La tarjeta de una cita, la misma en la lista del día y en las horas de la semana

@Composable
// Con quién es la cita: el nutriólogo ve a su paciente y el paciente, a su nutriólogo.
internal fun TarjetaAgenda(cita: Cita, tipo: TipoCita, rol: Rol, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val relleno = when (tipo) {
        TipoCita.CONFIRMADA -> colores.primary.copy(alpha = 0.14f)
        TipoCita.PENDIENTE -> colores.background
        TipoCita.CERRADA -> colores.surfaceVariant
    }
    val acento = if (tipo == TipoCita.CERRADA) colores.outline else colores.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(relleno)
            .drawBehind {
                // por confirmar: contorno discontinuo, para que se distinga de lo ya confirmado
                if (tipo == TipoCita.PENDIENTE) {
                    val grosor = 1.5.dp.toPx()
                    drawRoundRect(
                        color = acento,
                        topLeft = Offset(grosor / 2, grosor / 2),
                        size = Size(size.width - grosor, size.height - grosor),
                        cornerRadius = CornerRadius(18.dp.toPx()),
                        style = Stroke(grosor, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                    )
                }
            }
            .alPulsar(onClick, minimo = 0.98f)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(acento))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (rol == Rol.NUTRIOLOGO) cita.pacienteNombre else cita.nutriologoNombre,
                style = MaterialTheme.typography.titleSmall,
                color = if (tipo == TipoCita.CERRADA) colores.onSurfaceVariant else colores.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(painterResource(R.drawable.ic_reloj), contentDescription = null, modifier = Modifier.size(14.dp), tint = colores.onSurfaceVariant)
                Text("${horaCorta(cita.inicio)} – ${horaCorta(cita.inicio + UNA_HORA_MS)}", style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
            }
        }
        EtiquetaEstado(cita.estado, cita.vigente(System.currentTimeMillis()))
    }
}
