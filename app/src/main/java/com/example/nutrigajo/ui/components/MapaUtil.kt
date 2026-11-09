package com.example.nutrigajo.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.nutrigajo.data.local.AjustesApp
import com.example.nutrigajo.domain.EstadoPermiso
import com.example.nutrigajo.domain.PermisoApp
import com.example.nutrigajo.domain.debePedirseSolo
import com.example.nutrigajo.ui.permisos.rememberPermiso
import com.google.android.gms.maps.model.LatLng

// Donde abre el mapa cuando no se sabe dónde está el usuario: Ciudad de México.
val CENTRO_POR_DEFECTO = LatLng(19.4326, -99.1332)
const val ZOOM_CIUDAD = 11f
const val ZOOM_CALLE = 15f

private fun tienePermiso(contexto: Context) =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

// Ofrece la ubicación la primera vez que se abre un mapa y avisa si está concedida. Es opcional: sin ella el mapa funciona igual, y las
// aperturas siguientes no vuelven a preguntar (se puede activar en Configuración → Permisos).
@Composable
fun rememberUbicacionConcedida(): Boolean {
    val contexto = LocalContext.current
    val permiso = rememberPermiso(PermisoApp.UBICACION_MAPA)
    LaunchedEffect(Unit) {
        val grupo = PermisoApp.UBICACION_MAPA.grupo
        if (debePedirseSolo(permiso.estado, AjustesApp.yaSeOfrecio(contexto, grupo))) {
            AjustesApp.marcarOfrecido(contexto, grupo)
            permiso.pedir()
        }
    }
    return permiso.estado == EstadoPermiso.CONCEDIDO
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
