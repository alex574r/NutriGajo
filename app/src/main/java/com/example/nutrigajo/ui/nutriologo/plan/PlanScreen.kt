package com.example.nutrigajo.ui.nutriologo.plan

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.data.pdf.GeneradorPlanPdf
import com.example.nutrigajo.domain.model.Equivalentes
import com.example.nutrigajo.domain.model.GrupoAlimento
import com.example.nutrigajo.domain.model.ItemBanco
import com.example.nutrigajo.domain.model.MomentoComida
import com.example.nutrigajo.domain.model.Objetivo
import com.example.nutrigajo.domain.model.OpcionMenu
import com.example.nutrigajo.domain.model.PlanContenido
import com.example.nutrigajo.domain.model.RangoImc
import com.example.nutrigajo.domain.model.SesionEjercicio
import com.example.nutrigajo.domain.model.TipoBanco
import com.example.nutrigajo.domain.model.agregando
import com.example.nutrigajo.domain.model.con
import com.example.nutrigajo.domain.model.de
import com.example.nutrigajo.domain.model.nombreSugerido
import com.example.nutrigajo.domain.model.rangoImc
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoTexto
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.components.PantallaPdf
import com.example.nutrigajo.ui.components.SelectorEtiquetas
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.components.aparece
import com.example.nutrigajo.ui.theme.alineacionFoto
import com.example.nutrigajo.ui.theme.foto
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
    bancoVm: BancoViewModel = viewModel(),
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
            Editor(estado, vm, bancoVm, nombrePaciente, onSalir, onGenerar = { vm.verPrevia(generador, contexto.cacheDir) })
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

// El panel en dos niveles: la lista de secciones (cada una con su foto y lo que ya tiene) y, al tocar una, sus campos.
@Composable
private fun Editor(
    estado: PlanEstado,
    vm: PlanViewModel,
    bancoVm: BancoViewModel,
    nombrePaciente: String,
    onSalir: () -> Unit,
    onGenerar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    // la sección abierta (su posición en SeccionPlan); null: la lista
    var abierta by rememberSaveable { mutableStateOf<Int?>(null) }
    BackHandler(enabled = abierta != null) { abierta = null }
    // la lista conserva su desplazamiento al volver de una sección
    val desplazamiento = rememberScrollState()
    // la lista entra en cascada solo la primera vez
    var yaEntro by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { yaEntro = true }
    // el banco se abre encima de la sección, para volver a ella (y a su desplazamiento) al elegir o salir
    var destino by remember { mutableStateOf<DestinoBanco?>(null) }
    val acceso = remember(bancoVm) {
        AccesoBanco(
            abrir = { tipo, momento, elegir -> destino = DestinoBanco(tipo, momento) { item -> destino = null; elegir(item) } },
            guardar = { item, fin ->
                bancoVm.guardar(item) { bien ->
                    Toast.makeText(contexto, if (bien) R.string.banco_guardado else R.string.banco_error_guardar, Toast.LENGTH_SHORT).show()
                    fin(bien)
                }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = abierta,
            modifier = Modifier.fillMaxSize().background(colores.background),
            transitionSpec = {
                // la sección entra desde la derecha y la lista, desde la izquierda
                val sentido = if (targetState != null) 1 else -1
                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { sentido * it / 5 } + fadeIn(tween(220, delayMillis = 30))) togetherWith
                    (slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { -sentido * it / 5 } + fadeOut(tween(120)))
            },
            label = "seccion",
        ) { indice ->
            if (indice == null) {
                ListaSecciones(
                    estado, nombrePaciente, desplazamiento, onSalir, onGenerar,
                    onAbrir = { abierta = it.ordinal },
                    onBanco = { destino = DestinoBanco(TipoBanco.COMIDA) },
                    animarEntrada = !yaEntro,
                )
            } else {
                DetalleSeccion(SeccionPlan.entries[indice], estado.contenido, vm::editar, nombrePaciente, acceso, onCerrar = { abierta = null })
            }
        }
        AnimatedContent(
            targetState = destino,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (targetState != null) {
                    (slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 6 } + fadeIn(tween(220))) togetherWith ExitTransition.None
                } else {
                    EnterTransition.None togetherWith (slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it / 6 } + fadeOut(tween(160)))
                }
            },
            label = "banco",
        ) { actual ->
            if (actual != null) BancoScreen(actual, bancoVm, onSalir = { destino = null })
        }
    }
}

// Lo que el panel del plan hace con el banco: abrirlo para elegir algo (de ese tipo y, en comidas, de ese tiempo) que se pone
// en el plan, y guardar en él algo escrito en el plan (avisa si salió bien).
private class AccesoBanco(
    val abrir: (TipoBanco, MomentoComida?, (ItemBanco) -> Unit) -> Unit,
    val guardar: (ItemBanco, (Boolean) -> Unit) -> Unit,
)

@Composable
private fun ListaSecciones(
    estado: PlanEstado,
    nombrePaciente: String,
    desplazamiento: ScrollState,
    onSalir: () -> Unit,
    onGenerar: () -> Unit,
    onAbrir: (SeccionPlan) -> Unit,
    onBanco: () -> Unit,
    // la primera vez la lista llega en cascada; al volver de una sección ya está puesta
    animarEntrada: Boolean,
) {
    val colores = MaterialTheme.colorScheme
    val resumenes = SeccionPlan.entries.map { resumen(it, estado.contenido) }
    val llenas = resumenes.count { it != null }
    // la barra se llena con resorte cuando una sección gana o pierde contenido
    val avance by animateFloatAsState(llenas / resumenes.size.toFloat(), spring(dampingRatio = 0.6f, stiffness = 120f), label = "avance")

    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onSalir) { Text(stringResource(R.string.mapa_volver)) }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(desplazamiento)
                .padding(horizontal = MARGEN),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.aparece(0, animarEntrada), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            // cuántas secciones ya tienen algo
            Column(Modifier.aparece(1, animarEntrada), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // el número sube o baja como un rodillo según se gane o se pierda una sección
                AnimatedContent(
                    targetState = llenas,
                    transitionSpec = {
                        val sube = targetState > initialState
                        (slideInVertically(tween(220)) { if (sube) it else -it } + fadeIn(tween(220))) togetherWith
                            (slideOutVertically(tween(220)) { if (sube) -it else it } + fadeOut(tween(120)))
                    },
                    label = "secciones",
                ) { n ->
                    Text(
                        text = stringResource(R.string.plan_secciones_llenas, n, resumenes.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = colores.onSurfaceVariant,
                    )
                }
                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colores.surfaceVariant)) {
                    Box(Modifier.fillMaxWidth(avance.coerceIn(0f, 1f)).fillMaxHeight().background(colores.primary))
                }
            }
            FilaAjuste(
                icono = R.drawable.ic_guardado,
                titulo = stringResource(R.string.plan_banco_entrada),
                modifier = Modifier.aparece(2, animarEntrada),
                etiqueta = stringResource(R.string.plan_banco_entrada_ayuda),
                onClick = onBanco,
            )
            SeccionPlan.entries.forEachIndexed { i, seccion ->
                TarjetaSeccion(seccion, resumenes[i], Modifier.aparece(3 + i, animarEntrada)) { onAbrir(seccion) }
            }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(MARGEN), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.error?.let { AlertaError(stringResource(it)) }
            BotonPrimario(
                texto = stringResource(R.string.plan_generar),
                onClick = onGenerar,
                modifier = Modifier.fillMaxWidth(),
                cargando = estado.generando,
                // mientras se arma el PDF el botón cuenta (Generate, de Bencho) en lugar de dar vueltas
                progreso = estado.progreso,
            )
        }
    }
}

// Los campos de una sección, bajo su foto. «listo» (o regresar) vuelve a la lista.
@Composable
private fun DetalleSeccion(
    seccion: SeccionPlan,
    plan: PlanContenido,
    editar: ((PlanContenido) -> PlanContenido) -> Unit,
    nombrePaciente: String,
    banco: AccesoBanco,
    onCerrar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding().imePadding()) {
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
                    .clickable(role = Role.Button, onClick = onCerrar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.expediente_regresar), tint = colores.onBackground)
            }
            Text(
                text = stringResource(R.string.plan_titulo, nombrePaciente),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = colores.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MARGEN - ANILLO_FOCO),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeroSeccion(seccion, 176.dp, Modifier.padding(horizontal = ANILLO_FOCO).clip(RoundedCornerShape(24.dp)), grande = true)
            when (seccion) {
                SeccionPlan.DATOS -> {
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
                SeccionPlan.MENSAJE -> CampoTexto(
                    etiqueta = stringResource(R.string.plan_mensaje_campo),
                    valor = plan.mensaje,
                    onValorChange = { v -> editar { it.copy(mensaje = v.take(PlanContenido.MAX_TEXTO)) } },
                    modifier = Modifier.fillMaxWidth(),
                    lineas = 7,
                )
                SeccionPlan.METAS -> SelectorEtiquetas(
                    comunes = stringArrayResource(R.array.plan_metas_comunes).toList(),
                    seleccionadas = plan.metas,
                    onCambio = { v -> editar { it.copy(metas = v) } },
                    etiquetaCampo = stringResource(R.string.plan_meta_campo),
                    etiquetaComunes = stringResource(R.string.plan_meta_comunes),
                    etiquetaAgregar = stringResource(R.string.plan_meta_agregar),
                    modifier = Modifier.padding(horizontal = ANILLO_FOCO),
                )
                SeccionPlan.ALIMENTACION -> {
                    CampoEntero(R.string.plan_calorias, plan.calorias) { v -> editar { it.copy(calorias = v) } }
                    GrupoAlimento.entries.forEach { grupo ->
                        TarjetaGrupo(grupo, plan.equivalentes[grupo] ?: Equivalentes()) { nuevo ->
                            editar { it.copy(equivalentes = it.equivalentes + (grupo to nuevo)) }
                        }
                    }
                }
                SeccionPlan.MENU -> {
                    plan.opciones.forEachIndexed { i, opcion ->
                        TarjetaOpcion(
                            numero = i + 1,
                            opcion = opcion,
                            onCambio = { nueva -> editar { it.copy(opciones = it.opciones.mapIndexed { j, o -> if (j == i) nueva else o }) } },
                            onQuitar = { editar { it.copy(opciones = it.opciones.filterIndexed { j, _ -> j != i }) } },
                            onUsarBanco = { momento ->
                                banco.abrir(TipoBanco.COMIDA, momento) { item ->
                                    editar { p ->
                                        p.copy(
                                            opciones = p.opciones.mapIndexed { j, o ->
                                                if (j == i) o.con(momento, o.de(momento).agregando(item.textoParaPlan(), PlanContenido.MAX_MENU)) else o
                                            },
                                        )
                                    }
                                }
                            },
                            onGuardarBanco = { momento, texto, fin ->
                                banco.guardar(
                                    ItemBanco(tipo = TipoBanco.COMIDA, nombre = nombreSugerido(texto), texto = texto.trim(), momento = momento),
                                    fin,
                                )
                            },
                        )
                    }
                    if (plan.opciones.size < PlanContenido.MAX_OPCIONES) {
                        Agregar(R.string.plan_agregar_opcion) { editar { it.copy(opciones = it.opciones + OpcionMenu()) } }
                    }
                }
                SeccionPlan.EJERCICIO -> {
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
                            onUsarBanco = {
                                banco.abrir(TipoBanco.EJERCICIO, null) { item ->
                                    editar { p ->
                                        p.copy(
                                            sesiones = p.sesiones.mapIndexed { j, s ->
                                                if (j == i) s.copy(enfoque = s.enfoque.ifBlank { item.nombre }, detalle = s.detalle.agregando(item.texto, PlanContenido.MAX_MENU)) else s
                                            },
                                        )
                                    }
                                }
                            },
                            onGuardarBanco = { enfoque, detalle, fin ->
                                banco.guardar(
                                    ItemBanco(tipo = TipoBanco.EJERCICIO, nombre = enfoque.trim().ifEmpty { nombreSugerido(detalle) }, texto = detalle.trim()),
                                    fin,
                                )
                            },
                        )
                    }
                    if (plan.sesiones.size < PlanContenido.MAX_SESIONES) {
                        Agregar(R.string.plan_agregar_sesion) { editar { it.copy(sesiones = it.sesiones + SesionEjercicio()) } }
                    }
                }
                SeccionPlan.NOTAS -> CampoTexto(
                    etiqueta = stringResource(R.string.plan_notas_campo),
                    valor = plan.notas,
                    onValorChange = { v -> editar { it.copy(notas = v.take(PlanContenido.MAX_TEXTO)) } },
                    modifier = Modifier.fillMaxWidth(),
                    lineas = 4,
                )
            }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(MARGEN)) {
            BotonPrimario(
                texto = stringResource(R.string.plan_listo),
                onClick = onCerrar,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
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
    val forma = RoundedCornerShape(22.dp)
    // la tarjeta lleva la foto del grupo en una franja arriba y los pasos debajo
    Column(
        modifier = Modifier
            .padding(horizontal = ANILLO_FOCO)
            .fillMaxWidth()
            .clip(forma)
            .background(colores.background)
            .border(1.dp, colores.outlineVariant, forma),
    ) {
        Box(Modifier.fillMaxWidth().height(92.dp)) {
            Image(
                painter = painterResource(grupo.foto),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alignment = grupo.alineacionFoto,
            )
            // el degradado oscuro deja leer el título sobre cualquier foto
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))))
            Row(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(grupo.titulo), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = Color.White)
                // el total sube o baja como un rodillo cuando cambia una porción
                AnimatedContent(
                    targetState = equivalentes.total,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
                    transitionSpec = {
                        val sube = targetState > initialState
                        (slideInVertically(tween(160)) { if (sube) it else -it } + fadeIn(tween(160))) togetherWith
                            (slideOutVertically(tween(160)) { if (sube) -it else it } + fadeOut(tween(100)))
                    },
                    label = "total",
                ) { total ->
                    Text(
                        text = pluralStringResource(R.plurals.plan_total_equivalentes, total, total),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
        }
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            // la porción sube o baja como un rodillo según el botón que se toque
            AnimatedContent(
                targetState = valor,
                transitionSpec = {
                    val sube = targetState > initialState
                    (slideInVertically(tween(160)) { if (sube) it else -it } + fadeIn(tween(160))) togetherWith
                        (slideOutVertically(tween(160)) { if (sube) -it else it } + fadeOut(tween(100)))
                },
                label = "porcion",
            ) { v -> Text(v.toString(), style = MaterialTheme.typography.titleMedium, color = colores.onBackground) }
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
            .alPulsar(onClick, enabled = enabled, minimo = 0.8f),
        contentAlignment = Alignment.Center,
    ) {
        Text(simbolo, style = MaterialTheme.typography.titleLarge, color = if (enabled) colores.primary else colores.outline)
    }
}

@Composable
private fun TarjetaOpcion(
    numero: Int,
    opcion: OpcionMenu,
    onCambio: (OpcionMenu) -> Unit,
    onQuitar: () -> Unit,
    // elegir una comida del banco para ese tiempo; guardar en él lo escrito en ese tiempo (con su aviso de si salió bien)
    onUsarBanco: (MomentoComida) -> Unit,
    onGuardarBanco: (MomentoComida, String, (Boolean) -> Unit) -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.pdf_opcion, numero), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
            TextButton(onClick = onQuitar) { Text(stringResource(R.string.plan_quitar), color = colores.error) }
        }
        MomentoComida.entries.forEach { momento ->
            val texto = opcion.de(momento)
            CampoTexto(stringResource(momento.titulo()), texto, { onCambio(opcion.con(momento, it.take(PlanContenido.MAX_MENU))) }, modifier = Modifier.fillMaxWidth(), lineas = 3)
            AccionesBanco(texto, onUsar = { onUsarBanco(momento) }, onGuardar = { fin -> onGuardarBanco(momento, texto, fin) })
        }
    }
}

// Bajo un campo del plan: elegir algo del banco y guardar en él lo escrito. Una vez guardado, el botón queda en «Guardado»
// hasta que el texto cambie.
@Composable
private fun AccionesBanco(texto: String, onUsar: () -> Unit, onGuardar: ((Boolean) -> Unit) -> Unit) {
    val colores = MaterialTheme.colorScheme
    var guardado by remember { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        if (texto.isNotBlank()) {
            // el botón se cambia por «Guardado» con un rebote: entra pequeño y se asienta con resorte
            AnimatedContent(
                targetState = guardado == texto,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = 0.5f, stiffness = 500f), initialScale = 0.8f) + fadeIn(tween(120))) togetherWith
                        (scaleOut(tween(120), targetScale = 0.8f) + fadeOut(tween(120)))
                },
                label = "guardado",
            ) { yaGuardado ->
                if (yaGuardado) {
                    Text(
                        text = stringResource(R.string.plan_banco_guardado),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = colores.onSurfaceVariant,
                    )
                } else {
                    PildoraAccion(R.string.plan_banco_guardar) { onGuardar { bien -> if (bien) guardado = texto } }
                }
            }
        }
        PildoraAccion(R.string.plan_banco_usar, onClick = onUsar)
    }
}

@Composable
private fun PildoraAccion(@StringRes texto: Int, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Text(
        text = stringResource(texto),
        modifier = Modifier
            .alPulsar(onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(colores.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        style = MaterialTheme.typography.labelLarge,
        color = colores.onPrimaryContainer,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaSesion(
    numero: Int,
    sesion: SesionEjercicio,
    onCambio: (SesionEjercicio) -> Unit,
    onQuitar: () -> Unit,
    // elegir una rutina del banco; guardar en él el enfoque y el detalle escritos (con su aviso de si salió bien)
    onUsarBanco: () -> Unit,
    onGuardarBanco: (String, String, (Boolean) -> Unit) -> Unit,
) {
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
        AccionesBanco(sesion.detalle, onUsar = onUsarBanco, onGuardar = { fin -> onGuardarBanco(sesion.enfoque, sesion.detalle, fin) })
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
            .alPulsar(onClick, minimo = 0.97f)
            .clip(RoundedCornerShape(19.dp))
            .background(colores.surfaceVariant)
            .padding(vertical = 16.dp),
        style = MaterialTheme.typography.titleMedium,
        color = colores.primary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}
