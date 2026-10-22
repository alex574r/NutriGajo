package com.example.nutrigajo.ui.nutriologo.inicio

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.CodigoVinculacion
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun fechaMedia(millis: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

// Abre el menú de compartir del sistema con la invitación y el código.
internal fun compartirCodigo(contexto: Context, texto: String) {
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    contexto.startActivity(Intent.createChooser(envio, null))
}

// Código de vinculación con sus acciones; es el contenido de la hoja que abre el botón central de la barra.
@Composable
fun TarjetaCodigo(codigo: CodigoVinculacion?, generando: Boolean, onGenerar: () -> Unit, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    val negro = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colores.surfaceVariant, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.pacientes_codigo_titulo),
            style = MaterialTheme.typography.titleMedium,
            color = colores.onBackground,
        )
        if (codigo != null) {
            Text(
                text = codigo.formateado,
                style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 3.sp),
                color = colores.onBackground,
            )
            val vence = remember(codigo.expiraEn) { fechaMedia(codigo.expiraEn) }
            Text(
                text = stringResource(R.string.pacientes_codigo_vigencia, vence),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.pacientes_codigo_ayuda),
            style = MaterialTheme.typography.bodySmall,
            color = colores.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (codigo != null) {
                val texto = stringResource(R.string.pacientes_compartir_texto, codigo.formateado)
                Button(
                    onClick = { compartirCodigo(contexto, texto) },
                    shape = RoundedCornerShape(21.dp),
                    colors = negro,
                ) { Text(stringResource(R.string.pacientes_compartir)) }
                OutlinedButton(onClick = onGenerar, enabled = !generando, shape = RoundedCornerShape(21.dp)) {
                    Text(stringResource(R.string.pacientes_generar_nuevo))
                }
            } else {
                Button(onClick = onGenerar, enabled = !generando, shape = RoundedCornerShape(21.dp), colors = negro) {
                    Text(stringResource(R.string.pacientes_generar))
                }
            }
        }
    }
}
