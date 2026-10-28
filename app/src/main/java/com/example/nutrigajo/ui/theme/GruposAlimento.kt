package com.example.nutrigajo.ui.theme

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
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

// La foto de cada grupo, para la cabecera de su tarjeta en el plan. Fotos de Pexels (uso libre, sin atribución): verduras 2255999 ·
// frutas 8426464 · cereales 12284683 · origen animal 5704254 · leguminosas 6187598 · lácteos 236010 · grasas con proteína 15613811 ·
// grasas 25745503. Medidas a 720 px de ancho.
val GrupoAlimento.foto: Int
    @DrawableRes get() = when (this) {
        GrupoAlimento.VERDURAS -> R.drawable.grupo_verduras
        GrupoAlimento.FRUTAS -> R.drawable.grupo_frutas
        GrupoAlimento.CEREALES -> R.drawable.grupo_cereales
        GrupoAlimento.ANIMAL -> R.drawable.grupo_animal
        GrupoAlimento.LEGUMINOSAS -> R.drawable.grupo_leguminosas
        GrupoAlimento.LACTEOS -> R.drawable.grupo_lacteos
        GrupoAlimento.GRASAS_PROTEINA -> R.drawable.grupo_grasas_proteina
        GrupoAlimento.GRASAS -> R.drawable.grupo_grasas
    }

// Qué parte de la foto se ve en la franja de la cabecera: la de las grasas es vertical y lo que importa (la jarra) queda en el medio;
// en la de verduras se baja el encuadre para que no salga el letrero del puesto.
val GrupoAlimento.alineacionFoto: Alignment
    get() = when (this) {
        GrupoAlimento.GRASAS -> BiasAlignment(0f, -0.05f)
        GrupoAlimento.VERDURAS -> BiasAlignment(0f, 0.85f)
        else -> Alignment.Center
    }

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
