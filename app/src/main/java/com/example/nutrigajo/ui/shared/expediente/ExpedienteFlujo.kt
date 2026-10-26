package com.example.nutrigajo.ui.shared.expediente

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.components.PantallaCarga

// Expediente de un paciente: resumen y asistente para llenarlo o editarlo. Lo usan el paciente (su propio expediente)
// y el nutriólogo (el de cada uno de sus pacientes); `editor` es quien firma lo que se guarde.
// El paciente entra directo al asistente si aún no lo completa; el nutriólogo ve primero lo que hay.
@Composable
fun ExpedienteFlujo(
    pacienteId: String,
    nombrePaciente: String,
    editor: Rol,
    onSalir: () -> Unit,
    // el nutriólogo puede pasar del expediente al panel del plan de su paciente; el paciente no lo tiene
    onPlan: (() -> Unit)? = null,
    // clave propia: la tarjeta del inicio del paciente comparte este ViewModel
    vm: ExpedienteViewModel = viewModel(key = "expediente-$pacienteId-${editor.name}"),
) {
    val estado by vm.estado.collectAsState()
    // null: lo decide el rol y si el expediente está completo; true/false: lo eligió quien lo usa
    var enAsistente by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(pacienteId) { vm.cargar(pacienteId) }
    LaunchedEffect(estado.listo) {
        if (estado.listo) {
            vm.consumirListo()
            enAsistente = false
        }
    }

    when {
        estado.cargando -> PantallaCarga()
        (enAsistente ?: (editor == Rol.PACIENTE && !estado.expediente.completo)) -> AsistenteScreen(
            estado = estado,
            onCambiar = vm::cambiar,
            onContinuar = { vm.avanzar(pacienteId, editor) },
            onOmitir = { vm.avanzar(pacienteId, editor, omitir = true) },
            onRegresar = {
                when {
                    estado.paso > 0 -> vm.retroceder()
                    // se abrió desde el resumen: se vuelve a él; si no, se sale
                    enAsistente == true -> enAsistente = false
                    else -> onSalir()
                }
            },
        )
        else -> ResumenScreen(
            nombrePaciente = nombrePaciente,
            expediente = estado.expediente,
            onEditar = { enAsistente = true },
            onVolver = onSalir,
            onPlan = onPlan,
        )
    }
}
