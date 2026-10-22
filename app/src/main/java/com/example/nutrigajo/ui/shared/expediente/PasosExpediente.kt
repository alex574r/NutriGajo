package com.example.nutrigajo.ui.shared.expediente

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.CalidadSueno
import com.example.nutrigajo.domain.model.Dieta
import com.example.nutrigajo.domain.model.EjercicioPreferido
import com.example.nutrigajo.domain.model.Expediente
import com.example.nutrigajo.domain.model.Objetivo
import com.example.nutrigajo.domain.model.Sexo
import com.example.nutrigajo.ui.components.OpcionLista
import com.example.nutrigajo.ui.components.OpcionTarjeta
import com.example.nutrigajo.ui.components.Regla
import com.example.nutrigajo.ui.components.SelectorEtiquetas
import com.example.nutrigajo.ui.components.SelectorNumerico
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val PESO_INICIAL = 70.0
private const val ESTATURA_INICIAL = 170.0
private const val CALORIAS_INICIALES = 2000
private const val PASO_CALORIAS = 50

private typealias Cambiar = ((Expediente) -> Expediente) -> Unit

@StringRes
fun tituloPaso(paso: Int): Int = when (paso) {
    0 -> R.string.paso_objetivo
    1 -> R.string.paso_sexo
    2 -> R.string.paso_fecha
    3 -> R.string.paso_peso
    4 -> R.string.paso_estatura
    5 -> R.string.paso_actividad
    6 -> R.string.paso_condiciones
    7 -> R.string.paso_alergias
    8 -> R.string.paso_dieta
    9 -> R.string.paso_dias
    10 -> R.string.paso_ejercicios
    11 -> R.string.paso_suplementos
    12 -> R.string.paso_calorias
    else -> R.string.paso_sueno
}

// Contenido de cada paso del asistente; el orden es el de ExpedienteViewModel.sinPaso.
@Composable
fun ContenidoPaso(paso: Int, expediente: Expediente, cambiar: Cambiar) {
    when (paso) {
        0 -> PasoObjetivo(expediente, cambiar)
        1 -> PasoSexo(expediente, cambiar)
        2 -> PasoFecha(expediente, cambiar)
        3 -> PasoPeso(expediente, cambiar)
        4 -> PasoEstatura(expediente, cambiar)
        5 -> PasoActividad(expediente, cambiar)
        6 -> SelectorEtiquetas(
            comunes = stringArrayResource(R.array.condiciones_comunes).toList(),
            seleccionadas = expediente.condiciones,
            onCambio = { lista -> cambiar { it.copy(condiciones = lista) } },
            etiquetaCampo = stringResource(R.string.etiquetas_condicion),
            etiquetaComunes = stringResource(R.string.etiquetas_comunes),
            etiquetaAgregar = stringResource(R.string.etiquetas_agregar),
        )
        7 -> SelectorEtiquetas(
            comunes = stringArrayResource(R.array.alergias_comunes).toList(),
            seleccionadas = expediente.alergias,
            onCambio = { lista -> cambiar { it.copy(alergias = lista) } },
            etiquetaCampo = stringResource(R.string.etiquetas_alergia),
            etiquetaComunes = stringResource(R.string.etiquetas_comunes),
            etiquetaAgregar = stringResource(R.string.etiquetas_agregar),
        )
        8 -> PasoDieta(expediente, cambiar)
        9 -> PasoDias(expediente, cambiar)
        10 -> PasoEjercicios(expediente, cambiar)
        11 -> SelectorEtiquetas(
            comunes = stringArrayResource(R.array.suplementos_comunes).toList(),
            seleccionadas = expediente.suplementos,
            onCambio = { lista -> cambiar { it.copy(suplementos = lista) } },
            etiquetaCampo = stringResource(R.string.etiquetas_suplemento),
            etiquetaComunes = stringResource(R.string.etiquetas_comunes),
            etiquetaAgregar = stringResource(R.string.etiquetas_agregar),
        )
        12 -> PasoCalorias(expediente, cambiar)
        else -> PasoSueno(expediente, cambiar)
    }
}

@Composable
private fun PasoObjetivo(expediente: Expediente, cambiar: Cambiar) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Objetivo.entries.forEach { objetivo ->
            OpcionLista(
                texto = stringResource(objetivo.etiqueta()),
                seleccionada = expediente.objetivo == objetivo,
                onClick = { cambiar { it.copy(objetivo = objetivo) } },
            )
        }
    }
}

@Composable
private fun PasoSexo(expediente: Expediente, cambiar: Cambiar) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Sexo.entries.forEach { sexo ->
            OpcionLista(
                texto = stringResource(sexo.etiqueta()),
                seleccionada = expediente.sexo == sexo,
                onClick = { cambiar { it.copy(sexo = sexo) } },
            )
        }
    }
}

// La fecha de nacimiento se guarda y la edad se calcula: así no se desactualiza.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasoFecha(expediente: Expediente, cambiar: Cambiar) {
    val colores = MaterialTheme.colorScheme
    var abierto by remember { mutableStateOf(false) }
    val fecha = expediente.fechaNacimiento?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val edad = fecha?.let { Period.between(it, LocalDate.now()).years }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumeroGrande(edad?.toString())
            if (edad != null) {
                Text(
                    text = stringResource(R.string.expediente_edad_anios),
                    modifier = Modifier.padding(bottom = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = colores.onSurfaceVariant,
                )
            }
        }
        fecha?.let {
            Text(
                text = it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                style = MaterialTheme.typography.bodyLarge,
                color = colores.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { abierto = true }, shape = RoundedCornerShape(21.dp)) {
            Text(stringResource(R.string.expediente_fecha_elegir))
        }
    }

    if (abierto) {
        val hoy = LocalDate.now()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = (fecha ?: hoy.minusYears(30)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = 1920..hoy.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { ms ->
                        val elegida = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        cambiar { it.copy(fechaNacimiento = elegida) }
                    }
                    abierto = false
                }) { Text(stringResource(R.string.expediente_aceptar)) }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text(stringResource(R.string.verificacion_cancelar)) } },
        ) { DatePicker(state = estado) }
    }
}

@Composable
private fun PasoPeso(expediente: Expediente, cambiar: Cambiar) {
    val valor = expediente.pesoKg ?: PESO_INICIAL
    NumeroConRegla(
        texto = if (valor % 1.0 == 0.0) valor.toInt().toString() else "%.1f".format(valor),
        unidad = "kg",
    ) {
        Regla(valor = valor, onValor = { v -> cambiar { it.copy(pesoKg = v) } }, min = 30.0, max = 250.0, paso = 0.5, cadaAlta = 2)
    }
}

@Composable
private fun PasoEstatura(expediente: Expediente, cambiar: Cambiar) {
    val valor = expediente.estaturaCm ?: ESTATURA_INICIAL
    NumeroConRegla(texto = valor.toInt().toString(), unidad = "cm") {
        Regla(valor = valor, onValor = { v -> cambiar { it.copy(estaturaCm = v) } }, min = 100.0, max = 230.0, paso = 1.0, cadaAlta = 5, etiquetaCada = 2)
    }
}

// Cifra de pantalla completa; sin respuesta se muestra un guion en gris para que no parezca un dato.
@Composable
private fun NumeroGrande(texto: String?) {
    val colores = MaterialTheme.colorScheme
    Text(
        text = texto ?: "–",
        style = MaterialTheme.typography.displayLarge,
        color = if (texto == null) colores.outlineVariant else colores.onBackground,
    )
}

// Número grande con su unidad y, debajo, la regla que lo mueve.
@Composable
private fun NumeroConRegla(texto: String, unidad: String, regla: @Composable () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(texto, style = MaterialTheme.typography.displayLarge, color = colores.onBackground)
            Text(unidad, modifier = Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.titleLarge, color = colores.onSurfaceVariant)
        }
        Spacer(Modifier.height(40.dp))
        regla()
    }
}

@Composable
private fun PasoActividad(expediente: Expediente, cambiar: Cambiar) {
    val colores = MaterialTheme.colorScheme
    val nivel = expediente.nivelActividad
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        NumeroGrande(nivel?.toString())
        Text(
            text = nivel?.let { stringResource(etiquetaActividad(it)) } ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = colores.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        SelectorNumerico(valores = 1..5, seleccionado = nivel, onSeleccion = { n -> cambiar { it.copy(nivelActividad = n) } })
    }
}

@Composable
private fun PasoDieta(expediente: Expediente, cambiar: Cambiar) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Dieta.entries.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                fila.forEach { dieta ->
                    OpcionTarjeta(
                        titulo = stringResource(dieta.etiqueta()),
                        subtitulo = stringResource(dieta.ejemplos()),
                        seleccionada = expediente.dieta == dieta,
                        onClick = { cambiar { it.copy(dieta = dieta) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PasoDias(expediente: Expediente, cambiar: Cambiar) {
    val colores = MaterialTheme.colorScheme
    val dias = expediente.diasEjercicio
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        NumeroGrande(dias?.let { "${it}x" })
        Spacer(Modifier.height(32.dp))
        SelectorNumerico(valores = 0..7, seleccionado = dias, onSeleccion = { n -> cambiar { it.copy(diasEjercicio = n) } })
        Spacer(Modifier.height(16.dp))
        if (dias != null) {
            Text(
                text = stringResource(R.string.expediente_dias_ayuda, dias),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PasoEjercicios(expediente: Expediente, cambiar: Cambiar) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // dos columnas: "Senderismo" no cabe en tres
        EjercicioPreferido.entries.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                fila.forEach { ejercicio ->
                    val elegido = ejercicio in expediente.ejercicios
                    OpcionTarjeta(
                        titulo = stringResource(ejercicio.etiqueta()),
                        imagen = ejercicio.foto(),
                        seleccionada = elegido,
                        multiple = true,
                        onClick = {
                            cambiar { it.copy(ejercicios = if (elegido) it.ejercicios - ejercicio else it.ejercicios + ejercicio) }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (fila.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PasoCalorias(expediente: Expediente, cambiar: Cambiar) {
    val colores = MaterialTheme.colorScheme
    val calorias = expediente.caloriasObjetivo
    fun mover(delta: Int) = cambiar { it.copy(caloriasObjetivo = ((it.caloriasObjetivo ?: CALORIAS_INICIALES) + delta).coerceIn(800, 6000)) }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "%,d".format(calorias ?: CALORIAS_INICIALES),
            style = MaterialTheme.typography.displayLarge,
            // en gris mientras sea solo la sugerencia
            color = if (calorias == null) colores.outline else colores.onBackground,
        )
        Text(stringResource(R.string.expediente_calorias_dia), style = MaterialTheme.typography.titleMedium, color = colores.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonCalorias("−", colores.surfaceVariant, colores.onBackground) { mover(-PASO_CALORIAS) }
            BotonCalorias("+", colores.primary, colores.onPrimary) { mover(PASO_CALORIAS) }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.expediente_calorias_ayuda),
            style = MaterialTheme.typography.bodySmall,
            color = colores.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BotonCalorias(signo: String, fondo: androidx.compose.ui.graphics.Color, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 56.dp)
            .background(fondo, RoundedCornerShape(19.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(signo, style = MaterialTheme.typography.headlineMedium, color = color)
    }
}

@Composable
private fun PasoSueno(expediente: Expediente, cambiar: Cambiar) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CalidadSueno.entries.forEach { calidad ->
            OpcionLista(
                texto = stringResource(calidad.etiqueta()),
                detalle = stringResource(calidad.horas()),
                seleccionada = expediente.calidadSueno == calidad,
                onClick = { cambiar { it.copy(calidadSueno = calidad) } },
            )
        }
    }
}
