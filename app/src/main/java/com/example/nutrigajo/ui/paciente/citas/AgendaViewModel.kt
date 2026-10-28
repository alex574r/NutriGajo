package com.example.nutrigajo.ui.paciente.citas

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.CitasRepository
import com.example.nutrigajo.domain.Agenda
import com.example.nutrigajo.domain.model.Cita
import com.example.nutrigajo.domain.model.EstadoCita
import com.example.nutrigajo.domain.model.NutriologoEnMapa
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class AgendaEstado(
    // a quién se le agenda; null con la hoja cerrada
    val nutriologoId: String? = null,
    val nutriologoNombre: String = "",
    // la cita que se pasa a otra hora; null cuando se pide una nueva
    val reagendando: Cita? = null,
    // la cita vigente que impide pedir otra (solo se puede tener una a la vez); null si no tiene
    val citaVigente: Cita? = null,
    val cancelando: Boolean = false,
    val cargando: Boolean = false,
    val ocupados: Set<Long> = emptySet(),
    val dias: List<LocalDate> = emptyList(),
    val dia: LocalDate? = null,
    // inicio de la hora elegida, en milisegundos
    val hora: Long? = null,
    val enviando: Boolean = false,
    val enviada: Boolean = false,
    @param:StringRes val error: Int? = null,
)

// El paciente elige día y hora de la agenda de un nutriólogo y manda la solicitud; el nutriólogo la confirma después.
// Solo puede tener una cita vigente: si ya la tiene, en vez de la agenda se le muestra esa cita para reagendarla o cancelarla.
// Reagendar usa la misma agenda con la cita ya existente: elige otra hora de ese mismo nutriólogo.
class AgendaViewModel(private val repo: CitasRepository = CitasRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(AgendaEstado())
    val estado: StateFlow<AgendaEstado> = _estado.asStateFlow()

    fun abrir(nutriologo: NutriologoEnMapa) {
        _estado.value = AgendaEstado(nutriologoId = nutriologo.id, nutriologoNombre = nutriologo.nombre, cargando = true)
        viewModelScope.launch {
            // si no se pudo ver si ya tiene una cita, se sigue: las reglas la rechazarían de todos modos
            val vigente = try {
                repo.citaVigente()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (vigente != null) {
                _estado.update { if (it.nutriologoId != nutriologo.id) it else it.copy(cargando = false, citaVigente = vigente) }
            } else {
                cargarHoras(nutriologo.id)
            }
        }
    }

    // Abre la agenda del nutriólogo de esa cita para pasarla a otra hora.
    fun reagendar(cita: Cita) {
        _estado.value = AgendaEstado(nutriologoId = cita.nutriologoId, nutriologoNombre = cita.nutriologoNombre, reagendando = cita, cargando = true)
        cargarHoras(cita.nutriologoId)
    }

    // Cancela la cita vigente que impedía pedir otra y deja ver la agenda.
    fun cancelarVigente() {
        val actual = _estado.value
        val vigente = actual.citaVigente ?: return
        val nutriologoId = actual.nutriologoId ?: return
        if (actual.cancelando) return
        _estado.update { it.copy(cancelando = true, error = null) }
        viewModelScope.launch {
            try {
                repo.cambiarEstado(vigente, EstadoCita.CANCELADA)
                _estado.update { it.copy(cancelando = false, citaVigente = null, cargando = true) }
                cargarHoras(nutriologoId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cancelando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun elegirDia(dia: LocalDate) = _estado.update { it.copy(dia = dia, hora = null, error = null) }

    fun elegirHora(inicio: Long) = _estado.update { it.copy(hora = inicio, error = null) }

    fun cerrar() {
        _estado.value = AgendaEstado()
    }

    fun enviar(paciente: Perfil) {
        val actual = _estado.value
        val nutriologoId = actual.nutriologoId ?: return
        val hora = actual.hora ?: return
        if (actual.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            try {
                val reagendando = actual.reagendando
                if (reagendando != null) repo.reagendar(reagendando, hora) else repo.agendar(nutriologoId, actual.nutriologoNombre, paciente, hora)
                _estado.update { it.copy(enviando = false, enviada = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(enviando = false) }
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    // las reglas rechazan el lote por dos motivos: otro paciente tomó la hora mientras esta persona elegía, o ya
                    // tiene una cita vigente (la pidió desde otro teléfono); se mira cuál
                    val vigente = if (actual.reagendando == null) {
                        try {
                            repo.citaVigente()
                        } catch (c: CancellationException) {
                            throw c
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                    if (vigente != null) {
                        _estado.update { it.copy(citaVigente = vigente) }
                    } else {
                        cargarHoras(nutriologoId, R.string.agendar_error_ocupada)
                    }
                } else {
                    _estado.update { it.copy(error = mensajeDeFirestore(e)) }
                }
            }
        }
    }

    private fun cargarHoras(nutriologoId: String, @StringRes error: Int? = null) {
        viewModelScope.launch {
            try {
                val ocupados = repo.horasOcupadas(nutriologoId)
                val dias = Agenda.dias(ocupados, Instant.now(), ZoneId.systemDefault())
                // si la hoja ya se cerró o se abrió para otro nutriólogo, esta respuesta llegó tarde
                _estado.update {
                    if (it.nutriologoId != nutriologoId) it
                    else it.copy(cargando = false, ocupados = ocupados, dias = dias, dia = it.dia?.takeIf { d -> d in dias } ?: dias.firstOrNull(), hora = null, error = error)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { if (it.nutriologoId != nutriologoId) it else it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }
}
