package com.example.nutrigajo.ui.nutriologo.citas

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.AvisoCitaRepository
import com.example.nutrigajo.data.repository.CitasRepository
import com.example.nutrigajo.domain.Agenda
import com.example.nutrigajo.domain.model.Cita
import com.example.nutrigajo.domain.model.NOTA_MAX
import com.example.nutrigajo.domain.model.PacienteResumen
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
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

// Qué decir cuando las reglas rechazan el lote: «ocupada» solo si, al volver a leer las horas, esa hora ya está tomada; si sigue libre el
// rechazo es por otra cosa y decir «ocupada» dejaría al nutriólogo sin salida.
internal fun errorAlRechazarse(hora: Long, ocupados: Set<Long>, @StringRes generico: Int): Int =
    if (hora in ocupados) R.string.nueva_cita_error_ocupada else generico

data class NuevaCitaEstado(
    // la hoja está abierta
    val abierta: Boolean = false,
    // a quién se le agenda; null: la hoja le pide al nutriólogo que elija uno de sus pacientes
    val paciente: PacienteResumen? = null,
    val cargando: Boolean = false,
    val ocupados: Set<Long> = emptySet(),
    val dias: List<LocalDate> = emptyList(),
    val dia: LocalDate? = null,
    // inicio de la hora elegida, en milisegundos
    val hora: Long? = null,
    val nota: String = "",
    // la cita vigente que ese paciente ya tiene con este nutriólogo: se le dice, pero puede agendar otra
    val yaTiene: Cita? = null,
    val enviando: Boolean = false,
    val agendada: Boolean = false,
    // la cita quedó, pero el aviso al paciente no salió (sin dispositivo, o el Worker falló): el paciente la verá en su app
    val sinAviso: Boolean = false,
    @param:StringRes val error: Int? = null,
)

// El nutriólogo agenda a un paciente suyo: elige día y hora libres de su agenda y deja una nota si quiere. La cita nace confirmada y al
// paciente se le avisa por notificación; el aviso va aparte para no hacer esperar al nutriólogo y, si falla, la cita queda igual.
class NuevaCitaViewModel(
    private val repo: CitasRepository = CitasRepository(),
    private val avisos: AvisoCitaRepository = AvisoCitaRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(NuevaCitaEstado())
    val estado: StateFlow<NuevaCitaEstado> = _estado.asStateFlow()

    private var nutriologo: Perfil? = null

    // sube al abrir y al cerrar la hoja: lo que llega de una hoja que ya no es esta (porque se cerró y se abrió otra) se descarta
    private var sesion = 0

    // `paciente` null abre la hoja para elegirlo (desde la agenda); con uno (desde su expediente) ya viene elegido.
    fun abrir(nutriologo: Perfil, paciente: PacienteResumen?) {
        this.nutriologo = nutriologo
        sesion++
        _estado.value = NuevaCitaEstado(abierta = true, paciente = paciente, cargando = true)
        cargarHoras(nutriologo.uid)
        paciente?.let(::buscarCitaVigente)
    }

    fun elegirPaciente(paciente: PacienteResumen) {
        _estado.update { it.copy(paciente = paciente, yaTiene = null, error = null) }
        buscarCitaVigente(paciente)
    }

    fun elegirDia(dia: LocalDate) = _estado.update { it.copy(dia = dia, hora = null, error = null) }

    fun elegirHora(inicio: Long) = _estado.update { it.copy(hora = inicio, error = null) }

    fun escribirNota(texto: String) = _estado.update { it.copy(nota = texto.take(NOTA_MAX)) }

    fun cerrar() {
        sesion++
        _estado.value = NuevaCitaEstado()
    }

    fun agendar() {
        val actual = _estado.value
        val nutriologo = nutriologo ?: return
        val paciente = actual.paciente ?: return
        val hora = actual.hora ?: return
        if (actual.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        val mia = sesion
        viewModelScope.launch {
            val citaId = try {
                repo.agendarComoNutriologo(nutriologo, paciente, hora, actual.nota)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (sesion != mia) return@launch
                _estado.update { it.copy(enviando = false) }
                // las reglas rechazan el lote casi siempre porque otro tomó esa hora mientras se elegía: se vuelven a leer las horas y,
                // si la hora ya está ocupada, se dice; si no, el rechazo es otro (el nombre del consultorio, el paciente…) y se muestra su error
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    cargarHoras(nutriologo.uid) { ocupados -> errorAlRechazarse(hora, ocupados, mensajeDeFirestore(e)) }
                } else {
                    _estado.update { it.copy(error = mensajeDeFirestore(e)) }
                }
                return@launch
            }
            if (sesion != mia) return@launch
            _estado.update { it.copy(enviando = false, agendada = true) }
            val salio = try {
                avisos.avisar(citaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("NuevaCitaViewModel", "No se pudo avisar al paciente de la cita $citaId", e)
                false
            }
            if (!salio && sesion == mia) _estado.update { it.copy(sinAviso = true) }
        }
    }

    // `error` dice qué mostrar según las horas ocupadas que se acaban de leer (null: nada)
    private fun cargarHoras(nutriologoId: String, error: (Set<Long>) -> Int? = { null }) {
        viewModelScope.launch {
            try {
                val ocupados = repo.horasOcupadas(nutriologoId)
                val dias = Agenda.dias(ocupados, Instant.now(), ZoneId.systemDefault(), Agenda.DIAS_NUTRIOLOGO)
                // si la hoja ya se cerró, esta respuesta llegó tarde
                _estado.update {
                    if (!it.abierta) it
                    else it.copy(cargando = false, ocupados = ocupados, dias = dias, dia = it.dia?.takeIf { d -> d in dias } ?: dias.firstOrNull(), hora = null, error = error(ocupados))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { if (!it.abierta) it else it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    private fun buscarCitaVigente(paciente: PacienteResumen) {
        viewModelScope.launch {
            // si no se pudo ver, no pasa nada: es solo un aviso en la hoja
            val vigente = try {
                val ahora = System.currentTimeMillis()
                repo.citas(Rol.NUTRIOLOGO).firstOrNull { it.usuarioId == paciente.usuarioId && it.vigente(ahora) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            _estado.update { if (it.paciente?.id != paciente.id) it else it.copy(yaTiene = vigente) }
        }
    }
}
