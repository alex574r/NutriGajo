package com.example.nutrigajo.ui.nutriologo.verificacion

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.EstadoVerificacion
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.Esqueleto

// Estado de la verificación del nutriólogo en su inicio: invita a enviar la cédula, avisa que está en revisión,
// muestra el motivo de un rechazo (con opción de reenviar) o confirma la cuenta verificada.
@Composable
fun TarjetaVerificacion(
    estado: VerificacionEstado,
    onVerificar: () -> Unit,
    modifier: Modifier = Modifier,
    // con la solicitud en revisión ofrece volver a consultar el resultado, que es lo que pide el texto de la tarjeta
    onActualizar: (() -> Unit)? = null,
) {
    val colores = MaterialTheme.colorScheme
    when {
        estado.cargando -> Esqueleto(modifier.fillMaxWidth().height(142.dp), RoundedCornerShape(24.dp))
        estado.error != null -> AlertaError(stringResource(estado.error), modifier)
        else -> {
            val verificacion = estado.verificacion
            val titulo = when (verificacion?.estado) {
                null -> R.string.verificacion_titulo
                EstadoVerificacion.PENDIENTE -> R.string.verificacion_pendiente_titulo
                EstadoVerificacion.RECHAZADA -> R.string.verificacion_rechazada_titulo
                EstadoVerificacion.APROBADA -> R.string.verificacion_aprobada_titulo
            }
            val texto = when (verificacion?.estado) {
                null -> stringResource(R.string.verificacion_tarjeta_ayuda)
                EstadoVerificacion.PENDIENTE -> stringResource(R.string.verificacion_pendiente_ayuda)
                EstadoVerificacion.RECHAZADA -> stringResource(R.string.verificacion_rechazada_motivo, verificacion?.motivo.orEmpty())
                EstadoVerificacion.APROBADA -> stringResource(R.string.verificacion_aprobada_ayuda)
            }
            val boton = when (verificacion?.estado) {
                null -> R.string.verificacion_boton
                EstadoVerificacion.RECHAZADA -> R.string.verificacion_reenviar
                else -> null
            }
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .background(colores.surfaceVariant, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
                Text(texto, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                if (boton != null) {
                    Button(
                        onClick = onVerificar,
                        shape = RoundedCornerShape(21.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
                    ) { Text(stringResource(boton)) }
                }
                if (verificacion?.estado == EstadoVerificacion.PENDIENTE && onActualizar != null) {
                    OutlinedButton(onClick = onActualizar, shape = RoundedCornerShape(21.dp)) {
                        Text(stringResource(R.string.pacientes_actualizar))
                    }
                }
            }
        }
    }
}
