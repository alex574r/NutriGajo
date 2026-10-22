package com.example.nutrigajo.ui.paciente.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.shared.expediente.ExpedienteEstado

// Invita al paciente a generar su expediente (opcional), lo retoma si lo dejó a medias o lo deja consultar si ya está.
@Composable
fun TarjetaExpediente(estado: ExpedienteEstado, onAbrir: () -> Unit, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    when {
        estado.cargando -> Esqueleto(modifier.fillMaxWidth().height(142.dp), RoundedCornerShape(24.dp))
        estado.error != null -> AlertaError(stringResource(estado.error), modifier)
        else -> {
            val expediente = estado.expediente
            val enCurso = !expediente.completo && (!expediente.vacio || expediente.paso > 0)
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .background(colores.surfaceVariant, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.expediente_tarjeta_titulo), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
                Text(
                    text = stringResource(
                        when {
                            expediente.completo -> R.string.expediente_tarjeta_completo
                            enCurso -> R.string.expediente_tarjeta_en_curso
                            else -> R.string.expediente_tarjeta_ayuda
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                )
                Button(
                    onClick = onAbrir,
                    shape = RoundedCornerShape(21.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
                ) {
                    Text(
                        stringResource(
                            when {
                                expediente.completo -> R.string.expediente_ver
                                enCurso -> R.string.expediente_retomar
                                else -> R.string.expediente_generar
                            },
                        ),
                    )
                }
            }
        }
    }
}
