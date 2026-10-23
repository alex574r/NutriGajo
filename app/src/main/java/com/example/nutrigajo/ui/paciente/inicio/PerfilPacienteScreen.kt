package com.example.nutrigajo.ui.paciente.inicio

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Vinculo
import com.example.nutrigajo.ui.components.AccionPerfil
import com.example.nutrigajo.ui.components.TransicionPerfil
import com.example.nutrigajo.ui.shared.perfil.DatoPerfil
import com.example.nutrigajo.ui.shared.perfil.PerfilScreen
import com.example.nutrigajo.ui.shared.perfil.altaCorta
import com.example.nutrigajo.ui.shared.perfil.datosBase

// Perfil del paciente: sus datos con el nutriólogo que lo atiende, el expediente a un toque y la configuración de la cuenta.
@Composable
fun PerfilPacienteScreen(
    perfil: Perfil,
    vinculo: Vinculo,
    onExpediente: () -> Unit,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit,
    transicion: TransicionPerfil? = null,
) {
    PerfilScreen(
        perfil = perfil,
        rol = R.string.rol_paciente,
        verificado = false,
        estadisticas = listOfNotNull(
            stringResource(R.string.perfil_con_nutriologo, vinculo.nutriologoNombre),
            perfil.altaCorta()?.let { stringResource(R.string.perfil_desde, it) },
        ),
        principal = AccionPerfil(R.drawable.ic_corazon, R.string.inicio_menu_expediente, onExpediente),
        secundaria = null,
        datos = datosBase(perfil, R.string.rol_paciente) +
            DatoPerfil(R.drawable.ic_pacientes, R.string.perfil_dato_nutriologo, vinculo.nutriologoNombre),
        onCerrarSesion = onCerrarSesion,
        onVolver = onVolver,
        transicion = transicion,
    )
}
