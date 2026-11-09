package com.example.nutrigajo.ui.shared.ajustes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.RegistroCerrado
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.TarjetaAjustes
import com.example.nutrigajo.ui.paciente.plan.PlanPacienteScreen
import com.example.nutrigajo.ui.paciente.plan.PlanPacienteViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// «Vínculos anteriores» (Configuración): los vínculos que ya se cerraron —el paciente con cada nutriólogo que dejó, o el nutriólogo con cada
// paciente que se fue o dio de baja— y, al abrir uno, los planes que se enviaron en él (con el visor de PDF de siempre). Es solo para leer:
// no hay chat, ni metas, ni planes nuevos.
@Composable
fun VinculosAnterioresScreen(
    comoPaciente: Boolean,
    onVolver: () -> Unit,
    espacioInferior: Dp = 0.dp,
    // claves propias: los ViewModels de las otras pantallas del mismo usuario no se pisan con estos
    vm: VinculosAnterioresViewModel = viewModel(key = "vinculos-anteriores-$comoPaciente"),
    planVm: PlanPacienteViewModel = viewModel(key = "vinculos-anteriores-plan"),
) {
    val estado by vm.estado.collectAsState()
    var planAbierto by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(comoPaciente) { vm.cargar(comoPaciente) }
    val abierto = estado.abierto

    val plan = planAbierto
    if (abierto != null && plan != null) {
        PlanPacienteScreen(pacienteId = abierto.pacienteId, onVolver = { planAbierto = null }, vm = planVm, planId = plan)
        return
    }

    val formato = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()) }
    val fecha = { ms: Long -> formato.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())) }
    val colores = MaterialTheme.colorScheme
    PantallaAjuste(
        titulo = abierto?.nombre?.ifBlank { null } ?: stringResource(R.string.ajustes_vinculos_anteriores),
        onVolver = { if (abierto != null) vm.volverALaLista() else onVolver() },
        espacioInferior = espacioInferior,
    ) {
        Text(stringResource(R.string.anteriores_solo_lectura), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
        estado.error?.let { AlertaError(stringResource(it), Modifier.fillMaxWidth()) }
        when {
            abierto == null && estado.cargando -> repeat(3) { Esqueleto(Modifier.fillMaxWidth().height(72.dp), RoundedCornerShape(22.dp)) }
            abierto == null && estado.registros.isEmpty() ->
                Text(stringResource(R.string.anteriores_vacio), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
            abierto == null -> TarjetaAjustes {
                estado.registros.forEach { registro ->
                    FilaAjuste(
                        icono = R.drawable.ic_pacientes,
                        titulo = registro.nombre.ifBlank { stringResource(R.string.anteriores_sin_nombre) },
                        descripcion = descripcion(registro, fecha),
                        plana = true,
                        onClick = { vm.abrir(registro) },
                    )
                }
            }
            estado.cargandoPlanes -> repeat(2) { Esqueleto(Modifier.fillMaxWidth().height(72.dp), RoundedCornerShape(22.dp)) }
            estado.planes.isEmpty() ->
                Text(stringResource(R.string.anteriores_planes_vacio), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
            else -> TarjetaAjustes {
                estado.planes.forEach { p ->
                    FilaAjuste(
                        icono = R.drawable.ic_documento,
                        titulo = stringResource(R.string.anteriores_plan, fecha(p.enviadoEn)),
                        descripcion = p.nutriologoNombre,
                        plana = true,
                        onClick = { planAbierto = p.id },
                    )
                }
            }
        }
    }
}

// «Cerrado el 5 nov. 2026», y «· Cuenta eliminada» si el vínculo lo cerró el borrado de una cuenta.
@Composable
private fun descripcion(registro: RegistroCerrado, fecha: (Long) -> String): String {
    val cierre = if (registro.cerradoEn > 0) stringResource(R.string.anteriores_cerrado, fecha(registro.cerradoEn)) else ""
    val eliminada = if (registro.porCuentaEliminada) stringResource(R.string.anteriores_cuenta_eliminada) else ""
    return listOf(cierre, eliminada).filter { it.isNotEmpty() }.joinToString(" · ")
}
