package com.example.nutrigajo.ui.shared.platillos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.EstadoPlatillo
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Platillo
import com.example.nutrigajo.domain.model.duplicado
import com.example.nutrigajo.ui.components.ActualizarAlDeslizar
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.AvisoInformativo
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.nutriologo.plan.BarraSuperior

private val MARGEN = 16.dp

// «Mis platillos» de un nutriólogo o un entrenador: los suyos con el estado de cada uno, el formulario para crear o editar y el detalle
// con las acciones que su estado permite. Cubre toda la pantalla. Solo con la cuenta verificada (`puedeCrear`) se crea uno nuevo.
// ponytail: se carga al abrir y al deslizar; sin listener en tiempo real.
@Composable
fun MisPlatillosScreen(
    perfil: Perfil,
    puedeCrear: Boolean,
    onVolver: () -> Unit,
    // clave propia: el uid ya lo usan otros ViewModels del mismo usuario
    vm: PlatillosViewModel = viewModel(key = "platillos-${perfil.uid}"),
) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current
    var abiertoId by rememberSaveable { mutableStateOf<String?>(null) }
    var editando by remember { mutableStateOf<Platillo?>(null) }
    LaunchedEffect(Unit) { vm.cargarMios() }

    val abierto = estado.items.firstOrNull { it.id == abiertoId }
    val enEdicion = editando
    when {
        enEdicion != null -> EditorPlatillo(
            inicial = enEdicion,
            guardando = estado.guardando,
            error = estado.error,
            onGuardar = { platillo, enviar, foto ->
                vm.guardar(platillo, perfil.nombre, perfil.rol, enviar, foto, contexto.contentResolver, contexto.cacheDir) { bien ->
                    if (bien) {
                        editando = null
                        abiertoId = null
                    }
                }
            },
            onVolver = {
                vm.limpiarError()
                editando = null
            },
        )
        abierto != null -> DetallePlatillo(
            platillo = abierto,
            onVolver = { abiertoId = null },
            acciones = {
                AccionesPlatillo(
                    platillo = abierto,
                    estado = estado,
                    onEditar = { editando = abierto },
                    onDuplicar = { editando = abierto.duplicado() },
                    onRetirar = { vm.retirar(abierto) { vm.cargarMios(); abiertoId = null } },
                    onSacarDeRevision = { vm.sacarDeRevision(abierto) { vm.cargarMios(); abiertoId = null } },
                    onEliminar = { vm.eliminar(abierto) { abiertoId = null } },
                )
            },
        )
        else -> {
            BackHandler(onBack = onVolver)
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
                BarraSuperior(titulo = stringResource(R.string.platillo_mis_platillos), onVolver = onVolver)
                ActualizarAlDeslizar(refrescando = estado.cargando, onActualizar = vm::cargarMios, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item { AvisoInformativo(stringResource(R.string.platillo_aviso_autor)) }
                        estado.error?.let { item { AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN)) } }
                        when {
                            !puedeCrear -> item { Aviso(R.string.platillo_sin_verificar) }
                            estado.cargando && estado.items.isEmpty() -> items(2) {
                                Esqueleto(Modifier.fillMaxWidth().padding(horizontal = MARGEN).height(233.dp), RoundedCornerShape(28.dp))
                            }
                            estado.items.isEmpty() -> item { Aviso(R.string.platillo_vacio) }
                            else -> items(estado.items, key = { it.id }) { platillo ->
                                TarjetaPlatillo(
                                    platillo = platillo,
                                    onClick = { abiertoId = platillo.id },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
                                    mostrarEstado = true,
                                )
                            }
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(MARGEN)) {
                    BotonPrimario(
                        texto = stringResource(R.string.platillo_nuevo),
                        onClick = {
                            vm.limpiarError()
                            editando = Platillo()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = puedeCrear,
                    )
                }
            }
        }
    }
}

@Composable
private fun Aviso(texto: Int) {
    Text(
        text = stringResource(texto),
        modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN * 2),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

// Los botones de un platillo según su estado: se edita lo que es borrador o rechazado, se saca de revisión lo que espera al ADMIN y lo
// publicado o retirado solo se duplica (un platillo publicado no cambia). Retirar y eliminar piden confirmación.
@Composable
private fun ColumnScope.AccionesPlatillo(
    platillo: Platillo,
    estado: PlatillosEstado,
    onEditar: () -> Unit,
    onDuplicar: () -> Unit,
    onRetirar: () -> Unit,
    onSacarDeRevision: () -> Unit,
    onEliminar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    var confirmando by remember { mutableStateOf<Confirmacion?>(null) }
    val trabajando = estado.trabajando != null
    estado.error?.let { AlertaError(stringResource(it)) }

    when (platillo.estado) {
        EstadoPlatillo.BORRADOR, EstadoPlatillo.RECHAZADO -> {
            BotonAccion(R.string.ejercicio_editar, principal = true, enabled = !trabajando, onClick = onEditar)
            BotonAccion(R.string.ejercicio_eliminar, enabled = !trabajando) { confirmando = Confirmacion.ELIMINAR }
        }
        EstadoPlatillo.EN_REVISION -> BotonAccion(R.string.ejercicio_sacar_revision, principal = true, enabled = !trabajando, onClick = onSacarDeRevision)
        EstadoPlatillo.PUBLICADO -> {
            BotonAccion(R.string.ejercicio_duplicar, principal = true, enabled = !trabajando, onClick = onDuplicar)
            BotonAccion(R.string.ejercicio_retirar, enabled = !trabajando) { confirmando = Confirmacion.RETIRAR }
        }
        EstadoPlatillo.RETIRADO -> {
            BotonAccion(R.string.ejercicio_duplicar, principal = true, enabled = !trabajando, onClick = onDuplicar)
            BotonAccion(R.string.ejercicio_eliminar, enabled = !trabajando) { confirmando = Confirmacion.ELIMINAR }
        }
    }

    confirmando?.let { tipo ->
        val eliminar = tipo == Confirmacion.ELIMINAR
        AlertDialog(
            onDismissRequest = { confirmando = null },
            containerColor = colores.background,
            title = { Text(stringResource(if (eliminar) R.string.platillo_eliminar_titulo else R.string.platillo_retirar_titulo)) },
            text = { Text(stringResource(if (eliminar) R.string.platillo_eliminar_texto else R.string.platillo_retirar_texto, platillo.nombre)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmando = null
                    if (eliminar) onEliminar() else onRetirar()
                }) { Text(stringResource(if (eliminar) R.string.ejercicio_eliminar else R.string.ejercicio_retirar), color = colores.error) }
            },
            dismissButton = { TextButton(onClick = { confirmando = null }) { Text(stringResource(R.string.cita_dialogo_volver)) } },
        )
    }
}

private enum class Confirmacion { ELIMINAR, RETIRAR }

@Composable
private fun BotonAccion(texto: Int, principal: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val modificador = Modifier.fillMaxWidth().height(48.dp)
    if (principal) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modificador,
            shape = RoundedCornerShape(21.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
        ) { Text(stringResource(texto)) }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = modificador, shape = RoundedCornerShape(21.dp)) { Text(stringResource(texto)) }
    }
}
