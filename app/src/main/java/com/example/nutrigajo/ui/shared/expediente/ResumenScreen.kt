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
import androidx.compose.foundation.layout.offset
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
import com.example.nutrigajo.ui.components.PuntoNuevo
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
    // el botón de editar de cada categoría: abre el asistente solo con los pasos de esa categoría
    onEditarCategoria: (List<Int>) -> Unit,
    // la categoría que se acaba de guardar da un pulso al volver; null si no se vuelve de editar una
    categoriaGuardada: List<Int>?,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    // solo el nutriólogo: abre el panel del plan de alimentación y ejercicio de este paciente
    onPlan: (() -> Unit)? = null,
    // el paciente ya recibió un plan: el botón pasa de crearlo a editarlo y reenviarlo (el panel parte del último enviado)
    planEnviado: Boolean = false,
    // solo el nutriólogo: abre el chat con este paciente; el punto marca un mensaje suyo sin leer
    onChat: (() -> Unit)? = null,
    chatSinLeer: Boolean = false,
    // solo el nutriólogo: fija las metas diarias de este paciente (agua, pasos, calorías y minutos de movimiento)
    onMetas: (() -> Unit)? = null,
    // solo el nutriólogo: abre la hoja para agendarle una cita a este paciente
    onCita: (() -> Unit)? = null,
    // solo el nutriólogo: da de baja a este paciente (cierra el vínculo; la confirmación la pide quien lo llama)
    onDarDeBaja: (() -> Unit)? = null,
    // solo el nutriólogo: false muestra el menú general del paciente y true su expediente; el estado vive afuera para que sobreviva al asistente
    enExpediente: Boolean = true,
    onEnExpediente: (Boolean) -> Unit = {},
) {
    val esNutriologo = onPlan != null || onChat != null || onMetas != null || onCita != null || onDarDeBaja != null
    // en el expediente del nutriólogo, atrás vuelve al menú; desde el menú (o para el paciente) sale
    BackHandler { if (esNutriologo && enExpediente) onEnExpediente(false) else onVolver() }
    val colores = MaterialTheme.colorScheme
    val respondidos = expediente.respondidos()

    if (esNutriologo && !enExpediente) {
        val primerNombre = nombrePaciente.substringBefore(' ')
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colores.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onVolver) { Text(stringResource(R.string.mapa_volver)) }
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = nombrePaciente,
                    style = MaterialTheme.typography.headlineMedium.copy(lineHeight = 1.2.em),
                    color = colores.onBackground,
                )
                MenuPaciente(
                    nombrePaciente = nombrePaciente,
                    resumenExpediente = stringResource(R.string.resumen_datos, respondidos, TOTAL_DATOS),
                    onExpediente = { onEnExpediente(true) },
                    modifier = Modifier.padding(bottom = 16.dp),
                    resumenPlan = onPlan?.let { stringResource(if (planEnviado) R.string.plan_boton_editar else R.string.plan_boton_crear) },
                    onPlan = onPlan,
                    resumenChat = stringResource(if (chatSinLeer) R.string.menu_paciente_chat_nuevo else R.string.menu_paciente_chat_resumen, primerNombre),
                    chatSinLeer = chatSinLeer,
                    onChat = onChat,
                    onMetas = onMetas,
                    onCita = onCita,
                    onDarDeBaja = onDarDeBaja,
                )
            }
        }
        return
    }

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
            CategoriasExpediente(expediente, Modifier.padding(bottom = 8.dp), onEditar = onEditarCategoria, resaltada = categoriaGuardada)
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // el chat, las metas, la cita y el plan del nutriólogo están en el menú general del paciente (MenuPaciente), no bajo el expediente
            BotonPrimario(
                texto = stringResource(if (expediente.completo) R.string.expediente_editar else R.string.expediente_completar),
                onClick = onEditar,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
            )
        }
    }
}
