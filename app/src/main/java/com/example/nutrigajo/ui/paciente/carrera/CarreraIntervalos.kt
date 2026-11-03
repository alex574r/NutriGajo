package com.example.nutrigajo.ui.paciente.carrera

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.AvanceTramo
import com.example.nutrigajo.domain.model.Ritmo
import com.example.nutrigajo.domain.model.Tramo

// Lo de las sesiones con intervalos que se ve en pantalla: el nombre de cada ritmo, el perfil de la sesión (una barra por tramo, más
// alta cuanto más fuerte) y la tarjeta de la carrera en curso con la cuenta regresiva del tramo.

@get:StringRes
internal val Ritmo.nombre: Int
    get() = when (this) {
        Ritmo.CAMINAR -> R.string.carrera_caminar
        Ritmo.RAPIDO -> R.string.carrera_paso_rapido
        Ritmo.TROTAR -> R.string.carrera_trotar
        Ritmo.CORRER -> R.string.carrera_correr
    }

private val Ritmo.alto: Dp
    get() = when (this) {
        Ritmo.CAMINAR -> 8.dp
        Ritmo.RAPIDO -> 14.dp
        Ritmo.TROTAR -> 22.dp
        Ritmo.CORRER -> 30.dp
    }

@Composable
private fun Ritmo.color(): Color {
    val colores = MaterialTheme.colorScheme
    return when (this) {
        Ritmo.CAMINAR -> colores.outline
        Ritmo.RAPIDO -> colores.primary.copy(alpha = 0.35f)
        Ritmo.TROTAR -> colores.primary.copy(alpha = 0.65f)
        Ritmo.CORRER -> colores.primary
    }
}

// Los tramos uno junto a otro, cada uno tan ancho como dura. Con `actual` los que siguen se ven grises; sin él, todos de color.
@Composable
internal fun PerfilSesion(tramos: List<Tramo>, modifier: Modifier = Modifier, actual: Int? = null) {
    val pendiente = MaterialTheme.colorScheme.outlineVariant
    Row(modifier.fillMaxWidth().height(30.dp), horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.Bottom) {
        tramos.forEachIndexed { i, tramo ->
            val color = if (actual == null || i <= actual) tramo.ritmo.color() else pendiente
            Box(Modifier.weight(tramo.segundos.toFloat()).height(tramo.ritmo.alto).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

// «2 min caminar», «30 s correr», «1 min 30 s trotar»
@Composable
internal fun textoTramo(tramo: Tramo): String {
    val ritmo = stringResource(tramo.ritmo.nombre).lowercase()
    val min = tramo.segundos / 60
    val seg = tramo.segundos % 60
    return when {
        seg == 0 -> stringResource(R.string.carrera_tramo_min, min, ritmo)
        min == 0 -> stringResource(R.string.carrera_tramo_seg, seg, ritmo)
        else -> stringResource(R.string.carrera_tramo_min_seg, min, seg, ritmo)
    }
}

// «2 min caminar + 1 min trotar»: lo que se repite en cada ronda
@Composable
internal fun textoRonda(ronda: List<Tramo>): String = ronda.map { textoTramo(it) }.joinToString(" + ")

// La tarjeta que flota sobre el mapa: qué toca ahora y cuánto falta, qué sigue y el perfil de la sesión con lo hecho en color. `avance`
// es null cuando ya pasaron todos los tramos (se puede seguir o terminar).
@Composable
internal fun TarjetaIntervalo(tramos: List<Tramo>, avance: AvanceTramo?, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colores.background.copy(alpha = 0.96f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (avance == null) {
                    Text(stringResource(R.string.carrera_intervalo_fin), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = colores.primary)
                    Text(stringResource(R.string.carrera_intervalo_fin_ayuda), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                } else {
                    Text(
                        text = stringResource(avance.tramo.ritmo.nombre),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (avance.tramo.ritmo == Ritmo.CAMINAR) colores.onBackground else colores.primary,
                    )
                    Text(
                        text = avance.siguiente?.let { stringResource(R.string.carrera_intervalo_sigue, stringResource(it.ritmo.nombre), duracion(it.segundos.toLong())) }
                            ?: stringResource(R.string.carrera_intervalo_ultimo),
                        style = MaterialTheme.typography.bodySmall,
                        color = colores.onSurfaceVariant,
                    )
                }
            }
            if (avance != null) {
                // se redondea hacia arriba: con 0.3 s de tramo todavía se ve 00:01 y el cambio llega al llegar a 00:00
                Text(
                    text = duracion((avance.restanteMs + 999) / 1000),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    color = colores.onBackground,
                )
            }
        }
        PerfilSesion(tramos, actual = avance?.indice ?: tramos.size)
    }
}
