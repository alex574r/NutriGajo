package com.example.nutrigajo.ui.nutriologo.plan

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.PlanContenido
import com.example.nutrigajo.ui.components.PERIODOS_FLOTA
import com.example.nutrigajo.ui.components.flota
import com.example.nutrigajo.ui.components.formandose
import com.example.nutrigajo.ui.components.hundible
import java.util.Locale

private val RADIO_TARJETA = 24.dp

// Las secciones en que se divide el panel del plan, en el orden del PDF. Cada una lleva su foto: las de `recorte` son
// imágenes sin fondo (un plato, la nutrióloga) que van enteras sobre negro; el resto se recorta a la tarjeta.
internal enum class SeccionPlan(
    @param:StringRes val titulo: Int,
    @param:StringRes val ayuda: Int?,
    @param:DrawableRes val foto: Int,
    val alineacion: Alignment = Alignment.Center,
    val recorte: Boolean = false,
) {
    DATOS(R.string.plan_sec_datos, R.string.plan_datos_ayuda, R.drawable.onboarding_medidas, BiasAlignment(0f, 0.25f)),
    MENSAJE(R.string.plan_sec_mensaje, R.string.plan_mensaje_ayuda, R.drawable.rol_nutriologo, BiasAlignment(0f, -0.55f)),
    METAS(R.string.plan_sec_metas, R.string.plan_metas_ayuda, R.drawable.onboarding_planes, BiasAlignment(0f, -0.1f)),
    PERIODO(R.string.plan_sec_periodo, R.string.plan_periodo_ayuda, R.drawable.ej_senderismo),
    ALIMENTACION(R.string.plan_sec_alimentacion, R.string.plan_alimentacion_ayuda, R.drawable.onboarding_alimentacion, BiasAlignment(0f, 0.5f)),
    MENU(R.string.plan_sec_menu, R.string.plan_menu_ayuda, R.drawable.inicio_comida, recorte = true),
    EJERCICIO(R.string.plan_sec_ejercicio, R.string.plan_ejercicio_ayuda, R.drawable.ej_pesas),
    NOTAS(R.string.plan_sec_notas, R.string.plan_notas_ayuda, R.drawable.inicio_coach, recorte = true),
}

// Foto de la sección con su título (y, si `grande`, su ayuda) sobre un degradado oscuro. Quien la usa la recorta con su forma.
@Composable
internal fun HeroSeccion(seccion: SeccionPlan, alto: Dp, modifier: Modifier = Modifier, grande: Boolean = false) {
    val colores = MaterialTheme.colorScheme
    // la foto grande de una sección abierta se forma al llegar: desenfocada y grande, y se afina (Generate, de Bencho)
    val formar = if (grande) Modifier.formandose(seccion) else Modifier
    Box(modifier.fillMaxWidth().height(alto).background(colores.onBackground)) {
        if (seccion.recorte) {
            Image(
                painter = painterResource(seccion.foto),
                contentDescription = null,
                // el plato y la nutrióloga flotan en reposo, cada uno con su periodo (la deriva del Carousel)
                modifier = Modifier
                    .matchParentSize()
                    .then(formar)
                    .flota(PERIODOS_FLOTA[seccion.ordinal % PERIODOS_FLOTA.size])
                    .padding(top = 8.dp, bottom = 8.dp, end = 12.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterEnd,
            )
        } else {
            Image(
                painter = painterResource(seccion.foto),
                contentDescription = null,
                modifier = Modifier.matchParentSize().then(formar),
                contentScale = ContentScale.Crop,
                alignment = seccion.alineacion,
            )
            // el degradado deja leer el título sobre cualquier foto
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))))
        }
        Column(
            // sobre un recorte el texto no pasa de la mitad, para no taparlo
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(if (seccion.recorte) 0.55f else 1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(seccion.titulo),
                style = if (grande) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            if (grande && seccion.ayuda != null) {
                Text(stringResource(seccion.ayuda), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

// La sección en la lista del panel: su foto y debajo lo que ya tiene (o «Sin llenar»). Al tocarla se abre para editarla; bajo
// el dedo la tarjeta se hunde (el Carousel de Bencho).
@Composable
internal fun TarjetaSeccion(seccion: SeccionPlan, resumen: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(RADIO_TARJETA)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .hundible(forma)
            .clip(forma)
            .background(colores.surfaceVariant)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
    ) {
        HeroSeccion(seccion, alto = 104.dp)
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = resumen ?: stringResource(R.string.plan_sin_llenar),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (resumen != null) colores.onBackground else colores.outline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = null, modifier = Modifier.size(20.dp), tint = colores.onSurfaceVariant)
        }
    }
}

// Lo que tiene la sección en una línea; null si está vacía.
// ponytail: Datos clave solo resume peso, talla e IMC; con solo cintura, cadera o grasa llenas se ve como vacía.
@Composable
internal fun resumen(seccion: SeccionPlan, plan: PlanContenido): String? = when (seccion) {
    SeccionPlan.DATOS -> listOfNotNull(
        plan.pesoKg?.let { "${numero(it)} kg" },
        plan.tallaCm?.let { "${numero(it)} cm" },
        plan.imc?.let { stringResource(R.string.plan_resumen_imc, "%.1f".format(Locale.US, it)) },
    ).joinToString(" · ").ifEmpty { null }
    SeccionPlan.MENSAJE -> plan.mensaje.trim().ifEmpty { null }
    SeccionPlan.METAS -> plan.metas.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.plan_resumen_metas, it, it) }
    SeccionPlan.PERIODO -> plan.duracionDias?.let { pluralStringResource(R.plurals.plan_resumen_periodo, it, it) } ?: stringResource(R.string.plan_resumen_semanal)
    SeccionPlan.ALIMENTACION -> {
        val equivalentes = plan.equivalentes.values.sumOf { it.total }
        listOfNotNull(
            plan.calorias?.let { "%,d kcal".format(it) },
            equivalentes.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.plan_total_equivalentes, it, it) },
        ).joinToString(" · ").ifEmpty { null }
    }
    SeccionPlan.MENU -> plan.opciones.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.plan_resumen_opciones, it, it) }
    SeccionPlan.EJERCICIO -> listOfNotNull(
        plan.sesiones.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.plan_resumen_sesiones, it, it) },
        plan.ejerciciosAsignados.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.plan_resumen_asignados, it, it) },
    ).joinToString(" · ").ifEmpty { plan.ejercicioNotas.trim().ifEmpty { null } }
    SeccionPlan.NOTAS -> plan.notas.trim().ifEmpty { null }
}

private fun numero(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
