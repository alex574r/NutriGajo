package com.example.nutrigajo.ui.paciente.citas

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.CitasRepository
import com.example.nutrigajo.domain.Agenda
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
    // nutriólogo al que se le agenda; null con la hoja cerrada
    val nutriologo: NutriologoEnMapa? = null,
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
class AgendaViewModel(private val repo: CitasRepository = CitasRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(AgendaEstado())
    val estado: StateFlow<AgendaEstado> = _estado.asStateFlow()

    fun abrir(nutriologo: NutriologoEnMapa) {
        _estado.value = AgendaEstado(nutriologo = nutriologo, cargando = true)
        cargarHoras(nutriologo)
    }

    fun elegirDia(dia: LocalDate) = _estado.update { it.copy(dia = dia, hora = null, error = null) }

    fun elegirHora(inicio: Long) = _estado.update { it.copy(hora = inicio, error = null) }

    fun cerrar() {
        _estado.value = AgendaEstado()
    }

    fun enviar(paciente: Perfil) {
        val actual = _estado.value
        val nutriologo = actual.nutriologo ?: return
        val hora = actual.hora ?: return
        if (actual.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            try {
                repo.agendar(nutriologo, paciente, hora)
                _estado.update { it.copy(enviando = false, enviada = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(enviando = false) }
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    // las reglas rechazan el lote cuando otro paciente tomó la hora mientras esta persona elegía
                    cargarHoras(nutriologo, R.string.agendar_error_ocupada)
                } else {
                    _estado.update { it.copy(error = mensajeDeFirestore(e)) }
                }
            }
        }
    }

    private fun cargarHoras(nutriologo: NutriologoEnMapa, @StringRes error: Int? = null) {
        viewModelScope.launch {
            try {
                val ocupados = repo.horasOcupadas(nutriologo.id)
                val dias = Agenda.dias(ocupados, Instant.now(), ZoneId.systemDefault())
                // si la hoja ya se cerró o se abrió para otro nutriólogo, esta respuesta llegó tarde
                _estado.update {
                    if (it.nutriologo?.id != nutriologo.id) it
                    else it.copy(cargando = false, ocupados = ocupados, dias = dias, dia = it.dia?.takeIf { d -> d in dias } ?: dias.firstOrNull(), hora = null, error = error)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { if (it.nutriologo?.id != nutriologo.id) it else it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }
}
