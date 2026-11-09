package com.example.nutrigajo.ui.actualizacion

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.BuildConfig
import com.example.nutrigajo.R
import com.example.nutrigajo.data.actualizacion.InstalacionReceiver
import com.example.nutrigajo.data.actualizacion.InstaladorApk
import com.example.nutrigajo.data.actualizacion.ResultadoInstalacion
import com.example.nutrigajo.data.repository.ActualizacionRepository
import com.example.nutrigajo.domain.Actualizacion
import com.example.nutrigajo.domain.EstadoActualizacion
import com.example.nutrigajo.domain.debeMostrar
import com.example.nutrigajo.domain.estadoDe
import com.example.nutrigajo.domain.instalacionSinNoticias
import com.example.nutrigajo.domain.jsonDe
import com.example.nutrigajo.domain.obligatoriaGuardada
import com.example.nutrigajo.domain.tocaConsultar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

// En qué va la actualización de la app. `obligatoria` viaja en cada fase porque decide qué pantalla se ve (la hoja o la pantalla completa).
sealed interface FaseActualizacion {
    data object Ociosa : FaseActualizacion
    data object Consultando : FaseActualizacion
    data object AlDia : FaseActualizacion
    data class Disponible(val actualizacion: Actualizacion, val obligatoria: Boolean) : FaseActualizacion
    data class Descargando(val actualizacion: Actualizacion, val bytes: Long, val obligatoria: Boolean) : FaseActualizacion
    // falta el permiso «Instalar apps desconocidas»: se abre su pantalla del sistema y se retoma al volver
    data class PermisoPendiente(val actualizacion: Actualizacion, val obligatoria: Boolean) : FaseActualizacion
    data class Instalando(val actualizacion: Actualizacion, val obligatoria: Boolean) : FaseActualizacion
    // `actualizacion` es null si falló la búsqueda y no hay nada que reintentar descargando
    data class Error(@param:StringRes val mensaje: Int, val actualizacion: Actualizacion?, val obligatoria: Boolean) : FaseActualizacion
}

val FaseActualizacion.actualizacion: Actualizacion?
    get() = when (this) {
        is FaseActualizacion.Disponible -> actualizacion
        is FaseActualizacion.Descargando -> actualizacion
        is FaseActualizacion.PermisoPendiente -> actualizacion
        is FaseActualizacion.Instalando -> actualizacion
        is FaseActualizacion.Error -> actualizacion
        else -> null
    }

val FaseActualizacion.obligatoria: Boolean
    get() = when (this) {
        is FaseActualizacion.Disponible -> obligatoria
        is FaseActualizacion.Descargando -> obligatoria
        is FaseActualizacion.PermisoPendiente -> obligatoria
        is FaseActualizacion.Instalando -> obligatoria
        is FaseActualizacion.Error -> obligatoria
        else -> false
    }

// Qué decir de una descarga que falló: un archivo que no coincide con lo publicado no se parece a una red caída ni a un teléfono sin espacio.
@StringRes
internal fun mensajeDeErrorDescarga(e: Throwable): Int {
    val texto = e.message.orEmpty()
    return when {
        texto.contains("no coincide", ignoreCase = true) || texto.contains("más grande de lo publicado", ignoreCase = true) -> R.string.actualiza_error_no_coincide
        texto.contains("ENOSPC") || texto.contains("No space left", ignoreCase = true) -> R.string.actualiza_error_espacio
        else -> R.string.actualiza_error_descarga
    }
}

@StringRes
internal fun mensajeDeResultado(resultado: ResultadoInstalacion): Int = when (resultado) {
    ResultadoInstalacion.FIRMA_DISTINTA -> R.string.actualiza_error_firma
    ResultadoInstalacion.CANCELADA -> R.string.actualiza_error_cancelada
    ResultadoInstalacion.SIN_ESPACIO -> R.string.actualiza_error_espacio
    ResultadoInstalacion.OTRO, ResultadoInstalacion.EXITO -> R.string.actualiza_error_instalar
}

// Busca, descarga e instala las actualizaciones de la app. Una sola instancia para toda la actividad: la hoja del inicio y la fila de «Acerca de»
// la comparten. La búsqueda automática (al entrar a la sesión) respeta las 12 horas y no dice nada si falla; la manual siempre contesta.
class ActualizacionViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ActualizacionRepository()
    private val prefs = app.getSharedPreferences("nutrigajo", Context.MODE_PRIVATE)
    private val carpeta = File(app.cacheDir, "actualizaciones")
    private var trabajo: Job? = null
    private var instalandoDesde = 0L

    // «abre la pantalla del permiso»: un evento que se consume una sola vez (si fuera un estado, girar el teléfono la abriría otra vez)
    private val _permiso = Channel<Unit>(Channel.BUFFERED)
    val pedirPermiso: Flow<Unit> = _permiso.receiveAsFlow()

    private val _fase = MutableStateFlow<FaseActualizacion>(FaseActualizacion.Ociosa)
    val fase: StateFlow<FaseActualizacion> = _fase.asStateFlow()

    // la hoja (o la pantalla obligatoria) está a la vista; «más tarde» la cierra sin perder la fase, así «Acerca de» sigue avisando
    private val _hoja = MutableStateFlow(false)
    val hoja: StateFlow<Boolean> = _hoja.asStateFlow()

    private val versionActual: Long
        get() = getApplication<Application>().let { it.packageManager.getPackageInfo(it.packageName, 0).longVersionCode }

    init {
        // lo que haya quedado de una descarga o instalación anterior no sirve
        carpeta.deleteRecursively()
        InstalacionReceiver.limpiar()
        viewModelScope.launch {
            InstalacionReceiver.resultado.collect { resultado ->
                val actual = _fase.value
                if (resultado != null && resultado != ResultadoInstalacion.EXITO && actual is FaseActualizacion.Instalando) {
                    _fase.value = FaseActualizacion.Error(mensajeDeResultado(resultado), actual.actualizacion, actual.obligatoria)
                }
            }
        }
    }

    fun verificar(manual: Boolean) {
        if (!BuildConfig.ACTUALIZACION_INTERNA) return
        when (val actual = _fase.value) {
            is FaseActualizacion.Consultando, is FaseActualizacion.Descargando, is FaseActualizacion.Instalando, is FaseActualizacion.PermisoPendiente -> return
            is FaseActualizacion.Error -> if (actual.actualizacion != null) return
            else -> Unit
        }
        // una obligatoria que ya se conocía se vuelve a exigir en cada apertura, pasen o no las 12 horas y haya o no red
        val guardada = obligatoriaGuardada(versionActual, prefs.getString(MANIFIESTO, null))
        if (guardada != null) {
            _fase.value = FaseActualizacion.Disponible(guardada, obligatoria = true)
            _hoja.value = true
        }
        val ahora = System.currentTimeMillis()
        if (!manual && guardada == null && !tocaConsultar(ahora, prefs.getLong(ULTIMA_CONSULTA, 0L))) return
        if (guardada == null) _fase.value = FaseActualizacion.Consultando
        trabajo = viewModelScope.launch {
            try {
                val publicada = repo.consultar()
                prefs.edit().putLong(ULTIMA_CONSULTA, ahora).putString(MANIFIESTO, publicada?.let(::jsonDe)).apply()
                val estado = publicada?.let { estadoDe(versionActual, it) }
                if (publicada == null || estado == EstadoActualizacion.AL_DIA) {
                    _fase.value = FaseActualizacion.AlDia
                    _hoja.value = false
                    return@launch
                }
                _fase.value = FaseActualizacion.Disponible(publicada, obligatoria = estado == EstadoActualizacion.OBLIGATORIA)
                _hoja.value = manual || debeMostrar(estado!!, publicada.versionCode, prefs.getLong(APLAZADA, 0L))
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                // sin red o con el Worker caído: la manual lo dice, la automática se queda callada (y una obligatoria guardada sigue a la vista)
                if (guardada == null) _fase.value = if (manual) FaseActualizacion.Error(R.string.actualiza_error_red, null, false) else FaseActualizacion.Ociosa
            }
        }
    }

    // Descarga el APK, lo comprueba y lo instala (pidiendo antes el permiso si falta).
    fun actualizar() {
        val actual = _fase.value
        val actualizacion = actual.actualizacion ?: return
        if (actual is FaseActualizacion.Descargando || actual is FaseActualizacion.Instalando || actual is FaseActualizacion.PermisoPendiente) return
        val obligatoria = actual.obligatoria
        _fase.value = FaseActualizacion.Descargando(actualizacion, 0, obligatoria)
        trabajo = viewModelScope.launch {
            try {
                repo.descargar(actualizacion, archivoDe(actualizacion)) { bytes -> _fase.value = FaseActualizacion.Descargando(actualizacion, bytes, obligatoria) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _fase.value = FaseActualizacion.Error(mensajeDeErrorDescarga(e), actualizacion, obligatoria)
                return@launch
            }
            if (InstaladorApk.puedeInstalar(getApplication())) {
                instalar(actualizacion, obligatoria)
            } else {
                _fase.value = FaseActualizacion.PermisoPendiente(actualizacion, obligatoria)
                _permiso.trySend(Unit)
            }
        }
    }

    // Al volver de la pantalla del permiso: si ya se dio, se sigue con la instalación del APK que ya está descargado.
    // Si empezó a instalarse y vuelve sin resultado (salió de la ventana de Android sin confirmar ni cancelar), se da por cancelada para no
    // dejar la hoja en «Instalando…» para siempre.
    fun retomar() {
        val actual = _fase.value
        if (actual is FaseActualizacion.PermisoPendiente && InstaladorApk.puedeInstalar(getApplication())) {
            instalar(actual.actualizacion, actual.obligatoria)
        } else if (actual is FaseActualizacion.Instalando && instalacionSinNoticias(instalandoDesde, SystemClock.elapsedRealtime())) {
            _fase.value = FaseActualizacion.Error(R.string.actualiza_error_cancelada, actual.actualizacion, actual.obligatoria)
        }
    }

    fun reintentar() {
        val actual = _fase.value as? FaseActualizacion.Error ?: return
        if (actual.actualizacion != null) actualizar() else verificar(manual = true)
    }

    // «Más tarde»: esa versión no vuelve a salir sola (otra nueva sí). Una descarga en curso se corta. La obligatoria no se pospone.
    fun masTarde() {
        val actual = _fase.value
        val actualizacion = actual.actualizacion ?: return
        if (actual.obligatoria) return
        trabajo?.cancel()
        prefs.edit().putLong(APLAZADA, actualizacion.versionCode).apply()
        _fase.value = FaseActualizacion.Disponible(actualizacion, obligatoria = false)
        _hoja.value = false
    }

    // Cierra la hoja tras un error o al terminar; la fase vuelve a «disponible» para que «Acerca de» siga avisando.
    fun cerrarHoja() {
        val actual = _fase.value
        if (actual is FaseActualizacion.Error) {
            _fase.value = actual.actualizacion?.let { FaseActualizacion.Disponible(it, actual.obligatoria) } ?: FaseActualizacion.Ociosa
        }
        _hoja.value = false
    }

    fun mostrarHoja() {
        _hoja.value = true
    }

    private fun archivoDe(actualizacion: Actualizacion) = File(carpeta, "nutrigajo-${actualizacion.versionCode}.apk")

    private fun instalar(actualizacion: Actualizacion, obligatoria: Boolean) {
        val archivo = archivoDe(actualizacion)
        if (!archivo.isFile) {
            _fase.value = FaseActualizacion.Error(R.string.actualiza_error_descarga, actualizacion, obligatoria)
            return
        }
        _fase.value = FaseActualizacion.Instalando(actualizacion, obligatoria)
        instalandoDesde = SystemClock.elapsedRealtime()
        InstalacionReceiver.limpiar()
        try {
            InstaladorApk.instalar(getApplication(), archivo)
        } catch (_: Exception) {
            _fase.value = FaseActualizacion.Error(R.string.actualiza_error_instalar, actualizacion, obligatoria)
        }
    }

    private companion object {
        const val ULTIMA_CONSULTA = "actualizacion_ultima_consulta"
        const val APLAZADA = "actualizacion_aplazada"
        const val MANIFIESTO = "actualizacion_manifiesto"
    }
}
