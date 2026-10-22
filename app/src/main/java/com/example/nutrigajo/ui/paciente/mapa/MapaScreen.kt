package com.example.nutrigajo.ui.paciente.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.CENTRO_POR_DEFECTO
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.components.ZOOM_CIUDAD
import com.example.nutrigajo.ui.components.rememberUbicacionConcedida
import com.example.nutrigajo.ui.components.ultimaUbicacion
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

private const val ZOOM_ZONA = 13f

// El paciente ve en el mapa a los nutriólogos verificados; al tocar un marcador aparece su nombre.
// Para vincularse sigue haciendo falta el código del nutriólogo.
@Composable
fun MapaScreen(
    estado: MapaEstado,
    onActualizar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(CENTRO_POR_DEFECTO, ZOOM_CIUDAD)
    }
    val permiso = rememberUbicacionConcedida()
    val verificado = stringResource(R.string.mapa_verificado)

    // el mapa abre donde está el paciente; si no se sabe, en el primer nutriólogo (una sola vez cada caso)
    var centrado by remember { mutableStateOf(false) }
    LaunchedEffect(permiso) {
        if (permiso) {
            ultimaUbicacion(contexto)?.let {
                camara.animate(CameraUpdateFactory.newLatLngZoom(it, ZOOM_ZONA))
                centrado = true
            }
        }
    }
    LaunchedEffect(estado.cargando) {
        if (!centrado && !estado.cargando) {
            estado.nutriologos.firstOrNull()?.let {
                camara.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.lat, it.lng), ZOOM_ZONA))
                centrado = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colores.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onVolver) { Text(stringResource(R.string.mapa_volver)) }
                Text(
                    text = stringResource(R.string.mapa_titulo),
                    style = MaterialTheme.typography.titleLarge,
                    color = colores.onBackground,
                )
            }
            TextButton(onClick = onActualizar, enabled = !estado.cargando) {
                Text(stringResource(R.string.pacientes_actualizar))
            }
        }
        GoogleMap(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            cameraPositionState = camara,
            properties = MapProperties(isMyLocationEnabled = permiso),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = permiso),
        ) {
            estado.nutriologos.forEach { nutriologo ->
                Marker(
                    state = rememberUpdatedMarkerState(position = LatLng(nutriologo.lat, nutriologo.lng)),
                    title = nutriologo.nombre,
                    snippet = verificado,
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            estado.error?.let { AlertaError(stringResource(it)) }
            if (estado.cargando) {
                Esqueleto(Modifier.fillMaxWidth(0.7f).height(14.dp))
            } else {
                Text(
                    text = stringResource(if (estado.nutriologos.isEmpty()) R.string.mapa_vacio else R.string.mapa_ayuda),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                )
            }
        }
    }
}
