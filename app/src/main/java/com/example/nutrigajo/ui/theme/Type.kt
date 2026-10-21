package com.example.nutrigajo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R

// Única familia de la interfaz: una sola fuente variable (res/font/inter.ttf) cubre todos los pesos.
@OptIn(ExperimentalTextApi::class)
val Inter = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)
        .map { peso ->
            Font(
                R.font.inter,
                weight = peso,
                // Settings(peso, estilo) ya fija el eje wght; agregarlo a mano lo duplica y crashea
                variationSettings = FontVariation.Settings(peso, FontStyle.Normal),
            )
        },
)

// Escala del Figma (px = sp, para respetar el tamaño de texto del sistema).
// Tracking en % del tamaño; negativo salvo LabelMd. Las pantallas usan estos estilos, no tamaños sueltos.
private fun estilo(peso: FontWeight, tamano: Int, interlineado: TextUnit, trackingPct: Double) = TextStyle(
    fontFamily = Inter,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = interlineado,
    letterSpacing = (trackingPct / 100).em,
)

val DisplaySm = estilo(FontWeight.ExtraBold, 96, 1.em, -2.0)
val HeadingLg = estilo(FontWeight.Medium, 48, 56.sp, -1.5)
val HeadingMd = estilo(FontWeight.Bold, 36, 44.sp, -1.2)
val HeadingSm = estilo(FontWeight.Bold, 30, 38.sp, -1.0)
val Text2xl = estilo(FontWeight.Medium, 24, 1.em, -0.8)
val TextXl = estilo(FontWeight.Bold, 20, 1.em, -0.5)
val TextLgMedium = estilo(FontWeight.Medium, 18, 1.em, -0.4)
val TextLgSemiBold = estilo(FontWeight.SemiBold, 18, 1.em, -0.4)
val TextMdMedium = estilo(FontWeight.Medium, 16, 1.em, -0.3)
val TextMdSemiBold = estilo(FontWeight.SemiBold, 16, 1.em, -0.3)
val TextMdBold = estilo(FontWeight.Bold, 16, 1.em, -0.3)
val TextSmMedium = estilo(FontWeight.Medium, 14, 1.em, -0.2)
val TextSmSemiBold = estilo(FontWeight.SemiBold, 14, 1.em, -0.2)
val TextSmBold = estilo(FontWeight.Bold, 14, 1.em, -0.2)
val ParagraphMd = estilo(FontWeight.Normal, 16, 1.6.em, -0.3)
val LabelMd = estilo(FontWeight.Bold, 14, 1.em, 10.0)

// Slots de Material 3 apuntando a la escala. Los botones, tags y opciones fijan su estilo explícito
// (p. ej. TextLgSemiBold en el botón primario).
val Typography = Typography(
    displayLarge = DisplaySm,
    displayMedium = HeadingLg,
    displaySmall = HeadingMd,
    headlineLarge = HeadingSm,
    headlineMedium = Text2xl,
    headlineSmall = TextXl,
    titleLarge = TextLgSemiBold,
    titleMedium = TextMdSemiBold,
    titleSmall = TextSmBold,
    bodyLarge = ParagraphMd,
    bodyMedium = TextMdMedium,
    bodySmall = TextSmMedium,
    labelLarge = TextSmSemiBold,
    labelMedium = LabelMd,
    labelSmall = TextSmMedium,
)
