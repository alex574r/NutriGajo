package com.example.nutrigajo.ui.shared.expediente

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Expediente
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.components.BotonPrimario
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Lo que hay en el expediente, para el paciente y para su nutriólogo, por categorías. Desde aquí se abre el asistente para editarlo.
@Composable
fun ResumenScreen(
    nombrePaciente: String,
    expediente: Expediente,
    onEditar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    // solo el nutriólogo: abre el panel del plan de alimentación y ejercicio de este paciente
    onPlan: (() -> Unit)? = null,
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    val respondidos = expediente.respondidos()

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
            } else {
                // avance del expediente entero
                Column(Modifier.padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.resumen_datos, respondidos, TOTAL_DATOS),
                        style = MaterialTheme.typography.bodySmall,
                        color = colores.onSurfaceVariant,
                    )
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colores.surfaceVariant)) {
                        Box(Modifier.fillMaxWidth(respondidos / TOTAL_DATOS.toFloat()).fillMaxHeight().background(colores.primary))
                    }
                }
            }
            CategoriasExpediente(expediente, Modifier.padding(bottom = 8.dp))
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onPlan != null) {
                BotonPrimario(texto = stringResource(R.string.plan_boton_crear), onClick = onPlan, modifier = Modifier.fillMaxWidth())
            }
            BotonPrimario(
                texto = stringResource(if (expediente.completo) R.string.expediente_editar else R.string.expediente_completar),
                onClick = onEditar,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
            )
        }
    }
}
