package com.example.nutrigajo.ui.shared.metas

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.MetasDiarias
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.ContadorConBotones
import com.example.nutrigajo.ui.components.alPulsar

// Las cuatro metas diarias de un paciente, cada una con su contador rodante: agua, pasos, calorías y minutos de movimiento. Lo usan
// el nutriólogo (a pantalla completa, desde el expediente de su paciente) y el paciente (en una hoja, desde las pantallas de
// métricas); los dos escriben el mismo registro y la última escritura manda, con quién la dejó en `por`.
@Composable
fun MetasEditor(
    metas: MetasDiarias,
    estado: MetasEstado,
    onGuardar: (MetasDiarias) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    // si las metas del registro cambian (tras guardar) los contadores parten de ellas
    var agua by rememberSaveable(metas) { mutableIntStateOf(metas.aguaMl) }
    var pasos by rememberSaveable(metas) { mutableIntStateOf(metas.pasos) }
    var kcal by rememberSaveable(metas) { mutableIntStateOf(metas.kcal) }
    var minutos by rememberSaveable(metas) { mutableIntStateOf(metas.minutos) }
    val cambio = agua != metas.aguaMl || pasos != metas.pasos || kcal != metas.kcal || minutos != metas.minutos

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metas.por?.let {
            Text(
                text = stringResource(if (it == Rol.NUTRIOLOGO) R.string.metas_ultima_nutriologo else R.string.metas_ultima_paciente),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
            )
        }
        // la aceleración de cada contador (pasos a la vez tras 1.5 s mantenido) es la que cruza su rango en unos 3 s: el agua tiene
        // 23 pasos de 250 y no acelera; los pasos y las calorías, un centenar, aceleran ×2
        FilaMeta(R.drawable.ic_botella, R.string.metrica_hidratacion, R.string.meta_unidad_agua, agua, 4, MetasDiarias.PASO_AGUA_ML, 1) {
            agua = (agua + it).coerceIn(MetasDiarias.AGUA_ML)
        }
        FilaMeta(R.drawable.ic_corredor, R.string.metrica_pasos, R.string.meta_unidad_pasos, pasos, 5, MetasDiarias.PASO_PASOS, 2) {
            pasos = (pasos + it).coerceIn(MetasDiarias.PASOS)
        }
        FilaMeta(R.drawable.ic_llama_solida, R.string.metrica_calorias, R.string.meta_unidad_kcal, kcal, 4, MetasDiarias.PASO_KCAL, 2) {
            kcal = (kcal + it).coerceIn(MetasDiarias.KCAL)
        }
        FilaMeta(R.drawable.ic_reloj, R.string.meta_movimiento, R.string.meta_unidad_minutos, minutos, 3, MetasDiarias.PASO_MINUTOS, 1) {
            minutos = (minutos + it).coerceIn(MetasDiarias.MINUTOS)
        }
        estado.error?.let { AlertaError(stringResource(it)) }
        BotonPrimario(
            texto = stringResource(R.string.metas_guardar),
            onClick = { onGuardar(MetasDiarias(agua, pasos, kcal, minutos)) },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            enabled = cambio && !estado.guardando,
            cargando = estado.guardando,
        )
    }
}

// Una meta: su icono y nombre a la izquierda y, a la derecha, el contador. `delta` llega en unidades de la meta (pasos del
// contador × `paso`), con signo.
@Composable
private fun FilaMeta(
    @DrawableRes icono: Int,
    @StringRes titulo: Int,
    @StringRes unidad: Int,
    valor: Int,
    digitos: Int,
    paso: Int,
    aceleracion: Int,
    onDelta: (Int) -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(colores.background)
            .border(1.dp, colores.outlineVariant, forma)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(colores.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(18.dp), tint = colores.onPrimaryContainer)
            }
            Column {
                Text(stringResource(titulo), style = MaterialTheme.typography.titleSmall, color = colores.onBackground, maxLines = 1)
                Text(stringResource(unidad), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant, maxLines = 1)
            }
        }
        ContadorConBotones(
            valor = valor,
            onPasos = { onDelta(it * paso) },
            digitos = digitos,
            modifier = Modifier.fillMaxWidth(),
            estilo = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.Medium),
            aceleracion = aceleracion,
        )
    }
}

// A pantalla completa, para el nutriólogo: el atrás del diseño, «Metas de Fulano» y el editor. Al guardar vuelve solo.
@Composable
fun MetasScreen(
    nombrePaciente: String,
    metas: MetasDiarias,
    estado: MetasEstado,
    onGuardar: (MetasDiarias) -> Unit,
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    LaunchedEffect(estado.guardado) { if (estado.guardado) onGuardado() }
    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding().navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colores.surfaceVariant).alPulsar(onVolver),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.mapa_volver),
                    modifier = Modifier.size(22.dp),
                    tint = colores.onBackground,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.metas_de, nombrePaciente.substringBefore(' ')),
                    style = MaterialTheme.typography.titleMedium,
                    color = colores.onBackground,
                    maxLines = 1,
                )
                Text(stringResource(R.string.metas_ayuda_nutriologo), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 16.dp)) {
            MetasEditor(metas = metas, estado = estado, onGuardar = onGuardar)
        }
    }
}

// En una hoja, para el paciente: se abre desde el engrane de las pantallas de métricas. Al guardar se cierra sola.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasSheet(
    metas: MetasDiarias,
    estado: MetasEstado,
    onGuardar: (MetasDiarias) -> Unit,
    onGuardado: () -> Unit,
    onCerrar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    LaunchedEffect(estado.guardado) { if (estado.guardado) onGuardado() }
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colores.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.metas_titulo), style = MaterialTheme.typography.headlineMedium, color = colores.onBackground)
                Text(stringResource(R.string.metas_ayuda_paciente), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
            }
            MetasEditor(metas = metas, estado = estado, onGuardar = onGuardar)
        }
    }
}
