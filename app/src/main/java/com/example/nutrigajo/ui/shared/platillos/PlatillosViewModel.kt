package com.example.nutrigajo.ui.shared.platillos

import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.FotoPerfilRepository
import com.example.nutrigajo.data.repository.PlatillosRepository
import com.example.nutrigajo.domain.model.Platillo
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

data class PlatillosEstado(
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    // id del platillo sobre el que se actúa (retirar, publicar…): bloquea sus botones
    val trabajando: String? = null,
    val items: List<Platillo> = emptyList(),
    @param:StringRes val error: Int? = null,
)

// Una lista de platillos según quién la pide: los del autor, los publicados (Explorar y el selector del plan) o los que espera el ADMIN.
// Cada pantalla usa su propia instancia (clave propia), así las listas no se mezclan.
// ponytail: se carga al abrir y al actualizar; un listener en tiempo real si el banco crece.
class PlatillosViewModel(
    private val repo: PlatillosRepository = PlatillosRepository(),
    private val fotos: FotoPerfilRepository = FotoPerfilRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(PlatillosEstado())
    val estado: StateFlow<PlatillosEstado> = _estado.asStateFlow()

    fun cargarMios() = cargar(repo::mios)

    fun cargarPublicados() = cargar(repo::publicados)

    fun cargarEnRevision() = cargar(repo::enRevision)

    private fun cargar(leer: suspend () -> List<Platillo>) {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val items = leer()
                _estado.update { it.copy(cargando = false, items = items) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    // La foto elegida se reduce a 1280 px y se pasa a JPEG antes de subirla. `alTerminar(true)` si todo quedó guardado.
    fun guardar(
        platillo: Platillo,
        autorNombre: String,
        autorRol: Rol,
        enviarARevision: Boolean,
        foto: Uri?,
        resolver: ContentResolver,
        cache: File,
        alTerminar: (Boolean) -> Unit,
    ) {
        if (_estado.value.guardando) return
        _estado.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            try {
                val jpeg = foto?.let {
                    try {
                        fotos.preparar(resolver, it, LADO_FOTO, cuadrada = false)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        throw ErrorDeFoto(e)
                    }
                }
                val guardado = repo.guardar(platillo, autorNombre, autorRol, enviarARevision, jpeg, cache)
                _estado.update { estado ->
                    estado.copy(guardando = false, items = (listOf(guardado) + estado.items.filterNot { it.id == guardado.id }))
                }
                alTerminar(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorDeFoto) {
                _estado.update { it.copy(guardando = false, error = R.string.platillo_error_foto) }
                alTerminar(false)
            } catch (e: Exception) {
                _estado.update { it.copy(guardando = false, error = mensajeDeFirestore(e)) }
                alTerminar(false)
            }
        }
    }

    fun retirar(platillo: Platillo, alTerminar: () -> Unit = {}) = actuar(platillo.id, alTerminar) { repo.retirar(platillo.id) }

    fun sacarDeRevision(platillo: Platillo, alTerminar: () -> Unit = {}) = actuar(platillo.id, alTerminar) { repo.sacarDeRevision(platillo.id) }

    fun eliminar(platillo: Platillo, alTerminar: () -> Unit = {}) = actuar(platillo.id, alTerminar) { repo.eliminar(platillo.id) }

    fun publicar(platillo: Platillo, alTerminar: () -> Unit = {}) = actuar(platillo.id, alTerminar) { repo.publicar(platillo.id) }

    fun rechazar(platillo: Platillo, motivo: String, alTerminar: () -> Unit = {}) =
        actuar(platillo.id, alTerminar) { repo.rechazar(platillo.id, motivo) }

    // Hecha la acción, el platillo sale de la lista (el ADMIN lo resolvió) o la lista se lee otra vez (el autor lo cambió de estado).
    private fun actuar(id: String, alTerminar: () -> Unit, accion: suspend () -> Unit) {
        if (_estado.value.trabajando != null) return
        _estado.update { it.copy(trabajando = id, error = null) }
        viewModelScope.launch {
            try {
                accion()
                _estado.update { estado -> estado.copy(trabajando = null, items = estado.items.filterNot { it.id == id }) }
                alTerminar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(trabajando = null, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun limpiarError() = _estado.update { if (it.error == null) it else it.copy(error = null) }

    private class ErrorDeFoto(causa: Exception) : IOException(causa)

    private companion object {
        const val LADO_FOTO = 1280
    }
}
