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
    // pasos de la categoría que se edita sola, en el orden del asistente; null es el asistente completo
    val pasos: List<Int>? = null,
    @param:StringRes val error: Int? = null,
)

// El paso que sigue a `actual`: en el asistente completo el siguiente de ORDEN_PASOS (módulo tras módulo), en una categoría el
// siguiente de su lista. null si era el último.
internal fun siguientePaso(pasos: List<Int>?, actual: Int): Int? {
    val orden = pasos ?: ORDEN_PASOS
    return orden.getOrNull(orden.indexOf(actual) + 1)
}

// El que va antes de `actual` en el mismo orden; null si era el primero.
internal fun anteriorPaso(pasos: List<Int>?, actual: Int): Int? {
    val orden = pasos ?: ORDEN_PASOS
    return orden.getOrNull(orden.indexOf(actual) - 1)
}

// Dónde retoma el asistente completo: un expediente completo, desde el principio; uno a medias, donde se quedó (`paso` es el
// número del paso que tocaba). ponytail: un expediente que quedó a medias antes de que el asistente fuera por módulos puede retomar
// en un paso que ya no cae donde caía; todas las preguntas son opcionales, así que no se pierde nada obligatorio.
private fun pasoInicial(expediente: Expediente) =
    if (expediente.completo || expediente.paso !in ORDEN_PASOS) ORDEN_PASOS.first() else expediente.paso

// El asistente del expediente guarda cada paso, así que se puede dejar a medias y retomar.
// Sirve igual para el paciente que para su nutriólogo: cambia solo quién firma la edición.
// ponytail: repositorio por defecto en el constructor; pasa a inyección (Hilt) cuando haya más ViewModels.
class ExpedienteViewModel(private val repo: ExpedienteRepository = ExpedienteRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(ExpedienteEstado())
    val estado: StateFlow<ExpedienteEstado> = _estado.asStateFlow()

    fun cargar(pacienteId: String) {
        _estado.update { it.copy(cargando = true, error = null, listo = false, pasos = null) }
        viewModelScope.launch {
            try {
                val expediente = repo.leer(pacienteId)
                _estado.update { it.copy(cargando = false, expediente = expediente, paso = pasoInicial(expediente)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun cambiar(transformar: (Expediente) -> Expediente) = _estado.update { it.copy(expediente = transformar(it.expediente)) }

    // Abre el asistente solo con los pasos de una categoría, para editarla sin recorrer el resto del expediente.
    fun editarPasos(pasos: List<Int>) = _estado.update { it.copy(pasos = pasos, paso = pasos.first(), error = null) }

    // Deja la edición de una categoría sin terminarla: lo guardado paso a paso se queda y el asistente vuelve a su lugar.
    fun salirDePasos() = _estado.update { it.copy(pasos = null, paso = pasoInicial(it.expediente), error = null) }

    fun retroceder() = _estado.update { anteriorPaso(it.pasos, it.paso)?.let { anterior -> it.copy(paso = anterior, error = null) } ?: it }

    // Salta al primer paso de un módulo desde el asistente completo (las barras de módulos). Lo respondido hasta ahí en este paso
    // no se pierde: se guarda con el siguiente «continuar».
    fun irAPaso(paso: Int) = _estado.update { if (it.pasos == null && paso in ORDEN_PASOS) it.copy(paso = paso, error = null) else it }

    // Guarda lo respondido y pasa al siguiente paso. Omitir borra la respuesta de este paso antes de seguir.
    fun avanzar(pacienteId: String, editor: Rol, omitir: Boolean = false) {
        val actual = _estado.value
        if (actual.guardando) return
        val siguiente = siguientePaso(actual.pasos, actual.paso)
        val ultimo = siguiente == null
        val base = if (omitir) actual.expediente.sinPaso(actual.paso) else actual.expediente
        val aGuardar = when {
            // una categoría suelta no toca por dónde va el asistente ni si el expediente está completo
            actual.pasos != null -> base
            // un expediente ya completo sigue completo aunque se abandone la edición a medias
            else -> base.copy(paso = if (ultimo || base.completo) 0 else siguiente ?: 0, completo = ultimo || base.completo)
        }
        _estado.update { it.copy(guardando = true, error = null, expediente = base) }
        viewModelScope.launch {
            try {
                repo.guardar(pacienteId, aGuardar, editor)
                _estado.update {
                    it.copy(
                        guardando = false,
                        expediente = aGuardar.copy(actualizadoPor = editor, actualizadoEn = System.currentTimeMillis()),
                        // al terminar una categoría el paso se queda donde estaba hasta que el asistente se cierre
                        paso = siguiente ?: if (actual.pasos != null) it.paso else ORDEN_PASOS.first(),
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

    fun consumirListo() = _estado.update { it.copy(listo = false, pasos = null, paso = pasoInicial(it.expediente)) }

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
