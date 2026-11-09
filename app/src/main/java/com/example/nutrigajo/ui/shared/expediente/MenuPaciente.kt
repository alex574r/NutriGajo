package com.example.nutrigajo.ui.shared.expediente

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.PuntoNuevo
import com.example.nutrigajo.ui.components.hundible

// El menú general de un paciente para su nutriólogo: una tarjeta grande con foto por cada cosa que puede hacer con él (ver el expediente, armar
// el plan, chatear, fijar metas, agendar una cita). Cada una abre su pantalla completa, así el expediente no queda apretado bajo una pila de
// botones. Una opción que no se pasa (`null`) no aparece.
@Composable
internal fun MenuPaciente(
    nombrePaciente: String,
    resumenExpediente: String,
    onExpediente: () -> Unit,
    modifier: Modifier = Modifier,
    resumenPlan: String? = null,
    onPlan: (() -> Unit)? = null,
    resumenChat: String? = null,
    chatSinLeer: Boolean = false,
    onChat: (() -> Unit)? = null,
    onMetas: (() -> Unit)? = null,
    onCita: (() -> Unit)? = null,
    // solo el nutriólogo: cierra el vínculo con este paciente (pide confirmación afuera); va al final, en rojo
    onDarDeBaja: (() -> Unit)? = null,
) {
    val primerNombre = nombrePaciente.substringBefore(' ')
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TarjetaMenu(R.string.menu_paciente_expediente, R.drawable.exp_salud, resumenExpediente, onExpediente)
        if (onPlan != null) {
            TarjetaMenu(R.string.menu_paciente_plan, R.drawable.onboarding_alimentacion, resumenPlan.orEmpty(), onPlan, BiasAlignment(0f, 0.5f))
        }
        if (onChat != null) {
            TarjetaMenu(R.string.menu_paciente_chat, R.drawable.rol_nutriologo, resumenChat.orEmpty(), onChat, BiasAlignment(0f, -0.55f), sinLeer = chatSinLeer)
        }
        if (onMetas != null) {
            TarjetaMenu(R.string.menu_paciente_metas, R.drawable.rol_paciente, stringResource(R.string.menu_paciente_metas_resumen), onMetas, BiasAlignment(0f, -0.2f))
        }
        if (onCita != null) {
            TarjetaMenu(R.string.menu_paciente_citas, R.drawable.onboarding_planes, stringResource(R.string.menu_paciente_citas_resumen, primerNombre), onCita, BiasAlignment(0f, -0.1f))
        }
        if (onDarDeBaja != null) {
            FilaAjuste(
                icono = R.drawable.ic_flecha_atras,
                titulo = stringResource(R.string.baja_paciente_titulo),
                descripcion = stringResource(R.string.baja_paciente_desc, primerNombre),
                destructiva = true,
                onClick = onDarDeBaja,
            )
        }
    }
}

// La foto con su título sobre un degradado oscuro y debajo una línea de lo que hay dentro, como las tarjetas del panel del plan.
@Composable
private fun TarjetaMenu(
    titulo: Int,
    @DrawableRes foto: Int,
    resumen: String,
    onClick: () -> Unit,
    alineacion: Alignment = Alignment.Center,
    sinLeer: Boolean = false,
) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .hundible(forma)
            .clip(forma)
            .background(colores.surfaceVariant)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(112.dp).background(colores.onBackground)) {
            Image(painterResource(foto), contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop, alignment = alineacion)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))))
            Text(
                text = stringResource(titulo),
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            if (sinLeer) PuntoNuevo(Modifier.align(Alignment.TopEnd).padding(14.dp))
        }
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = resumen,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = colores.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = null, modifier = Modifier.size(20.dp), tint = colores.onSurfaceVariant)
        }
    }
}
