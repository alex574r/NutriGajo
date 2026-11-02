package com.example.nutrigajo.ui.nutriologo.verificacion

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.VerificacionException
import com.example.nutrigajo.data.repository.VerificacionRepository
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Verificacion
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerificacionEstado(
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    // null: aún no ha enviado su cédula (o su credencial, si es entrenador)
    val verificacion: Verificacion? = null,
    @param:StringRes val error: Int? = null,
)

// ponytail: repositorio por defecto en el constructor; pasa a inyección (Hilt) cuando haya más ViewModels.
class VerificacionViewModel(private val repo: VerificacionRepository = VerificacionRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(VerificacionEstado())
    val estado: StateFlow<VerificacionEstado> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val verificacion = repo.miVerificacion()
                _estado.update { it.copy(cargando = false, verificacion = verificacion) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun enviar(perfil: Perfil, nombreCedula: String, cedula: String) {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            try {
                repo.enviar(perfil, nombreCedula, cedula)
                // se relee para mostrar lo que quedó guardado
                val verificacion = repo.miVerificacion()
                _estado.update { it.copy(enviando = false, verificacion = verificacion) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(enviando = false, error = mensajeDe(e)) }
            }
        }
    }

    fun enviarCredencial(perfil: Perfil, credencial: String) {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            try {
                repo.enviarCredencial(perfil, credencial)
                val verificacion = repo.miVerificacion()
                _estado.update { it.copy(enviando = false, verificacion = verificacion) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(enviando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun limpiarError() = _estado.update { if (it.error == null) it else it.copy(error = null) }

    @StringRes
    private fun mensajeDe(e: Exception): Int = when {
        e is VerificacionException -> when (e.motivo) {
            VerificacionException.Motivo.CEDULA_INVALIDA -> R.string.error_cedula_invalida
            VerificacionException.Motivo.NOMBRE_DISTINTO -> R.string.error_nombre_distinto
            VerificacionException.Motivo.CEDULA_EN_USO -> R.string.error_cedula_en_uso
        }
        // otra cuenta reservó la cédula justo antes: las reglas rechazan el lote
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_cedula_en_uso
        else -> mensajeDeFirestore(e)
    }
}
