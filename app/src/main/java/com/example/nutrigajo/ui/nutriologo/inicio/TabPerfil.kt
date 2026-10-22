package com.example.nutrigajo.ui.nutriologo.inicio

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.components.ALTO_BARRA_INFERIOR
import com.example.nutrigajo.ui.nutriologo.ubicacion.TarjetaUbicacion
import com.example.nutrigajo.ui.nutriologo.verificacion.TarjetaVerificacion
import com.example.nutrigajo.ui.nutriologo.verificacion.VerificacionEstado

// Cuenta del nutriólogo: sus datos, el estado de la verificación, el mapa (si está verificado) y cerrar sesión.
@Composable
fun TabPerfil(
    perfil: Perfil,
    verificacion: VerificacionEstado,
    aprobada: Boolean,
    onVerificar: () -> Unit,
    onMapa: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = ALTO_BARRA_INFERIOR + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarInicial(perfil.nombre, 96.dp, MaterialTheme.typography.headlineLarge)
                Text(
                    text = perfil.nombre,
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colores.onBackground,
                )
                Text(perfil.correo, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                Text(
                    text = stringResource(R.string.rol_nutriologo),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .border(1.dp, colores.outline, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                    color = colores.onSurfaceVariant,
                )
            }
        }
        item { TarjetaVerificacion(verificacion, onVerificar) }
        if (aprobada) item { TarjetaUbicacion(onMapa) }
        item {
            OutlinedButton(
                onClick = onCerrarSesion,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(21.dp),
            ) { Text(stringResource(R.string.inicio_cerrar_sesion), style = MaterialTheme.typography.titleMedium) }
        }
    }
}
