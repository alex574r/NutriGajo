package com.example.nutrigajo.ui.paciente.rutina

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.EntrenamientosRepository
import com.example.nutrigajo.domain.model.Entrenamiento
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import com.example.nutrigajo.widgets.AlmacenWidget
import com.example.nutrigajo.widgets.actualizarWidgets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntrenamientosEstado(
    val cargando: Boolean = true,
    val items: List<Entrenamiento> = emptyList(),
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    @param:StringRes val error: Int? = null,
    @param:StringRes val errorGuardar: Int? = null,
)

// El historial de «Entrenar ahora». Cada entrenamiento que cuenta suma su día a la racha: se guarda en el teléfono (el mismo conjunto de
// días de las caminatas) para el inicio y los widgets, antes de esperar a la red.
// AndroidViewModel porque actualizar los widgets pide un Context.
class EntrenamientosViewModel @JvmOverloads constructor(
    app: Application,
    private val repo: EntrenamientosRepository = EntrenamientosRepository(),
) : AndroidViewModel(app) {
    private val _estado = MutableStateFlow(EntrenamientosEstado())
    val estado: StateFlow<EntrenamientosEstado> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val items = repo.listar()
                // lo que ya se guardó también cuenta para la racha (por ejemplo tras reinstalar la app)
                registrarDias(items)
                _estado.update { it.copy(cargando = false, items = items) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun guardar(entrenamiento: Entrenamiento) {
        val actual = _estado.value
        if (actual.guardando || actual.guardado) return
        _estado.update { it.copy(guardando = true, errorGuardar = null) }
        // la racha no espera a la red: el día queda anotado aunque no haya conexión
        registrarDias(listOf(entrenamiento))
        viewModelScope.launch {
            try {
                repo.guardar(entrenamiento)
                _estado.update { it.copy(guardando = false, guardado = true, items = listOf(entrenamiento) + it.items) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(guardando = false, errorGuardar = R.string.entrenar_error_guardar) }
            }
        }
    }

    // Al empezar otra rutina el aviso de «guardado» de la anterior ya no aplica.
    fun reiniciarGuardado() = _estado.update { it.copy(guardando = false, guardado = false, errorGuardar = null) }

    private fun registrarDias(lista: List<Entrenamiento>) {
        val dias = lista.filter { it.cuentaParaLaRacha }.map { it.dia() }
        if (dias.isEmpty()) return
        val app = getApplication<Application>()
        AlmacenWidget.agregarDiasCarrera(app, dias)
        actualizarWidgets(app)
    }
}
