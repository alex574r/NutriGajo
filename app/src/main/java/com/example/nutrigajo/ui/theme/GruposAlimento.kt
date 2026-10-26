package com.example.nutrigajo.ui.theme

import androidx.annotation.StringRes
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.GrupoAlimento

// Cada grupo de alimentos lleva su color, como en el formato de los planes (la interfaz y el PDF usan el mismo): son tonos
// oscuros para que el texto blanco encima se lea (contraste de 4.5:1 o más). ARGB.
val GrupoAlimento.color: Int
    get() = when (this) {
        GrupoAlimento.VERDURAS -> 0xFF4D7C0F
        GrupoAlimento.FRUTAS -> 0xFFA16207
        GrupoAlimento.CEREALES -> 0xFFC2410C
        GrupoAlimento.ANIMAL -> 0xFF9D174D
        GrupoAlimento.LEGUMINOSAS -> 0xFFBE123C
        GrupoAlimento.LACTEOS -> 0xFF0369A1
        GrupoAlimento.GRASAS_PROTEINA -> 0xFF6D28D9
        GrupoAlimento.GRASAS -> 0xFF854D0E
    }.toInt()

val GrupoAlimento.titulo: Int
    @StringRes get() = when (this) {
        GrupoAlimento.VERDURAS -> R.string.pdf_grupo_verduras
        GrupoAlimento.FRUTAS -> R.string.pdf_grupo_frutas
        GrupoAlimento.CEREALES -> R.string.pdf_grupo_cereales
        GrupoAlimento.ANIMAL -> R.string.pdf_grupo_animal
        GrupoAlimento.LEGUMINOSAS -> R.string.pdf_grupo_leguminosas
        GrupoAlimento.LACTEOS -> R.string.pdf_grupo_lacteos
        GrupoAlimento.GRASAS_PROTEINA -> R.string.pdf_grupo_grasas_proteina
        GrupoAlimento.GRASAS -> R.string.pdf_grupo_grasas
    }
