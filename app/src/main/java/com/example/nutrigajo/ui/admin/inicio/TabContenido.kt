package com.example.nutrigajo.ui.admin.inicio

import com.example.nutrigajo.ui.shared.platillos.TarjetaPlatillo
import com.example.nutrigajo.ui.shared.platillos.PlatillosEstado
import com.example.nutrigajo.ui.components.SelectorPestanas
import com.example.nutrigajo.domain.model.Platillo
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.annotation.StringRes
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

// Las colas de ejercicios y de platillos que esperan revisión, cada una del más antiguo al más reciente. Tocar uno abre su detalle para
// publicarlo o rechazarlo.
@Composable
fun TabContenido(
    perfil: Perfil,
    ejercicios: EjerciciosEstado,
    platillos: PlatillosEstado,
    onEjercicio: (Ejercicio) -> Unit,
    onPlatillo: (Platillo) -> Unit,
    onActualizar: () -> Unit,
    onPerfil: () -> Unit,
    modifier: Modifier = Modifier,
    transicion: TransicionPerfil? = null,
) {
    val colores = MaterialTheme.colorScheme
    var seccion by rememberSaveable { mutableIntStateOf(0) }
    val deEjercicios = seccion == 0
    val error = if (deEjercicios) ejercicios.error else platillos.error
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
        item {
            SelectorPestanas(
                titulos = listOf(R.string.admin_contenido_ejercicios, R.string.admin_contenido_platillos),
                seleccionada = seccion,
                onSeleccionar = { seccion = it },
                modifier = Modifier.padding(horizontal = MARGEN),
            )
        }
        item { AvisoInformativo(stringResource(if (deEjercicios) R.string.admin_contenido_aviso else R.string.admin_contenido_aviso_platillos)) }
        error?.let { item { AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN)) } }
        if (deEjercicios) {
            item { EncabezadoSeccion(stringResource(R.string.admin_contenido_titulo), contador = "(${ejercicios.items.size})") }
            when {
                ejercicios.cargando && ejercicios.items.isEmpty() -> items(3) {
                    Esqueleto(Modifier.fillMaxWidth().padding(horizontal = MARGEN).height(104.dp), RoundedCornerShape(28.dp))
                }
                ejercicios.items.isEmpty() -> item { Vacio(R.string.admin_contenido_vacio) }
                else -> items(ejercicios.items, key = { it.id }) { ejercicio ->
                    TarjetaEjercicio(ejercicio, onClick = { onEjercicio(ejercicio) }, modifier = Modifier.padding(horizontal = MARGEN))
                }
            }
        } else {
            item { EncabezadoSeccion(stringResource(R.string.admin_contenido_titulo_platillos), contador = "(${platillos.items.size})") }
            when {
                platillos.cargando && platillos.items.isEmpty() -> items(2) {
                    Esqueleto(Modifier.fillMaxWidth().padding(horizontal = MARGEN).height(233.dp), RoundedCornerShape(28.dp))
                }
                platillos.items.isEmpty() -> item { Vacio(R.string.admin_contenido_vacio_platillos) }
                else -> items(platillos.items, key = { it.id }) { platillo ->
                    TarjetaPlatillo(platillo, onClick = { onPlatillo(platillo) }, modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN))
                }
            }
        }
    }
}

@Composable
private fun Vacio(@StringRes texto: Int) {
    Text(
        text = stringResource(texto),
        modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
