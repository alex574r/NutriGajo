package com.example.nutrigajo.ui.shared.expediente

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.EjercicioPreferido
import com.example.nutrigajo.domain.model.Expediente
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

// El expediente agrupado por categoría: cada una con su foto, cuántos datos tiene y los datos a la vista.
// Fotos de Pexels (uso libre): salud 6320167 · descanso 7556587; el resto sale de las del onboarding y el inicio.
@Composable
fun CategoriasExpediente(expediente: Expediente, modifier: Modifier = Modifier) {
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
        TarjetaCategoria(R.drawable.onboarding_planes, R.string.dato_objetivo, listOf(objetivo).count { it != null }, 1, BiasAlignment(0f, -0.1f)) {
            ValorGrande(objetivo)
        }
        TarjetaCategoria(
            R.drawable.onboarding_medidas, R.string.resumen_cat_cuerpo, listOf(sexo, edad, peso, estatura).count { it != null }, 4,
            BiasAlignment(0f, 0.25f),
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
        TarjetaCategoria(R.drawable.exp_salud, R.string.resumen_cat_salud, listOf(condiciones, alergias).count { it != null }, 2) {
            Subtitulo(R.string.dato_condiciones)
            Chips(condiciones)
            Subtitulo(R.string.dato_alergias)
            Chips(alergias)
        }
        TarjetaCategoria(
            R.drawable.onboarding_alimentacion, R.string.resumen_cat_alimentacion, listOf(dieta, calorias, suplementos).count { it != null }, 3,
            BiasAlignment(0f, 0.5f),
        ) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metrica(R.string.dato_dieta, dieta, modifier = Modifier.weight(1f))
                Metrica(R.string.dato_calorias, calorias, "kcal", Modifier.weight(1f))
            }
            Subtitulo(R.string.dato_suplementos)
            Chips(suplementos)
        }
        TarjetaCategoria(
            R.drawable.inicio_rutina, R.string.resumen_cat_actividad, listOf(nivel, dias, ejercicios).count { it != null }, 3,
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
        TarjetaCategoria(R.drawable.exp_descanso, R.string.resumen_cat_descanso, listOf(sueno).count { it != null }, 1) {
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
    @DrawableRes foto: Int,
    @StringRes titulo: Int,
    respondidos: Int,
    total: Int,
    alineacionFoto: Alignment = Alignment.Center,
    contenido: @Composable () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(RADIO_TARJETA)).background(colores.surfaceVariant)) {
        Box(Modifier.fillMaxWidth().height(ALTO_FOTO)) {
            Image(
                painter = painterResource(foto),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alignment = alineacionFoto,
            )
            // el degradado oscuro deja leer el título sobre cualquier foto
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))))
            Text(
                text = stringResource(titulo),
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.expediente_avance, respondidos, total),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(7.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = Color.White,
            )
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
