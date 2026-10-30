package com.example.nutrigajo.ui.paciente.plan

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.PlanesRepository
import com.example.nutrigajo.domain.model.PlanEnviado
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

data class PlanPacienteEstado(
    val cargando: Boolean = true,
    // el último plan que le envió su nutriólogo; null si aún no tiene
    val plan: PlanEnviado? = null,
    // todos los que ha recibido, del más nuevo al más viejo: las notificaciones de la campana
    val planes: List<PlanEnviado> = emptyList(),
    val abriendo: Boolean = false,
    // el PDF del plan ya descargado
    val pdf: File? = null,
    @param:StringRes val error: Int? = null,
)

// El plan que el paciente recibió de su nutriólogo: el aviso del inicio y el PDF para verlo.
class PlanPacienteViewModel(private val repo: PlanesRepository = PlanesRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(PlanPacienteEstado())
    val estado: StateFlow<PlanPacienteEstado> = _estado.asStateFlow()

    fun cargar(pacienteId: String) {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val planes = repo.todos(pacienteId)
                _estado.update { it.copy(cargando = false, plan = planes.firstOrNull(), planes = planes) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    // Trae el PDF del plan `planId` o, sin él, del último (si el teléfono ya lo tiene, usa esa copia) y lo deja listo para el visor.
    fun abrir(pacienteId: String, cache: File, planId: String? = null) {
        if (_estado.value.abriendo) return
        _estado.update { it.copy(abriendo = true, error = null) }
        viewModelScope.launch {
            try {
                // sin id se vuelve a preguntar cuál es el último: puede haber uno más nuevo que el de la última carga
                val ultimo = if (planId == null) repo.ultimo(pacienteId) else null
                val pdf = (planId ?: ultimo?.id)?.let { repo.pdf(pacienteId, it, cache) }
                _estado.update {
                    it.copy(abriendo = false, plan = if (planId == null) ultimo else it.plan, pdf = pdf, error = if (pdf == null) R.string.plan_paciente_error else null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(abriendo = false, error = if (e is IOException) R.string.plan_paciente_error else mensajeDeFirestore(e)) }
            }
        }
    }

    fun cerrar() = _estado.update { it.copy(pdf = null, error = null) }
}
