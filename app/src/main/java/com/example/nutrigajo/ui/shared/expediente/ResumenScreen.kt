package com.example.nutrigajo.ui.shared.expediente

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Expediente
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.components.BotonPrimario
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Lo que hay en el expediente, para el paciente y para su nutriólogo. Desde aquí se abre el asistente para editarlo.
@Composable
fun ResumenScreen(
    nombrePaciente: String,
    expediente: Expediente,
    onEditar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    val sinResponder = stringResource(R.string.expediente_sin_responder)

    val filas = listOf(
        R.string.dato_objetivo to expediente.objetivo?.let { stringResource(it.etiqueta()) },
        R.string.dato_sexo to expediente.sexo?.let { stringResource(it.etiqueta()) },
        R.string.dato_edad to expediente.fechaNacimiento
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let { "${Period.between(it, LocalDate.now()).years} ${stringResource(R.string.expediente_edad_anios)}" },
        R.string.dato_peso to expediente.pesoKg?.let { "${if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it)} kg" },
        R.string.dato_estatura to expediente.estaturaCm?.let { "${it.toInt()} cm" },
        R.string.dato_actividad to expediente.nivelActividad?.let { "$it · ${stringResource(etiquetaActividad(it))}" },
        R.string.dato_condiciones to expediente.condiciones.takeIf { it.isNotEmpty() }?.joinToString(", "),
        R.string.dato_alergias to expediente.alergias.takeIf { it.isNotEmpty() }?.joinToString(", "),
        R.string.dato_dieta to expediente.dieta?.let { stringResource(it.etiqueta()) },
        R.string.dato_dias to expediente.diasEjercicio?.let { "${it}x" },
        // map es inline: ahí sí se puede llamar a stringResource
        R.string.dato_ejercicios to expediente.ejercicios.takeIf { it.isNotEmpty() }
            ?.map { stringResource(it.etiqueta()) }?.joinToString(", "),
        R.string.dato_suplementos to expediente.suplementos.takeIf { it.isNotEmpty() }?.joinToString(", "),
        R.string.dato_calorias to expediente.caloriasObjetivo?.let { "%,d kcal".format(it) },
        R.string.dato_sueno to expediente.calidadSueno?.let { "${stringResource(it.etiqueta())} · ${stringResource(it.horas())}" },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colores.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onVolver) { Text(stringResource(R.string.mapa_volver)) }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.expediente_resumen_titulo, nombrePaciente),
                style = MaterialTheme.typography.headlineMedium.copy(lineHeight = 1.2.em),
                color = colores.onBackground,
            )
            expediente.actualizadoPor?.let { editor ->
                val fecha = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .format(Instant.ofEpochMilli(expediente.actualizadoEn).atZone(ZoneId.systemDefault()))
                Text(
                    text = stringResource(
                        if (editor == Rol.PACIENTE) R.string.expediente_editado_paciente else R.string.expediente_editado_nutriologo,
                        fecha,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                )
            }
            if (expediente.vacio) {
                Text(
                    text = stringResource(R.string.expediente_vacio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colores.surfaceVariant, RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                filas.forEachIndexed { indice, (etiqueta, valor) ->
                    if (indice > 0) HorizontalDivider(color = colores.outlineVariant)
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(stringResource(etiqueta), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                        Text(
                            text = valor ?: sinResponder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (valor == null) colores.outline else colores.onBackground,
                        )
                    }
                }
            }
        }
        BotonPrimario(
            texto = stringResource(if (expediente.completo) R.string.expediente_editar else R.string.expediente_completar),
            onClick = onEditar,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
        )
    }
}
