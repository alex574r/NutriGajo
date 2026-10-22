package com.example.nutrigajo.ui.paciente.inicio

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CRISTAL = Color(0xFFBDBDBD).copy(alpha = 0.21f)

// Cabecera del inicio: foto con la fecha, la campana, el avatar (abre el menú de cuenta) y el saludo.
// Mide 215 dp bajo la barra de estado (44 más que en el diseño, que se veía pequeño en el teléfono);
// la foto llega hasta el borde superior de la pantalla.
@Composable
fun CabeceraInicio(
    nombre: String,
    resumen: ResumenDia,
    onExpediente: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val fecha = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())).uppercase().replace(".", "")
    }
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))) {
        Image(
            painter = painterResource(R.drawable.inicio_cabecera),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        // la foto es clara en algunas zonas: el velo mantiene el contraste del texto blanco
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.25f)))
        Column(Modifier.statusBarsPadding().height(215.dp).padding(horizontal = 21.dp)) {
            Spacer(Modifier.height(62.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(painterResource(R.drawable.ic_calendario), contentDescription = null, tint = colores.outline)
                Text(fecha, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = colores.outline)
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(nombre, onExpediente, onCerrarSesion)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.inicio_hola, nombre),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Dato(R.drawable.ic_llama, stringResource(R.string.inicio_score, resumen.score))
                        Dato(R.drawable.ic_montana, stringResource(R.string.inicio_avance, resumen.avance))
                        Dato(R.drawable.ic_corona, stringResource(R.string.inicio_plan))
                    }
                    Text(
                        text = FRASE_DEL_DIA,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // ponytail: la campana todavía no abre nada; las notificaciones llegan con FCM
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 19.dp)
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CRISTAL),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_campana),
                contentDescription = stringResource(R.string.inicio_notificaciones),
                tint = Color(0xFFF9F9F9),
            )
        }
    }
}

// Aún no hay foto de perfil (llegará con el almacenamiento): inicial del nombre sobre naranja.
@Composable
private fun Avatar(nombre: String, onExpediente: () -> Unit, onCerrarSesion: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    var abierto by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(colores.primary)
                .clickable(onClickLabel = stringResource(R.string.inicio_menu_cuenta), role = Role.Button) { abierto = true },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = nombre.firstOrNull()?.uppercase().orEmpty(),
                style = MaterialTheme.typography.headlineLarge,
                color = colores.onPrimary,
            )
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.inicio_menu_expediente)) },
                onClick = {
                    abierto = false
                    onExpediente()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.inicio_cerrar_sesion)) },
                onClick = {
                    abierto = false
                    onCerrarSesion()
                },
            )
        }
    }
}

@Composable
private fun Dato(@DrawableRes icono: Int, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
        Text(texto, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Color.White, maxLines = 1)
    }
}
