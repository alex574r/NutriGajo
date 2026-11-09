package com.example.nutrigajo.ui.paciente.platillos

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Dieta
import com.example.nutrigajo.domain.model.EtiquetaPlatillo
import com.example.nutrigajo.domain.model.MomentoComida
import com.example.nutrigajo.domain.model.coincide
import com.example.nutrigajo.domain.model.paraPaciente
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.ActualizarAlDeslizar
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.nutriologo.plan.BarraSuperior
import com.example.nutrigajo.ui.nutriologo.plan.titulo
import com.example.nutrigajo.ui.paciente.inicio.MenuDeHoy
import com.example.nutrigajo.ui.paciente.inicio.TarjetaMenuDeHoy
import com.example.nutrigajo.ui.shared.ejercicios.GrupoOpciones
import com.example.nutrigajo.ui.shared.platillos.DetallePlatillo
import com.example.nutrigajo.ui.shared.platillos.FilaPlatillo
import com.example.nutrigajo.ui.shared.platillos.PlatillosViewModel
import com.example.nutrigajo.ui.shared.platillos.titulo

private val MARGEN = 16.dp

// El banco global de platillos que ve el paciente: búsqueda, filtros por momento y etiqueta y la lista; tocar uno abre su receta con el
// aviso de que no sustituye las indicaciones de su nutriólogo. Se esconden los que nombran una de sus alergias (`alergias`, del
// expediente) y, según su `dieta`, se prefieren los que le tocan. Con `abrirId` (el platillo que se tocó en el inicio) se abre ese
// directo y atrás vuelve al inicio.
// ponytail: el filtrado es local sobre la lista completa; filtrar en la consulta cuando el banco pase de unos cientos.
@Composable
fun ExplorarPlatillosScreen(
    onVolver: () -> Unit,
    abrirId: String? = null,
    alergias: List<String> = emptyList(),
    dieta: Dieta? = null,
    // el expediente (de donde salen `alergias` y `dieta`) ya se leyó; mientras no, no se muestra ningún platillo: sin las alergias no
    // se puede cuidar nada. `expedienteCargando` distingue «aún carga» de «no se pudo leer»
    expedienteListo: Boolean = true,
    expedienteCargando: Boolean = false,
    onActualizarExpediente: () -> Unit = {},
    // en un plan con duración, el menú que toca hoy: va arriba de todo, antes de la búsqueda
    menuDeHoy: MenuDeHoy? = null,
    // clave propia: la lista de publicados es de cualquiera, no del usuario
    vm: PlatillosViewModel = viewModel(key = "explorar-platillos"),
) {
    val colores = MaterialTheme.colorScheme
    val estado by vm.estado.collectAsState()
    LaunchedEffect(Unit) { vm.cargarPublicados() }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var momento by rememberSaveable { mutableStateOf<MomentoComida?>(null) }
    var etiqueta by rememberSaveable { mutableStateOf<EtiquetaPlatillo?>(null) }
    var abiertoId by rememberSaveable { mutableStateOf(abrirId) }

    val permitidos = remember(estado.items, alergias, dieta, expedienteListo) { estado.items.paraPaciente(alergias, dieta, expedienteListo) }
    // un platillo que se pide por su id también pasa por el filtro de alergias
    val abierto = permitidos.firstOrNull { it.id == abiertoId }
    if (abierto != null) {
        DetallePlatillo(
            platillo = abierto,
            onVolver = { if (abrirId != null) onVolver() else abiertoId = null },
            aviso = stringResource(R.string.platillo_aviso),
        )
        return
    }
    BackHandler(onBack = onVolver)

    val visibles = permitidos.filter { it.coincide(busqueda, momento, etiqueta) }
    val ocultos = if (expedienteListo) estado.items.size - permitidos.size else 0
    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding().navigationBarsPadding()) {
        BarraSuperior(titulo = stringResource(R.string.platillo_explorar_titulo), onVolver = onVolver)
        ActualizarAlDeslizar(
            refrescando = estado.cargando || expedienteCargando,
            onActualizar = {
                vm.cargarPublicados()
                onActualizarExpediente()
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                menuDeHoy?.let { menu -> item { TarjetaMenuDeHoy(menu) } }
                item {
                    Column(Modifier.padding(horizontal = MARGEN - ANILLO_FOCO), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CampoTexto(
                            etiqueta = stringResource(R.string.platillo_buscar),
                            valor = busqueda,
                            onValorChange = { busqueda = it.take(40) },
                            icono = R.drawable.ic_buscar,
                            modifier = Modifier.fillMaxWidth(),
                            teclado = KeyboardOptions(imeAction = ImeAction.Search),
                        )
                        Column(Modifier.padding(horizontal = ANILLO_FOCO), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GrupoOpciones(null, MomentoComida.entries, momento, { it.titulo() }, { momento = it }, conTodos = true)
                            GrupoOpciones(null, EtiquetaPlatillo.entries, etiqueta, { it.titulo() }, { etiqueta = it }, conTodos = true)
                        }
                    }
                }
                estado.error?.let { item { AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN)) } }
                if (ocultos > 0) {
                    item {
                        Text(
                            text = stringResource(R.string.platillo_ocultos, ocultos),
                            modifier = Modifier.padding(horizontal = MARGEN),
                            style = MaterialTheme.typography.bodySmall,
                            color = colores.onSurfaceVariant,
                        )
                    }
                }
                when {
                    (estado.cargando && estado.items.isEmpty()) || expedienteCargando -> items(4) {
                        Esqueleto(Modifier.fillMaxWidth().padding(horizontal = MARGEN).height(107.dp), RoundedCornerShape(34.dp))
                    }
                    !expedienteListo -> item {
                        Text(
                            text = stringResource(R.string.platillo_expediente_error),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN * 2, vertical = 24.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colores.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    visibles.isEmpty() -> item {
                        Text(
                            text = stringResource(if (estado.items.isEmpty()) R.string.platillo_explorar_sin_publicados else R.string.platillo_explorar_vacio),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN * 2, vertical = 24.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colores.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> items(visibles, key = { it.id }) { platillo ->
                        FilaPlatillo(platillo, onClick = { abiertoId = platillo.id }, modifier = Modifier.padding(horizontal = MARGEN).animateItem())
                    }
                }
            }
        }
    }
}
