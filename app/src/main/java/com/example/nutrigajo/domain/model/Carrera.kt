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

/// ---- Planes: entrenamientos guiados

// Cómo se mueve uno en un tramo: caminando tranquilo, a paso rápido, trotando o corriendo.
enum class Ritmo { CAMINAR, RAPIDO, TROTAR, CORRER }

data class Tramo(val ritmo: Ritmo, val segundos: Int)

// Dónde va una sesión guiada: el tramo en curso (el `indice` es su lugar en la lista), lo que le queda y el que sigue.
data class AvanceTramo(val indice: Int, val tramo: Tramo, val restanteMs: Long, val siguiente: Tramo?)

// El tramo en el que se está a los `transcurridoMs` de empezar (sin contar pausas); null cuando ya pasaron todos.
fun List<Tramo>.avance(transcurridoMs: Long): AvanceTramo? {
    var fin = 0L
    forEachIndexed { i, tramo ->
        fin += tramo.segundos * 1000L
        if (transcurridoMs < fin) return AvanceTramo(i, tramo, fin - transcurridoMs, getOrNull(i + 1))
    }
    return null
}

// Cada entrenamiento empieza y termina caminando.
const val CALENTAMIENTO_SEG = 300
const val ENFRIAMIENTO_SEG = 300

// Una ronda: los segundos de cada ritmo, siempre en este orden, de menos a más fuerte (el 0 se omite).
private fun unaRonda(caminar: Int = 0, rapido: Int = 0, trotar: Int = 0, correr: Int = 0) =
    listOf(Tramo(Ritmo.CAMINAR, caminar), Tramo(Ritmo.RAPIDO, rapido), Tramo(Ritmo.TROTAR, trotar), Tramo(Ritmo.CORRER, correr))
        .filter { it.segundos > 0 }

// Los entrenamientos que se ofrecen. Elegir uno lo empieza: la app lleva la cuenta de cada tramo y el teléfono vibra en cada cambio de
// ritmo. La sesión es el calentamiento, la `ronda` repetida `rondas` veces y el enfriamiento. Los tres primeros son de caminata (con
// tramos a paso rápido) y los demás llevan trote y carrera.
enum class PlanCaminata(val rondas: Int, val ronda: List<Tramo>) {
    PRIMEROS_PASOS(4, unaRonda(caminar = 180, rapido = 60)),
    RITMO_CONSTANTE(6, unaRonda(caminar = 120, rapido = 120)),
    RESISTENCIA(4, unaRonda(caminar = 120, rapido = 480)),
    INTERVALOS_PRINCIPIANTE(6, unaRonda(caminar = 120, trotar = 90)),
    INTERVALOS_INTERMEDIO(5, unaRonda(caminar = 60, trotar = 120, correr = 120)),
    INTERVALOS_AVANZADO(5, unaRonda(caminar = 60, trotar = 60, correr = 240)),
    // muchas rondas con ráfagas y descansos cortos, para mantener el pulso alto
    QUEMAGRASA(8, unaRonda(caminar = 45, trotar = 90, correr = 60)),
    ;

    val tramos: List<Tramo>
        get() = listOf(Tramo(Ritmo.CAMINAR, CALENTAMIENTO_SEG)) + List(rondas) { ronda }.flatten() + Tramo(Ritmo.CAMINAR, ENFRIAMIENTO_SEG)

    val minutos: Int get() = ceil(tramos.sumOf { it.segundos } / 60.0).toInt()

    // se guarda como caminata si nunca se trota ni se corre, y como carrera si sí (cambia las calorías)
    val tipo: TipoActividad
        get() = if (ronda.any { it.ritmo == Ritmo.TROTAR || it.ritmo == Ritmo.CORRER }) TipoActividad.CORRER else TipoActividad.CAMINAR

    val conTrote: Boolean get() = tipo == TipoActividad.CORRER
}

const val DIA_MS = 86_400_000L
