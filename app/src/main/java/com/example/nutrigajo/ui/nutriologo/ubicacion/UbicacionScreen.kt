package com.example.nutrigajo.ui.nutriologo.ubicacion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.DatosConsultorio
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CENTRO_POR_DEFECTO
import com.example.nutrigajo.ui.components.CampoTexto
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

private val MARGEN = 16.dp
private val ALTO_MAPA = 260.dp

// El nutriólogo verificado toca el mapa para marcar su consultorio y cuenta de él; los pacientes lo ven junto a su nombre
// y, al tocar «Ver más», en su ficha.
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
            onGuardar = { lat, lng, consultorio -> vm.publicar(perfil.nombre, lat, lng, consultorio) },
            onQuitar = vm::quitar,
            onVolver = onVolver,
        )
    }
}

@Composable
private fun UbicacionContenido(
    nombre: String,
    estado: UbicacionEstado,
    onGuardar: (lat: Double, lng: Double, consultorio: DatosConsultorio) -> Unit,
    onQuitar: () -> Unit,
    onVolver: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    val inicial = estado.ubicacion?.let { LatLng(it.lat, it.lng) }
    var punto by rememberSaveable { mutableStateOf(inicial) }
    // el formulario parte de lo ya publicado
    val publicado = estado.ubicacion?.consultorio ?: DatosConsultorio()
    var especialidad by rememberSaveable { mutableStateOf(publicado.especialidad) }
    var direccion by rememberSaveable { mutableStateOf(publicado.direccion) }
    var telefono by rememberSaveable { mutableStateOf(publicado.telefono) }
    var horario by rememberSaveable { mutableStateOf(publicado.horario) }
    var descripcion by rememberSaveable { mutableStateOf(publicado.descripcion) }
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
            .navigationBarsPadding()
            .imePadding(),
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
        // alto fijo: debajo se desplaza el formulario, y el dedo sobre el mapa mueve el mapa, no la pantalla
        GoogleMap(
            modifier = Modifier.height(ALTO_MAPA).fillMaxWidth(),
            cameraPositionState = camara,
            properties = MapProperties(isMyLocationEnabled = permiso),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = permiso),
            onMapClick = { punto = it },
        ) {
            punto?.let { Marker(state = rememberUpdatedMarkerState(position = it), title = nombre) }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = MARGEN),
        ) {
            Text(
                text = stringResource(R.string.ubicacion_ayuda),
                modifier = Modifier.padding(horizontal = MARGEN),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.consultorio_titulo),
                modifier = Modifier.padding(horizontal = MARGEN),
                style = MaterialTheme.typography.titleMedium,
                color = colores.onBackground,
            )
            Text(
                text = stringResource(R.string.consultorio_ayuda),
                modifier = Modifier.padding(horizontal = MARGEN),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN - ANILLO_FOCO),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CampoTexto(
                    etiqueta = stringResource(R.string.consultorio_especialidad),
                    valor = especialidad,
                    onValorChange = { especialidad = it.take(DatosConsultorio.MAX_ESPECIALIDAD) },
                    icono = R.drawable.ic_valoracion,
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                )
                CampoTexto(
                    etiqueta = stringResource(R.string.consultorio_direccion),
                    valor = direccion,
                    onValorChange = { direccion = it.take(DatosConsultorio.MAX_DIRECCION) },
                    icono = R.drawable.ic_ubicacion,
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )
                CampoTexto(
                    etiqueta = stringResource(R.string.consultorio_telefono),
                    valor = telefono,
                    onValorChange = { texto -> telefono = texto.filter { it.isDigit() || it in "+-() " }.take(DatosConsultorio.MAX_TELEFONO) },
                    icono = R.drawable.ic_telefono,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                )
                CampoTexto(
                    etiqueta = stringResource(R.string.consultorio_horario),
                    valor = horario,
                    onValorChange = { horario = it.take(DatosConsultorio.MAX_HORARIO) },
                    icono = R.drawable.ic_reloj,
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                )
                CampoTexto(
                    etiqueta = stringResource(R.string.consultorio_descripcion),
                    valor = descripcion,
                    onValorChange = { descripcion = it.take(DatosConsultorio.MAX_DESCRIPCION) },
                    icono = R.drawable.ic_informacion,
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
                    lineas = 3,
                )
            }
            estado.error?.let {
                Spacer(Modifier.height(12.dp))
                AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN))
            }
            Spacer(Modifier.height(20.dp))
            BotonPrimario(
                texto = stringResource(R.string.ubicacion_guardar),
                onClick = {
                    punto?.let {
                        onGuardar(it.latitude, it.longitude, DatosConsultorio(especialidad, direccion, telefono, horario, descripcion))
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
                enabled = punto != null,
                colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
                cargando = estado.guardando,
            )
            if (estado.ubicacion != null) {
                TextButton(onClick = onQuitar, enabled = !estado.guardando, modifier = Modifier.padding(horizontal = MARGEN - 8.dp)) {
                    Text(stringResource(R.string.ubicacion_quitar))
                }
            }
        }
    }
}
