package com.example.nutrigajo.ui.shared.ajustes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Vinculo
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoCodigo
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.TarjetaAjustes
import com.example.nutrigajo.ui.paciente.inicio.VinculoEstado

private const val LARGO_CODIGO = 4

// «Mi nutriólogo» (Configuración del paciente): con quién está y dos acciones. «Cambiar de nutriólogo» pide el código del nuevo, enseña
// «Dejarás a X y pasarás con Y» con la casilla de compartir el expediente (apagada) y recién entonces cambia, todo en un solo paso: nunca
// se queda sin nutriólogo a medias. «Desvincularme» cierra el vínculo tras una confirmación. Nada se borra: lo anterior queda cerrado, solo
// para leer.
@Composable
fun MiNutriologoScreen(
    vinculo: Vinculo,
    estado: VinculoEstado,
    onDesvincular: () -> Unit,
    onRevisarCodigo: (String) -> Unit,
    onDescartarRevision: () -> Unit,
    onCambiar: (codigo: String, compartirExpediente: Boolean) -> Unit,
    onLimpiarError: () -> Unit,
    onVolver: () -> Unit,
    espacioInferior: Dp = 0.dp,
) {
    val colores = MaterialTheme.colorScheme
    var cambiando by rememberSaveable { mutableStateOf(false) }
    var confirmando by rememberSaveable { mutableStateOf(false) }
    var codigo by rememberSaveable { mutableStateOf("") }
    var compartir by rememberSaveable { mutableStateOf(false) }
    val nuevo = estado.nuevoNutriologo

    // al cambiar de nutriólogo el vínculo es otro: se vuelve a la lista, ya con el nuevo
    LaunchedEffect(vinculo.pacienteId) {
        cambiando = false
        codigo = ""
        compartir = false
    }

    // atrás deshace un paso: de la confirmación al código, del código a la lista y de la lista al perfil
    val volver = {
        when {
            nuevo != null -> onDescartarRevision()
            cambiando -> {
                cambiando = false
                codigo = ""
                compartir = false
                onLimpiarError()
            }
            else -> onVolver()
        }
    }

    PantallaAjuste(stringResource(R.string.ajustes_mi_nutriologo), volver, espacioInferior = espacioInferior) {
        when {
            !cambiando -> {
                Text(stringResource(R.string.mi_nutriologo_actual, vinculo.nutriologoNombre), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
                Text(stringResource(R.string.mi_nutriologo_conserva, vinculo.nutriologoNombre), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                TarjetaAjustes {
                    FilaAjuste(
                        icono = R.drawable.ic_pacientes,
                        titulo = stringResource(R.string.mi_nutriologo_cambiar),
                        descripcion = stringResource(R.string.mi_nutriologo_cambiar_desc),
                        plana = true,
                        onClick = {
                            onLimpiarError()
                            cambiando = true
                        },
                    )
                }
                TarjetaAjustes(destructiva = true) {
                    FilaAjuste(
                        icono = R.drawable.ic_flecha_atras,
                        titulo = stringResource(R.string.mi_nutriologo_desvincular),
                        descripcion = stringResource(R.string.mi_nutriologo_desvincular_desc, vinculo.nutriologoNombre),
                        plana = true,
                        destructiva = true,
                        onClick = {
                            onLimpiarError()
                            confirmando = true
                        },
                    )
                }
                estado.error?.let { AlertaError(stringResource(it), Modifier.fillMaxWidth()) }
            }

            nuevo == null -> {
                Text(stringResource(R.string.mi_nutriologo_codigo_titulo), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
                Text(stringResource(R.string.mi_nutriologo_codigo_ayuda), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                CampoCodigo(
                    valor = codigo,
                    onValorChange = {
                        codigo = it
                        if (estado.error != null) onLimpiarError()
                    },
                    largo = LARGO_CODIGO,
                    descripcion = stringResource(R.string.vincular_codigo),
                    modifier = Modifier.fillMaxWidth(),
                    onListo = { if (codigo.length == LARGO_CODIGO && !estado.cerrando) onRevisarCodigo(codigo) },
                    error = estado.error != null,
                    verificando = estado.cerrando,
                )
                estado.error?.let { AlertaError(stringResource(it), Modifier.fillMaxWidth()) }
                BotonPrimario(
                    texto = stringResource(R.string.mi_nutriologo_continuar),
                    onClick = { onRevisarCodigo(codigo) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = codigo.length == LARGO_CODIGO,
                    cargando = estado.cerrando,
                )
            }

            else -> {
                Text(stringResource(R.string.mi_nutriologo_resumen, vinculo.nutriologoNombre, nuevo), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
                TarjetaAjustes {
                    FilaAjuste(
                        icono = R.drawable.ic_documento,
                        titulo = stringResource(R.string.mi_nutriologo_compartir),
                        descripcion = stringResource(R.string.mi_nutriologo_compartir_desc),
                        plana = true,
                        activa = compartir,
                        onActivaChange = { compartir = it },
                    )
                }
                estado.error?.let { AlertaError(stringResource(it), Modifier.fillMaxWidth()) }
                BotonPrimario(
                    texto = stringResource(R.string.mi_nutriologo_confirmar),
                    onClick = { onCambiar(codigo, compartir) },
                    modifier = Modifier.fillMaxWidth(),
                    cargando = estado.cerrando,
                )
                Text(
                    text = stringResource(R.string.mi_nutriologo_otro_codigo),
                    modifier = Modifier.clickable(role = Role.Button, onClick = onDescartarRevision).padding(vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = colores.primary,
                )
            }
        }
    }

    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text(stringResource(R.string.mi_nutriologo_desvincular_titulo, vinculo.nutriologoNombre)) },
            text = { Text(stringResource(R.string.mi_nutriologo_desvincular_texto)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmando = false
                        onDesvincular()
                    },
                    enabled = !estado.cerrando,
                ) { Text(stringResource(R.string.mi_nutriologo_desvincular), color = colores.error) }
            },
            dismissButton = { TextButton(onClick = { confirmando = false }) { Text(stringResource(R.string.verificacion_cancelar)) } },
        )
    }
}
