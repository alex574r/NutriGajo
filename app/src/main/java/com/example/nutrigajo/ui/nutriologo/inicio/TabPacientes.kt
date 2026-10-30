package com.example.nutrigajo.ui.nutriologo.inicio

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.PacienteResumen
import com.example.nutrigajo.ui.components.ALTO_BARRA_INFERIOR
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.PuntoNuevo
import com.example.nutrigajo.ui.nutriologo.pacientes.PacientesEstado

// Lista completa de pacientes vinculados; tocar uno abre su expediente.
@Composable
fun TabPacientes(
    estado: PacientesEstado,
    onPaciente: (PacienteResumen) -> Unit,
    onActualizar: () -> Unit,
    onCodigo: () -> Unit,
    fotos: Map<String, Bitmap>,
    // ese paciente le escribió algo que aún no ha visto: su fila lleva un punto
    sinLeer: (PacienteResumen) -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = ALTO_BARRA_INFERIOR + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.pacientes_lista_titulo, estado.pacientes.size),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colores.onBackground,
                )
                TextButton(onClick = onActualizar, enabled = !estado.cargando) {
                    Text(stringResource(R.string.pacientes_actualizar))
                }
            }
        }
        estado.error?.let { item { AlertaError(stringResource(it)) } }
        if (estado.pacientes.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.pacientes_vacio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = onCodigo) { Text(stringResource(R.string.nutri_invitar)) }
                }
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.expediente_pacientes_ayuda),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                )
            }
            items(estado.pacientes, key = { it.id }) { paciente ->
                // el mapa trae Bitmap; se convierte una vez por foto y no en cada recomposición de la fila
                val foto = fotos[paciente.usuarioId]
                FilaPaciente(paciente, remember(foto) { foto?.asImageBitmap() }, sinLeer = sinLeer(paciente), onClick = { onPaciente(paciente) })
            }
        }
    }
}

// Fila del diseño de "mis rutinas": miniatura a la izquierda, nombre con su etiqueta y los datos debajo.
@Composable
private fun FilaPaciente(paciente: PacienteResumen, foto: ImageBitmap?, sinLeer: Boolean, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colores.surfaceVariant)
            .clickable(onClickLabel = stringResource(R.string.nutri_abrir_expediente, paciente.nombre), role = Role.Button, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(colores.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                // su foto de perfil si la tiene; mientras llega, o si no la tiene, la inicial
                if (foto != null) {
                    Image(bitmap = foto, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                } else {
                    Text(
                        text = paciente.nombre.firstOrNull()?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colores.primary,
                    )
                }
            }
            // un mensaje suyo sin leer: el punto va sobre la esquina de su foto
            if (sinLeer) PuntoNuevo(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = paciente.nombre,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleMedium,
                    color = colores.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.nutri_etiqueta_paciente),
                    modifier = Modifier.border(1.dp, colores.outline, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                    color = colores.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(
                text = paciente.correo,
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_calendario), contentDescription = null, modifier = Modifier.size(14.dp), tint = colores.onSurfaceVariant)
                Text(
                    text = stringResource(R.string.nutri_alta, remember(paciente.fechaAlta) { fechaMedia(paciente.fechaAlta) }),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                    color = colores.onSurfaceVariant,
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_flecha_adelante),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = colores.onSurfaceVariant,
        )
    }
}
