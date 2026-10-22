package com.example.nutrigajo.ui.shared.expediente

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Expediente
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario

private val MARGEN = 16.dp

// Una pregunta por pantalla, como el asistente del diseño. Regresar en el primer paso sale del asistente.
// Todas las preguntas son opcionales: "omitir" borra la respuesta de ese paso y sigue.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AsistenteScreen(
    estado: ExpedienteEstado,
    onCambiar: ((Expediente) -> Expediente) -> Unit,
    onContinuar: () -> Unit,
    onOmitir: () -> Unit,
    onRegresar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onRegresar)
    val colores = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colores.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colores.surfaceVariant)
                    .clickable(role = Role.Button, onClick = onRegresar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.expediente_regresar),
                    tint = colores.onBackground,
                )
            }
            Text(
                text = stringResource(R.string.expediente_titulo),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = colores.onBackground,
            )
            Text(
                text = stringResource(R.string.expediente_avance, estado.paso + 1, TOTAL_PASOS),
                modifier = Modifier
                    .background(colores.secondaryContainer, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelLarge,
                color = colores.secondary,
            )
        }
        Text(
            text = stringResource(tituloPaso(estado.paso)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            // el 100% de la escala no sirve para texto de varias líneas
            style = MaterialTheme.typography.headlineLarge.copy(lineHeight = 1.2.em),
            color = colores.onBackground,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                // las opciones llevan su anillo de foco: se descuenta del margen para que el borde quede a 16dp
                .padding(horizontal = MARGEN - ANILLO_FOCO, vertical = 8.dp),
        ) {
            ContenidoPaso(paso = estado.paso, expediente = estado.expediente, cambiar = onCambiar)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            estado.error?.let { AlertaError(stringResource(it)) }
            // con el teclado abierto no queda espacio para las opciones: se esconde
            if (!WindowInsets.isImeVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(colores.primaryContainer)
                        .clickable(role = Role.Button, enabled = !estado.guardando, onClick = onOmitir),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.expediente_omitir) + "  ✕",
                        style = MaterialTheme.typography.labelLarge,
                        color = colores.primary,
                    )
                }
            }
            BotonPrimario(
                texto = stringResource(if (estado.paso >= TOTAL_PASOS - 1) R.string.expediente_terminar else R.string.expediente_continuar),
                onClick = onContinuar,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
                cargando = estado.guardando,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}
