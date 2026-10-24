package com.example.nutrigajo.ui.nutriologo.ubicacion

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.repository.DirectorioRepository
import com.example.nutrigajo.domain.model.DatosConsultorio
import com.example.nutrigajo.domain.model.NutriologoEnMapa
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UbicacionEstado(
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    // null: aún no está en el mapa
    val ubicacion: NutriologoEnMapa? = null,
    // se guardó o se quitó el punto: la pantalla se cierra y lo consume
    val listo: Boolean = false,
    @param:StringRes val error: Int? = null,
)

// ponytail: repositorio por defecto en el constructor; pasa a inyección (Hilt) cuando haya más ViewModels.
class UbicacionViewModel(private val repo: DirectorioRepository = DirectorioRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(UbicacionEstado())
    val estado: StateFlow<UbicacionEstado> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null, listo = false) }
        viewModelScope.launch {
            try {
                val ubicacion = repo.miUbicacion()
                _estado.update { it.copy(cargando = false, ubicacion = ubicacion) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun publicar(nombre: String, lat: Double, lng: Double, consultorio: DatosConsultorio) =
        guardar { repo.publicar(nombre, lat, lng, consultorio) }

    fun quitar() = guardar { repo.quitar() }

    private fun guardar(accion: suspend () -> Unit) {
        if (_estado.value.guardando) return
        _estado.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            try {
                accion()
                _estado.update { it.copy(guardando = false, listo = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(guardando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun consumirListo() = _estado.update { it.copy(listo = false) }

    fun limpiarError() = _estado.update { if (it.error == null) it else it.copy(error = null) }
}
