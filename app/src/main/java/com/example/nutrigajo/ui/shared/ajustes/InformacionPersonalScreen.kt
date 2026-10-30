package com.example.nutrigajo.ui.shared.ajustes

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.shared.auth.AuthViewModel
import com.example.nutrigajo.ui.shared.auth.limpiarEspacios

// Nombres y apellidos editables; el correo se ve pero no se cambia desde aquí. Al guardar se vuelve a leer el perfil, así que el
// nombre cambia de inmediato en toda la app. Las cuentas viejas solo guardaron el nombre completo: ahí queda todo en «Nombres».
@Composable
fun InformacionPersonalScreen(
    perfil: Perfil,
    onVolver: () -> Unit,
    espacioInferior: Dp,
    vm: AjustesViewModel = viewModel(key = "ajustes-${perfil.uid}"),
    cuenta: AuthViewModel = viewModel(),
) {
    val estado by vm.nombre.collectAsState()
    val inicialNombres = perfil.nombres.ifBlank { perfil.nombre }
    var nombres by rememberSaveable(perfil.uid) { mutableStateOf(inicialNombres) }
    var apellidos by rememberSaveable(perfil.uid) { mutableStateOf(perfil.apellidos) }
    val limpioNombres = limpiarEspacios(nombres)
    val limpioApellidos = limpiarEspacios(apellidos)
    val cambio = limpioNombres != inicialNombres || limpioApellidos != perfil.apellidos
    // guardado: el perfil nuevo ya llegó, así que `perfil` trae lo escrito y el botón vuelve a quedar apagado
    LaunchedEffect(estado.listo) { if (estado.listo) cuenta.cargarPerfil() }

    PantallaAjuste(stringResource(R.string.info_titulo), onVolver = { vm.limpiar(); onVolver() }, espacioInferior = espacioInferior) {
        Text(stringResource(R.string.info_ayuda), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        CampoTexto(stringResource(R.string.info_nombres), nombres, { nombres = it.take(60) }, icono = R.drawable.ic_person, modifier = Modifier.fillMaxWidth())
        CampoTexto(stringResource(R.string.info_apellidos), apellidos, { apellidos = it.take(60) }, icono = R.drawable.ic_person, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        FilaAjuste(R.drawable.ic_mail, titulo = perfil.correo, etiqueta = stringResource(R.string.info_correo))
        Text(stringResource(R.string.info_correo_ayuda), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        estado.error?.let { AlertaError(stringResource(it)) }
        if (estado.listo && !cambio) {
            Text(stringResource(R.string.info_guardado), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        BotonPrimario(
            texto = stringResource(R.string.info_guardar),
            onClick = { vm.guardarNombre(limpioNombres, limpioApellidos) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = cambio && limpioNombres.isNotEmpty() && !estado.enviando,
            cargando = estado.enviando,
        )
    }
}
