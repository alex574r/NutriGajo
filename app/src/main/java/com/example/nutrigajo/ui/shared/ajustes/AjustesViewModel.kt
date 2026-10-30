package com.example.nutrigajo.ui.shared.ajustes

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.repository.AuthRepository
import com.example.nutrigajo.data.repository.ComentariosRepository
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Lo que una pantalla de ajustes está haciendo en la red: guardar el nombre o enviar un comentario.
data class AjusteEstado(
    val enviando: Boolean = false,
    val listo: Boolean = false,
    @param:StringRes val error: Int? = null,
)

// ponytail: repositorios por defecto en el constructor; pasan a inyección (Hilt) cuando haya más ViewModels.
class AjustesViewModel(
    private val cuentas: AuthRepository = AuthRepository(),
    private val comentarios: ComentariosRepository = ComentariosRepository(),
) : ViewModel() {
    private val _nombre = MutableStateFlow(AjusteEstado())
    val nombre: StateFlow<AjusteEstado> = _nombre.asStateFlow()

    private val _comentario = MutableStateFlow(AjusteEstado())
    val comentario: StateFlow<AjusteEstado> = _comentario.asStateFlow()

    fun guardarNombre(nombres: String, apellidos: String) = lanzar(_nombre) { cuentas.actualizarNombre(nombres, apellidos) }

    fun enviarComentario(rol: Rol, areas: List<String>, mensaje: String, versionApp: String) =
        lanzar(_comentario) { comentarios.enviar(rol, areas, mensaje, versionApp) }

    // al salir de la pantalla se olvida el resultado: la próxima vez que se abra empieza limpia
    fun limpiar() {
        _nombre.value = AjusteEstado()
        _comentario.value = AjusteEstado()
    }

    private fun lanzar(estado: MutableStateFlow<AjusteEstado>, accion: suspend () -> Unit) {
        if (estado.value.enviando) return
        estado.value = AjusteEstado(enviando = true)
        viewModelScope.launch {
            try {
                accion()
                estado.update { AjusteEstado(listo = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                estado.update { AjusteEstado(error = mensajeDeFirestore(e)) }
            }
        }
    }
}
