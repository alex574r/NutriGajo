package com.example.nutrigajo.ui.shared.ajustes

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.repository.PlanesRepository
import com.example.nutrigajo.data.repository.VinculacionRepository
import com.example.nutrigajo.domain.model.PlanEnviado
import com.example.nutrigajo.domain.model.RegistroCerrado
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnterioresEstado(
    val cargando: Boolean = true,
    val registros: List<RegistroCerrado> = emptyList(),
    // el vínculo cerrado que se abrió para ver sus planes, con los planes (null mientras no se abre ninguno)
    val abierto: RegistroCerrado? = null,
    val planes: List<PlanEnviado> = emptyList(),
    val cargandoPlanes: Boolean = false,
    @param:StringRes val error: Int? = null,
)

// Los vínculos anteriores de una cuenta, solo para leer: la lista y, al abrir uno, los planes que se enviaron en él.
// ponytail: repositorios por defecto en el constructor; pasan a inyección (Hilt) cuando haya más ViewModels.
class VinculosAnterioresViewModel(
    private val repo: VinculacionRepository = VinculacionRepository(),
    private val planes: PlanesRepository = PlanesRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(AnterioresEstado())
    val estado: StateFlow<AnterioresEstado> = _estado.asStateFlow()

    fun cargar(comoPaciente: Boolean) {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val lista = repo.vinculosCerrados(comoPaciente)
                _estado.update { it.copy(cargando = false, registros = lista) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun abrir(registro: RegistroCerrado) {
        _estado.update { it.copy(abierto = registro, planes = emptyList(), cargandoPlanes = true, error = null) }
        viewModelScope.launch {
            try {
                val lista = planes.todos(registro.pacienteId)
                _estado.update { it.copy(cargandoPlanes = false, planes = lista) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargandoPlanes = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun volverALaLista() = _estado.update { it.copy(abierto = null, planes = emptyList(), error = null) }
}
