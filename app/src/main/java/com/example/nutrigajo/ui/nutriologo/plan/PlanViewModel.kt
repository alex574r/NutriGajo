package com.example.nutrigajo.ui.nutriologo.plan

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.pdf.DatosPlanPdf
import com.example.nutrigajo.data.pdf.GeneradorPlanPdf
import com.example.nutrigajo.data.repository.DirectorioRepository
import com.example.nutrigajo.data.repository.ExpedienteRepository
import com.example.nutrigajo.data.repository.PlanesRepository
import com.example.nutrigajo.domain.model.Expediente
import com.example.nutrigajo.domain.model.Objetivo
import com.example.nutrigajo.domain.model.PlanContenido
import com.example.nutrigajo.ui.shared.mensajeDeFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PlanEstado(
    val cargando: Boolean = true,
    val contenido: PlanContenido = PlanContenido(),
    // contacto del consultorio, para el cierre del PDF
    val telefono: String = "",
    val direccion: String = "",
    // cuándo se envió el plan anterior; null si es el primero
    val ultimoEnvio: Long? = null,
    val generando: Boolean = false,
    val enviando: Boolean = false,
    // PDF de la vista previa abierto
    val previa: File? = null,
    // el plan se envió; `avisado` dice si la notificación al paciente también salió
    val enviado: Boolean = false,
    val avisado: Boolean = false,
    @param:StringRes val error: Int? = null,
)

// El panel donde el nutriólogo arma el plan de alimentación y ejercicio de un paciente, lo ve como PDF y lo envía.
// Parte del último plan enviado; si no hay, de lo que dice el expediente. Lo editado vive aquí mientras se usa el panel.
// ponytail: sin borrador guardado; si se sale del panel sin enviar, se pierde lo editado.
class PlanViewModel(
    private val planes: PlanesRepository = PlanesRepository(),
    private val expedientes: ExpedienteRepository = ExpedienteRepository(),
    private val directorio: DirectorioRepository = DirectorioRepository(),
) : ViewModel() {
    private val _estado = MutableStateFlow(PlanEstado())
    val estado: StateFlow<PlanEstado> = _estado.asStateFlow()

    private var cargado: String? = null
    private var paciente = ""
    private var nutriologo = ""

    // `saludo` arma el mensaje de bienvenida a partir del objetivo del paciente (necesita los textos de la app).
    fun cargar(pacienteId: String, nombrePaciente: String, nombreNutriologo: String, saludo: (Objetivo?) -> String) {
        if (cargado == pacienteId) return
        cargado = pacienteId
        paciente = nombrePaciente
        nutriologo = nombreNutriologo
        viewModelScope.launch {
            // cada lectura es opcional: sin una, el panel abre igual y el nutriólogo llena lo que falte
            val expediente = leer { expedientes.leer(pacienteId) } ?: Expediente()
            val ultimo = leer { planes.ultimo(pacienteId) }
            val consultorio = leer { directorio.miUbicacion() }?.consultorio
            val inicial = ultimo?.contenido ?: PlanContenido(
                mensaje = saludo(expediente.objetivo),
                pesoKg = expediente.pesoKg,
                tallaCm = expediente.estaturaCm,
                calorias = expediente.caloriasObjetivo,
            )
            _estado.update {
                it.copy(
                    cargando = false,
                    contenido = inicial,
                    telefono = consultorio?.telefono.orEmpty(),
                    direccion = consultorio?.direccion.orEmpty(),
                    ultimoEnvio = ultimo?.enviadoEn,
                )
            }
        }
    }

    private suspend fun <T> leer(lectura: suspend () -> T): T? = try {
        lectura()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    fun editar(cambio: (PlanContenido) -> PlanContenido) = _estado.update { it.copy(contenido = cambio(it.contenido), error = null) }

    fun cerrarPrevia() = _estado.update { it.copy(previa = null) }

    fun consumirError() = _estado.update { it.copy(error = null) }

    // Arma el PDF con lo que hay en el panel y lo abre para revisarlo.
    fun verPrevia(generador: GeneradorPlanPdf, cache: File) {
        if (_estado.value.generando || _estado.value.enviando) return
        viewModelScope.launch {
            val pdf = generarPdf(generador, cache) ?: return@launch
            _estado.update { it.copy(generando = false, previa = pdf) }
        }
    }

    fun enviar(generador: GeneradorPlanPdf, cache: File, pacienteId: String) {
        if (_estado.value.generando || _estado.value.enviando) return
        viewModelScope.launch {
            val pdf = generarPdf(generador, cache) ?: return@launch
            _estado.update { it.copy(generando = false, enviando = true) }
            try {
                val avisado = planes.enviar(pacienteId, nutriologo, _estado.value.contenido.limpio(), pdf)
                _estado.update { it.copy(enviando = false, previa = null, enviado = true, avisado = avisado) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(enviando = false, error = if (e is IOException) R.string.plan_error_enviar else mensajeDeFirestore(e)) }
            }
        }
    }

    // null si no hay nada que mandar o el PDF no se pudo armar (el error queda en el estado).
    private suspend fun generarPdf(generador: GeneradorPlanPdf, cache: File): File? {
        val estado = _estado.value
        val contenido = estado.contenido.limpio()
        if (contenido.vacio) {
            _estado.update { it.copy(error = R.string.plan_error_vacio) }
            return null
        }
        _estado.update { it.copy(generando = true, error = null) }
        return try {
            withContext(Dispatchers.Default) {
                val archivo = File(cache, "plan-previa.pdf")
                val fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-MX")))
                generador.generar(DatosPlanPdf(paciente, nutriologo, fecha, contenido, estado.telefono, estado.direccion), archivo)
                archivo
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _estado.update { it.copy(generando = false, error = R.string.plan_error_pdf) }
            null
        }
    }
}
