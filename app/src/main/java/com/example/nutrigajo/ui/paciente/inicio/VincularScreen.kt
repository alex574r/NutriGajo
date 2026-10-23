package com.example.nutrigajo.ui.paciente.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.components.ANILLO_FOCO
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.CampoCodigo
import com.example.nutrigajo.ui.theme.NutriGajoTheme

private val MARGEN = 16.dp
private const val LARGO_CODIGO = 8

// El paciente sin nutriólogo escribe el código que este le dio (XXXX-XXXX; el guion lo pone la pantalla).
@Composable
fun VincularScreen(
    estado: VinculoEstado,
    onCanjear: (codigo: String) -> Unit,
    onEditar: () -> Unit,
    onMapa: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    // el código ya se canjeó: las casillas se funden en la cápsula "Vinculado" mientras llega el inicio
    vinculado: Boolean = false,
) {
    val colores = MaterialTheme.colorScheme
    var codigo by rememberSaveable { mutableStateOf("") }
    val completo = codigo.length == LARGO_CODIGO

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colores.background)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BarraSuperior(onAtras = onCerrarSesion)
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.vincular_titulo),
            modifier = Modifier.padding(horizontal = MARGEN),
            style = MaterialTheme.typography.headlineLarge,
            color = colores.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.vincular_ayuda),
            modifier = Modifier.padding(horizontal = MARGEN),
            style = MaterialTheme.typography.bodyLarge,
            color = colores.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))
        CampoCodigo(
            valor = codigo,
            onValorChange = { codigo = it; onEditar() },
            largo = LARGO_CODIGO,
            descripcion = stringResource(R.string.vincular_codigo),
            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN - ANILLO_FOCO),
            onListo = { if (completo && !estado.canjeando && !vinculado) onCanjear(codigo) },
            error = estado.error != null,
            verificando = estado.canjeando,
            exito = if (vinculado) stringResource(R.string.vincular_exito) else null,
        )
        estado.error?.let {
            Spacer(Modifier.height(16.dp))
            AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN))
        }
        Spacer(Modifier.height(24.dp))
        BotonPrimario(
            texto = stringResource(R.string.vincular_boton),
            onClick = { onCanjear(codigo) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
            enabled = completo && !vinculado,
            colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
            cargando = estado.canjeando,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.padding(horizontal = MARGEN),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.vincular_sin_codigo),
                style = MaterialTheme.typography.labelLarge,
                color = colores.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.vincular_buscar),
                modifier = Modifier.clickable(role = Role.Button, onClick = onMapa).padding(vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge,
                color = colores.primary,
                textDecoration = TextDecoration.Underline,
            )
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}

// Botón de atrás y título como en el diseño; aquí atrás es salir de la cuenta, porque no hay pantalla previa.
@Composable
private fun BarraSuperior(onAtras: () -> Unit, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = MARGEN, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(forma)
                .background(colores.surfaceVariant)
                .border(1.dp, colores.outline, forma)
                .clickable(role = Role.Button, onClick = onAtras),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_flecha_atras),
                contentDescription = stringResource(R.string.inicio_cerrar_sesion),
                modifier = Modifier.size(20.dp),
                tint = colores.onBackground,
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.vincular_barra),
            style = MaterialTheme.typography.titleLarge,
            color = colores.onBackground,
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun VincularScreenPreview() {
    NutriGajoTheme {
        VincularScreen(estado = VinculoEstado(cargando = false), onCanjear = {}, onEditar = {}, onMapa = {}, onCerrarSesion = {})
    }
}
