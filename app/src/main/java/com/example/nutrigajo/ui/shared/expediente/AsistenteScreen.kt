package com.example.nutrigajo.ui.shared.expediente

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Expediente
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.components.formandose

private val MARGEN = 16.dp

// Una pregunta por pantalla, como el asistente del diseño. Regresar en el primer paso sale del asistente.
// Todas las preguntas son opcionales: "omitir" borra la respuesta de ese paso y sigue.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AsistenteScreen(
    estado: ExpedienteEstado,
    onCambiar: ((Expediente) -> Expediente) -> Unit,
    onContinuar: () -> Unit,
    onOmitir: () -> Unit,
    onRegresar: () -> Unit,
    // saltar a otro módulo desde las barras de arriba; solo en el asistente completo
    onIrAModulo: (ModuloExpediente) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onRegresar)
    val colores = MaterialTheme.colorScheme
    // las preguntas van por módulos: el avance cuenta dentro del módulo en que se está («2 de 4» en Cuerpo), y editando una
    // categoría suelta es el único módulo que hay
    val pasos = estado.pasos
    val modulo = moduloDe(estado.paso)
    val posicion = modulo.pasos.indexOf(estado.paso) + 1
    val total = modulo.pasos.size
    val esUltimo = siguientePaso(pasos, estado.paso) == null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colores.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .alPulsar(onRegresar, minimo = 0.88f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colores.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.expediente_regresar),
                    tint = colores.onBackground,
                )
            }
            Text(
                text = stringResource(R.string.expediente_titulo),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = colores.onBackground,
            )
            // el número sube o baja como un rodillo al cambiar de pregunta
            AnimatedContent(
                targetState = posicion,
                modifier = Modifier
                    .background(colores.secondaryContainer, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                transitionSpec = {
                    val sube = targetState > initialState
                    (slideInVertically(tween(220)) { if (sube) it else -it } + fadeIn(tween(220))) togetherWith
                        (slideOutVertically(tween(220)) { if (sube) -it else it } + fadeOut(tween(120)))
                },
                label = "avance",
            ) { p ->
                Text(
                    text = stringResource(R.string.expediente_avance, p, total),
                    style = MaterialTheme.typography.labelLarge,
                    color = colores.secondary,
                )
            }
        }
        // el expediente va por módulos: una barra por módulo (tocar una salta a ese módulo) y, debajo, la foto del módulo actual
        if (pasos == null) BarraModulos(modulo, posicion, onIrAModulo)
        CabeceraModulo(modulo, numero = if (pasos == null) modulo.ordinal + 1 else null)
        // la pregunta sale hacia el lado de donde vino y la siguiente entra del otro, como las pestañas del perfil; regresar
        // las mueve al revés
        AnimatedContent(
            targetState = estado.paso,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                val sentido = if (ORDEN_PASOS.indexOf(targetState) > ORDEN_PASOS.indexOf(initialState)) 1 else -1
                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { sentido * it / 5 } + fadeIn(tween(220, delayMillis = 30))) togetherWith
                    (slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { -sentido * it / 5 } + fadeOut(tween(120)))
            },
            label = "paso",
        ) { paso ->
            Column(Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(tituloPaso(paso)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    // el 100% de la escala no sirve para texto de varias líneas
                    style = MaterialTheme.typography.headlineLarge.copy(lineHeight = 1.2.em),
                    color = colores.onBackground,
                    textAlign = TextAlign.Center,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        // las opciones llevan su anillo de foco: se descuenta del margen para que el borde quede a 16dp
                        .padding(horizontal = MARGEN - ANILLO_FOCO, vertical = 8.dp),
                ) {
                    ContenidoPaso(paso = paso, expediente = estado.expediente, cambiar = onCambiar)
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            estado.error?.let { AlertaError(stringResource(it)) }
            // con el teclado abierto no queda espacio para las opciones: se esconde
            if (!WindowInsets.isImeVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .alPulsar(onOmitir, enabled = !estado.guardando, minimo = 0.97f)
                        .clip(RoundedCornerShape(19.dp))
                        .background(colores.primaryContainer),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.expediente_omitir) + "  ✕",
                        style = MaterialTheme.typography.labelLarge,
                        color = colores.primary,
                    )
                }
            }
            BotonPrimario(
                texto = stringResource(
                    when {
                        !esUltimo -> R.string.expediente_continuar
                        pasos != null -> R.string.expediente_guardar
                        else -> R.string.expediente_terminar
                    },
                ),
                onClick = onContinuar,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
                cargando = estado.guardando,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

// Una barra por módulo: las de módulos ya pasados, llenas; la del actual, llena según la pregunta en que va (con resorte); las
// demás, vacías. Tocar una salta al primer paso de ese módulo. El área de toque mide 24dp; la barra, 6.
@Composable
private fun BarraModulos(actual: ModuloExpediente, posicion: Int, onIrAModulo: (ModuloExpediente) -> Unit) {
    val colores = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = MARGEN), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ModuloExpediente.entries.forEach { m ->
            val lleno = when {
                m.ordinal < actual.ordinal -> 1f
                m == actual -> posicion / m.pasos.size.toFloat()
                else -> 0f
            }
            val avance by animateFloatAsState(lleno, spring(dampingRatio = 0.7f, stiffness = 200f), label = "modulo")
            val nombre = stringResource(m.titulo)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .alPulsar({ onIrAModulo(m) }, minimo = 0.9f)
                    .semantics {
                        contentDescription = nombre
                        selected = m == actual
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colores.surfaceVariant)) {
                    Box(Modifier.fillMaxWidth(avance.coerceIn(0f, 1f)).fillMaxHeight().background(colores.primary))
                }
            }
        }
    }
}

// La foto del módulo en que se está, con su nombre (y «Módulo 2 de 6» en el asistente completo). Al cambiar de módulo la foto
// se forma: llega desenfocada y se afina (la de Generate, de Bencho).
@Composable
private fun CabeceraModulo(modulo: ModuloExpediente, numero: Int?) {
    AnimatedContent(
        targetState = modulo,
        modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN, vertical = 8.dp),
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
        label = "cabecera",
    ) { m ->
        Box(Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(20.dp))) {
            Image(
                painter = painterResource(m.foto),
                contentDescription = null,
                modifier = Modifier.matchParentSize().formandose(m, 600),
                contentScale = ContentScale.Crop,
                alignment = m.alineacion,
            )
            // el degradado oscuro deja leer el nombre sobre cualquier foto
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))))
            Text(
                text = stringResource(m.titulo),
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            if (numero != null) {
                Text(
                    text = stringResource(R.string.expediente_modulo, numero, ModuloExpediente.entries.size),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(7.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                    color = Color.White,
                )
            }
        }
    }
}
