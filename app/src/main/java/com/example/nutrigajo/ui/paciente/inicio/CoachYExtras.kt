package com.example.nutrigajo.ui.paciente.inicio

import com.example.nutrigajo.ui.shared.ejercicios.FilaEjercicio
import com.example.nutrigajo.ui.shared.ejercicios.EjerciciosEstado
import com.example.nutrigajo.ui.components.Esqueleto
import androidx.compose.runtime.getValue
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.TOPE_PENDIENTES
import com.example.nutrigajo.ui.components.Etiqueta
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.alPulsar

// La entrada al chat con el nutriólogo: toda la tarjeta lo abre. Cuenta sus mensajes sin leer o, si no hay, muestra el último;
// las etiquetas salen del plan que aún no ha visto (la campana lleva el punto mientras tanto).
// ponytail: la foto del coach es la del diseño, no la del nutriólogo; los tres puntos del encabezado no abren nada.
@Composable
fun TarjetaCoach(
    nutriologo: String,
    pendientes: Int,
    ultimo: String,
    nuevaRutina: Boolean,
    nuevaDieta: Boolean,
    onChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val tipos = MaterialTheme.typography
    Column(modifier) {
        EncabezadoSeccion(stringResource(R.string.inicio_coach), contador = nutriologo, menu = true)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(227.dp)
                .alPulsar(onChat, minimo = 0.97f)
                .clip(RoundedCornerShape(29.dp))
                .background(colores.primary),
        ) {
            Image(
                painter = painterResource(R.drawable.inicio_coach),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp).size(167.dp, 250.dp),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color(0xFFA64400).copy(alpha = 0.52f)),
                ),
            )
            Column(Modifier.padding(start = 16.dp, top = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (nuevaRutina) Etiqueta(stringResource(R.string.coach_nueva_rutina))
                if (nuevaDieta) Etiqueta(stringResource(R.string.coach_nueva_dieta))
            }
            // el ancho se queda de este lado de la foto y del botón
            Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp).widthIn(max = 180.dp)) {
                if (pendientes > 0) {
                    Text(
                        text = if (pendientes >= TOPE_PENDIENTES) "${TOPE_PENDIENTES - 1}+" else pendientes.toString(),
                        style = tipos.headlineLarge,
                        color = Color.White,
                    )
                    Text(stringResource(if (pendientes == 1) R.string.coach_mensaje else R.string.coach_mensajes), style = tipos.titleLarge, color = Color.White)
                } else {
                    Text(ultimo, style = tipos.titleLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 15.dp)
                    .size(55.dp, 56.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mensaje),
                    contentDescription = stringResource(R.string.coach_abrir_mensajes),
                    tint = colores.primary,
                )
            }
        }
    }
}

// «Más ejercicios extra»: los más recientes del banco global (los que publican los entrenadores verificados) en filas compactas;
// tocar uno abre su detalle y «Ver todas», el banco completo. Mientras cargan se ven esqueletos; sin ejercicios o sin red queda la
// entrada de siempre, para no perder el acceso.
@Composable
fun BancoEjercicios(estado: EjerciciosEstado, onAbrir: (String?) -> Unit, modifier: Modifier = Modifier) {
    val recientes = estado.items.take(RECIENTES)
    Column(modifier) {
        EncabezadoSeccion(stringResource(R.string.inicio_extra), verTodas = recientes.isNotEmpty(), onVerTodas = { onAbrir(null) })
        Spacer(Modifier.height(12.dp))
        when {
            recientes.isNotEmpty() -> Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recientes.forEach { FilaEjercicio(it, onClick = { onAbrir(it.id) }) }
            }
            estado.cargando -> Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) { Esqueleto(Modifier.fillMaxWidth().height(107.dp), RoundedCornerShape(34.dp)) }
            }
            else -> FilaAjuste(
                icono = R.drawable.ic_corredor,
                titulo = stringResource(R.string.inicio_banco_explorar),
                modifier = Modifier.padding(horizontal = 16.dp),
                etiqueta = stringResource(R.string.inicio_banco_etiqueta),
                onClick = { onAbrir(null) },
            )
        }
    }
}

private const val RECIENTES = 3
