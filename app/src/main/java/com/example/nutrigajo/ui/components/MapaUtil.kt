package com.example.nutrigajo.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng

// Donde abre el mapa cuando no se sabe dónde está el usuario: Ciudad de México.
val CENTRO_POR_DEFECTO = LatLng(19.4326, -99.1332)
const val ZOOM_CIUDAD = 11f
const val ZOOM_CALLE = 15f

private fun tienePermiso(contexto: Context) =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

// Pide la ubicación al abrir el mapa y avisa si se concedió. Es opcional: sin ella el mapa funciona igual.
@Composable
fun rememberUbicacionConcedida(): Boolean {
    val contexto = LocalContext.current
    var concedida by remember { mutableStateOf(tienePermiso(contexto)) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        concedida = resultado.values.any { it }
    }
    LaunchedEffect(Unit) {
        if (!concedida) pedir.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    return concedida
}

// Último punto que el sistema conoce, sin encender el GPS. Null si no hay permiso o aún no hay ninguno.
@SuppressLint("MissingPermission")
fun ultimaUbicacion(contexto: Context): LatLng? {
    if (!tienePermiso(contexto)) return null
    val gestor = contexto.getSystemService(LocationManager::class.java) ?: return null
    return gestor.getProviders(true)
        .mapNotNull { runCatching { gestor.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
        ?.let { LatLng(it.latitude, it.longitude) }
}
