package com.example.nutrigajo.ui.shared.expediente

import androidx.annotation.StringRes
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.CalidadSueno
import com.example.nutrigajo.domain.model.Dieta
import com.example.nutrigajo.domain.model.EjercicioPreferido
import com.example.nutrigajo.domain.model.Objetivo
import com.example.nutrigajo.domain.model.Sexo

// Textos de cada opción del expediente. Viven en la interfaz: el dominio solo guarda el nombre del enum.

@StringRes
fun Objetivo.etiqueta(): Int = when (this) {
    Objetivo.BAJAR_PESO -> R.string.objetivo_bajar_peso
    Objetivo.GANAR_MUSCULO -> R.string.objetivo_ganar_musculo
    Objetivo.MEJORAR_ALIMENTACION -> R.string.objetivo_mejorar_alimentacion
    Objetivo.MEJORAR_RESISTENCIA -> R.string.objetivo_mejorar_resistencia
    Objetivo.CONDICION_SALUD -> R.string.objetivo_condicion_salud
}

@StringRes
fun Sexo.etiqueta(): Int = when (this) {
    Sexo.FEMENINO -> R.string.sexo_femenino
    Sexo.MASCULINO -> R.string.sexo_masculino
    Sexo.OTRO -> R.string.sexo_otro
}

@StringRes
fun Dieta.etiqueta(): Int = when (this) {
    Dieta.PLANTAS -> R.string.dieta_plantas
    Dieta.BAJA_CARBOHIDRATOS -> R.string.dieta_baja_carbohidratos
    Dieta.ESPECIALIZADA -> R.string.dieta_especializada
    Dieta.TRADICIONAL -> R.string.dieta_tradicional
}

@StringRes
fun Dieta.ejemplos(): Int = when (this) {
    Dieta.PLANTAS -> R.string.dieta_plantas_ej
    Dieta.BAJA_CARBOHIDRATOS -> R.string.dieta_baja_carbohidratos_ej
    Dieta.ESPECIALIZADA -> R.string.dieta_especializada_ej
    Dieta.TRADICIONAL -> R.string.dieta_tradicional_ej
}

@StringRes
fun EjercicioPreferido.etiqueta(): Int = when (this) {
    EjercicioPreferido.CORRER -> R.string.ejercicio_correr
    EjercicioPreferido.CAMINAR -> R.string.ejercicio_caminar
    EjercicioPreferido.SENDERISMO -> R.string.ejercicio_senderismo
    EjercicioPreferido.PATINAR -> R.string.ejercicio_patinar
    EjercicioPreferido.CICLISMO -> R.string.ejercicio_ciclismo
    EjercicioPreferido.PESAS -> R.string.ejercicio_pesas
    EjercicioPreferido.CARDIO -> R.string.ejercicio_cardio
    EjercicioPreferido.YOGA -> R.string.ejercicio_yoga
    EjercicioPreferido.OTRO -> R.string.ejercicio_otro
}

@StringRes
fun CalidadSueno.etiqueta(): Int = when (this) {
    CalidadSueno.EXCELENTE -> R.string.sueno_excelente
    CalidadSueno.BUENA -> R.string.sueno_buena
    CalidadSueno.NORMAL -> R.string.sueno_normal
    CalidadSueno.MALA -> R.string.sueno_mala
    CalidadSueno.INSOMNIO -> R.string.sueno_insomnio
}

@StringRes
fun CalidadSueno.horas(): Int = when (this) {
    CalidadSueno.EXCELENTE -> R.string.sueno_excelente_horas
    CalidadSueno.BUENA -> R.string.sueno_buena_horas
    CalidadSueno.NORMAL -> R.string.sueno_normal_horas
    CalidadSueno.MALA -> R.string.sueno_mala_horas
    CalidadSueno.INSOMNIO -> R.string.sueno_insomnio_horas
}

// nivel de actividad 1 a 5
@StringRes
fun etiquetaActividad(nivel: Int): Int = when (nivel) {
    1 -> R.string.actividad_1
    2 -> R.string.actividad_2
    3 -> R.string.actividad_3
    4 -> R.string.actividad_4
    else -> R.string.actividad_5
}
