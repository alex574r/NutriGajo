package com.example.nutrigajo.ui.shared.ejercicios

import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.EjerciciosRepository
import com.example.nutrigajo.data.repository.FotoPerfilRepository
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

data class EjerciciosEstado(
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    // id del ejercicio sobre el que se actúa (retirar, publicar…): bloquea sus botones
    val trabajando: String? = null,
    val items: List<Ejercicio> = emptyList(),
    @param:StringRes val error: Int? = null,
)

// Una lista de ejercicios según quién la pide: los del entrenador, los publicados (Explorar) o los que espera el ADMIN. Cada pantalla
// usa su propia instancia (clave propia), así las listas no se mezclan.
// ponytail: se carga al abrir y al actualizar; un listener en tiempo real si el banco crece.
class EjerciciosViewModel(
    private val repo: EjerciciosRepository = EjerciciosRepository(),
    private val fotos: FotoPerfilRepository = FotoPerfilRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(EjerciciosEstado())
    val estado: StateFlow<EjerciciosEstado> = _estado.asStateFlow()

    fun cargarMios() = cargar(repo::mios)

    fun cargarPublicados() = cargar(repo::publicados)

    fun cargarEnRevision() = cargar(repo::enRevision)

    private fun cargar(leer: suspend () -> List<Ejercicio>) {
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
        ejercicio: Ejercicio,
        autorNombre: String,
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
                val guardado = repo.guardar(ejercicio, autorNombre, enviarARevision, jpeg, cache)
                _estado.update { estado ->
                    estado.copy(guardando = false, items = (listOf(guardado) + estado.items.filterNot { it.id == guardado.id }))
                }
                alTerminar(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorDeFoto) {
                _estado.update { it.copy(guardando = false, error = R.string.ejercicio_error_foto) }
                alTerminar(false)
            } catch (e: Exception) {
                _estado.update { it.copy(guardando = false, error = mensajeDeFirestore(e)) }
                alTerminar(false)
            }
        }
    }

    fun retirar(ejercicio: Ejercicio, alTerminar: () -> Unit = {}) = actuar(ejercicio.id, alTerminar) { repo.retirar(ejercicio.id) }

    fun sacarDeRevision(ejercicio: Ejercicio, alTerminar: () -> Unit = {}) = actuar(ejercicio.id, alTerminar) { repo.sacarDeRevision(ejercicio.id) }

    fun eliminar(ejercicio: Ejercicio, alTerminar: () -> Unit = {}) = actuar(ejercicio.id, alTerminar) { repo.eliminar(ejercicio.id) }

    fun publicar(ejercicio: Ejercicio, alTerminar: () -> Unit = {}) = actuar(ejercicio.id, alTerminar) { repo.publicar(ejercicio.id) }

    fun rechazar(ejercicio: Ejercicio, motivo: String, alTerminar: () -> Unit = {}) =
        actuar(ejercicio.id, alTerminar) { repo.rechazar(ejercicio.id, motivo) }

    // Hecha la acción, el ejercicio sale de la lista (el ADMIN lo resolvió) o la lista se lee otra vez (el entrenador lo cambió de estado).
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
