package com.example.nutrigajo.ui.paciente.inicio

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.VinculacionRepository
import com.example.nutrigajo.data.repository.VinculoException
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Vinculo
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VinculoEstado(
    val cargando: Boolean = true,
    val canjeando: Boolean = false,
    val vinculo: Vinculo? = null,
    @param:StringRes val error: Int? = null,
)

// ponytail: repositorio por defecto en el constructor; pasa a inyección (Hilt) cuando haya más ViewModels.
class VinculoViewModel(private val repo: VinculacionRepository = VinculacionRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(VinculoEstado())
    val estado: StateFlow<VinculoEstado> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val vinculo = repo.vinculoActual()
                _estado.update { it.copy(cargando = false, vinculo = vinculo) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // sin vínculo conocido se ofrece escribir el código; el error avisa por qué no se pudo consultar
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    // Vuelve a leer el vínculo sin tapar la pantalla con la carga: al volver del chat, para el punto de «sin leer».
    fun refrescar() {
        viewModelScope.launch {
            try {
                repo.vinculoActual()?.let { vinculo -> _estado.update { it.copy(vinculo = vinculo) } }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun canjear(codigo: String, paciente: Perfil) {
        if (_estado.value.canjeando) return
        _estado.update { it.copy(canjeando = true, error = null) }
        viewModelScope.launch {
            try {
                val vinculo = repo.canjear(codigo, paciente)
                _estado.update { it.copy(canjeando = false, vinculo = vinculo) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(canjeando = false, error = mensajeDe(e)) }
            }
        }
    }

    fun limpiarError() = _estado.update { if (it.error == null) it else it.copy(error = null) }

    @StringRes
    private fun mensajeDe(e: Exception): Int = when {
        e is VinculoException -> when (e.motivo) {
            VinculoException.Motivo.INVALIDO -> R.string.error_codigo_invalido
            VinculoException.Motivo.USADO -> R.string.error_codigo_usado
            VinculoException.Motivo.VENCIDO -> R.string.error_codigo_vencido
        }
        // otro paciente lo canjeó justo antes: las reglas rechazan el lote
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_codigo_usado
        else -> mensajeDeFirestore(e)
    }
}
