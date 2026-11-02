package com.example.nutrigajo.ui.paciente.inicio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.PlanCaminata
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.components.cristal

// La entrada al modo carrera: foto, qué se puede hacer (caminar, correr, planes) y el botón que lo abre; toda la tarjeta lo abre.
// Con una ruta empezada avisa y invita a volver al mapa.
@Composable
fun TarjetaCarrera(enCurso: Boolean, onAbrir: () -> Unit, modifier: Modifier = Modifier) {
    val tipos = MaterialTheme.typography
    Column(modifier) {
        EncabezadoSeccion(stringResource(R.string.carrera_titulo))
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(234.dp)
                .alPulsar(onAbrir, minimo = 0.97f)
                .clip(RoundedCornerShape(31.dp)),
        ) {
            Image(
                painter = painterResource(R.drawable.carrera_tarjeta),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.3f), 0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f)),
                ),
            )
            Row(Modifier.padding(start = 15.dp, top = 19.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_caminar), contentDescription = null, modifier = Modifier.size(20.dp), tint = TEXTO_SOBRE_FOTO)
                Text(
                    text = stringResource(R.string.carrera_caminar),
                    modifier = Modifier.padding(start = 4.dp),
                    style = tipos.labelLarge,
                    color = TEXTO_SOBRE_FOTO,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_correr),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 14.dp).size(20.dp),
                    tint = TEXTO_SOBRE_FOTO,
                )
                Text(
                    text = stringResource(R.string.carrera_correr),
                    modifier = Modifier.padding(start = 4.dp),
                    style = tipos.labelLarge,
                    color = TEXTO_SOBRE_FOTO,
                )
            }
            // el texto se queda de este lado del botón
            Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 72.dp, bottom = 22.dp)) {
                Text(
                    text = stringResource(if (enCurso) R.string.carrera_entrada_en_curso else R.string.carrera_tarjeta_titulo),
                    style = tipos.titleLarge.copy(shadow = SOMBRA_TEXTO),
                    color = Color.White,
                )
                Text(
                    text = stringResource(if (enCurso) R.string.carrera_tarjeta_volver else R.string.carrera_tarjeta_detalle),
                    modifier = Modifier.padding(top = 4.dp),
                    style = tipos.bodySmall.copy(shadow = SOMBRA_TEXTO),
                    color = Color.White,
                )
                if (!enCurso) {
                    Text(
                        text = stringResource(R.string.carrera_tarjeta_planes, PlanCaminata.entries.size),
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .cristal(RoundedCornerShape(7.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        style = tipos.labelLarge,
                        color = Color.White,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 21.dp)
                    .size(36.dp)
                    // el botón de jugar es cristal teñido de naranja, como el de la rutina
                    .cristal(RoundedCornerShape(12.dp), velo = Color(0xFFFF974E))
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_play), contentDescription = stringResource(R.string.carrera_titulo), tint = Color.White)
            }
        }
    }
}
