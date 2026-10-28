package com.example.nutrigajo.ui.shared.expediente

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.EjercicioPreferido
import com.example.nutrigajo.domain.model.Expediente
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.components.hundible
import java.time.LocalDate
import java.time.Period

const val TOTAL_DATOS = 14

private val RADIO_TARJETA = 24.dp
private val RADIO_DATO = 19.dp
private val ALTO_FOTO = 104.dp

// Datos contestados de los 14 posibles (lo que se cuenta en el avance de cada categoría y en el del expediente entero).
fun Expediente.respondidos(): Int = listOf(
    objetivo, sexo, fechaNacimiento, pesoKg, estaturaCm, nivelActividad,
    condiciones.takeIf { it.isNotEmpty() }, alergias.takeIf { it.isNotEmpty() },
    dieta, diasEjercicio, ejercicios.takeIf { it.isNotEmpty() }, suplementos.takeIf { it.isNotEmpty() },
    caloriasObjetivo, calidadSueno,
).count { it != null }

// El expediente agrupado por módulo (ModuloExpediente): cada uno con su foto, cuántos datos tiene y los datos a la vista.
// `onEditar` pone un botón de editar en cada módulo y recibe sus pasos del asistente (en su orden) para editarlo solo.
@Composable
// `resaltada` son los pasos de la categoría que se acaba de guardar: su tarjeta da un pulso para que se vea qué cambió.
fun CategoriasExpediente(
    expediente: Expediente,
    modifier: Modifier = Modifier,
    onEditar: ((List<Int>) -> Unit)? = null,
    resaltada: List<Int>? = null,
) {
    fun editar(pasos: List<Int>): (() -> Unit)? = onEditar?.let { { it(pasos) } }
    val objetivo = expediente.objetivo?.let { stringResource(it.etiqueta()) }
    val sexo = expediente.sexo?.let { stringResource(it.etiqueta()) }
    val edad = expediente.fechaNacimiento
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.let { Period.between(it, LocalDate.now()).years.toString() }
    val peso = expediente.pesoKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) }
    val estatura = expediente.estaturaCm?.let { it.toInt().toString() }
    val condiciones = expediente.condiciones.takeIf { it.isNotEmpty() }
    val alergias = expediente.alergias.takeIf { it.isNotEmpty() }
    val dieta = expediente.dieta?.let { stringResource(it.etiqueta()) }
    val calorias = expediente.caloriasObjetivo?.let { "%,d".format(it) }
    val suplementos = expediente.suplementos.takeIf { it.isNotEmpty() }
    val nivel = expediente.nivelActividad
    val dias = expediente.diasEjercicio
    val ejercicios = expediente.ejercicios.takeIf { it.isNotEmpty() }
    val sueno = expediente.calidadSueno

    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TarjetaCategoria(
            ModuloExpediente.OBJETIVO, listOf(objetivo).count { it != null },
            onEditar = editar(ModuloExpediente.OBJETIVO.pasos),
            resaltada = resaltada == ModuloExpediente.OBJETIVO.pasos,
        ) {
            ValorGrande(objetivo)
        }
        TarjetaCategoria(
            ModuloExpediente.CUERPO, listOf(sexo, edad, peso, estatura).count { it != null },
            onEditar = editar(ModuloExpediente.CUERPO.pasos),
            resaltada = resaltada == ModuloExpediente.CUERPO.pasos,
        ) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metrica(R.string.dato_sexo, sexo, modifier = Modifier.weight(1f))
                Metrica(R.string.dato_edad, edad, stringResource(R.string.expediente_edad_anios), Modifier.weight(1f))
            }
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metrica(R.string.dato_peso, peso, "kg", Modifier.weight(1f))
                Metrica(R.string.dato_estatura, estatura, "cm", Modifier.weight(1f))
            }
        }
        TarjetaCategoria(
            ModuloExpediente.SALUD, listOf(condiciones, alergias).count { it != null },
            onEditar = editar(ModuloExpediente.SALUD.pasos),
            resaltada = resaltada == ModuloExpediente.SALUD.pasos,
        ) {
            Subtitulo(R.string.dato_condiciones)
            Chips(condiciones)
            Subtitulo(R.string.dato_alergias)
            Chips(alergias)
        }
        TarjetaCategoria(
            ModuloExpediente.ALIMENTACION, listOf(dieta, calorias, suplementos).count { it != null },
            onEditar = editar(ModuloExpediente.ALIMENTACION.pasos),
            resaltada = resaltada == ModuloExpediente.ALIMENTACION.pasos,
        ) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metrica(R.string.dato_dieta, dieta, modifier = Modifier.weight(1f))
                Metrica(R.string.dato_calorias, calorias, "kcal", Modifier.weight(1f))
            }
            Subtitulo(R.string.dato_suplementos)
            Chips(suplementos)
        }
        TarjetaCategoria(
            ModuloExpediente.ACTIVIDAD, listOf(nivel, dias, ejercicios).count { it != null },
            onEditar = editar(ModuloExpediente.ACTIVIDAD.pasos),
            resaltada = resaltada == ModuloExpediente.ACTIVIDAD.pasos,
        ) {
            Subtitulo(R.string.dato_actividad)
            if (nivel != null) {
                Segmentos(nivel, 5)
                Text(
                    text = "$nivel · ${stringResource(etiquetaActividad(nivel))}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            } else {
                SinResponder()
            }
            Subtitulo(R.string.dato_dias)
            if (dias != null) {
                Puntos(dias, 7)
                Text(
                    text = stringResource(R.string.resumen_dias_valor, dias),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            } else {
                SinResponder()
            }
            Subtitulo(R.string.dato_ejercicios)
            if (ejercicios != null) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ejercicios.forEach { FotoEjercicio(it) }
                }
            } else {
                SinResponder()
            }
        }
        TarjetaCategoria(
            ModuloExpediente.DESCANSO, listOf(sueno).count { it != null },
            onEditar = editar(ModuloExpediente.DESCANSO.pasos),
            resaltada = resaltada == ModuloExpediente.DESCANSO.pasos,
        ) {
            if (sueno != null) {
                // de 1 (insomnio) a 5 (excelente): el enum va de mejor a peor
                Segmentos(5 - sueno.ordinal, 5)
            }
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metrica(R.string.resumen_calidad, sueno?.let { stringResource(it.etiqueta()) }, modifier = Modifier.weight(1f))
                Metrica(R.string.resumen_horas, sueno?.let { stringResource(it.horas()) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

// Foto de la categoría con su título y cuántos datos tiene; debajo, los datos sobre la superficie gris.
@Composable
private fun TarjetaCategoria(
    modulo: ModuloExpediente,
    respondidos: Int,
    // null: la tarjeta no se edita suelta
    onEditar: (() -> Unit)? = null,
    // acaba de guardarse: da un pulso con el anillo naranja y se pasa un poco de tamaño, y se asienta
    resaltada: Boolean = false,
    contenido: @Composable () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(RADIO_TARJETA)
    val pulso = remember { Animatable(0f) }
    LaunchedEffect(resaltada) {
        if (resaltada) {
            pulso.snapTo(1f)
            pulso.animateTo(0f, tween(1100, easing = FastOutSlowInEasing))
        }
    }
    val anillo = colores.primary
    // se hunde bajo el dedo (el Carousel de Bencho); el pulso va dentro, para que ambos se sumen
    Column(
        Modifier
            .fillMaxWidth()
            .hundible(forma)
            .graphicsLayer {
                val e = 1f + 0.015f * pulso.value
                scaleX = e
                scaleY = e
            }
            .drawWithContent {
                drawContent()
                if (pulso.value > 0.01f) {
                    drawRoundRect(
                        color = anillo.copy(alpha = 0.85f * pulso.value),
                        cornerRadius = CornerRadius(RADIO_TARJETA.toPx()),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }
            .clip(forma)
            .background(colores.surfaceVariant),
    ) {
        Box(Modifier.fillMaxWidth().height(ALTO_FOTO)) {
            Image(
                painter = painterResource(modulo.foto),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alignment = modulo.alineacion,
            )
            // el degradado oscuro deja leer el título sobre cualquier foto
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))))
            Text(
                text = stringResource(modulo.titulo),
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.expediente_avance, respondidos, modulo.pasos.size),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(7.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = Color.White,
            )
            if (onEditar != null) {
                // el área de toque mide 48dp; la pastilla, 36dp
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(48.dp)
                        .alPulsar(onEditar, minimo = 0.8f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(36.dp).background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_editar),
                            contentDescription = stringResource(R.string.resumen_editar_categoria, stringResource(modulo.titulo)),
                            modifier = Modifier.size(18.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { contenido() }
    }
}

@Composable
private fun Subtitulo(@StringRes texto: Int) =
    Text(stringResource(texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun SinResponder() =
    Text(stringResource(R.string.expediente_sin_responder), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)

@Composable
private fun ValorGrande(valor: String?) {
    if (valor == null) SinResponder() else Text(
        text = valor,
        style = MaterialTheme.typography.titleLarge.copy(lineHeight = 1.25.em),
        color = MaterialTheme.colorScheme.onBackground,
    )
}

// Dato suelto con su unidad en pequeño, sobre una pastilla blanca.
@Composable
private fun Metrica(@StringRes etiqueta: Int, valor: String?, unidad: String? = null, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Column(
        // las pastillas de una fila miden lo mismo aunque una diga «Sin responder»
        modifier = modifier.fillMaxHeight().background(colores.background, RoundedCornerShape(RADIO_DATO)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Subtitulo(etiqueta)
        if (valor == null) {
            SinResponder()
        } else {
            Text(
                text = buildAnnotatedString {
                    append(valor)
                    if (unidad != null) withStyle(SpanStyle(fontSize = 14.sp, color = colores.onSurfaceVariant)) { append(" $unidad") }
                },
                style = MaterialTheme.typography.titleLarge,
                color = colores.onBackground,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(textos: List<String>?) {
    if (textos == null) {
        SinResponder()
        return
    }
    val colores = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        textos.forEach {
            Text(
                text = it,
                modifier = Modifier.background(colores.primaryContainer, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = colores.onPrimaryContainer,
            )
        }
    }
}

// Barra de `total` tramos con los primeros `nivel` encendidos.
@Composable
private fun Segmentos(nivel: Int, total: Int) {
    val colores = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(if (i < nivel) colores.primary else colores.outlineVariant))
        }
    }
}

@Composable
private fun Puntos(encendidos: Int, total: Int) {
    val colores = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(Modifier.size(22.dp).clip(CircleShape).background(if (i < encendidos) colores.primary else colores.background))
        }
    }
}

@Composable
private fun FotoEjercicio(ejercicio: EjercicioPreferido) {
    Box(Modifier.size(width = 96.dp, height = 120.dp).clip(RoundedCornerShape(RADIO_DATO))) {
        Image(
            painter = painterResource(ejercicio.foto()),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
        Text(
            text = stringResource(ejercicio.etiqueta()),
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            color = Color.White,
            maxLines = 1,
        )
    }
}
