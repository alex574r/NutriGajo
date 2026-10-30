package com.example.nutrigajo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Tema claro: formularios, evaluación y dashboard.
private val LightColorScheme = lightColorScheme(
    primary = PulseOrange50,
    onPrimary = White,
    primaryContainer = PulseOrange10,
    onPrimaryContainer = PulseOrange50,
    secondary = TabataBlue60,
    onSecondary = White,
    secondaryContainer = TabataBlue10,
    onSecondaryContainer = TabataBlue60,
    tertiary = Gray80,
    onTertiary = White,
    tertiaryContainer = Gray10,
    onTertiaryContainer = Gray100,
    background = White,
    onBackground = Gray100,
    surface = White,
    onSurface = Gray100,
    surfaceVariant = Gray10,
    onSurfaceVariant = Gray60,
    surfaceTint = White,
    inverseSurface = Gray100,
    inverseOnSurface = White,
    inversePrimary = PulseOrange50,
    outline = Gray30,
    outlineVariant = Gray20,
    scrim = Gray100,
    error = EnduranceRed50,
    onError = White,
    errorContainer = EnduranceRed10,
    onErrorContainer = Gray100,
    surfaceContainerLowest = White,
    surfaceContainerLow = Gray10,
    surfaceContainer = Gray10,
    surfaceContainerHigh = Gray10,
    surfaceContainerHighest = Gray10,
)

// Tema oscuro: onboarding con fotografía. El Figma solo define fondo, texto y naranja;
// los contenedores y bordes oscuros son provisionales.
private val DarkColorScheme = darkColorScheme(
    primary = PulseOrange50,
    onPrimary = White,
    primaryContainer = PulseOrange10,
    onPrimaryContainer = PulseOrange50,
    secondary = TabataBlue60,
    onSecondary = White,
    secondaryContainer = TabataBlue10,
    onSecondaryContainer = TabataBlue60,
    tertiary = Gray30,
    onTertiary = Gray100,
    tertiaryContainer = Gray80,
    onTertiaryContainer = White,
    background = Gray100,
    onBackground = White,
    surface = Gray100,
    onSurface = White,
    surfaceVariant = Gray80,
    onSurfaceVariant = Gray30,
    surfaceTint = Gray100,
    inverseSurface = White,
    inverseOnSurface = Gray100,
    inversePrimary = PulseOrange50,
    outline = Gray60,
    outlineVariant = Gray80,
    scrim = Gray100,
    error = EnduranceRed50,
    onError = White,
    errorContainer = EnduranceRed10,
    onErrorContainer = Gray100,
    surfaceContainerLowest = Gray100,
    surfaceContainerLow = Gray80,
    surfaceContainer = Gray80,
    surfaceContainerHigh = Gray80,
    surfaceContainerHighest = Gray80,
)

// Tema mixto: la app es clara; solo las pantallas de bienvenida piden NutriGajoTheme(darkTheme = true).
// Sin color dinámico ni modo del sistema: la paleta de marca no debe cambiar con el fondo de pantalla.
@Composable
fun NutriGajoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

// El «negro» de las tarjetas, cabeceras y botones oscuros del diseño (lo que en el tema claro es `onBackground`). En el oscuro
// `onBackground` es blanco, así que ahí es un gris oscuro que se sigue distinguiendo del fondo; el texto encima es siempre blanco.
val ColorScheme.negro: Color get() = if (background.luminance() < 0.5f) Gray80 else Gray100

// El gris medio de las tarjetas de color (calorías): no cambia con el tema, para que el blanco de encima se lea siempre.
val ColorScheme.grisTarjeta: Color get() = Gray60
