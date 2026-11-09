package com.example.nutrigajo.ui.shared.ajustes

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.example.nutrigajo.R

// Las notas de una versión de la app para «Acerca de → Notas de versión». `fecha` es el texto de cuándo se publicó (null en la que aún no sale).
data class VersionNotas(val version: String, @param:StringRes val fecha: Int?, @param:ArrayRes val notas: Int)

// De la versión más nueva a la más vieja. Se escribe a mano al publicar una versión: sus notas van en strings.xml (`notas_{versión}`) y son las
// mismas que se mandan con `npm run publicar-apk -- … --notas "una;otra"` (esas salen en la hoja de «hay una versión nueva»).
val NOTAS_DE_VERSION = listOf(
    VersionNotas("1.4", R.string.notas_proxima, R.array.notas_1_4),
    VersionNotas("1.3", R.string.notas_fecha_1_3, R.array.notas_1_3),
)

// La que dice la propia app (BuildConfig / PackageInfo) es la instalada: se marca en la lista.
fun esLaInstalada(version: String, versionApp: String): Boolean = version == versionApp.trim()
