package com.example.nutrigajo.ui.paciente.ejercicios

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.NivelEjercicio
import com.example.nutrigajo.domain.model.TipoEjercicio
import com.example.nutrigajo.domain.model.ZonaCuerpo
import com.example.nutrigajo.domain.model.coincide
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.ActualizarAlDeslizar
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.nutriologo.plan.BarraSuperior
import com.example.nutrigajo.ui.shared.ejercicios.DetalleEjercicio
import com.example.nutrigajo.ui.shared.ejercicios.EjerciciosViewModel
import com.example.nutrigajo.ui.shared.ejercicios.GrupoOpciones
import com.example.nutrigajo.ui.shared.ejercicios.TarjetaEjercicio
import com.example.nutrigajo.ui.shared.ejercicios.titulo

private val MARGEN = 16.dp

// El banco global de ejercicios: lo que publican los entrenadores verificados, con búsqueda y filtros por zona, tipo y nivel.
// Tocar uno abre su detalle, con el aviso de que no sustituye las indicaciones de su nutriólogo.
// ponytail: el filtrado es local sobre la lista completa; filtrar en la consulta cuando el banco pase de unos cientos.
@Composable
fun ExplorarEjerciciosScreen(
    onVolver: () -> Unit,
    // clave propia: la lista de publicados es de cualquiera, no del usuario
    vm: EjerciciosViewModel = viewModel(key = "explorar-ejercicios"),
) {
    val colores = MaterialTheme.colorScheme
    val estado by vm.estado.collectAsState()
    LaunchedEffect(Unit) { vm.cargarPublicados() }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var zona by rememberSaveable { mutableStateOf<ZonaCuerpo?>(null) }
    var tipo by rememberSaveable { mutableStateOf<TipoEjercicio?>(null) }
    var nivel by rememberSaveable { mutableStateOf<NivelEjercicio?>(null) }
    var abiertoId by rememberSaveable { mutableStateOf<String?>(null) }

    val abierto = estado.items.firstOrNull { it.id == abiertoId }
    if (abierto != null) {
        DetalleEjercicio(ejercicio = abierto, onVolver = { abiertoId = null }, aviso = stringResource(R.string.explorar_aviso))
        return
    }
    BackHandler(onBack = onVolver)

    val visibles = estado.items.filter { it.coincide(busqueda, zona, tipo, nivel) }
    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding()) {
        BarraSuperior(titulo = stringResource(R.string.explorar_titulo), onVolver = onVolver)
        Column(Modifier.padding(horizontal = MARGEN - ANILLO_FOCO)) {
            CampoTexto(
                etiqueta = stringResource(R.string.explorar_buscar),
                valor = busqueda,
                onValorChange = { busqueda = it.take(40) },
                icono = R.drawable.ic_buscar,
                modifier = Modifier.fillMaxWidth(),
                teclado = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
        Column(Modifier.padding(horizontal = MARGEN, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GrupoOpciones(null, ZonaCuerpo.entries, zona, ZonaCuerpo::titulo, { zona = it }, conTodos = true)
            GrupoOpciones(null, TipoEjercicio.entries, tipo, TipoEjercicio::titulo, { tipo = it }, conTodos = true)
            GrupoOpciones(null, NivelEjercicio.entries, nivel, NivelEjercicio::titulo, { nivel = it }, conTodos = true)
        }
        ActualizarAlDeslizar(refrescando = estado.cargando, onActualizar = vm::cargarPublicados, modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                estado.cargando && estado.items.isEmpty() -> Column(Modifier.padding(horizontal = MARGEN), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) { Esqueleto(Modifier.fillMaxWidth().height(104.dp), RoundedCornerShape(28.dp)) }
                }
                visibles.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = MARGEN * 2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    estado.error?.let { AlertaError(stringResource(it), Modifier.padding(bottom = 16.dp)) }
                    Text(
                        text = stringResource(if (estado.items.isEmpty()) R.string.explorar_sin_publicados else R.string.explorar_vacio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = MARGEN, end = MARGEN, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    estado.error?.let { item { AlertaError(stringResource(it)) } }
                    items(visibles, key = { it.id }) { ejercicio ->
                        TarjetaEjercicio(ejercicio, onClick = { abiertoId = ejercicio.id }, modifier = Modifier.animateItem())
                    }
                    item { Text(stringResource(R.string.explorar_aviso), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant, modifier = Modifier.navigationBarsPadding()) }
                }
            }
        }
    }
}
