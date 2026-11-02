package com.example.nutrigajo.ui.paciente.ejercicios

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.nutrigajo.ui.components.BarraEstadoClara
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.components.Etiqueta
import com.example.nutrigajo.ui.shared.ejercicios.BotonVolverCristal
import com.example.nutrigajo.ui.shared.ejercicios.CabeceraOscura
import com.example.nutrigajo.ui.shared.ejercicios.DetalleEjercicio
import com.example.nutrigajo.ui.shared.ejercicios.EjerciciosViewModel
import com.example.nutrigajo.ui.shared.ejercicios.FilaEjercicio
import com.example.nutrigajo.ui.shared.ejercicios.GrupoOpciones
import com.example.nutrigajo.ui.shared.ejercicios.GuiaEjercicioScreen
import com.example.nutrigajo.ui.shared.ejercicios.montadaSobreLaCabecera
import com.example.nutrigajo.ui.shared.ejercicios.titulo

private val MARGEN = 16.dp
private const val SEMANA_MS = 7L * 24 * 60 * 60 * 1000

// El banco global de ejercicios con el aspecto «Workout»: la cabecera oscura con el total, y debajo la hoja blanca con la búsqueda, los
// filtros por zona, tipo y nivel y la lista. Tocar uno abre su detalle (con las instrucciones paso a paso y «empezar ejercicio»), con el
// aviso de que no sustituye las indicaciones de su nutriólogo.
// ponytail: el filtrado es local sobre la lista completa; filtrar en la consulta cuando el banco pase de unos cientos.
@Composable
fun ExplorarEjerciciosScreen(
    onVolver: () -> Unit,
    // el ejercicio que se tocó en el inicio: se abre directo y atrás vuelve al inicio
    abrirId: String? = null,
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
    var abiertoId by rememberSaveable { mutableStateOf(abrirId) }
    // el paso en que se abrió la guía del ejercicio abierto; null es el detalle
    var guiaPaso by rememberSaveable { mutableStateOf<Int?>(null) }

    val abierto = estado.items.firstOrNull { it.id == abiertoId }
    if (abierto != null) {
        val paso = guiaPaso
        if (paso != null) {
            GuiaEjercicioScreen(ejercicio = abierto, inicio = paso, onSalir = { guiaPaso = null })
        } else {
            DetalleEjercicio(
                ejercicio = abierto,
                onVolver = { if (abrirId != null) onVolver() else abiertoId = null },
                aviso = stringResource(R.string.explorar_aviso),
                onEmpezar = { guiaPaso = it },
            )
        }
        return
    }
    BackHandler(onBack = onVolver)
    BarraEstadoClara()

    val visibles = estado.items.filter { it.coincide(busqueda, zona, tipo, nivel) }
    val ahora = remember { System.currentTimeMillis() }
    Box(Modifier.fillMaxSize().background(colores.background)) {
        ActualizarAlDeslizar(refrescando = estado.cargando, onActualizar = vm::cargarPublicados, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    CabeceraOscura(
                        alto = 300.dp,
                        foto = {
                            Image(
                                painter = painterResource(R.drawable.ej_pesas),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop,
                                alignment = BiasAlignment(0f, -0.2f),
                            )
                        },
                    ) {
                        Etiqueta(pluralStringResource(R.plurals.explorar_en_total, estado.items.size, estado.items.size).uppercase())
                        Text(stringResource(R.string.explorar_titulo), style = MaterialTheme.typography.headlineLarge, color = Color.White)
                        Text(
                            text = stringResource(R.string.explorar_descripcion),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                // la hoja blanca: se monta sobre la cabecera y lleva el título de la lista, la búsqueda y los filtros
                item {
                    Column(
                        modifier = Modifier
                            .montadaSobreLaCabecera()
                            .fillMaxWidth()
                            .background(colores.background, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .padding(start = MARGEN - ANILLO_FOCO, end = MARGEN - ANILLO_FOCO, top = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = ANILLO_FOCO), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.explorar_todos_ejercicios),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colores.onBackground,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(stringResource(R.string.explorar_orden), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                        }
                        CampoTexto(
                            etiqueta = stringResource(R.string.explorar_buscar),
                            valor = busqueda,
                            onValorChange = { busqueda = it.take(40) },
                            icono = R.drawable.ic_buscar,
                            modifier = Modifier.fillMaxWidth(),
                            teclado = KeyboardOptions(imeAction = ImeAction.Search),
                        )
                        Column(Modifier.padding(horizontal = ANILLO_FOCO), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GrupoOpciones(null, ZonaCuerpo.entries, zona, ZonaCuerpo::titulo, { zona = it }, conTodos = true)
                            GrupoOpciones(null, TipoEjercicio.entries, tipo, TipoEjercicio::titulo, { tipo = it }, conTodos = true)
                            GrupoOpciones(null, NivelEjercicio.entries, nivel, NivelEjercicio::titulo, { nivel = it }, conTodos = true)
                        }
                    }
                }
                estado.error?.let { item { AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN)) } }
                when {
                    estado.cargando && estado.items.isEmpty() -> items(4) {
                        Esqueleto(Modifier.fillMaxWidth().padding(horizontal = MARGEN).height(107.dp), RoundedCornerShape(34.dp))
                    }
                    visibles.isEmpty() -> item {
                        Text(
                            text = stringResource(if (estado.items.isEmpty()) R.string.explorar_sin_publicados else R.string.explorar_vacio),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN * 2, vertical = 24.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colores.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> items(visibles, key = { it.id }) { ejercicio ->
                        FilaEjercicio(
                            ejercicio = ejercicio,
                            onClick = { abiertoId = ejercicio.id },
                            modifier = Modifier.padding(horizontal = MARGEN).animateItem(),
                            nuevo = ejercicio.publicadoEn?.let { ahora - it < SEMANA_MS } == true,
                        )
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.explorar_aviso),
                        modifier = Modifier.padding(horizontal = MARGEN).navigationBarsPadding(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colores.onSurfaceVariant,
                    )
                }
            }
        }
        BotonVolverCristal(onVolver, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp))
    }
}
