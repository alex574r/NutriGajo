package com.example.nutrigajo.ui.paciente.avisos

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.PlanEnviado
import com.example.nutrigajo.ui.components.ActualizarAlDeslizar
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.PuntoNuevo
import com.example.nutrigajo.ui.components.aparece
import com.example.nutrigajo.ui.nutriologo.inicio.fechaMedia

private val MARGEN = 20.dp

// Lo último que vio esta persona en sus notificaciones: la fecha de envío del plan más nuevo que ya abrió la lista. Vive en este
// teléfono (no en Firestore): basta para el punto de la campana.
// ponytail: SharedPreferences como la bandera del onboarding; si se quiere el mismo estado en varios teléfonos, pasa a Firestore.
internal fun vistoHasta(contexto: Context, uid: String): Long = contexto.prefs().getLong("planes_vistos_$uid", 0L)

internal fun marcarVisto(contexto: Context, uid: String, hasta: Long) = contexto.prefs().edit().putLong("planes_vistos_$uid", hasta).apply()

private fun Context.prefs() = getSharedPreferences("nutrigajo", Context.MODE_PRIVATE)

// Las notificaciones del paciente: un renglón por cada plan que le envió su nutriólogo, del más nuevo al más viejo; al tocarlo se
// abre ese plan, así que todos se pueden volver a ver cuando quiera. Los que llegaron después de `vistoAlAbrir` llevan un punto.
@Composable
fun NotificacionesScreen(
    planes: List<PlanEnviado>,
    cargando: Boolean,
    error: Int?,
    vistoAlAbrir: Long,
    onActualizar: () -> Unit,
    onPlan: (PlanEnviado) -> Unit,
    onVolver: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    ActualizarAlDeslizar(
        refrescando = cargando,
        onActualizar = onActualizar,
        modifier = Modifier.fillMaxSize().background(colores.background),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onVolver) { Text(stringResource(R.string.mapa_volver)) }
                Text(stringResource(R.string.inicio_notificaciones), style = MaterialTheme.typography.titleLarge, color = colores.onBackground)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                error?.let { item { AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN)) } }
                if (cargando && planes.isEmpty()) {
                    items(3) { Esqueleto(Modifier.padding(horizontal = MARGEN).fillMaxWidth().height(70.dp), RoundedCornerShape(22.dp)) }
                } else if (planes.isEmpty() && error == null) {
                    item {
                        Text(
                            text = stringResource(R.string.avisos_vacio),
                            modifier = Modifier.padding(horizontal = MARGEN),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colores.onSurfaceVariant,
                        )
                    }
                }
                itemsIndexed(planes, key = { _, plan -> plan.id }) { indice, plan ->
                    Box(Modifier.padding(horizontal = MARGEN).aparece(indice)) {
                        FilaAjuste(
                            icono = R.drawable.ic_documento,
                            // la fecha va en el título y el nombre arriba: juntos no caben en un renglón y se parten
                            titulo = stringResource(R.string.avisos_plan_titulo, fechaMedia(plan.enviadoEn)),
                            etiqueta = stringResource(R.string.avisos_plan_de, plan.nutriologoNombre),
                            onClick = { onPlan(plan) },
                        )
                        if (plan.enviadoEn > vistoAlAbrir) PuntoNuevo(Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 6.dp))
                    }
                }
            }
        }
    }
}
