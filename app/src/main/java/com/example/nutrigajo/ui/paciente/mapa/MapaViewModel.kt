package com.example.nutrigajo.ui.paciente.mapa

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.repository.DirectorioRepository
import com.example.nutrigajo.domain.model.NutriologoEnMapa
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapaEstado(
    val cargando: Boolean = true,
    val nutriologos: List<NutriologoEnMapa> = emptyList(),
    @param:StringRes val error: Int? = null,
)

// ponytail: repositorio por defecto en el constructor; pasa a inyección (Hilt) cuando haya más ViewModels.
class MapaViewModel(private val repo: DirectorioRepository = DirectorioRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(MapaEstado())
    val estado: StateFlow<MapaEstado> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val nutriologos = repo.nutriologos()
                _estado.update { it.copy(cargando = false, nutriologos = nutriologos) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }
}
