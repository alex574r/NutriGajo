package com.example.nutrigajo.ui.shared.registro

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.theme.NutriGajoTheme

private class OpcionRol(
    val rol: Rol,
    @param:StringRes val titulo: Int,
    @param:DrawableRes val imagen: Int?,
)

// ponytail: sin fotografías todavía; al tener los recursos se asigna `imagen` (PNG recortado, se alinea abajo a la derecha).
// ADMIN no se ofrece: se da de alta a mano, nadie se asigna ese rol.
private val opciones = listOf(
    OpcionRol(Rol.NUTRIOLOGO, R.string.rol_nutriologo, imagen = null),
    OpcionRol(Rol.PACIENTE, R.string.rol_paciente, imagen = null),
)

private val MARGEN = 16.dp
private val RADIO_TARJETA = 34.dp
private val ALTO_TARJETA = 136.dp
private val ANILLO = 4.dp

// Primer paso del registro (tema claro). El rol elegido llega al formulario de registro cuando exista.
@Composable
fun RolScreen(
    onVolver: () -> Unit,
    onContinuar: (Rol) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    var elegido by rememberSaveable { mutableStateOf<Rol?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colores.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MARGEN, end = MARGEN, top = 16.dp)
                .height(48.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(R.string.rol_registro),
                style = MaterialTheme.typography.headlineSmall,
                color = colores.onBackground,
            )
        }
        Spacer(Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.rol_titulo),
            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
            style = MaterialTheme.typography.headlineLarge,
            color = colores.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(48.dp))
        // el anillo de foco de cada tarjeta ocupa 4dp: se descuenta del margen para que el borde quede a 16dp
        Column(modifier = Modifier.selectableGroup().padding(horizontal = MARGEN - ANILLO)) {
            opciones.forEach { opcion ->
                TarjetaRol(
                    opcion = opcion,
                    seleccionada = elegido == opcion.rol,
                    onClick = { elegido = opcion.rol },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        BotonPrimario(
            texto = stringResource(R.string.rol_continuar),
            onClick = { elegido?.let(onContinuar) },
            modifier = Modifier.fillMaxWidth().padding(start = MARGEN, end = MARGEN, bottom = MARGEN),
            enabled = elegido != null,
            colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
        )
    }
}

@Composable
private fun TarjetaRol(
    opcion: OpcionRol,
    seleccionada: Boolean,
    onClick: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(RADIO_TARJETA)
    // el anillo (naranja al 25%) siempre ocupa su espacio para que las tarjetas no se muevan al elegir
    val anillo = if (seleccionada) colores.primary.copy(alpha = 0.25f) else colores.background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(ANILLO, anillo, RoundedCornerShape(RADIO_TARJETA + ANILLO))
            .padding(ANILLO),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ALTO_TARJETA)
                .clip(forma)
                .background(colores.surfaceVariant)
                .border(1.dp, if (seleccionada) colores.primary else colores.surfaceVariant, forma)
                .selectable(selected = seleccionada, onClick = onClick, role = Role.RadioButton),
        ) {
            opcion.imagen?.let {
                Image(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.BottomEnd).fillMaxHeight(),
                    contentScale = ContentScale.FillHeight,
                    alignment = Alignment.BottomEnd,
                )
            }
            Text(
                text = stringResource(opcion.titulo),
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = colores.onBackground,
            )
            Indicador(seleccionada, Modifier.align(Alignment.BottomStart).padding(16.dp))
        }
    }
}

// Casilla cuadrada con punto: es de selección única, pero así está en el diseño.
@Composable
private fun Indicador(seleccionado: Boolean, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = modifier.size(24.dp).border(2.dp, color, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (seleccionado) Box(Modifier.size(10.dp).background(color, CircleShape))
    }
}

@Preview(showSystemUi = true)
@Composable
private fun RolScreenPreview() {
    NutriGajoTheme { RolScreen(onVolver = {}, onContinuar = {}) }
}
