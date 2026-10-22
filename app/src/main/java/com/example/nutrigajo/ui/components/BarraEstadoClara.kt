package com.example.nutrigajo.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Íconos de la barra de estado claros sobre una foto oscura, sin importar el modo del sistema.
// Llamarlo una sola vez por pantalla (no dentro de las páginas de un pager).
// `sobreFoto = false` deja los íconos como estaban (p. ej. al desplazar la foto fuera de la pantalla).
@Composable
fun BarraEstadoClara(sobreFoto: Boolean = true) {
    val view = LocalView.current
    DisposableEffect(sobreFoto) {
        val controller = WindowCompat.getInsetsController((view.context as Activity).window, view)
        val anterior = controller.isAppearanceLightStatusBars
        if (sobreFoto) controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = anterior }
    }
}
