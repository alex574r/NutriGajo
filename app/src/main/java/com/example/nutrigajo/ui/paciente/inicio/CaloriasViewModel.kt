package com.example.nutrigajo.ui.paciente.inicio

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.repository.CarrerasRepository
import com.example.nutrigajo.data.repository.EntrenamientosRepository
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CaloriasAppEstado(val cargando: Boolean = true, val actividades: List<ActividadFila> = emptyList(), @param:StringRes val error: Int? = null) {
    // lo que cuenta para las calorías: cada actividad en su momento
    val gastos: List<GastoApp> get() = actividades.map { GastoApp(it.instante, it.kcal) }
}

// Lo que la app misma sabe que se gastó: las caminatas y carreras del Modo carrera y las rutinas de «Entrenar ahora». Lo del reloj lo
// lee el RelojViewModel; el inicio junta las dos fuentes.
// ponytail: solo se leen las últimas LIMITE de cada una (las carreras traen su ruta entera, y leer cientos en cada apertura del inicio
// pesaría); y como el Modo carrera cuenta por MET (gasto bruto) y el reloj solo las calorías activas, la app puede quedar unas kcal arriba.
// Si hace falta más historia, guardar un total por día en un documento aparte.
class CaloriasViewModel(
    private val carreras: CarrerasRepository = CarrerasRepository(),
    private val entrenamientos: EntrenamientosRepository = EntrenamientosRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(CaloriasAppEstado())
    val estado: StateFlow<CaloriasAppEstado> = _estado.asStateFlow()

    private var carga: Job? = null
    private var cargadoEn = 0L

    // lo último que se leyó de cada fuente: si una falla, se queda lo que ya se tenía de ella
    private var deCarreras = emptyList<ActividadFila>()
    private var deRutinas = emptyList<ActividadFila>()

    // Al abrir el inicio solo se lee si lo que hay tiene más de VIGENCIA_MS; al volver del Modo carrera o de una rutina se fuerza, porque
    // acaba de guardarse algo.
    fun cargar(forzar: Boolean = false) {
        if (!forzar && System.currentTimeMillis() - cargadoEn < VIGENCIA_MS) return
        carga?.cancel()
        carga = viewModelScope.launch {
            // el esqueleto solo se ve la primera vez; después se queda lo anterior mientras se vuelve a leer
            val primera = cargadoEn == 0L
            if (primera) _estado.update { it.copy(cargando = true, error = null) }
            var fallo: Throwable? = null
            coroutineScope {
                val c = async { leer { carreras.listar(LIMITE) } }
                val r = async { leer { entrenamientos.listar(LIMITE) } }
                c.await().fold({ lista -> deCarreras = lista.map { it.aFila() } }, { fallo = it })
                // `fecha` es cuando se terminó la rutina: sus kcal caen en esa hora
                r.await().fold({ lista -> deRutinas = lista.map { it.aFila() } }, { fallo = it })
            }
            if (fallo == null) cargadoEn = System.currentTimeMillis()
            _estado.update { it.copy(cargando = false, actividades = deCarreras + deRutinas, error = fallo?.let { mensajeDeFirestore(it as Exception) }) }
        }
    }

    private suspend fun <T> leer(lectura: suspend () -> T): Result<T> =
        try {
            Result.success(lectura())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private companion object {
        const val LIMITE = 100L
        const val VIGENCIA_MS = 5 * 60_000L
    }
}
