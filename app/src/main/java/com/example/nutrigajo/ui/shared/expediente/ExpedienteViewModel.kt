package com.example.nutrigajo.ui.shared.expediente

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.repository.ExpedienteRepository
import com.example.nutrigajo.domain.model.Expediente
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val TOTAL_PASOS = 14

data class ExpedienteEstado(
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val expediente: Expediente = Expediente(),
    // paso visible del asistente, de 0 a TOTAL_PASOS - 1
    val paso: Int = 0,
    // se guardó el último paso: el asistente se cierra y lo consume
    val listo: Boolean = false,
    @param:StringRes val error: Int? = null,
)

// El asistente del expediente guarda cada paso, así que se puede dejar a medias y retomar.
// Sirve igual para el paciente que para su nutriólogo: cambia solo quién firma la edición.
// ponytail: repositorio por defecto en el constructor; pasa a inyección (Hilt) cuando haya más ViewModels.
class ExpedienteViewModel(private val repo: ExpedienteRepository = ExpedienteRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(ExpedienteEstado())
    val estado: StateFlow<ExpedienteEstado> = _estado.asStateFlow()

    fun cargar(pacienteId: String) {
        _estado.update { it.copy(cargando = true, error = null, listo = false) }
        viewModelScope.launch {
            try {
                val expediente = repo.leer(pacienteId)
                // incompleto: se retoma donde se quedó; completo: se edita desde el principio
                val paso = if (expediente.completo) 0 else expediente.paso.coerceIn(0, TOTAL_PASOS - 1)
                _estado.update { it.copy(cargando = false, expediente = expediente, paso = paso) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun cambiar(transformar: (Expediente) -> Expediente) = _estado.update { it.copy(expediente = transformar(it.expediente)) }

    fun retroceder() = _estado.update { if (it.paso > 0) it.copy(paso = it.paso - 1, error = null) else it }

    // Guarda lo respondido y pasa al siguiente paso. Omitir borra la respuesta de este paso antes de seguir.
    fun avanzar(pacienteId: String, editor: Rol, omitir: Boolean = false) {
        val actual = _estado.value
        if (actual.guardando) return
        val ultimo = actual.paso >= TOTAL_PASOS - 1
        val base = if (omitir) actual.expediente.sinPaso(actual.paso) else actual.expediente
        // un expediente ya completo sigue completo aunque se abandone la edición a medias
        val aGuardar = base.copy(
            paso = if (ultimo || base.completo) 0 else actual.paso + 1,
            completo = ultimo || base.completo,
        )
        _estado.update { it.copy(guardando = true, error = null, expediente = base) }
        viewModelScope.launch {
            try {
                repo.guardar(pacienteId, aGuardar, editor)
                _estado.update {
                    it.copy(
                        guardando = false,
                        expediente = aGuardar.copy(actualizadoPor = editor, actualizadoEn = System.currentTimeMillis()),
                        paso = if (ultimo) 0 else it.paso + 1,
                        listo = ultimo,
                    )
                }
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

// Quita las respuestas del paso indicado; el orden es el del asistente.
private fun Expediente.sinPaso(paso: Int): Expediente = when (paso) {
    0 -> copy(objetivo = null)
    1 -> copy(sexo = null)
    2 -> copy(fechaNacimiento = null)
    3 -> copy(pesoKg = null)
    4 -> copy(estaturaCm = null)
    5 -> copy(nivelActividad = null)
    6 -> copy(condiciones = emptyList())
    7 -> copy(alergias = emptyList())
    8 -> copy(dieta = null)
    9 -> copy(diasEjercicio = null)
    10 -> copy(ejercicios = emptyList())
    11 -> copy(suplementos = emptyList())
    12 -> copy(caloriasObjetivo = null)
    else -> copy(calidadSueno = null)
}
