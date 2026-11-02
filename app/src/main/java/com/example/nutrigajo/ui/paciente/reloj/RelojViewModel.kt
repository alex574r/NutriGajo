package com.example.nutrigajo.ui.paciente.reloj

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.salud.Disponibilidad
import com.example.nutrigajo.data.salud.RelojRepository
import com.example.nutrigajo.domain.model.PeriodoPulso
import com.example.nutrigajo.domain.model.PulsoPeriodo
import com.example.nutrigajo.domain.model.ResumenReloj
import com.example.nutrigajo.widgets.AlmacenWidget
import com.example.nutrigajo.widgets.DatosWidget
import com.example.nutrigajo.widgets.actualizarWidgets
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// En qué punto está la conexión con el reloj: de «falta instalar Health Connect» a «ya llegan datos».
enum class FaseReloj { CARGANDO, NO_DISPONIBLE, INSTALAR, PERMISO, CONECTADO, ERROR }

data class RelojEstado(
    val fase: FaseReloj = FaseReloj.CARGANDO,
    val periodo: PeriodoPulso = PeriodoPulso.HOY,
    val resumen: ResumenReloj? = null,
    // null mientras llega (al abrir y al cambiar de periodo)
    val pulso: PulsoPeriodo? = null,
)

// AndroidViewModel porque Health Connect pide un Context.
// ponytail: repositorio por defecto en el constructor, como el resto de los ViewModels.
class RelojViewModel @JvmOverloads constructor(
    app: Application,
    private val repo: RelojRepository = RelojRepository(app),
) : AndroidViewModel(app) {
    private val _estado = MutableStateFlow(RelojEstado())
    val estado: StateFlow<RelojEstado> = _estado.asStateFlow()

    val permisos get() = repo.permisos

    // Se llama al abrir y cada vez que se vuelve a la pantalla (de instalar Health Connect o de darle el permiso): lo que ya se veía
    // se queda mientras se vuelve a leer.
    private var carga: Job? = null

    fun cargar() {
        // el inicio y la pantalla del reloj la piden a la vez al volver: la segunda reemplaza a la primera
        carga?.cancel()
        carga = viewModelScope.launch {
            val fase = try {
                when (repo.disponibilidad()) {
                    Disponibilidad.NO_DISPONIBLE -> FaseReloj.NO_DISPONIBLE
                    Disponibilidad.INSTALAR -> FaseReloj.INSTALAR
                    Disponibilidad.DISPONIBLE -> if (repo.conPermiso()) FaseReloj.CONECTADO else FaseReloj.PERMISO
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                FaseReloj.ERROR
            }
            if (fase != FaseReloj.CONECTADO) {
                _estado.update { it.copy(fase = fase) }
                return@launch
            }
            try {
                val resumen = repo.resumen()
                val periodo = _estado.value.periodo
                val pulso = repo.pulso(periodo)
                // los widgets enseñan el rango de hoy, sea cual sea el periodo que se está viendo
                publicarWidgets(resumen, if (periodo == PeriodoPulso.HOY) pulso else repo.pulso(PeriodoPulso.HOY))
                _estado.update { it.copy(fase = FaseReloj.CONECTADO, resumen = resumen, pulso = pulso) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _estado.update { it.copy(fase = FaseReloj.ERROR) }
            }
        }
    }

    // Guarda lo leído para los widgets de la pantalla de inicio y los dibuja otra vez.
    private fun publicarWidgets(resumen: ResumenReloj, hoy: PulsoPeriodo) {
        val app = getApplication<Application>()
        AlmacenWidget.guardar(
            app,
            DatosWidget(
                dia = LocalDate.now().toEpochDay(),
                ultimoBpm = resumen.ultimoBpm,
                ultimaLectura = resumen.ultimaLectura?.toEpochMilli(),
                bpmMin = hoy.minimo,
                bpmMax = hoy.maximo,
                pasos = resumen.actividad?.pasos,
                kcal = resumen.actividad?.kcal,
                minutos = resumen.actividad?.minutos,
                sueno = resumen.sueno.associate { it.fecha.toEpochDay() to it.minutos },
                pasosPorDia = resumen.pasosPorDia,
            ),
        )
        actualizarWidgets(app)
    }

    fun elegirPeriodo(periodo: PeriodoPulso) {
        if (periodo == _estado.value.periodo) return
        _estado.update { it.copy(periodo = periodo, pulso = null) }
        viewModelScope.launch {
            try {
                val pulso = repo.pulso(periodo)
                // si mientras tanto se eligió otro periodo, esta lectura ya no sirve
                _estado.update { if (it.periodo == periodo) it.copy(pulso = pulso) else it }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _estado.update { it.copy(fase = FaseReloj.ERROR) }
            }
        }
    }
}
