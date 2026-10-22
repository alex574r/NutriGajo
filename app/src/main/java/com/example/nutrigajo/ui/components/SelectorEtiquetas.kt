package com.example.nutrigajo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R

private const val MAX_ETIQUETAS = 10

// Etiquetas libres con atajos: arriba lo elegido (naranja, se quita al tocar), abajo las más comunes (azul)
// y un campo para escribir otra. Sirve para condiciones de salud, alergias y suplementos.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorEtiquetas(
    comunes: List<String>,
    seleccionadas: List<String>,
    onCambio: (List<String>) -> Unit,
    etiquetaCampo: String,
    etiquetaComunes: String,
    etiquetaAgregar: String,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    var texto by rememberSaveable { mutableStateOf("") }

    fun agregar(nueva: String) {
        val limpia = nueva.trim().replace(Regex("\\s+"), " ").take(40)
        if (limpia.isEmpty() || seleccionadas.size >= MAX_ETIQUETAS || seleccionadas.any { it.equals(limpia, ignoreCase = true) }) return
        onCambio(seleccionadas + limpia)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (seleccionadas.isEmpty()) colores.outlineVariant else colores.primary, RoundedCornerShape(19.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            seleccionadas.forEach { etiqueta ->
                Etiqueta(
                    texto = "$etiqueta  ×",
                    fondo = colores.primaryContainer,
                    color = colores.primary,
                    onClick = { onCambio(seleccionadas - etiqueta) },
                )
            }
            if (seleccionadas.isEmpty()) {
                Text(stringResource(R.string.etiquetas_vacio), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
            }
        }
        Text(
            text = "${seleccionadas.size}/$MAX_ETIQUETAS",
            modifier = Modifier.align(Alignment.End),
            style = MaterialTheme.typography.bodySmall,
            color = colores.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it.take(40) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(etiquetaCampo) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    agregar(texto)
                    texto = ""
                }),
            )
            TextButton(
                onClick = {
                    agregar(texto)
                    texto = ""
                },
                enabled = texto.isNotBlank(),
            ) { Text(etiquetaAgregar) }
        }
        Text(etiquetaComunes, style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            comunes.forEach { comun ->
                val elegida = seleccionadas.any { it.equals(comun, ignoreCase = true) }
                Etiqueta(
                    texto = comun,
                    fondo = if (elegida) colores.secondary else colores.secondaryContainer,
                    color = if (elegida) colores.onSecondary else colores.secondary,
                    onClick = { if (elegida) onCambio(seleccionadas.filterNot { it.equals(comun, ignoreCase = true) }) else agregar(comun) },
                )
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, fondo: androidx.compose.ui.graphics.Color, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        text = texto,
        modifier = Modifier
            .background(fondo, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = color,
    )
}
