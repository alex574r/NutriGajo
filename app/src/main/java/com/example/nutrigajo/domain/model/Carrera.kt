package com.example.nutrigajo.domain.model

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// No renombrar valores: Firestore guarda el nombre del enum como texto.
enum class TipoActividad { CAMINAR, CORRER }

data class PuntoRuta(val lat: Double, val lng: Double)

// Una carrera o caminata terminada y guardada. `ruta` son los puntos que registró el GPS, en orden.
data class Carrera(
    val id: String = "",
    val tipo: TipoActividad,
    // milisegundos desde 1970 en que se tocó «comenzar»
    val inicio: Long,
    val duracionSeg: Int,
    val distanciaM: Int,
    val kcal: Int,
    val ruta: List<PuntoRuta> = emptyList(),
) {
    val ritmoSegKm: Int? get() = Ruta.ritmoSegPorKm(distanciaM.toDouble(), duracionSeg)
}

// Las cuentas del modo carrera: lo que cuenta como un paso del GPS, la distancia, el ritmo y las calorías.
object Ruta {
    private const val RADIO_TIERRA_M = 6_371_000.0

    // se descarta el punto cuyo error de posición es mayor
    const val PRECISION_MAX_M = 30f

    // menos que esto es el temblor del GPS con el teléfono quieto, no un paso; con mala precisión el mínimo sube a la mitad del
    // error del punto (un fix de ±14 m "se mueve" varios metros sin que nadie camine)
    const val PASO_MIN_M = 3.0

    // el GPS mide la velocidad por efecto Doppler: por debajo de esto (1 km/h) está quieto, aunque la posición baile
    const val VELOCIDAD_MIN_MS = 0.3f

    // más rápido que esto (43 km/h) no es alguien a pie: es un salto del GPS
    const val VELOCIDAD_MAX_MS = 12.0

    // lo que cabe en un documento de Firestore sin acercarse a su tope (1 MiB)
    const val PUNTOS_GUARDADOS = 2000

    // Haversine: metros entre dos puntos sobre la esfera terrestre.
    fun distanciaM(a: PuntoRuta, b: PuntoRuta): Double {
        val fa = a.lat * PI / 180
        val fb = b.lat * PI / 180
        val df = (b.lat - a.lat) * PI / 180
        val dl = (b.lng - a.lng) * PI / 180
        val h = sin(df / 2) * sin(df / 2) + cos(fa) * cos(fb) * sin(dl / 2) * sin(dl / 2)
        return 2 * RADIO_TIERRA_M * atan2(sqrt(h), sqrt(1 - h))
    }

    // Los metros que suma un punto nuevo al anterior; null si no cuenta (temblor, salto o fuera de orden). Los tiempos son los
    // de cada fix en milisegundos y `precisionM` el error del punto nuevo, si lo trae.
    fun paso(anterior: PuntoRuta, anteriorMs: Long, nuevo: PuntoRuta, nuevoMs: Long, precisionM: Float? = null): Double? {
        val metros = distanciaM(anterior, nuevo)
        val segundos = (nuevoMs - anteriorMs) / 1000.0
        val minimo = maxOf(PASO_MIN_M, (precisionM ?: 0f) / 2.0)
        return metros.takeIf { segundos > 0 && it >= minimo && it / segundos <= VELOCIDAD_MAX_MS }
    }

    // Segundos por kilómetro; sin ritmo mientras se lleven menos de 50 m (saldría un número sin sentido).
    fun ritmoSegPorKm(distanciaM: Double, duracionSeg: Int): Int? =
        if (distanciaM < 50 || duracionSeg <= 0) null else (duracionSeg / (distanciaM / 1000)).roundToInt()

    // Calorías con el método de los MET: MET × peso × horas. Al correr el MET es casi la velocidad en km/h (con un mínimo de 6); al
    // caminar sube con ella, de 2.7 a 3 km/h a 3.9 a 6 km/h. Es una estimación, no una medición.
    fun kcal(tipo: TipoActividad, pesoKg: Double, duracionSeg: Int, distanciaM: Double): Int {
        if (duracionSeg <= 0) return 0
        val horas = duracionSeg / 3600.0
        val kmh = distanciaM / 1000 / horas
        val met = when (tipo) {
            TipoActividad.CORRER -> kmh.coerceAtLeast(6.0)
            TipoActividad.CAMINAR -> (1.5 + 0.4 * kmh).coerceIn(2.0, 6.0)
        }
        return (met * pesoKg * horas).roundToInt()
    }

    // A lo más `max` puntos (uno de cada tantos), siempre con el primero y el último.
    fun aligerar(ruta: List<PuntoRuta>, max: Int = PUNTOS_GUARDADOS): List<PuntoRuta> {
        if (ruta.size <= max) return ruta
        val cada = ceil(ruta.size / max.toDouble()).toInt()
        val tomados = ruta.filterIndexed { i, _ -> i % cada == 0 }
        return if (tomados.last() == ruta.last()) tomados else tomados + ruta.last()
    }
}

// ---- Planes de caminata

// Lo que pide una semana del plan: cuántas caminatas y de cuántos minutos cada una.
data class SemanaPlan(val sesiones: Int, val minutos: Int)

// Los planes que se ofrecen; el paciente elige uno y cada caminata que guarda suma a su semana. No renombrar valores: Firestore
// guarda el nombre del enum como texto.
enum class PlanCaminata(val semanas: List<SemanaPlan>) {
    PRIMEROS_PASOS(listOf(SemanaPlan(3, 20), SemanaPlan(3, 25), SemanaPlan(3, 30), SemanaPlan(4, 30))),
    RITMO_CONSTANTE(
        listOf(SemanaPlan(4, 30), SemanaPlan(4, 35), SemanaPlan(4, 40), SemanaPlan(5, 40), SemanaPlan(5, 45), SemanaPlan(5, 45)),
    ),
    RESISTENCIA(
        listOf(
            SemanaPlan(5, 40), SemanaPlan(5, 45), SemanaPlan(5, 50), SemanaPlan(5, 55),
            SemanaPlan(6, 55), SemanaPlan(6, 60), SemanaPlan(6, 60), SemanaPlan(6, 60),
        ),
    ),
}

const val DIA_MS = 86_400_000L

// Dónde va quien sigue un plan: la semana en curso (desde 0), cuántas caminatas ha hecho en ella y qué pide. Con `terminado`, ya
// pasaron todas las semanas y lo que se ve es la última.
data class ProgresoPlan(val semana: Int, val hechas: Int, val meta: SemanaPlan, val terminado: Boolean)

// Las semanas se cuentan desde el día en que se eligió el plan. Una caminata suma si dura al menos el 90 % de lo que pide la
// semana (el GPS y el paso no dan la hora exacta) y cae dentro de ella.
fun PlanCaminata.progreso(inicio: Long, carreras: List<Carrera>, ahora: Long): ProgresoPlan {
    val transcurridas = ((ahora - inicio) / (7 * DIA_MS)).toInt().coerceAtLeast(0)
    val semana = transcurridas.coerceAtMost(semanas.lastIndex)
    val meta = semanas[semana]
    val desde = inicio + semana * 7 * DIA_MS
    val hechas = carreras.count {
        it.tipo == TipoActividad.CAMINAR && it.inicio >= desde && it.inicio < desde + 7 * DIA_MS && it.duracionSeg * 10 >= meta.minutos * 60 * 9
    }
    return ProgresoPlan(semana, hechas, meta, terminado = transcurridas >= semanas.size)
}
