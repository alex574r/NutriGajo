package com.example.nutrigajo.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.exp

// Lo que el teléfono le hace al agua de las métricas. La superficie se inclina hacia donde se inclina el teléfono, pero con un
// resorte poco amortiguado: se pasa de largo y vuelve, que es el chapoteo. Las sacudidas dan energía, que levanta el oleaje y
// se apaga sola. Sin acelerómetro queda el oleaje suave de reposo.
private const val TOPE_ENERGIA = 1.2f

// resorte de la inclinación: ζ ≈ 0.26, así que da un par de vaivenes antes de asentarse
private const val RIGIDEZ = 90f
private const val AMORTIGUACION = 5f

@Stable
class Agua {
    // -1..1: hacia qué lado sube la superficie (positivo = la derecha de la pantalla)
    var inclinacion by mutableFloatStateOf(0f)

    // 0..TOPE_ENERGIA: cuánto oleaje hay
    var energia by mutableFloatStateOf(0f)

    // radianes: por dónde va la ola
    var fase by mutableFloatStateOf(0f)

    // un golpe de agua, como al echar otro vaso
    fun salpicar(fuerza: Float) {
        energia = (energia + fuerza).coerceAtMost(TOPE_ENERGIA)
    }
}

// El acelerómetro en m/s² (x a la derecha del teléfono, y hacia arriba, z hacia afuera de la pantalla) mientras el composable
// está en pantalla. Parado y derecho marca la gravedad en y. El arreglo se actualiza en sitio: leerlo no recompone nada.
@Composable
fun rememberAceleracion(): FloatArray {
    val contexto = LocalContext.current
    val aceleracion = remember { floatArrayOf(0f, SensorManager.GRAVITY_EARTH, 0f) }
    DisposableEffect(contexto) {
        val sensores = contexto.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val oyente = object : SensorEventListener {
            override fun onSensorChanged(evento: SensorEvent) {
                aceleracion[0] = evento.values[0]
                aceleracion[1] = evento.values[1]
                aceleracion[2] = evento.values[2]
            }

            override fun onAccuracyChanged(sensor: Sensor?, precision: Int) = Unit
        }
        sensores?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensores.registerListener(oyente, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sensores?.unregisterListener(oyente) }
    }
    return aceleracion
}

// El agua viva: avanza un fotograma a la vez mientras el composable está en pantalla. Lo que se dibuja lee `inclinacion`,
// `energia` y `fase` dentro del dibujo, así que solo se vuelve a pintar, no a componer.
@Composable
fun rememberAgua(): Agua {
    val agua = remember { Agua() }
    val aceleracion = rememberAceleracion()
    LaunchedEffect(agua) {
        var velocidad = 0f
        var x = aceleracion[0]
        var y = aceleracion[1]
        var z = aceleracion[2]
        var ultimo = withFrameNanos { it }
        while (true) {
            val ahora = withFrameNanos { it }
            val dt = ((ahora - ultimo) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            ultimo = ahora
            // el agua queda horizontal en el mundo: en la pantalla sube del lado al que se inclina el teléfono
            val objetivo = (-aceleracion[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
            velocidad += (RIGIDEZ * (objetivo - agua.inclinacion) - AMORTIGUACION * velocidad) * dt
            agua.inclinacion = (agua.inclinacion + velocidad * dt).coerceIn(-1.3f, 1.3f)
            // la sacudida es cuánto cambió la aceleración desde el cuadro anterior; el ruido del sensor en reposo no cuenta
            val cambio = abs(aceleracion[0] - x) + abs(aceleracion[1] - y) + abs(aceleracion[2] - z)
            x = aceleracion[0]
            y = aceleracion[1]
            z = aceleracion[2]
            agua.energia = (agua.energia + (cambio - 0.2f).coerceAtLeast(0f) * 0.04f).coerceAtMost(TOPE_ENERGIA) * exp(-1.8f * dt)
            agua.fase += dt * (2.5f + 6f * agua.energia)
        }
    }
    return agua
}
