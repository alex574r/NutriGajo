package com.example.nutrigajo.data.carrera

import android.os.SystemClock
import com.example.nutrigajo.domain.model.PuntoRuta
import com.example.nutrigajo.domain.model.Ruta
import com.example.nutrigajo.domain.model.TipoActividad
import com.example.nutrigajo.domain.model.Tramo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class Fase { INICIO, CORRIENDO, PAUSADA, RESUMEN }

data class RastreoEstado(
    val fase: Fase = Fase.INICIO,
    val tipo: TipoActividad = TipoActividad.CAMINAR,
    // milisegundos desde 1970 en que se empezó
    val inicio: Long = 0,
    val puntos: List<PuntoRuta> = emptyList(),
    val distanciaM: Double = 0.0,
    // el tiempo de los tramos ya cerrados, sin contar las pausas
    val acumuladoMs: Long = 0,
    // el reloj (SystemClock) cuando empezó el tramo en curso; solo vale mientras se corre
    val reanudadoEn: Long = 0,
    // los tramos de una sesión con intervalos, en orden; vacío en una carrera o caminata libre
    val intervalos: List<Tramo> = emptyList(),
) {
    fun duracionMs(reloj: Long): Long = acumuladoMs + if (fase == Fase.CORRIENDO) reloj - reanudadoEn else 0
}

// La carrera en curso: qué fase va, los puntos del GPS, la distancia y el tiempo sin pausas. Vive fuera de la pantalla (un objeto
// por proceso) porque el servicio es quien recibe los puntos y la pantalla solo los muestra; así seguir corriendo con la pantalla
// apagada o girar el teléfono no pierde nada. Todo se llama desde el hilo principal. `reloj` es monótono (SystemClock.elapsedRealtime).
class RastreoRuta(private val reloj: () -> Long) {
    private val _estado = MutableStateFlow(RastreoEstado())
    val estado: StateFlow<RastreoEstado> = _estado.asStateFlow()

    // el último punto aceptado y el reloj de su fix; null al empezar y al reanudar, para no unir con una línea recta lo que se
    // avanzó con la pausa puesta
    private var ultimo: PuntoRuta? = null
    private var ultimoMs = 0L

    fun iniciar(tipo: TipoActividad, ahoraEpoch: Long, intervalos: List<Tramo> = emptyList()) {
        ultimo = null
        _estado.value = RastreoEstado(Fase.CORRIENDO, tipo, ahoraEpoch, reanudadoEn = reloj(), intervalos = intervalos)
    }

    fun pausar() {
        _estado.update { if (it.fase == Fase.CORRIENDO) it.copy(fase = Fase.PAUSADA, acumuladoMs = it.duracionMs(reloj()), reanudadoEn = 0) else it }
    }

    fun reanudar() {
        ultimo = null
        _estado.update { if (it.fase == Fase.PAUSADA) it.copy(fase = Fase.CORRIENDO, reanudadoEn = reloj()) else it }
    }

    fun terminar() {
        _estado.update {
            if (it.fase == Fase.CORRIENDO || it.fase == Fase.PAUSADA) it.copy(fase = Fase.RESUMEN, acumuladoMs = it.duracionMs(reloj()), reanudadoEn = 0) else it
        }
    }

    // Descartar la carrera o, ya guardada, volver al inicio.
    fun reiniciar() {
        ultimo = null
        _estado.value = RastreoEstado()
    }

    // Un fix del GPS. `tiempoMs` es el reloj del propio fix (Location.elapsedRealtimeNanos), `precisionM` su error y `velocidadMs`
    // su velocidad, si los trae. El primer punto siempre entra, como salida; después, uno que el GPS dice estar quieto no cuenta.
    fun ubicar(punto: PuntoRuta, tiempoMs: Long, precisionM: Float?, velocidadMs: Float? = null) {
        if (_estado.value.fase != Fase.CORRIENDO) return
        if (precisionM != null && precisionM > Ruta.PRECISION_MAX_M) return
        val anterior = ultimo
        if (anterior != null && velocidadMs != null && velocidadMs < Ruta.VELOCIDAD_MIN_MS) return
        val metros = if (anterior == null) 0.0 else Ruta.paso(anterior, ultimoMs, punto, tiempoMs, precisionM) ?: return
        ultimo = punto
        ultimoMs = tiempoMs
        _estado.update { it.copy(puntos = it.puntos + punto, distanciaM = it.distanciaM + metros) }
    }

    companion object {
        val global = RastreoRuta { SystemClock.elapsedRealtime() }
    }
}
