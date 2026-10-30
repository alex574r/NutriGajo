package com.example.nutrigajo.ui.nutriologo.pacientes

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.repository.FotoPerfilRepository
import com.example.nutrigajo.data.repository.PlanesRepository
import com.example.nutrigajo.data.repository.VinculacionRepository
import com.example.nutrigajo.domain.model.CodigoVinculacion
import com.example.nutrigajo.domain.model.PacienteResumen
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class PacientesEstado(
    val cargando: Boolean = true,
    val generando: Boolean = false,
    val codigo: CodigoVinculacion? = null,
    val pacientes: List<PacienteResumen> = emptyList(),
    @param:StringRes val error: Int? = null,
)

// ponytail: repositorios por defecto en el constructor; pasan a inyección (Hilt) cuando haya más ViewModels.
class PacientesViewModel(
    private val repo: VinculacionRepository = VinculacionRepository(),
    private val fotos: FotoPerfilRepository = FotoPerfilRepository(),
    private val planes: PlanesRepository = PlanesRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(PacientesEstado())
    val estado: StateFlow<PacientesEstado> = _estado.asStateFlow()

    // foto de perfil de cada paciente que tiene una, por el id de su cuenta
    private val _fotos = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val fotosPacientes: StateFlow<Map<String, Bitmap>> = _fotos.asStateFlow()

    // ids de los pacientes que ya recibieron un plan: su expediente ofrece editarlo y reenviarlo en vez de crearlo
    private val _conPlan = MutableStateFlow<Set<String>>(emptySet())
    val pacientesConPlan: StateFlow<Set<String>> = _conPlan.asStateFlow()

    // Mira si ese paciente ya tiene un plan enviado; si la consulta falla el botón se queda como "crear plan".
    fun cargarPlan(pacienteId: String) {
        viewModelScope.launch {
            try {
                val hay = planes.ultimo(pacienteId) != null
                _conPlan.update { if (hay) it + pacienteId else it - pacienteId }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    // Pide las fotos de los pacientes de la lista, de a una para no saturar la conexión; las que fallan se quedan con la inicial.
    fun cargarFotos(cache: File, pacientes: List<PacienteResumen>) {
        viewModelScope.launch {
            for (p in pacientes) {
                if (p.usuarioId.isEmpty()) continue
                try {
                    val foto = fotos.avatarAjeno(cache, p.usuarioId)
                    _fotos.update { if (foto != null) it + (p.usuarioId to foto) else it - p.usuarioId }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
        }
    }

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val codigo = repo.codigoVigente()
                val pacientes = repo.pacientes()
                _estado.update { it.copy(cargando = false, codigo = codigo, pacientes = pacientes) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun generarCodigo(nutriologoNombre: String) {
        if (_estado.value.generando) return
        _estado.update { it.copy(generando = true, error = null) }
        viewModelScope.launch {
            try {
                val codigo = repo.generarCodigo(nutriologoNombre)
                _estado.update { it.copy(generando = false, codigo = codigo) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(generando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }
}
