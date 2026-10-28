package com.example.nutrigajo.ui.nutriologo.plan

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.ItemBanco
import com.example.nutrigajo.domain.model.MomentoComida
import com.example.nutrigajo.domain.model.PlanContenido
import com.example.nutrigajo.domain.model.TipoBanco
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.components.SelectorPestanas
import com.example.nutrigajo.ui.components.hundible

private val MARGEN = 16.dp

// Qué se le pide al banco: de qué tipo y, para una comida, de qué tiempo. Con `onElegir` es un selector (tocar un elemento lo
// pone en el plan); sin él, el banco se administra: tocar un elemento lo edita.
internal class DestinoBanco(
    val tipo: TipoBanco,
    val momento: MomentoComida? = null,
    val onElegir: ((ItemBanco) -> Unit)? = null,
)

@StringRes
internal fun MomentoComida.titulo(): Int = when (this) {
    MomentoComida.DESAYUNO -> R.string.pdf_col_desayuno
    MomentoComida.COMIDA -> R.string.pdf_col_comida
    MomentoComida.CENA -> R.string.pdf_col_cena
}

// El banco de comidas y ejercicios del nutriólogo: lista de lo guardado (las comidas se filtran por tiempo de comida) y el
// formulario para crear o editar uno. Cubre toda la pantalla; regresar desde el formulario vuelve a la lista.
@Composable
internal fun BancoScreen(destino: DestinoBanco, vm: BancoViewModel, onSalir: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val estado by vm.estado.collectAsState()
    LaunchedEffect(Unit) { vm.cargar() }
    val elegir = destino.onElegir
    var tipo by remember { mutableStateOf(destino.tipo) }
    var filtro by remember { mutableStateOf(destino.momento) }
    var formulario by remember { mutableStateOf<ItemBanco?>(null) }
    BackHandler { if (formulario != null) formulario = null else onSalir() }

    val abierto = formulario
    if (abierto != null) {
        FormularioBanco(
            inicial = abierto,
            guardando = estado.guardando,
            error = estado.error,
            onGuardar = { item -> vm.guardar(item) { bien -> if (bien) formulario = null } },
            onEliminar = { vm.eliminar(abierto.id) { bien -> if (bien) formulario = null } },
            onVolver = { formulario = null },
        )
        return
    }

    val visibles = estado.items.filter { it.tipo == tipo && (filtro == null || it.momento == filtro) }
    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding()) {
        BarraSuperior(
            titulo = stringResource(
                when {
                    elegir == null -> R.string.banco_titulo
                    tipo == TipoBanco.COMIDA -> R.string.banco_titulo_comidas
                    else -> R.string.banco_titulo_ejercicios
                },
            ),
            onVolver = onSalir,
        )
        // administrando se pasa de comidas a ejercicios; eligiendo, el tipo ya lo decidió el campo desde el que se abrió
        if (elegir == null) {
            SelectorPestanas(
                titulos = listOf(R.string.banco_comidas, R.string.banco_ejercicios),
                seleccionada = tipo.ordinal,
                onSeleccionar = {
                    tipo = TipoBanco.entries[it]
                    filtro = null
                },
                modifier = Modifier.padding(horizontal = MARGEN, vertical = 4.dp),
            )
        }
        if (tipo == TipoBanco.COMIDA) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = MARGEN, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OpcionChip(stringResource(R.string.banco_todas), filtro == null) { filtro = null }
                MomentoComida.entries.forEach { OpcionChip(stringResource(it.titulo()), filtro == it) { filtro = it } }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                estado.cargando -> Column(Modifier.padding(horizontal = MARGEN, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val descripcion = stringResource(R.string.cargando)
                    repeat(3) {
                        Esqueleto(Modifier.fillMaxWidth().height(96.dp).semantics { contentDescription = descripcion }, RoundedCornerShape(22.dp))
                    }
                }
                visibles.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = MARGEN * 2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    estado.error?.let { AlertaError(stringResource(it), Modifier.padding(bottom = 16.dp)) }
                    Text(
                        text = stringResource(if (tipo == TipoBanco.COMIDA) R.string.banco_vacio_comidas else R.string.banco_vacio_ejercicios),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = MARGEN, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // los elementos se acomodan con resorte cuando se guarda, se quita o se filtra uno
                    items(visibles, key = { it.id }) { item ->
                        TarjetaItem(item, Modifier.animateItem()) { if (elegir != null) elegir(item) else formulario = item }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(MARGEN)) {
            BotonPrimario(
                texto = stringResource(if (tipo == TipoBanco.COMIDA) R.string.banco_nueva_comida else R.string.banco_nuevo_ejercicio),
                onClick = {
                    formulario = ItemBanco(
                        tipo = tipo,
                        nombre = "",
                        texto = "",
                        momento = if (tipo == TipoBanco.COMIDA) filtro ?: MomentoComida.DESAYUNO else null,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BarraSuperior(titulo: String, onVolver: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colores.surfaceVariant)
                .clickable(role = Role.Button, onClick = onVolver),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.expediente_regresar), tint = colores.onBackground)
        }
        Text(
            text = titulo,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = colores.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Un elemento del banco: su nombre (y el tiempo de comida) y las primeras líneas de su texto. Se hunde bajo el dedo.
@Composable
private fun TarjetaItem(item: ItemBanco, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .hundible(forma)
            .clip(forma)
            .background(colores.surfaceVariant)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = item.nombre,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = colores.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.momento?.let {
                Text(
                    text = stringResource(it.titulo()),
                    modifier = Modifier.background(colores.primaryContainer, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = colores.onPrimaryContainer,
                )
            }
        }
        Text(item.texto, style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

// Opción de un grupo (filtro o tiempo de comida): la elegida va en naranja.
@Composable
private fun OpcionChip(texto: String, elegida: Boolean, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    // el color pasa de una opción a otra con resorte, y la elegida se asienta con un pequeño rebote de tamaño
    val fondo by animateColorAsState(if (elegida) colores.primary else colores.surfaceVariant, spring(stiffness = 400f), label = "fondo")
    val letra by animateColorAsState(if (elegida) colores.onPrimary else colores.onBackground, spring(stiffness = 400f), label = "letra")
    val escala by animateFloatAsState(if (elegida) 1f else 0.97f, spring(dampingRatio = 0.4f, stiffness = 500f), label = "escala")
    Text(
        text = texto,
        modifier = Modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .selectable(selected = elegida, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = letra,
    )
}

// Crear (id vacío) o editar un elemento. Nombre y texto son obligatorios; las comidas llevan su tiempo de comida.
@Composable
private fun FormularioBanco(
    inicial: ItemBanco,
    guardando: Boolean,
    @StringRes error: Int?,
    onGuardar: (ItemBanco) -> Unit,
    onEliminar: () -> Unit,
    onVolver: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    var nombre by remember { mutableStateOf(inicial.nombre) }
    var texto by remember { mutableStateOf(inicial.texto) }
    var momento by remember { mutableStateOf(inicial.momento ?: MomentoComida.DESAYUNO) }
    var confirmando by remember { mutableStateOf(false) }
    val comida = inicial.tipo == TipoBanco.COMIDA
    val nuevo = inicial.id.isEmpty()

    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding().imePadding()) {
        BarraSuperior(
            titulo = stringResource(
                when {
                    comida && nuevo -> R.string.banco_form_nueva_comida
                    comida -> R.string.banco_form_editar_comida
                    nuevo -> R.string.banco_form_nuevo_ejercicio
                    else -> R.string.banco_form_editar_ejercicio
                },
            ),
            onVolver = onVolver,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MARGEN - ANILLO_FOCO),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CampoTexto(
                etiqueta = stringResource(if (comida) R.string.banco_nombre_comida else R.string.banco_nombre_ejercicio),
                valor = nombre,
                onValorChange = { nombre = it.take(ItemBanco.MAX_NOMBRE) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (comida) {
                Column(Modifier.padding(horizontal = ANILLO_FOCO), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.banco_momento), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MomentoComida.entries.forEach { OpcionChip(stringResource(it.titulo()), momento == it) { momento = it } }
                    }
                }
            }
            CampoTexto(
                etiqueta = stringResource(if (comida) R.string.banco_texto_comida else R.string.banco_texto_ejercicio),
                valor = texto,
                onValorChange = { texto = it.take(PlanContenido.MAX_MENU) },
                modifier = Modifier.fillMaxWidth(),
                lineas = 8,
            )
            error?.let { AlertaError(stringResource(it), Modifier.padding(horizontal = ANILLO_FOCO)) }
            if (!nuevo) {
                TextButton(onClick = { confirmando = true }, Modifier.padding(horizontal = ANILLO_FOCO)) {
                    Text(stringResource(R.string.banco_eliminar), color = colores.error)
                }
            }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(MARGEN)) {
            BotonPrimario(
                texto = stringResource(R.string.banco_guardar),
                onClick = {
                    onGuardar(inicial.copy(nombre = nombre.trim(), texto = texto.trim(), momento = if (comida) momento else null))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = nombre.isNotBlank() && texto.isNotBlank(),
                cargando = guardando,
            )
        }
    }

    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            containerColor = colores.background,
            title = { Text(stringResource(R.string.banco_eliminar_titulo)) },
            text = { Text(stringResource(R.string.banco_eliminar_texto, inicial.nombre)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmando = false
                    onEliminar()
                }) { Text(stringResource(R.string.banco_eliminar), color = colores.error) }
            },
            dismissButton = { TextButton(onClick = { confirmando = false }) { Text(stringResource(R.string.cita_dialogo_volver)) } },
        )
    }
}
