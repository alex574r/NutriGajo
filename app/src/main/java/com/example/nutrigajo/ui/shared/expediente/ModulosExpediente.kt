package com.example.nutrigajo.ui.shared.expediente

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import com.example.nutrigajo.R

// Los módulos del expediente, cada uno con los pasos del asistente que lo llenan y la foto de su tarjeta y de su cabecera.
// El asistente completo los recorre módulo por módulo: el orden de las preguntas es el de estas listas puestas una tras otra
// (ORDEN_PASOS), no el de su número, que es el de ExpedienteViewModel.sinPaso y el de tituloPaso.
// Fotos de Pexels (uso libre): salud 6320167 · descanso 7556587; el resto sale de las del onboarding y el inicio.
enum class ModuloExpediente(
    @param:StringRes val titulo: Int,
    @param:DrawableRes val foto: Int,
    val pasos: List<Int>,
    // qué parte de la foto se ve en la franja
    val alineacion: Alignment = Alignment.Center,
) {
    OBJETIVO(R.string.dato_objetivo, R.drawable.onboarding_planes, listOf(0), BiasAlignment(0f, -0.1f)),
    CUERPO(R.string.resumen_cat_cuerpo, R.drawable.onboarding_medidas, listOf(1, 2, 3, 4), BiasAlignment(0f, 0.25f)),
    SALUD(R.string.resumen_cat_salud, R.drawable.exp_salud, listOf(6, 7)),
    ALIMENTACION(R.string.resumen_cat_alimentacion, R.drawable.onboarding_alimentacion, listOf(8, 11, 12), BiasAlignment(0f, 0.5f)),
    ACTIVIDAD(R.string.resumen_cat_actividad, R.drawable.inicio_rutina, listOf(5, 9, 10)),
    DESCANSO(R.string.resumen_cat_descanso, R.drawable.exp_descanso, listOf(13)),
}

// Los 14 pasos en el orden en que el asistente completo los pregunta: módulo tras módulo.
val ORDEN_PASOS: List<Int> = ModuloExpediente.entries.flatMap { it.pasos }

fun moduloDe(paso: Int): ModuloExpediente = ModuloExpediente.entries.first { paso in it.pasos }
