package com.example.nutrigajo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R

// Cabecera de los inicios de nutriólogo y administrador: avatar (abre el perfil), saludo, nombre con la insignia
// de cuenta verificada y un botón redondo de acción a la derecha, como el de búsqueda del diseño.
@Composable
fun CabeceraUsuario(
    saludo: String,
    nombre: String,
    verificado: Boolean,
    descripcionAvatar: String,
    onAvatar: () -> Unit,
    @DrawableRes iconoAccion: Int,
    descripcionAccion: String,
    onAccion: () -> Unit,
    imagenAvatar: ImageBitmap? = null,
    // el avatar viaja al perfil cuando se abre
    transicion: TransicionPerfil? = null,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    Row(
        // el aire de arriba la separa de la barra de estado; la lista le suma sus 12 dp
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarInicial(
            nombre = nombre,
            tamano = 56.dp,
            estilo = MaterialTheme.typography.headlineSmall,
            imagen = imagenAvatar,
            forma = formaAvatar(transicion, circulo = true),
            modifier = Modifier
                .compartido(transicion, CLAVE_AVATAR)
                .clickable(onClickLabel = descripcionAvatar, role = Role.Button, onClick = onAvatar),
        )
        Column(Modifier.weight(1f)) {
            Text(saludo, style = MaterialTheme.typography.labelLarge, color = colores.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = nombre,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colores.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (verificado) {
                    Icon(
                        painter = painterResource(R.drawable.ic_verificado),
                        contentDescription = stringResource(R.string.nutri_verificado),
                        modifier = Modifier.size(22.dp),
                        tint = colores.primary,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.dp, colores.outline, CircleShape)
                .clickable(onClickLabel = descripcionAccion, role = Role.Button, onClick = onAccion),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(iconoAccion), contentDescription = null, modifier = Modifier.size(22.dp), tint = colores.onBackground)
        }
    }
}
