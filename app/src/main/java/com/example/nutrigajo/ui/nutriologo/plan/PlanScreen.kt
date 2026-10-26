package com.example.nutrigajo.ui.nutriologo.plan

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.data.pdf.GeneradorPlanPdf
import com.example.nutrigajo.domain.model.Equivalentes
import com.example.nutrigajo.domain.model.GrupoAlimento
import com.example.nutrigajo.domain.model.Objetivo
import com.example.nutrigajo.domain.model.OpcionMenu
import com.example.nutrigajo.domain.model.PlanContenido
import com.example.nutrigajo.domain.model.RangoImc
import com.example.nutrigajo.domain.model.SesionEjercicio
import com.example.nutrigajo.domain.model.rangoImc
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.components.PantallaPdf
import com.example.nutrigajo.ui.components.SelectorEtiquetas
import com.example.nutrigajo.ui.theme.color
import com.example.nutrigajo.ui.theme.titulo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val MARGEN = 16.dp

// El panel donde el nutriólogo arma el plan de alimentación y de ejercicio de un paciente: datos clave, mensaje, metas,
// equivalentes por tiempo de comida, opciones de menú y rutina. Lo ve como PDF y desde ahí lo envía al paciente.
@Composable
fun PlanScreen(
    pacienteId: String,
    nombrePaciente: String,
    nombreNutriologo: String,
    onSalir: () -> Unit,
    vm: PlanViewModel = viewModel(key = "plan-$pacienteId"),
) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current
    val generador = remember { GeneradorPlanPdf(contexto.applicationContext) }
    var confirmando by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(pacienteId) {
        vm.cargar(pacienteId, nombrePaciente, nombreNutriologo) { objetivo -> saludoInicial(contexto.applicationContext, nombrePaciente, nombreNutriologo, objetivo) }
    }
    val previa = estado.previa

    when {
        estado.cargando -> PantallaCarga()
        previa != null -> PantallaPdf(
            titulo = stringResource(R.string.plan_previa),
            archivo = previa,
            onVolver = vm::cerrarPrevia,
            abajo = {
                estado.error?.let { AlertaError(stringResource(it), Modifier.padding(bottom = 12.dp)) }
                BotonPrimario(
                    texto = stringResource(R.string.plan_enviar),
                    onClick = { confirmando = true },
                    modifier = Modifier.fillMaxWidth(),
                    cargando = estado.enviando,
                )
                TextButton(onClick = vm::cerrarPrevia, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.plan_seguir_editando)) }
            },
        )
        else -> {
            BackHandler(onBack = onSalir)
            Editor(estado, vm, nombrePaciente, onSalir, onGenerar = { vm.verPrevia(generador, contexto.cacheDir) })
        }
    }

    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text(stringResource(R.string.plan_confirmar_titulo)) },
            text = { Text(stringResource(R.string.plan_confirmar_texto, nombrePaciente)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmando = false
                    vm.enviar(generador, contexto.cacheDir, pacienteId)
                }) { Text(stringResource(R.string.plan_confirmar_si)) }
            },
            dismissButton = { TextButton(onClick = { confirmando = false }) { Text(stringResource(R.string.cita_dialogo_volver)) } },
        )
    }
    if (estado.enviado) {
        AlertDialog(
            onDismissRequest = onSalir,
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text(stringResource(R.string.plan_enviado_titulo)) },
            text = {
                Text(stringResource(if (estado.avisado) R.string.plan_enviado_avisado else R.string.plan_enviado_sin_aviso, nombrePaciente))
            },
            confirmButton = { TextButton(onClick = onSalir) { Text(stringResource(R.string.plan_enviado_listo)) } },
        )
    }
}

// Saludo con el que parte el plan; el nutriólogo lo cambia como quiera.
private fun saludoInicial(contexto: Context, paciente: String, nutriologo: String, objetivo: Objetivo?): String {
    val frase = when (objetivo) {
        Objetivo.BAJAR_PESO -> R.string.plan_saludo_bajar_peso
        Objetivo.GANAR_MUSCULO -> R.string.plan_saludo_ganar_musculo
        Objetivo.MEJORAR_ALIMENTACION -> R.string.plan_saludo_mejorar_alimentacion
        Objetivo.MEJORAR_RESISTENCIA -> R.string.plan_saludo_mejorar_resistencia
        Objetivo.CONDICION_SALUD -> R.string.plan_saludo_condicion_salud
        null -> null
    }?.let { contexto.getString(it) }
    return contexto.getString(R.string.plan_saludo, paciente.trim().substringBefore(' '), nutriologo, frase.orEmpty()).replace("  ", " ")
}

@Composable
private fun Editor(estado: PlanEstado, vm: PlanViewModel, nombrePaciente: String, onSalir: () -> Unit, onGenerar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val plan = estado.contenido
    val editar = vm::editar

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onSalir) { Text(stringResource(R.string.mapa_volver)) }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MARGEN - ANILLO_FOCO),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(Modifier.padding(horizontal = ANILLO_FOCO), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.plan_titulo, nombrePaciente), style = MaterialTheme.typography.headlineMedium, color = colores.onBackground)
                Text(stringResource(R.string.plan_intro), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
                estado.ultimoEnvio?.let {
                    Text(
                        text = stringResource(R.string.plan_ultimo_envio, remember(it) { fechaCorta(it) }),
                        style = MaterialTheme.typography.bodySmall,
                        color = colores.onSurfaceVariant,
                    )
                }
            }

            Seccion(R.string.plan_sec_datos) {
                CampoNumero(R.string.plan_peso, plan.pesoKg) { v -> editar { it.copy(pesoKg = v) } }
                CampoNumero(R.string.plan_talla, plan.tallaCm) { v -> editar { it.copy(tallaCm = v) } }
                plan.imc?.let { imc ->
                    Text(
                        text = stringResource(R.string.plan_imc, "%.1f".format(Locale.US, imc), stringResource(nombreRango(rangoImc(imc)))),
                        modifier = Modifier.padding(horizontal = ANILLO_FOCO),
                        style = MaterialTheme.typography.labelLarge,
                        color = colores.onSurfaceVariant,
                    )
                }
                CampoNumero(R.string.plan_cintura, plan.cinturaCm) { v -> editar { it.copy(cinturaCm = v) } }
                CampoNumero(R.string.plan_cadera, plan.caderaCm) { v -> editar { it.copy(caderaCm = v) } }
                CampoNumero(R.string.plan_grasa, plan.grasaPct) { v -> editar { it.copy(grasaPct = v) } }
                CampoTexto(
                    etiqueta = stringResource(R.string.plan_seguimiento),
                    valor = plan.seguimiento,
                    onValorChange = { v -> editar { it.copy(seguimiento = v.take(60)) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Seccion(R.string.plan_sec_mensaje) {
                CampoTexto(
                    etiqueta = stringResource(R.string.plan_mensaje_campo),
                    valor = plan.mensaje,
                    onValorChange = { v -> editar { it.copy(mensaje = v.take(PlanContenido.MAX_TEXTO)) } },
                    modifier = Modifier.fillMaxWidth(),
                    lineas = 7,
                )
            }

            Seccion(R.string.plan_sec_metas) {
                SelectorEtiquetas(
                    comunes = stringArrayResource(R.array.plan_metas_comunes).toList(),
                    seleccionadas = plan.metas,
                    onCambio = { v -> editar { it.copy(metas = v) } },
                    etiquetaCampo = stringResource(R.string.plan_meta_campo),
                    etiquetaComunes = stringResource(R.string.plan_meta_comunes),
                    etiquetaAgregar = stringResource(R.string.plan_meta_agregar),
                    modifier = Modifier.padding(horizontal = ANILLO_FOCO),
                )
            }

            Seccion(R.string.plan_sec_alimentacion, R.string.plan_alimentacion_ayuda) {
                CampoEntero(R.string.plan_calorias, plan.calorias) { v -> editar { it.copy(calorias = v) } }
                GrupoAlimento.entries.forEach { grupo ->
                    TarjetaGrupo(grupo, plan.equivalentes[grupo] ?: Equivalentes()) { nuevo ->
                        editar { it.copy(equivalentes = it.equivalentes + (grupo to nuevo)) }
                    }
                }
            }

            Seccion(R.string.plan_sec_menu, R.string.plan_menu_ayuda) {
                plan.opciones.forEachIndexed { i, opcion ->
                    TarjetaOpcion(
                        numero = i + 1,
                        opcion = opcion,
                        onCambio = { nueva -> editar { it.copy(opciones = it.opciones.mapIndexed { j, o -> if (j == i) nueva else o }) } },
                        onQuitar = { editar { it.copy(opciones = it.opciones.filterIndexed { j, _ -> j != i }) } },
                    )
                }
                if (plan.opciones.size < PlanContenido.MAX_OPCIONES) {
                    Agregar(R.string.plan_agregar_opcion) { editar { it.copy(opciones = it.opciones + OpcionMenu()) } }
                }
            }

            Seccion(R.string.plan_sec_ejercicio, R.string.plan_ejercicio_ayuda) {
                CampoTexto(
                    etiqueta = stringResource(R.string.plan_ejercicio_notas),
                    valor = plan.ejercicioNotas,
                    onValorChange = { v -> editar { it.copy(ejercicioNotas = v.take(PlanContenido.MAX_TEXTO)) } },
                    modifier = Modifier.fillMaxWidth(),
                    lineas = 4,
                )
                plan.sesiones.forEachIndexed { i, sesion ->
                    TarjetaSesion(
                        numero = i + 1,
                        sesion = sesion,
                        onCambio = { nueva -> editar { it.copy(sesiones = it.sesiones.mapIndexed { j, s -> if (j == i) nueva else s }) } },
                        onQuitar = { editar { it.copy(sesiones = it.sesiones.filterIndexed { j, _ -> j != i }) } },
                    )
                }
                if (plan.sesiones.size < PlanContenido.MAX_SESIONES) {
                    Agregar(R.string.plan_agregar_sesion) { editar { it.copy(sesiones = it.sesiones + SesionEjercicio()) } }
                }
            }

            Seccion(R.string.plan_sec_notas) {
                CampoTexto(
                    etiqueta = stringResource(R.string.plan_notas_campo),
                    valor = plan.notas,
                    onValorChange = { v -> editar { it.copy(notas = v.take(PlanContenido.MAX_TEXTO)) } },
                    modifier = Modifier.fillMaxWidth(),
                    lineas = 4,
                )
            }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(MARGEN), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.error?.let { AlertaError(stringResource(it)) }
            BotonPrimario(
                texto = stringResource(R.string.plan_generar),
                onClick = onGenerar,
                modifier = Modifier.fillMaxWidth(),
                cargando = estado.generando,
            )
        }
    }
}

@StringRes
private fun nombreRango(r: RangoImc): Int = when (r) {
    RangoImc.BAJO_PESO -> R.string.pdf_imc_bajo
    RangoImc.SALUDABLE -> R.string.pdf_imc_saludable
    RangoImc.SOBREPESO -> R.string.pdf_imc_sobrepeso
    RangoImc.OBESIDAD_I -> R.string.pdf_imc_obesidad1
    RangoImc.OBESIDAD_II -> R.string.pdf_imc_obesidad2
}

private fun fechaCorta(millis: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

// Título de sección con su ayuda y sus campos debajo.
@Composable
private fun Seccion(@StringRes titulo: Int, @StringRes ayuda: Int? = null, contenido: @Composable () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(horizontal = ANILLO_FOCO), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(titulo), style = MaterialTheme.typography.titleLarge, color = colores.onBackground)
            ayuda?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant) }
        }
        contenido()
    }
}

// Número con decimales (peso, medidas): el texto se escribe libre y solo se guarda lo que es un número.
@Composable
private fun CampoNumero(@StringRes etiqueta: Int, valor: Double?, onValor: (Double?) -> Unit) {
    var texto by rememberSaveable { mutableStateOf(valor?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    CampoTexto(
        etiqueta = stringResource(etiqueta),
        valor = texto,
        onValorChange = { nuevo ->
            texto = nuevo.filter { it.isDigit() || it == '.' }.take(6)
            onValor(texto.toDoubleOrNull())
        },
        modifier = Modifier.fillMaxWidth(),
        teclado = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun CampoEntero(@StringRes etiqueta: Int, valor: Int?, onValor: (Int?) -> Unit) {
    var texto by rememberSaveable { mutableStateOf(valor?.toString() ?: "") }
    CampoTexto(
        etiqueta = stringResource(etiqueta),
        valor = texto,
        onValorChange = { nuevo ->
            texto = nuevo.filter { it.isDigit() }.take(5)
            onValor(texto.toIntOrNull())
        },
        modifier = Modifier.fillMaxWidth(),
        teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

// Tarjeta blanca con borde fino, como las del perfil.
@Composable
private fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .padding(horizontal = ANILLO_FOCO)
            .fillMaxWidth()
            .background(colores.background, forma)
            .border(1.dp, colores.outlineVariant, forma)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { contenido() }
}

// Un grupo de alimentos con sus porciones en desayuno, comida y cena.
@Composable
private fun TarjetaGrupo(grupo: GrupoAlimento, equivalentes: Equivalentes, onCambio: (Equivalentes) -> Unit) {
    val colores = MaterialTheme.colorScheme
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(grupo.color)))
            Text(stringResource(grupo.titulo), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
            Text(
                text = pluralStringResource(R.plurals.plan_total_equivalentes, equivalentes.total, equivalentes.total),
                style = MaterialTheme.typography.labelLarge,
                color = colores.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Paso(R.string.pdf_col_desayuno, equivalentes.desayuno, Modifier.weight(1f)) { onCambio(equivalentes.copy(desayuno = it)) }
            Paso(R.string.pdf_col_comida, equivalentes.comida, Modifier.weight(1f)) { onCambio(equivalentes.copy(comida = it)) }
            Paso(R.string.pdf_col_cena, equivalentes.cena, Modifier.weight(1f)) { onCambio(equivalentes.copy(cena = it)) }
        }
    }
}

// Número entero con − y + para las porciones de un tiempo de comida.
@Composable
private fun Paso(@StringRes etiqueta: Int, valor: Int, modifier: Modifier, onCambio: (Int) -> Unit) {
    val colores = MaterialTheme.colorScheme
    val nombre = stringResource(etiqueta)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(nombre, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).background(colores.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotonPaso("−", stringResource(R.string.plan_menos, nombre), Modifier.weight(1f), enabled = valor > 0) { onCambio(valor - 1) }
            Text(valor.toString(), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
            BotonPaso("+", stringResource(R.string.plan_mas, nombre), Modifier.weight(1f), enabled = valor < 20) { onCambio(valor + 1) }
        }
    }
}

@Composable
private fun BotonPaso(simbolo: String, descripcion: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(44.dp)
            .semantics { contentDescription = descripcion }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(simbolo, style = MaterialTheme.typography.titleLarge, color = if (enabled) colores.primary else colores.outline)
    }
}

@Composable
private fun TarjetaOpcion(numero: Int, opcion: OpcionMenu, onCambio: (OpcionMenu) -> Unit, onQuitar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.pdf_opcion, numero), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
            TextButton(onClick = onQuitar) { Text(stringResource(R.string.plan_quitar), color = colores.error) }
        }
        CampoTexto(stringResource(R.string.pdf_col_desayuno), opcion.desayuno, { onCambio(opcion.copy(desayuno = it.take(PlanContenido.MAX_MENU))) }, modifier = Modifier.fillMaxWidth(), lineas = 3)
        CampoTexto(stringResource(R.string.pdf_col_comida), opcion.comida, { onCambio(opcion.copy(comida = it.take(PlanContenido.MAX_MENU))) }, modifier = Modifier.fillMaxWidth(), lineas = 3)
        CampoTexto(stringResource(R.string.pdf_col_cena), opcion.cena, { onCambio(opcion.copy(cena = it.take(PlanContenido.MAX_MENU))) }, modifier = Modifier.fillMaxWidth(), lineas = 3)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaSesion(numero: Int, sesion: SesionEjercicio, onCambio: (SesionEjercicio) -> Unit, onQuitar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.plan_sesion, numero), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
            TextButton(onClick = onQuitar) { Text(stringResource(R.string.plan_quitar), color = colores.error) }
        }
        Text(stringResource(R.string.plan_dia), style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stringArrayResource(R.array.plan_dias).forEach { dia ->
                val elegido = dia == sesion.dia
                Text(
                    text = dia,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (elegido) colores.primary else colores.surfaceVariant)
                        // volver a tocar el elegido lo quita: la sesión también puede no tener día fijo
                        .selectable(selected = elegido, role = Role.RadioButton, onClick = { onCambio(sesion.copy(dia = if (elegido) "" else dia)) })
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (elegido) colores.onPrimary else colores.onBackground,
                )
            }
        }
        CampoTexto(stringResource(R.string.plan_enfoque), sesion.enfoque, { onCambio(sesion.copy(enfoque = it.take(80))) }, modifier = Modifier.fillMaxWidth())
        CampoTexto(stringResource(R.string.plan_detalle), sesion.detalle, { onCambio(sesion.copy(detalle = it.take(PlanContenido.MAX_MENU))) }, modifier = Modifier.fillMaxWidth(), lineas = 4)
    }
}

@Composable
private fun Agregar(@StringRes texto: Int, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Text(
        text = stringResource(texto),
        modifier = Modifier
            .padding(horizontal = ANILLO_FOCO)
            .fillMaxWidth()
            .clip(RoundedCornerShape(19.dp))
            .background(colores.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp),
        style = MaterialTheme.typography.titleMedium,
        color = colores.primary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}
