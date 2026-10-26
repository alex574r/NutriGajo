package com.example.nutrigajo.ui.shared.auth

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.AuthRepository
import com.example.nutrigajo.data.repository.DispositivoRepository
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// La navegación reacciona a `sesion`, no a callbacks: así sobrevive a un giro de pantalla a medio inicio de sesión.
data class AuthEstado(
    val sesion: Boolean,
    val cargando: Boolean = false,
    @param:StringRes val error: Int? = null,
    val perfil: Perfil? = null,
    // el perfil no se pudo leer (sin conexión, documento ausente o con rol desconocido)
    val perfilFallo: Boolean = false,
)

private const val LARGO_MIN_CONTRASENA = 6 // mínimo de Firebase Auth

// ponytail: repositorio por defecto en el constructor; pasa a inyección (Hilt) cuando haya más ViewModels.
class AuthViewModel(
    private val repo: AuthRepository = AuthRepository(),
    private val dispositivo: DispositivoRepository = DispositivoRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(AuthEstado(sesion = repo.sesionIniciada))
    val estado: StateFlow<AuthEstado> = _estado.asStateFlow()

    // el correo va siempre en minúsculas: así coincide con el de Firebase Auth y con lo que piden las reglas
    fun ingresar(correo: String, contrasena: String) =
        lanzar { repo.ingresar(correo.trim().lowercase(), contrasena) }

    fun registrar(nombre: String, correo: String, contrasena: String, confirmacion: String, rol: Rol) {
        val error = when {
            contrasena != confirmacion -> R.string.error_contrasenas_distintas
            contrasena.length < LARGO_MIN_CONTRASENA -> R.string.error_contrasena_debil
            else -> null
        }
        if (error != null) _estado.update { it.copy(error = error) }
        else lanzar { repo.registrar(nombre.trim(), correo.trim().lowercase(), contrasena, rol) }
    }

    fun cargarPerfil() {
        _estado.update { it.copy(perfilFallo = false) }
        viewModelScope.launch {
            val perfil = try {
                repo.perfil()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _estado.update { it.copy(perfil = perfil, perfilFallo = perfil == null) }
            // con el perfil ya a la vista, el teléfono se da de alta para los avisos; si falla no se nota (se reintenta al abrir la app)
            if (perfil != null) {
                try {
                    dispositivo.registrar()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("AuthViewModel", "No se pudo registrar el teléfono para los avisos", e)
                }
            }
        }
    }

    fun cerrarSesion() {
        // este teléfono deja de recibir los avisos de la cuenta antes de cerrar la sesión (después ya no hay permiso para escribir
        // en el perfil); si no se logra en unos segundos, la sesión se cierra igual
        viewModelScope.launch {
            try {
                withTimeoutOrNull(3_000) {
                    try {
                        dispositivo.olvidar()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                    }
                }
            } finally {
                repo.cerrarSesion()
            }
        }
        _estado.value = AuthEstado(sesion = false)
    }

    fun limpiarError() = _estado.update { if (it.error == null) it else it.copy(error = null) }

    private fun lanzar(accion: suspend () -> Unit) {
        if (_estado.value.cargando) return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                accion()
                _estado.update { it.copy(cargando = false, sesion = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDe(e)) }
            }
        }
    }

    @StringRes
    private fun mensajeDe(e: Exception): Int = when (e) {
        is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> R.string.error_credenciales
        is FirebaseAuthUserCollisionException -> R.string.error_correo_en_uso
        is FirebaseAuthWeakPasswordException -> R.string.error_contrasena_debil
        is FirebaseNetworkException -> R.string.error_red
        is FirebaseTooManyRequestsException -> R.string.error_demasiados_intentos
        else -> R.string.error_generico
    }
}
