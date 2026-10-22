package com.example.nutrigajo.ui.nutriologo.pacientes

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.repository.VinculacionRepository
import com.example.nutrigajo.domain.model.CodigoVinculacion
import com.example.nutrigajo.domain.model.PacienteResumen
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PacientesEstado(
    val cargando: Boolean = true,
    val generando: Boolean = false,
    val codigo: CodigoVinculacion? = null,
    val pacientes: List<PacienteResumen> = emptyList(),
    @param:StringRes val error: Int? = null,
)

// ponytail: repositorio por defecto en el constructor; pasa a inyección (Hilt) cuando haya más ViewModels.
class PacientesViewModel(private val repo: VinculacionRepository = VinculacionRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(PacientesEstado())
    val estado: StateFlow<PacientesEstado> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val codigo = repo.codigoVigente()
                val pacientes = repo.pacientes()
                _estado.update { it.copy(cargando = false, codigo = codigo, pacientes = pacientes) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun generarCodigo(nutriologoNombre: String) {
        if (_estado.value.generando) return
        _estado.update { it.copy(generando = true, error = null) }
        viewModelScope.launch {
            try {
                val codigo = repo.generarCodigo(nutriologoNombre)
                _estado.update { it.copy(generando = false, codigo = codigo) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(generando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }
}
