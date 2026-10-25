package com.example.nutrigajo.ui.paciente.mapa

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.NutriologoEnMapa
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.AvatarInicial
import com.example.nutrigajo.ui.components.BotonPrimario
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
import kotlinx.coroutines.launch

private const val ZOOM_ZONA = 13f

// El paciente ve en el mapa a los nutriólogos verificados; al tocar un marcador el mapa lo centra y sale una tarjeta con
// «ver más», que abre su ficha y desde ahí puede agendar una cita. Para vincularse sigue haciendo falta el código del nutriólogo.
@Composable
fun MapaScreen(
    estado: MapaEstado,
    onActualizar: () -> Unit,
    onVolver: () -> Unit,
    onVerMas: (NutriologoEnMapa) -> Unit,
    onCerrarFicha: () -> Unit,
    onAgendar: (NutriologoEnMapa) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(CENTRO_POR_DEFECTO, ZOOM_CIUDAD)
    }
    val permiso = rememberUbicacionConcedida()
    val verificado = stringResource(R.string.mapa_verificado)
    var elegidoId by rememberSaveable { mutableStateOf<String?>(null) }
    val elegido = estado.nutriologos.firstOrNull { it.id == elegidoId }

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
        Box(Modifier.weight(1f).fillMaxWidth()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camara,
                properties = MapProperties(isMyLocationEnabled = permiso),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = permiso),
                onMapClick = { elegidoId = null },
            ) {
                estado.nutriologos.forEach { nutriologo ->
                    val posicion = LatLng(nutriologo.lat, nutriologo.lng)
                    // el globo de Maps es una imagen y no recibe toques: la tarjeta de abajo hace de globo y lleva el botón
                    Marker(
                        state = rememberUpdatedMarkerState(position = posicion),
                        title = nutriologo.nombre,
                        snippet = verificado,
                        onClick = {
                            elegidoId = nutriologo.id
                            alcance.launch { camara.animate(CameraUpdateFactory.newLatLng(posicion)) }
                            true
                        },
                    )
                }
            }
            TarjetaAnimada(elegido, onVerMas)
        }
        estado.seleccionado?.let { FichaNutriologo(it, estado.foto, onCerrarFicha, onAgendar = { onAgendar(it) }) }
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

// Función aparte porque dentro de la columna de la pantalla AnimatedVisibility se resuelve como la de ColumnScope.
@Composable
private fun BoxScope.TarjetaAnimada(nutriologo: NutriologoEnMapa?, onVerMas: (NutriologoEnMapa) -> Unit) {
    AnimatedVisibility(
        visible = nutriologo != null,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = fadeIn() + slideInVertically { it / 2 },
    ) {
        nutriologo?.let { TarjetaNutriologo(it, onVerMas = { onVerMas(it) }) }
    }
}

// Lo que sale sobre el mapa al tocar un marcador: quién es, dónde está su consultorio (si lo contó) y el botón de la ficha.
@Composable
private fun TarjetaNutriologo(nutriologo: NutriologoEnMapa, onVerMas: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val consultorio = nutriologo.consultorio
    val forma = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(forma)
            .background(colores.background)
            .border(1.dp, colores.surfaceVariant, forma)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AvatarInicial(nombre = nutriologo.nombre, tamano = 48.dp, estilo = MaterialTheme.typography.titleMedium)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = nutriologo.nombre,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleMedium,
                        color = colores.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_verificado),
                        contentDescription = stringResource(R.string.nutri_verificado),
                        modifier = Modifier.size(18.dp),
                        tint = colores.primary,
                    )
                }
                Text(
                    text = consultorio.especialidad.ifBlank { stringResource(R.string.mapa_verificado) },
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (consultorio.direccion.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(R.drawable.ic_ubicacion), contentDescription = null, modifier = Modifier.size(20.dp), tint = colores.onBackground)
                Text(
                    text = consultorio.direccion,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.4.em),
                    color = colores.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        BotonPrimario(
            texto = stringResource(R.string.mapa_ver_mas),
            onClick = onVerMas,
            modifier = Modifier.fillMaxWidth(),
            altura = 48.dp,
            colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
        )
    }
}
