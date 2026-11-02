package com.example.nutrigajo.ui.paciente.reloj

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.salud.Disponibilidad
import com.example.nutrigajo.data.salud.RelojRepository
import com.example.nutrigajo.domain.model.BarraPulso
import com.example.nutrigajo.domain.model.PeriodoPulso
import com.example.nutrigajo.domain.model.ResumenReloj
import kotlinx.coroutines.CancellationException
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
    val barras: List<BarraPulso> = emptyList(),
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
    fun cargar() {
        viewModelScope.launch {
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
                val barras = repo.barras(_estado.value.periodo)
                _estado.update { it.copy(fase = FaseReloj.CONECTADO, resumen = resumen, barras = barras) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _estado.update { it.copy(fase = FaseReloj.ERROR) }
            }
        }
    }

    fun elegirPeriodo(periodo: PeriodoPulso) {
        if (periodo == _estado.value.periodo) return
        _estado.update { it.copy(periodo = periodo, barras = emptyList()) }
        viewModelScope.launch {
            try {
                val barras = repo.barras(periodo)
                // si mientras tanto se eligió otro periodo, esta lectura ya no sirve
                _estado.update { if (it.periodo == periodo) it.copy(barras = barras) else it }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _estado.update { it.copy(fase = FaseReloj.ERROR) }
            }
        }
    }
}
