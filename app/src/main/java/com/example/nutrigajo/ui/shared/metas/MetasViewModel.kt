package com.example.nutrigajo.ui.shared.metas

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.repository.MetasRepository
import com.example.nutrigajo.domain.model.MetasDiarias
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MetasEstado(
    val guardando: Boolean = false,
    // las metas se guardaron: quien lo ve cierra el editor y vuelve a pedir el registro del paciente
    val guardado: Boolean = false,
    @param:StringRes val error: Int? = null,
)

// ponytail: repositorio por defecto en el constructor, como los demás ViewModels.
class MetasViewModel(private val repo: MetasRepository = MetasRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(MetasEstado())
    val estado: StateFlow<MetasEstado> = _estado.asStateFlow()

    fun guardar(pacienteId: String, metas: MetasDiarias, por: Rol) {
        if (_estado.value.guardando) return
        _estado.value = MetasEstado(guardando = true)
        viewModelScope.launch {
            try {
                repo.guardar(pacienteId, metas, por)
                _estado.update { it.copy(guardando = false, guardado = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(guardando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    // al abrir o cerrar el editor: sin el «guardado» de la vez anterior ni su error
    fun limpiar() {
        _estado.value = MetasEstado()
    }
}
