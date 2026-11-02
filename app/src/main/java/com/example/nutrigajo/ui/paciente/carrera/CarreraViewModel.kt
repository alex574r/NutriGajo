package com.example.nutrigajo.ui.paciente.carrera

import android.app.Application
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.carrera.RastreoRuta
import com.example.nutrigajo.data.carrera.ServicioCarrera
import com.example.nutrigajo.data.repository.CarrerasRepository
import com.example.nutrigajo.data.repository.PlanElegido
import com.example.nutrigajo.domain.model.Carrera
import com.example.nutrigajo.domain.model.PlanCaminata
import com.example.nutrigajo.domain.model.Ruta
import com.example.nutrigajo.domain.model.TipoActividad
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Lo guardado en Firestore (historial y plan); lo que va pasando en la carrera vive en RastreoRuta.global.
data class CarreraDatos(
    val cargando: Boolean = true,
    val carreras: List<Carrera> = emptyList(),
    val plan: PlanElegido? = null,
    @param:StringRes val error: Int? = null,
    val guardando: Boolean = false,
)

// Lo que pasa entre tocar «comenzar» y salir: se ve el mapa y, cuando está listo, corre la cuenta de 5 a 1 (`cuenta` es null antes).
data class Preparacion(val tipo: TipoActividad, val cuenta: Int? = null)

private const val SEGUNDOS_CUENTA = 5

// AndroidViewModel porque arrancar y detener el servicio del GPS pide un Context.
// ponytail: repositorio por defecto en el constructor, como el resto de los ViewModels.
class CarreraViewModel @JvmOverloads constructor(
    app: Application,
    private val repo: CarrerasRepository = CarrerasRepository(),
    private val rastreo: RastreoRuta = RastreoRuta.global,
) : AndroidViewModel(app) {
    private val _datos = MutableStateFlow(CarreraDatos())
    val datos: StateFlow<CarreraDatos> = _datos.asStateFlow()

    fun cargar() {
        _datos.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val carreras = repo.listar()
                val plan = repo.plan()
                _datos.update { it.copy(cargando = false, carreras = carreras, plan = plan) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _datos.update { it.copy(cargando = false, error = mensajeDeFirestore(e)) }
            }
        }
    }

    fun limpiarError() = _datos.update { it.copy(error = null) }

    private val _preparacion = MutableStateFlow<Preparacion?>(null)
    val preparacion: StateFlow<Preparacion?> = _preparacion.asStateFlow()
    private var cuenta: Job? = null

    // «Comenzar»: abre la preparación (el mapa). Quien llama ya comprobó el permiso de ubicación precisa (el servicio en primer plano lo exige).
    fun preparar(tipo: TipoActividad) {
        limpiarError()
        _preparacion.value = Preparacion(tipo)
    }

    // El mapa y el GPS ya están listos: 5, 4, 3, 2, 1 con una vibración por segundo y, al final, sale la carrera.
    // Llamarla otra vez con la cuenta en marcha no la reinicia.
    fun contar() {
        val tipo = _preparacion.value?.tipo ?: return
        if (cuenta?.isActive == true) return
        cuenta = viewModelScope.launch {
            for (n in SEGUNDOS_CUENTA downTo 1) {
                _preparacion.update { it?.copy(cuenta = n) }
                vibrar(120)
                delay(1000)
            }
            vibrar(500)
            _preparacion.value = null
            comenzar(tipo)
        }
    }

    fun cancelar() {
        cuenta?.cancel()
        _preparacion.value = null
    }

    private fun comenzar(tipo: TipoActividad) {
        rastreo.iniciar(tipo, System.currentTimeMillis())
        arrancarServicio()
    }

    private fun vibrar(milisegundos: Long) {
        runCatching {
            getApplication<Application>().getSystemService(Vibrator::class.java)
                ?.vibrate(VibrationEffect.createOneShot(milisegundos, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun pausar() {
        rastreo.pausar()
        ServicioCarrera.detenerServicio(getApplication())
    }

    fun reanudar() {
        rastreo.reanudar()
        arrancarServicio()
    }

    fun terminar() {
        rastreo.terminar()
        ServicioCarrera.detenerServicio(getApplication())
    }

    fun descartar() {
        rastreo.reiniciar()
        ServicioCarrera.detenerServicio(getApplication())
    }

    // Guarda la actividad terminada y vuelve al inicio; si falla, se queda en el resumen para intentarlo otra vez.
    fun guardar(pesoKg: Double) {
        val r = rastreo.estado.value
        val segundos = (r.duracionMs(0) / 1000).toInt()
        val carrera = Carrera(
            tipo = r.tipo,
            inicio = r.inicio,
            duracionSeg = segundos,
            distanciaM = r.distanciaM.roundToInt(),
            kcal = Ruta.kcal(r.tipo, pesoKg, segundos, r.distanciaM),
            ruta = r.puntos,
        )
        _datos.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            try {
                repo.guardar(carrera)
                rastreo.reiniciar()
                _datos.update { it.copy(guardando = false) }
                cargar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _datos.update { it.copy(guardando = false, error = R.string.carrera_error_guardar) }
            }
        }
    }

    fun eliminar(id: String, alTerminar: () -> Unit) = escribir(alTerminar = alTerminar) { repo.eliminar(id) }

    fun elegirPlan(plan: PlanCaminata) = escribir { repo.elegirPlan(plan, System.currentTimeMillis()) }

    fun quitarPlan() = escribir { repo.quitarPlan() }

    // Una escritura suelta (borrar, elegir o quitar plan): al terminar se vuelve a leer todo.
    private fun escribir(alTerminar: () -> Unit = {}, accion: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                accion()
                alTerminar()
                cargar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _datos.update { it.copy(error = mensajeDeFirestore(e)) }
            }
        }
    }

    private fun arrancarServicio() {
        try {
            ServicioCarrera.iniciarServicio(getApplication())
        } catch (e: Exception) {
            // sin servicio no hay GPS: si apenas empezaba se cancela; si ya llevaba ruta, se deja en pausa para no perderla
            if (rastreo.estado.value.puntos.isEmpty()) rastreo.reiniciar() else rastreo.pausar()
            _datos.update { it.copy(error = R.string.carrera_error_servicio) }
        }
    }
}
