package com.example.nutrigajo.ui.paciente.inicio

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.AvisoVinculoRepository
import com.example.nutrigajo.data.repository.CitasRepository
import com.example.nutrigajo.data.repository.VinculacionRepository
import com.example.nutrigajo.data.repository.VinculoException
import com.example.nutrigajo.domain.model.CerradoPor
import com.example.nutrigajo.domain.model.Cita
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.domain.model.Vinculo
import com.example.nutrigajo.domain.model.citasVigentesEntre
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VinculoEstado(
    val cargando: Boolean = true,
    val canjeando: Boolean = false,
    // se está cerrando el vínculo, revisando el código de otro nutriólogo o cambiando de uno a otro
    val cerrando: Boolean = false,
    // el nutriólogo del código que se escribió para cambiar (ya validado), a la espera de que el paciente confirme
    val nuevoNutriologo: String? = null,
    val vinculo: Vinculo? = null,
    @param:StringRes val error: Int? = null,
)

// ponytail: repositorio por defecto en el constructor; pasa a inyección (Hilt) cuando haya más ViewModels.
class VinculoViewModel(
    private val repo: VinculacionRepository = VinculacionRepository(),
    private val citas: CitasRepository = CitasRepository(),
    private val aviso: AvisoVinculoRepository = AvisoVinculoRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(VinculoEstado())
    val estado: StateFlow<VinculoEstado> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val vinculo = repo.vinculoActual()
                _estado.update { it.copy(cargando = false, vinculo = vinculo) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // sin vínculo conocido se ofrece escribir el código; el error avisa por qué no se pudo consultar
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    // Vuelve a leer el vínculo sin tapar la pantalla con la carga: al volver del chat, para el punto de «sin leer». Si ya no hay vínculo activo
    // (el nutriólogo lo dio de baja), el inicio vuelve a ofrecer el código; si la consulta falla, se deja lo que había.
    fun refrescar() {
        viewModelScope.launch {
            try {
                val vinculo = repo.vinculoActual()
                _estado.update { it.copy(vinculo = vinculo) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun canjear(codigo: String, paciente: Perfil) {
        if (_estado.value.canjeando) return
        _estado.update { it.copy(canjeando = true, error = null) }
        viewModelScope.launch {
            try {
                val vinculo = repo.canjear(codigo, paciente)
                _estado.update { it.copy(canjeando = false, vinculo = vinculo) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(canjeando = false, error = mensajeDe(e)) }
            }
        }
    }

    // Cierra el vínculo del paciente con su nutriólogo (y las citas vigentes entre los dos) y le avisa. Tras cerrarlo no queda vínculo: el
    // inicio vuelve a ofrecer el código. Si algo falla, el vínculo sigue como estaba y sale el error.
    fun desvincular(paciente: Perfil) {
        val actual = _estado.value.vinculo ?: return
        if (_estado.value.cerrando) return
        _estado.update { it.copy(cerrando = true, error = null) }
        viewModelScope.launch {
            try {
                repo.cerrarVinculo(actual.pacienteId, CerradoPor.PACIENTE, vigentesCon(actual, paciente))
                avisar(actual.pacienteId)
                _estado.update { it.copy(cerrando = false, vinculo = null, nuevoNutriologo = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cerrando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    // Primer paso de cambiar de nutriólogo: comprueba el código y guarda el nombre del nuevo para pedir la confirmación. No toca nada.
    fun revisarCodigo(codigo: String) {
        if (_estado.value.cerrando) return
        _estado.update { it.copy(cerrando = true, error = null) }
        viewModelScope.launch {
            try {
                val nombre = repo.nutriologoDelCodigo(codigo)
                _estado.update { it.copy(cerrando = false, nuevoNutriologo = nombre) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cerrando = false, error = mensajeDe(e)) }
            }
        }
    }

    fun descartarRevision() = _estado.update { it.copy(nuevoNutriologo = null, error = null) }

    // Segundo paso: cambia de nutriólogo en un solo lote (ver VinculacionRepository.cambiarNutriologo) y avisa al anterior. Si el código ya
    // no sirve o el lote falla, el vínculo actual sigue como estaba.
    fun cambiar(codigo: String, compartirExpediente: Boolean, paciente: Perfil) {
        val anterior = _estado.value.vinculo ?: return
        if (_estado.value.cerrando) return
        _estado.update { it.copy(cerrando = true, error = null) }
        viewModelScope.launch {
            try {
                val nuevo = repo.cambiarNutriologo(codigo, paciente, anterior, compartirExpediente, vigentesCon(anterior, paciente))
                avisar(anterior.pacienteId)
                _estado.update { it.copy(cerrando = false, vinculo = nuevo, nuevoNutriologo = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cerrando = false, nuevoNutriologo = null, error = mensajeDe(e)) }
            }
        }
    }

    // Las citas vigentes del paciente con ese nutriólogo, que se cierran junto con el vínculo.
    private suspend fun vigentesCon(vinculo: Vinculo, paciente: Perfil): List<Cita> =
        citasVigentesEntre(citas.citas(Rol.PACIENTE), vinculo.nutriologoId, paciente.uid, System.currentTimeMillis())

    // El aviso a la otra persona es de cortesía: si no sale, el vínculo ya quedó cerrado y no se nota.
    private suspend fun avisar(pacienteId: String) {
        try {
            aviso.avisar(pacienteId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    fun limpiarError() = _estado.update { if (it.error == null) it else it.copy(error = null) }

    @StringRes
    private fun mensajeDe(e: Exception): Int = when {
        e is VinculoException -> when (e.motivo) {
            VinculoException.Motivo.INVALIDO -> R.string.error_codigo_invalido
            VinculoException.Motivo.USADO -> R.string.error_codigo_usado
            VinculoException.Motivo.VENCIDO -> R.string.error_codigo_vencido
        }
        // otro paciente lo canjeó justo antes: las reglas rechazan el lote
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_codigo_usado
        else -> mensajeDeFirestore(e)
    }
}
