package com.example.nutrigajo.ui.admin.verificaciones

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.repository.VerificacionRepository
import com.example.nutrigajo.domain.model.SolicitudVerificacion
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerificacionesEstado(
    val cargando: Boolean = true,
    // uid del nutriólogo cuya solicitud se está resolviendo: bloquea sus botones
    val trabajando: String? = null,
    val solicitudes: List<SolicitudVerificacion> = emptyList(),
    @param:StringRes val error: Int? = null,
)

// ponytail: se carga al abrir y con "Actualizar"; un listener en tiempo real si la cola crece.
class VerificacionesViewModel(private val repo: VerificacionRepository = VerificacionRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(VerificacionesEstado())
    val estado: StateFlow<VerificacionesEstado> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val solicitudes = repo.pendientes()
                _estado.update { it.copy(cargando = false, solicitudes = solicitudes) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun aprobar(solicitud: SolicitudVerificacion) = resolver(solicitud) { repo.aprobar(solicitud) }

    fun rechazar(solicitud: SolicitudVerificacion, motivo: String) = resolver(solicitud) { repo.rechazar(solicitud, motivo) }

    private fun resolver(solicitud: SolicitudVerificacion, accion: suspend () -> Unit) {
        if (_estado.value.trabajando != null) return
        _estado.update { it.copy(trabajando = solicitud.nutriologoId, error = null) }
        viewModelScope.launch {
            try {
                accion()
                _estado.update {
                    it.copy(trabajando = null, solicitudes = it.solicitudes.filterNot { s -> s.nutriologoId == solicitud.nutriologoId })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(trabajando = null, error = mensajeDeFirestore(e)) }
            }
        }
    }
}
