package com.example.nutrigajo.ui.shared.platillos

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.EtiquetaPlatillo
import com.example.nutrigajo.domain.model.MomentoComida
import com.example.nutrigajo.domain.model.Platillo
import com.example.nutrigajo.domain.model.kcalNoCuadran
import com.example.nutrigajo.domain.model.kcalSugeridas
import com.example.nutrigajo.domain.model.problemaParaRevision
import com.example.nutrigajo.domain.model.puedeGuardarseComoBorrador
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.entrenador.ejercicios.rememberVistaPrevia
import com.example.nutrigajo.ui.nutriologo.plan.BarraSuperior
import com.example.nutrigajo.ui.nutriologo.plan.OpcionChip
import com.example.nutrigajo.ui.nutriologo.plan.titulo
import com.example.nutrigajo.ui.shared.ejercicios.FotoOMarcador
import com.example.nutrigajo.ui.shared.ejercicios.GrupoOpciones

private val MARGEN = 16.dp
private const val MINUTOS_POR_DEFECTO = 15

// Crear (id vacío) o editar un platillo propio. Un borrador solo pide el nombre; para mandarlo a revisión el resto tiene que estar
// completo y con foto. La foto elegida se sube al guardar. Se avisa si las kcal no cuadran con los macros (4·P + 4·C + 9·G).
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorPlatillo(
    inicial: Platillo,
    guardando: Boolean,
    @StringRes error: Int?,
    onGuardar: (platillo: Platillo, enviarARevision: Boolean, foto: Uri?) -> Unit,
    onVolver: () -> Unit,
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    var nombre by remember { mutableStateOf(inicial.nombre) }
    var resumen by remember { mutableStateOf(inicial.resumen) }
    var momento by remember { mutableStateOf(inicial.momento) }
    var proteina by remember { mutableStateOf(inicial.proteinaG.takeIf { it > 0 }?.toString().orEmpty()) }
    var grasas by remember { mutableStateOf(inicial.grasasG.takeIf { it > 0 }?.toString().orEmpty()) }
    var carbos by remember { mutableStateOf(inicial.carbosG.takeIf { it > 0 }?.toString().orEmpty()) }
    var kcal by remember { mutableStateOf(inicial.kcal.takeIf { it > 0 }?.toString().orEmpty()) }
    var minutos by remember { mutableStateOf(inicial.minutos.toString()) }
    val ingredientes = remember { mutableStateListOf<String>().apply { addAll(inicial.ingredientes.ifEmpty { listOf("") }) } }
    val pasos = remember { mutableStateListOf<String>().apply { addAll(inicial.pasos.ifEmpty { listOf("") }) } }
    val etiquetas = remember { mutableStateListOf<EtiquetaPlatillo>().apply { addAll(inicial.etiquetas) } }
    var foto by remember { mutableStateOf<Uri?>(null) }
    // qué botón se tocó, para que el indicador de progreso salga en ese
    var enviando by remember { mutableStateOf(false) }
    val nuevo = inicial.id.isEmpty()

    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) foto = uri }
    val vistaPrevia = rememberVistaPrevia(foto)
    val fotoGuardada = rememberFotoPlatillo(inicial)

    // lo escrito tal cual, para comprobar si ya se puede mandar a revisión (la foto elegida cuenta aunque aún no se haya subido)
    val escrito = inicial.copy(
        nombre = nombre,
        resumen = resumen,
        momento = momento,
        proteinaG = proteina.toIntOrNull() ?: 0,
        grasasG = grasas.toIntOrNull() ?: 0,
        carbosG = carbos.toIntOrNull() ?: 0,
        kcal = kcal.toIntOrNull() ?: 0,
        minutos = minutos.toIntOrNull() ?: 0,
        ingredientes = ingredientes.toList(),
        pasos = pasos.toList(),
        etiquetas = etiquetas.toList(),
        fotoVersion = inicial.fotoVersion ?: if (foto != null) 0L else null,
    )
    val problema = escrito.problemaParaRevision()

    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding().imePadding()) {
        BarraSuperior(
            titulo = stringResource(if (nuevo) R.string.platillo_form_nuevo else R.string.platillo_form_editar),
            onVolver = onVolver,
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = MARGEN - ANILLO_FOCO),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.padding(horizontal = ANILLO_FOCO), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.ejercicio_foto), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
                FotoOMarcador(vistaPrevia ?: fotoGuardada, Modifier.fillMaxWidth().aspectRatio(16f / 10f), RoundedCornerShape(24.dp))
                OutlinedButton(
                    onClick = { elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    shape = RoundedCornerShape(21.dp),
                ) {
                    Text(stringResource(if (foto != null || inicial.fotoVersion != null) R.string.ejercicio_cambiar_foto else R.string.ejercicio_elegir_foto))
                }
            }
            CampoTexto(
                etiqueta = stringResource(R.string.platillo_nombre),
                valor = nombre,
                onValorChange = { nombre = it.take(Platillo.MAX_NOMBRE) },
                modifier = Modifier.fillMaxWidth(),
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            CampoTexto(
                etiqueta = stringResource(R.string.platillo_resumen),
                valor = resumen,
                onValorChange = { resumen = it.take(Platillo.MAX_RESUMEN) },
                modifier = Modifier.fillMaxWidth(),
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                lineas = 3,
            )
            GrupoOpciones(
                stringResource(R.string.platillo_momento),
                MomentoComida.entries,
                momento,
                { it.titulo() },
                { momento = it ?: momento },
                Modifier.padding(horizontal = ANILLO_FOCO),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoNumero(R.string.platillo_campo_proteina, proteina, 3, { proteina = it }, Modifier.weight(1f))
                CampoNumero(R.string.platillo_campo_grasas, grasas, 3, { grasas = it }, Modifier.weight(1f))
                CampoNumero(R.string.platillo_campo_carbos, carbos, 3, { carbos = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoNumero(R.string.platillo_campo_kcal, kcal, 4, { kcal = it }, Modifier.weight(1f))
                CampoNumero(R.string.platillo_campo_minutos, minutos, 3, { minutos = it }, Modifier.weight(1f))
            }
            if (escrito.kcalNoCuadran()) {
                Text(
                    text = stringResource(R.string.platillo_kcal_no_cuadran, kcalSugeridas(escrito.proteinaG, escrito.grasasG, escrito.carbosG)),
                    modifier = Modifier.padding(horizontal = ANILLO_FOCO),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.error,
                )
            }
            Text(stringResource(R.string.platillo_etiquetas_titulo), Modifier.padding(horizontal = ANILLO_FOCO), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
            FlowRow(
                modifier = Modifier.padding(horizontal = ANILLO_FOCO),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EtiquetaPlatillo.entries.forEach { etiqueta ->
                    val puesta = etiqueta in etiquetas
                    OpcionChip(stringResource(etiqueta.titulo()), puesta) {
                        if (puesta) etiquetas.remove(etiqueta) else if (etiquetas.size < Platillo.MAX_ETIQUETAS) etiquetas.add(etiqueta)
                    }
                }
            }
            Text(stringResource(R.string.platillo_ingredientes), Modifier.padding(horizontal = ANILLO_FOCO), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
            Lista(ingredientes, R.string.platillo_ingrediente_n, R.string.platillo_quitar_ingrediente, Platillo.MAX_INGREDIENTE, lineas = 1)
            if (ingredientes.size < Platillo.MAX_INGREDIENTES) {
                TextButton(onClick = { ingredientes.add("") }, Modifier.padding(horizontal = ANILLO_FOCO)) { Text(stringResource(R.string.platillo_agregar_ingrediente)) }
            }
            Text(stringResource(R.string.platillo_preparacion), Modifier.padding(horizontal = ANILLO_FOCO), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
            Lista(pasos, R.string.ejercicio_paso_n, R.string.ejercicio_quitar_paso, Platillo.MAX_PASO, lineas = 2)
            if (pasos.size < Platillo.MAX_PASOS) {
                TextButton(onClick = { pasos.add("") }, Modifier.padding(horizontal = ANILLO_FOCO)) { Text(stringResource(R.string.ejercicio_agregar_paso)) }
            }
            error?.let { AlertaError(stringResource(it), Modifier.padding(horizontal = ANILLO_FOCO)) }
        }
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(MARGEN),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            problema?.let { Text(stringResource(it.mensaje()), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant) }
            BotonPrimario(
                texto = stringResource(R.string.ejercicio_enviar_revision),
                onClick = {
                    enviando = true
                    onGuardar(escrito.paraGuardar(), true, foto)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = problema == null && !guardando,
                cargando = guardando && enviando,
            )
            OutlinedButton(
                onClick = {
                    enviando = false
                    onGuardar(escrito.paraGuardar(), false, foto)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = escrito.puedeGuardarseComoBorrador() && !guardando,
                shape = RoundedCornerShape(21.dp),
            ) { Text(stringResource(R.string.ejercicio_guardar_borrador)) }
        }
    }
}

// Un campo de solo dígitos con su tope de cifras.
@Composable
private fun CampoNumero(@StringRes etiqueta: Int, valor: String, cifras: Int, onCambio: (String) -> Unit, modifier: Modifier = Modifier) {
    CampoTexto(
        etiqueta = stringResource(etiqueta),
        valor = valor,
        onValorChange = { onCambio(it.filter(Char::isDigit).take(cifras)) },
        modifier = modifier,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

// Una lista de textos que crece (ingredientes o pasos): un campo por elemento con su botón de quitar mientras quede más de uno.
@Composable
private fun Lista(elementos: MutableList<String>, @StringRes etiqueta: Int, @StringRes quitar: Int, tope: Int, lineas: Int) {
    val colores = MaterialTheme.colorScheme
    elementos.forEachIndexed { i, texto ->
        CampoTexto(
            etiqueta = stringResource(etiqueta, i + 1),
            valor = texto,
            onValorChange = { elementos[i] = it.take(tope) },
            modifier = Modifier.fillMaxWidth(),
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            extremo = {
                if (elementos.size > 1) {
                    IconButton(onClick = { elementos.removeAt(i) }) {
                        Icon(painterResource(R.drawable.ic_cerrar), stringResource(quitar), tint = colores.onSurfaceVariant)
                    }
                }
            },
            lineas = lineas,
        )
    }
}

// Un borrador no tiene que estar completo, pero las reglas piden los números dentro de su rango.
private fun Platillo.paraGuardar(): Platillo = copy(
    proteinaG = proteinaG.coerceIn(0, Platillo.MAX_MACRO_G),
    grasasG = grasasG.coerceIn(0, Platillo.MAX_MACRO_G),
    carbosG = carbosG.coerceIn(0, Platillo.MAX_MACRO_G),
    kcal = kcal.coerceIn(0, Platillo.MAX_KCAL),
    minutos = if (minutos in Platillo.MIN_MINUTOS..Platillo.MAX_MINUTOS) minutos else if (minutos > 0) Platillo.MAX_MINUTOS else MINUTOS_POR_DEFECTO,
)
