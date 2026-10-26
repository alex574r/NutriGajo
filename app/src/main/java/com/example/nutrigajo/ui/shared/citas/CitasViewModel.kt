package com.example.nutrigajo.ui.shared.citas

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.repository.AvisoCitaRepository
import com.example.nutrigajo.data.repository.CitasRepository
import com.example.nutrigajo.domain.model.Cita
import com.example.nutrigajo.domain.model.EstadoCita
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CitasEstado(
    val cargando: Boolean = true,
    val citas: List<Cita> = emptyList(),
    // cita que se está cambiando: sus botones esperan
    val trabajando: String? = null,
    @param:StringRes val error: Int? = null,
)

// Las citas del usuario según su rol (las que pidió el paciente o las que recibió el nutriólogo) y sus cambios de estado.
// ponytail: se carga al abrir y con "deslizar para actualizar"; un listener en tiempo real cuando haga falta.
class CitasViewModel(
    private val repo: CitasRepository = CitasRepository(),
    private val avisos: AvisoCitaRepository = AvisoCitaRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(CitasEstado())
    val estado: StateFlow<CitasEstado> = _estado.asStateFlow()

    private var rol = Rol.PACIENTE

    fun cargar(rol: Rol) {
        this.rol = rol
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val citas = repo.citas(rol)
                _estado.update { it.copy(cargando = false, citas = citas) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun cambiarEstado(cita: Cita, estado: EstadoCita) {
        if (_estado.value.trabajando != null) return
        _estado.update { it.copy(trabajando = cita.id, error = null) }
        viewModelScope.launch {
            val error = try {
                repo.cambiarEstado(cita, estado)
                if (estado == EstadoCita.CONFIRMADA) avisarAlPaciente(cita)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mensajeDeFirestore(e)
            }
            // la lista se vuelve a pedir: refleja el cambio, o lo que la otra parte ya cambió
            val citas = try {
                repo.citas(rol)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            _estado.update { it.copy(trabajando = null, error = error, citas = citas ?: it.citas) }
        }
    }

    // El push al paciente va aparte de la confirmación: no hace esperar al nutriólogo y, si el Worker falla, la cita queda confirmada igual.
    // ponytail: sin reintento; el paciente la verá en «Mis citas» aunque no le llegue el aviso.
    private fun avisarAlPaciente(cita: Cita) {
        viewModelScope.launch {
            try {
                avisos.confirmada(cita.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("CitasViewModel", "No se pudo avisar al paciente de la cita ${cita.id}", e)
            }
        }
    }
}
