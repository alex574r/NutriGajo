package com.example.nutrigajo.ui.admin.inicio

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.components.ALTO_BARRA_INFERIOR
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.AvisoInformativo
import com.example.nutrigajo.ui.components.CabeceraUsuario
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.components.TransicionPerfil
import com.example.nutrigajo.ui.paciente.inicio.EncabezadoSeccion
import com.example.nutrigajo.ui.shared.ejercicios.EjerciciosEstado
import com.example.nutrigajo.ui.shared.ejercicios.TarjetaEjercicio
import com.example.nutrigajo.ui.shared.perfil.rememberFotoAvatar

private val MARGEN = 20.dp

// La cola de ejercicios que esperan revisión, del más antiguo al más reciente. Tocar uno abre su detalle para publicarlo o rechazarlo.
@Composable
fun TabContenido(
    perfil: Perfil,
    estado: EjerciciosEstado,
    onEjercicio: (Ejercicio) -> Unit,
    onActualizar: () -> Unit,
    onPerfil: () -> Unit,
    modifier: Modifier = Modifier,
    transicion: TransicionPerfil? = null,
) {
    val colores = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = ALTO_BARRA_INFERIOR + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            CabeceraUsuario(
                saludo = stringResource(R.string.admin_panel),
                nombre = perfil.nombre,
                verificado = false,
                descripcionAvatar = stringResource(R.string.nutri_abrir_perfil),
                onAvatar = onPerfil,
                iconoAccion = R.drawable.ic_refrescar,
                descripcionAccion = stringResource(R.string.admin_actualizar),
                onAccion = onActualizar,
                imagenAvatar = rememberFotoAvatar(perfil),
                transicion = transicion,
            )
        }
        item { AvisoInformativo(stringResource(R.string.admin_contenido_aviso)) }
        estado.error?.let { item { AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN)) } }
        item { EncabezadoSeccion(stringResource(R.string.admin_contenido_titulo), contador = "(${estado.items.size})") }
        when {
            estado.cargando && estado.items.isEmpty() -> items(3) {
                Esqueleto(Modifier.fillMaxWidth().padding(horizontal = MARGEN).height(104.dp), RoundedCornerShape(28.dp))
            }
            estado.items.isEmpty() -> item {
                Text(
                    text = stringResource(R.string.admin_contenido_vacio),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            else -> items(estado.items, key = { it.id }) { ejercicio ->
                TarjetaEjercicio(ejercicio, onClick = { onEjercicio(ejercicio) }, modifier = Modifier.padding(horizontal = MARGEN))
            }
        }
    }
}
