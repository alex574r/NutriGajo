package com.example.nutrigajo.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Perfil

// ponytail: pantalla de paso hasta que existan los inicios reales de cada rol.
@Composable
fun InicioProvisional(perfil: Perfil, detalle: String?, onCerrarSesion: () -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.inicio_hola, perfil.nombre), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.inicio_rol, perfil.rol.name), style = MaterialTheme.typography.bodyLarge)
            if (detalle != null) Text(detalle, style = MaterialTheme.typography.bodyLarge)
            Text(perfil.correo, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onCerrarSesion) { Text(stringResource(R.string.inicio_cerrar_sesion)) }
        }
    }
}
