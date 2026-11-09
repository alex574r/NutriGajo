package com.example.nutrigajo.ui.nutriologo.inicio

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.shared.ajustes.VinculosAnterioresScreen
import com.example.nutrigajo.domain.model.EstadoVerificacion
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.components.ALTO_BARRA_INFERIOR
import com.example.nutrigajo.ui.components.TransicionPerfil
import com.example.nutrigajo.ui.components.AccionPerfil
import com.example.nutrigajo.ui.nutriologo.verificacion.TarjetaVerificacion
import com.example.nutrigajo.ui.nutriologo.verificacion.VerificacionEstado
import com.example.nutrigajo.ui.shared.perfil.DatoPerfil
import com.example.nutrigajo.ui.shared.perfil.PerfilScreen
import com.example.nutrigajo.ui.shared.perfil.altaCorta
import com.example.nutrigajo.ui.shared.perfil.datosBase

// Perfil del nutriólogo: sus datos con el estado de la verificación y la configuración de la cuenta.
// Con la cuenta verificada la cabecera lleva la insignia azul; el mapa y el código quedan a un toque.
@Composable
fun TabPerfil(
    perfil: Perfil,
    verificacion: VerificacionEstado,
    aprobada: Boolean,
    pacientes: Int,
    onVerificar: () -> Unit,
    onActualizar: () -> Unit,
    onMapa: () -> Unit,
    onCodigo: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    transicion: TransicionPerfil? = null,
) {
    val solicitud = verificacion.verificacion
    val estadoVerificacion = when (solicitud?.estado) {
        EstadoVerificacion.APROBADA -> R.string.perfil_estado_aprobada
        EstadoVerificacion.PENDIENTE -> R.string.perfil_estado_pendiente
        EstadoVerificacion.RECHAZADA -> R.string.perfil_estado_rechazada
        null -> R.string.perfil_estado_sin
    }
    PerfilScreen(
        perfil = perfil,
        rol = R.string.rol_nutriologo,
        verificado = aprobada,
        estadisticas = listOfNotNull(
            pluralStringResource(R.plurals.perfil_pacientes, pacientes, pacientes),
            stringResource(estadoVerificacion),
            perfil.altaCorta()?.let { stringResource(R.string.perfil_desde, it) },
        ),
        principal = AccionPerfil(R.drawable.ic_compartir, R.string.perfil_codigo, onCodigo),
        secundaria = AccionPerfil(R.drawable.ic_ubicacion, R.string.nutri_nav_mapa, onMapa),
        datos = datosBase(perfil, R.string.rol_nutriologo) +
            listOfNotNull(solicitud?.let { DatoPerfil(R.drawable.ic_verificado, R.string.perfil_dato_cedula, it.cedula) }),
        onCerrarSesion = onCerrarSesion,
        modifier = modifier,
        espacioInferior = ALTO_BARRA_INFERIOR,
        transicion = transicion,
        // los pacientes que ya no están (se fueron o se les dio de baja), solo para leer
        anteriores = { volver -> VinculosAnterioresScreen(comoPaciente = false, onVolver = volver, espacioInferior = ALTO_BARRA_INFERIOR) },
        datosExtra = { TarjetaVerificacion(verificacion, onVerificar, Modifier.padding(horizontal = 16.dp), onActualizar = onActualizar) },
    )
}
