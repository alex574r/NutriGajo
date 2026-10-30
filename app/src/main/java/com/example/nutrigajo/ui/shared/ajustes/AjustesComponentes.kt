package com.example.nutrigajo.ui.shared.ajustes

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.data.local.AjustesApp
import com.example.nutrigajo.data.local.Biometria
import com.example.nutrigajo.data.local.actividad
import com.example.nutrigajo.ui.components.alPulsar

// Una pantalla de ajustes: el atrás del diseño, el título al centro y el contenido que se desplaza. `espacioInferior` deja libre
// la barra flotante cuando el perfil es una pestaña del nutriólogo.
@Composable
fun PantallaAjuste(
    titulo: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    espacioInferior: Dp = 0.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    Column(modifier.fillMaxSize().background(colores.background).statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colores.surfaceVariant)
                    .alPulsar(onVolver),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_chevron_izquierda), stringResource(R.string.mapa_volver), tint = colores.onSurfaceVariant)
            }
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = colores.onBackground, maxLines = 1)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = espacioInferior + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = contenido,
        )
    }
}

// El título pequeño que abre cada grupo de filas.
@Composable
fun GrupoAjustes(titulo: String, modifier: Modifier = Modifier) {
    Text(
        text = titulo,
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, top = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// Activa o desactiva pedir la huella o el rostro al abrir. Para activarlo primero hay que lograr desbloquear una vez: así se sabe
// que funciona y que no se deja la app cerrada con algo que no se puede usar.
fun alternarBloqueo(contexto: Context, quiere: Boolean) {
    if (!quiere) {
        AjustesApp.cambiarBloqueo(contexto, false)
        return
    }
    val actividad = contexto.actividad()
    if (actividad == null || !Biometria.disponible(contexto)) {
        Toast.makeText(contexto, R.string.seguridad_no_disponible, Toast.LENGTH_LONG).show()
        return
    }
    Biometria.pedir(
        actividad = actividad,
        titulo = contexto.getString(R.string.ajustes_biometria),
        subtitulo = contexto.getString(R.string.seguridad_confirma),
        alExito = { AjustesApp.cambiarBloqueo(contexto, true) },
        alFallar = {},
    )
}
