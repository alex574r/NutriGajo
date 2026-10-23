package com.example.nutrigajo.ui.shared.perfil

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.AuthRepository
import com.example.nutrigajo.data.repository.FotoPerfil
import com.example.nutrigajo.data.repository.FotoPerfilRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class PerfilEstado(
    val enviando: Boolean = false,
    val enviado: Boolean = false,
    @param:StringRes val error: Int? = null,
)

// null mientras no haya foto propia: la pantalla usa la de siempre (la portada) o la inicial (el avatar)
data class FotoEstado(
    val imagen: Bitmap? = null,
    val version: Long? = null,
    val subiendo: Boolean = false,
    @param:StringRes val error: Int? = null,
)

// Lo que el perfil escribe fuera de la pantalla: el correo para cambiar la contraseña y las dos fotos.
// ponytail: repositorios por defecto en el constructor; pasan a inyección (Hilt) cuando haya más ViewModels.
class PerfilViewModel(
    private val repo: AuthRepository = AuthRepository(),
    private val fotos: FotoPerfilRepository = FotoPerfilRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(PerfilEstado())
    val estado: StateFlow<PerfilEstado> = _estado.asStateFlow()

    private val _portada = MutableStateFlow(FotoEstado())
    val portada: StateFlow<FotoEstado> = _portada.asStateFlow()

    private val _avatar = MutableStateFlow(FotoEstado())
    val avatar: StateFlow<FotoEstado> = _avatar.asStateFlow()

    private fun flujo(foto: FotoPerfil) = if (foto == FotoPerfil.PORTADA) _portada else _avatar

    fun restablecerContrasena(correo: String) {
        if (_estado.value.enviando) return
        _estado.value = PerfilEstado(enviando = true)
        viewModelScope.launch {
            _estado.value = try {
                repo.restablecerContrasena(correo)
                PerfilEstado(enviado = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PerfilEstado(
                    error = when (e) {
                        is FirebaseNetworkException -> R.string.error_red
                        is FirebaseTooManyRequestsException -> R.string.error_demasiados_intentos
                        else -> R.string.error_generico
                    },
                )
            }
        }
    }

    fun reiniciar() {
        if (!_estado.value.enviando) _estado.value = PerfilEstado()
    }

    // Trae la foto de la versión que dice el perfil; si falla se queda la de siempre.
    fun cargarFoto(foto: FotoPerfil, cache: File, uid: String, version: Long?) {
        val estado = flujo(foto)
        if (version == null || (estado.value.version ?: 0L) >= version) return
        viewModelScope.launch {
            try {
                val imagen = fotos.cargar(foto, cache, uid, version)
                estado.update { if ((it.version ?: 0L) >= version) it else it.copy(imagen = imagen, version = version) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun cambiarFoto(foto: FotoPerfil, uid: String, resolver: ContentResolver, cache: File, uri: Uri) {
        val estado = flujo(foto)
        if (estado.value.subiendo) return
        estado.update { it.copy(subiendo = true, error = null) }
        viewModelScope.launch {
            try {
                val version = fotos.subir(foto, cache, fotos.preparar(resolver, uri, foto))
                estado.value = FotoEstado(imagen = fotos.cargar(foto, cache, uid, version), version = version)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val sinRed = e is UnknownHostException || e is ConnectException || e is SocketTimeoutException
                estado.update { it.copy(subiendo = false, error = if (sinRed) R.string.error_red else R.string.perfil_foto_error) }
            }
        }
    }
}
