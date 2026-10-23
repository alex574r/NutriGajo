package com.example.nutrigajo.ui.components

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Deslizar hacia abajo desde el inicio de una lista vuelve a cargarla. El indicador baja del borde superior de la pantalla
// y se detiene bajo la barra de estado, en naranja como el resto de acciones; `refrescando` lo mantiene visible mientras carga.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActualizarAlDeslizar(
    refrescando: Boolean,
    onActualizar: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val estado = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refrescando,
        onRefresh = onActualizar,
        modifier = modifier,
        state = estado,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = estado,
                isRefreshing = refrescando,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
                containerColor = colores.background,
                color = colores.primary,
            )
        },
    ) {
        contenido()
    }
}
