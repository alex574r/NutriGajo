package com.example.nutrigajo.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R

private val CRISTAL = Color(0xFFBDBDBD).copy(alpha = 0.21f)
private val AVATAR = 96.dp
private val ALTO_PORTADA = 220.dp
private val SALIENTE_INSIGNIA = 14.dp


class AccionPerfil(
    @param:DrawableRes val icono: Int,
    @param:StringRes val texto: Int,
    val onClick: () -> Unit,
)

// Cabecera del perfil del diseño: portada con foto y esquinas redondeadas abajo, avatar cuadrado que la monta, nombre con la
// insignia de cuenta verificada, rol y correo, hasta dos acciones y una línea de datos separados por puntos.
// La portada llega hasta el borde superior de la pantalla; `onVolver` null oculta el botón de atrás.
@Composable
fun PerfilCabecera(
    nombre: String,
    correo: String,
    @StringRes rol: Int,
    verificado: Boolean,
    estadisticas: List<String>,
    principal: AccionPerfil,
    secundaria: AccionPerfil?,
    modifier: Modifier = Modifier,
    onVolver: (() -> Unit)? = null,
    // la portada propia; null deja la foto de siempre. Con `onCambiarPortada` la portada lleva el botón para elegir otra.
    imagenPortada: ImageBitmap? = null,
    subiendoPortada: Boolean = false,
    onCambiarPortada: (() -> Unit)? = null,
    // lo mismo para la foto de perfil: sin ella se dibuja la inicial
    imagenAvatar: ImageBitmap? = null,
    subiendoAvatar: Boolean = false,
    onCambiarAvatar: (() -> Unit)? = null,
    // el avatar viaja desde la pantalla anterior si la hay
    transicion: TransicionPerfil? = null,
) {
    val colores = MaterialTheme.colorScheme
    val portada = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + ALTO_PORTADA
    Box(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(portada).clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))) {
            if (imagenPortada != null) {
                Image(
                    bitmap = imagenPortada,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.inicio_cabecera),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                )
            }
            // la foto es clara en algunas zonas: el velo mantiene el contraste de los botones de cristal
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.25f)))
            if (onVolver != null) {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 19.dp, top = 8.dp)
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CRISTAL)
                        .clickable(role = Role.Button, onClick = onVolver),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_flecha_atras),
                        contentDescription = stringResource(R.string.perfil_volver),
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFF9F9F9),
                    )
                }
            }
            if (onCambiarPortada != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 19.dp, bottom = 16.dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CRISTAL)
                        .clickable(enabled = !subiendoPortada, role = Role.Button, onClick = onCambiarPortada),
                    contentAlignment = Alignment.Center,
                ) {
                    // como en los botones de acción, el progreso es un indicador pequeño dentro del botón
                    if (subiendoPortada) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFF9F9F9), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_editar),
                            contentDescription = stringResource(R.string.perfil_cambiar_portada),
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFF9F9F9),
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = portada - AVATAR / 2, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // la caja incluye lo que la insignia asoma bajo el avatar: Compose solo entrega los toques dentro de los límites del padre
            Box(Modifier.size(width = AVATAR, height = AVATAR + SALIENTE_INSIGNIA)) {
                // el anillo del color del fondo recorta el avatar de la portada; tocarlo abre el selector de fotos
                Box(
                    modifier = Modifier
                        .size(AVATAR)
                        .background(colores.background, RoundedCornerShape(32.dp))
                        .padding(4.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .then(
                            if (onCambiarAvatar != null) {
                                Modifier.clickable(
                                    enabled = !subiendoAvatar,
                                    onClickLabel = stringResource(R.string.perfil_cambiar_avatar),
                                    role = Role.Button,
                                    onClick = onCambiarAvatar,
                                )
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    AvatarInicial(
                        nombre = nombre,
                        tamano = AVATAR - 8.dp,
                        estilo = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.compartido(transicion, CLAVE_AVATAR),
                        forma = formaAvatar(transicion, circulo = false),
                        imagen = imagenAvatar,
                    )
                }
                // insignia del lápiz, a caballo entre el avatar y el nombre como en el diseño; abre el mismo selector
                if (onCambiarAvatar != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(34.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(colores.onBackground)
                            .border(3.dp, colores.background, RoundedCornerShape(11.dp))
                            .clickable(
                                enabled = !subiendoAvatar,
                                onClickLabel = stringResource(R.string.perfil_cambiar_avatar),
                                role = Role.Button,
                                onClick = onCambiarAvatar,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (subiendoAvatar) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colores.background, strokeWidth = 2.dp)
                        } else {
                            Icon(painterResource(R.drawable.ic_editar), contentDescription = null, modifier = Modifier.size(16.dp), tint = colores.background)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
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
                        tint = colores.secondary,
                    )
                }
            }
            Text(correo, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
            Text(
                text = stringResource(rol),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .border(1.dp, colores.outline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = colores.onSurfaceVariant,
            )
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = principal.onClick,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(painterResource(principal.icono), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(principal.texto))
                }
                secundaria?.let {
                    OutlinedButton(onClick = it.onClick, modifier = Modifier.height(44.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(painterResource(it.icono), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(it.texto))
                    }
                }
            }
            if (estadisticas.isNotEmpty()) {
                Text(
                    text = estadisticas.joinToString("  •  "),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
