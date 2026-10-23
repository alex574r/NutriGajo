package com.example.nutrigajo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R

// Botón de la tarjeta destacada: el naranja de abajo a la derecha es la acción principal y el círculo de arriba, la secundaria.
class AccionTarjeta(
    @param:DrawableRes val icono: Int,
    val descripcion: String,
    val habilitada: Boolean = true,
    val onClick: () -> Unit,
)

// Tarjeta grande con foto del diseño ("Featured"): etiqueta arriba, título y dato abajo y, si se pasan, dos acciones.
@Composable
fun TarjetaDestacada(
    etiqueta: String,
    titulo: String,
    meta: String,
    modifier: Modifier = Modifier,
    espaciadoTitulo: TextUnit = 3.sp,
    principal: AccionTarjeta? = null,
    secundaria: AccionTarjeta? = null,
) {
    val colores = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(29.dp))) {
        Image(
            painter = painterResource(R.drawable.onboarding_alimentacion),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(0f, 0.3f),
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.78f))),
            ),
        )
        Etiqueta(etiqueta, Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 16.dp))
        secundaria?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .clickable(enabled = it.habilitada, onClickLabel = it.descripcion, role = Role.Button, onClick = it.onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(it.icono), contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
            }
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 18.dp, end = if (principal != null) 88.dp else 16.dp),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = espaciadoTitulo),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(painterResource(R.drawable.ic_reloj), contentDescription = null, modifier = Modifier.size(16.dp), tint = colores.outline)
                Text(text = meta, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = colores.outline)
            }
        }
        principal?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colores.primary)
                    .clickable(enabled = it.habilitada, onClickLabel = it.descripcion, role = Role.Button, onClick = it.onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(it.icono), contentDescription = null, modifier = Modifier.size(26.dp), tint = colores.onPrimary)
            }
        }
    }
}
