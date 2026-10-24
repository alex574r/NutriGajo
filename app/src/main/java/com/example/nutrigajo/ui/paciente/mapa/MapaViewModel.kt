package com.example.nutrigajo.ui.paciente.mapa

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.repository.DirectorioRepository
import com.example.nutrigajo.data.repository.FotoPerfilRepository
import com.example.nutrigajo.domain.model.NutriologoEnMapa
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class MapaEstado(
    val cargando: Boolean = true,
    val nutriologos: List<NutriologoEnMapa> = emptyList(),
    @param:StringRes val error: Int? = null,
    // nutriólogo de la ficha abierta y su foto de perfil (null mientras llega, o si no tiene una)
    val seleccionado: NutriologoEnMapa? = null,
    val foto: Bitmap? = null,
)

// ponytail: repositorios por defecto en el constructor; pasan a inyección (Hilt) cuando haya más ViewModels.
class MapaViewModel(
    private val repo: DirectorioRepository = DirectorioRepository(),
    private val fotos: FotoPerfilRepository = FotoPerfilRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(MapaEstado())
    val estado: StateFlow<MapaEstado> = _estado.asStateFlow()

    // Abre la ficha con lo que ya se sabe del nutriólogo y trae su foto; sin ella queda la inicial.
    fun abrirFicha(nutriologo: NutriologoEnMapa, cache: File) {
        _estado.update { it.copy(seleccionado = nutriologo, foto = null) }
        viewModelScope.launch {
            try {
                val foto = fotos.avatarAjeno(cache, nutriologo.id)
                _estado.update { if (it.seleccionado?.id == nutriologo.id) it.copy(foto = foto) else it }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun cerrarFicha() = _estado.update { it.copy(seleccionado = null, foto = null) }

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val nutriologos = repo.nutriologos()
                _estado.update { it.copy(cargando = false, nutriologos = nutriologos) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }
}
