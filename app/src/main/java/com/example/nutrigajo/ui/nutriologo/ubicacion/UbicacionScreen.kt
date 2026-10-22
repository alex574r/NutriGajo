package com.example.nutrigajo.ui.nutriologo.ubicacion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CENTRO_POR_DEFECTO
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.components.ZOOM_CALLE
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

// El nutriólogo verificado toca el mapa para marcar su consultorio; los pacientes lo ven junto a su nombre.
@Composable
fun UbicacionScreen(
    perfil: Perfil,
    onVolver: () -> Unit,
    // clave propia: el uid ya la usan otros ViewModels del mismo usuario
    vm: UbicacionViewModel = viewModel(key = "ubicacion-${perfil.uid}"),
) {
    val estado by vm.estado.collectAsState()
    LaunchedEffect(Unit) { vm.cargar() }
    LaunchedEffect(estado.listo) {
        if (estado.listo) {
            vm.consumirListo()
            onVolver()
        }
    }
    BackHandler(onBack = onVolver)

    if (estado.cargando) {
        PantallaCarga()
    } else {
        UbicacionContenido(
            nombre = perfil.nombre,
            estado = estado,
            onGuardar = { lat, lng -> vm.publicar(perfil.nombre, lat, lng) },
            onQuitar = vm::quitar,
            onVolver = onVolver,
        )
    }
}

@Composable
private fun UbicacionContenido(
    nombre: String,
    estado: UbicacionEstado,
    onGuardar: (lat: Double, lng: Double) -> Unit,
    onQuitar: () -> Unit,
    onVolver: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    val inicial = estado.ubicacion?.let { LatLng(it.lat, it.lng) }
    var punto by rememberSaveable { mutableStateOf(inicial) }
    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(inicial ?: CENTRO_POR_DEFECTO, if (inicial != null) ZOOM_CALLE else ZOOM_CIUDAD)
    }
    val permiso = rememberUbicacionConcedida()
    // sin punto guardado, el mapa abre donde está el nutriólogo
    LaunchedEffect(permiso) {
        if (permiso && inicial == null) {
            ultimaUbicacion(contexto)?.let { camara.animate(CameraUpdateFactory.newLatLngZoom(it, ZOOM_CALLE)) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onVolver) { Text(stringResource(R.string.mapa_volver)) }
            Text(
                text = stringResource(R.string.ubicacion_titulo),
                style = MaterialTheme.typography.titleLarge,
                color = colores.onBackground,
            )
        }
        GoogleMap(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            cameraPositionState = camara,
            properties = MapProperties(isMyLocationEnabled = permiso),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = permiso),
            onMapClick = { punto = it },
        ) {
            punto?.let { Marker(state = rememberUpdatedMarkerState(position = it), title = nombre) }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.ubicacion_ayuda),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
            )
            estado.error?.let { AlertaError(stringResource(it)) }
            BotonPrimario(
                texto = stringResource(R.string.ubicacion_guardar),
                onClick = { punto?.let { onGuardar(it.latitude, it.longitude) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = punto != null,
                colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
                cargando = estado.guardando,
            )
            if (estado.ubicacion != null) {
                TextButton(onClick = onQuitar, enabled = !estado.guardando) {
                    Text(stringResource(R.string.ubicacion_quitar))
                }
            }
        }
    }
}
