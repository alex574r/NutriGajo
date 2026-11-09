package com.example.nutrigajo.ui.shared.ajustes

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.AuthRepository
import com.example.nutrigajo.data.repository.ConfirmacionVencidaException
import com.example.nutrigajo.data.repository.CuentaRepository
import com.example.nutrigajo.data.repository.EnlaceInvalidoException
import com.example.nutrigajo.data.repository.VinculacionRepository
import com.example.nutrigajo.domain.extraerEnlace
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Los pasos de borrar la cuenta: leer el aviso, esperar a que la persona abra el enlace del correo y, ya confirmada, eliminar.
enum class PasoEliminar { AVISO, ENLACE, CONFIRMADO }

data class EliminarCuentaEstado(
    val paso: PasoEliminar = PasoEliminar.AVISO,
    val trabajando: Boolean = false,
    // solo para un nutriólogo: cuántos pacientes con vínculo activo tiene (null mientras se lee o si no se pudo leer)
    val pacientes: Int? = null,
    @param:StringRes val error: Int? = null,
    // el Worker borró la cuenta: la pantalla cierra la sesión
    val eliminada: Boolean = false,
)

// ponytail: repositorios por defecto en el constructor; pasan a inyección (Hilt) cuando haya más ViewModels.
class EliminarCuentaViewModel(
    private val auth: AuthRepository = AuthRepository(),
    private val cuenta: CuentaRepository = CuentaRepository(),
    private val vinculos: VinculacionRepository = VinculacionRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(EliminarCuentaEstado())
    val estado: StateFlow<EliminarCuentaEstado> = _estado.asStateFlow()

    // El aviso de un nutriólogo dice cuántos pacientes tiene: cada vínculo se cierra. Si no se puede leer, el aviso va sin el número.
    fun cargarPacientes() {
        viewModelScope.launch {
            try {
                val total = vinculos.pacientes().size
                _estado.update { it.copy(pacientes = total) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun enviarEnlace() = accion(
        { auth.enviarEnlaceDeConfirmacion() },
        alTerminar = { it.copy(paso = PasoEliminar.ENLACE) },
        mensajeDe = { e ->
            when (e) {
                is FirebaseNetworkException -> R.string.error_red
                is FirebaseTooManyRequestsException -> R.string.error_demasiados_intentos
                else -> R.string.eliminar_cuenta_error_enlace
            }
        },
    )

    // Con lo pegado en la casilla: saca el enlace del texto y vuelve a confirmar la identidad con él.
    fun confirmar(textoPegado: String) {
        val enlace = extraerEnlace(textoPegado)
        if (enlace == null) {
            _estado.update { it.copy(error = R.string.eliminar_cuenta_enlace_invalido) }
            return
        }
        accion(
            { auth.confirmarConEnlace(enlace) },
            alTerminar = { it.copy(paso = PasoEliminar.CONFIRMADO) },
            mensajeDe = { e ->
                when (e) {
                    is EnlaceInvalidoException -> R.string.eliminar_cuenta_enlace_invalido
                    is FirebaseNetworkException -> R.string.error_red
                    else -> R.string.eliminar_cuenta_enlace_vencido
                }
            },
        )
    }

    // El último paso: el Worker borra todo. Si la confirmación ya venció se vuelve al principio; si falló otra cosa se puede reintentar aquí.
    fun eliminar() = accion(
        { cuenta.eliminar() },
        alTerminar = { it.copy(eliminada = true) },
        mensajeDe = { e -> if (e is ConfirmacionVencidaException) R.string.eliminar_cuenta_vencida else R.string.eliminar_cuenta_error },
        alFallar = { e, estado -> if (e is ConfirmacionVencidaException) estado.copy(paso = PasoEliminar.AVISO) else estado },
    )

    fun limpiarError() = _estado.update { if (it.error == null) it else it.copy(error = null) }

    private fun accion(
        trabajo: suspend () -> Unit,
        alTerminar: (EliminarCuentaEstado) -> EliminarCuentaEstado,
        @StringRes mensajeDe: (Exception) -> Int,
        alFallar: (Exception, EliminarCuentaEstado) -> EliminarCuentaEstado = { _, estado -> estado },
    ) {
        if (_estado.value.trabajando) return
        _estado.update { it.copy(trabajando = true, error = null) }
        viewModelScope.launch {
            try {
                trabajo()
                _estado.update { alTerminar(it).copy(trabajando = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { alFallar(e, it).copy(trabajando = false, error = mensajeDe(e)) }
            }
        }
    }
}
