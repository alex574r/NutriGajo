package com.example.nutrigajo.ui.admin.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.shared.perfil.rememberFotoAvatar
import com.example.nutrigajo.domain.Preverificacion
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.SolicitudVerificacion
import com.example.nutrigajo.ui.admin.verificaciones.VerificacionesEstado
import com.example.nutrigajo.ui.components.ALTO_BARRA_INFERIOR
import com.example.nutrigajo.ui.components.TransicionPerfil
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.AvisoInformativo
import com.example.nutrigajo.ui.components.CabeceraUsuario
import com.example.nutrigajo.ui.components.TarjetaDestacada
import com.example.nutrigajo.ui.paciente.inicio.EncabezadoSeccion

private val MARGEN = 20.dp

@Composable
fun TabSolicitudes(
    perfil: Perfil,
    estado: VerificacionesEstado,
    onRevisar: (SolicitudVerificacion) -> Unit,
    onAprobar: (SolicitudVerificacion) -> Unit,
    onRechazar: (SolicitudVerificacion) -> Unit,
    onSep: () -> Unit,
    onPerfil: () -> Unit,
    modifier: Modifier = Modifier,
    transicion: TransicionPerfil? = null,
) {
    val colores = MaterialTheme.colorScheme
    val pendientes = estado.solicitudes.size
    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = ALTO_BARRA_INFERIOR + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            CabeceraUsuario(
                saludo = stringResource(R.string.admin_panel),
                nombre = perfil.nombre,
                verificado = false,
                descripcionAvatar = stringResource(R.string.nutri_abrir_perfil),
                onAvatar = onPerfil,
                iconoAccion = R.drawable.ic_buscar,
                descripcionAccion = stringResource(R.string.admin_abrir_sep),
                onAccion = onSep,
                imagenAvatar = rememberFotoAvatar(perfil),
                transicion = transicion,
            )
        }
        item { AvisoInformativo(stringResource(R.string.admin_aviso)) }
        estado.error?.let { item { AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN)) } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EncabezadoSeccion(stringResource(R.string.admin_resumen))
                TarjetaDestacada(
                    etiqueta = stringResource(R.string.admin_etiqueta),
                    titulo = pluralStringResource(R.plurals.admin_pendientes, pendientes, pendientes),
                    meta = stringResource(if (pendientes > 0) R.string.admin_orden else R.string.admin_al_dia),
                    modifier = Modifier.padding(horizontal = MARGEN),
                    espaciadoTitulo = (-1).sp,
                )
            }
        }
        item { EncabezadoSeccion(stringResource(R.string.admin_solicitudes), contador = "($pendientes)") }
        if (estado.solicitudes.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.admin_vacio),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = MARGEN),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            items(estado.solicitudes, key = { it.uid }) { solicitud ->
                TarjetaSolicitud(
                    solicitud = solicitud,
                    trabajando = estado.trabajando != null,
                    onRevisar = { onRevisar(solicitud) },
                    onAprobar = { onAprobar(solicitud) },
                    onRechazar = { onRechazar(solicitud) },
                    modifier = Modifier.padding(horizontal = MARGEN),
                )
            }
        }
    }
}

// Solicitud con el diseño de las filas de la lista: inicial a la izquierda, datos de la cuenta y de la cédula y los botones.
@Composable
private fun TarjetaSolicitud(
    solicitud: SolicitudVerificacion,
    trabajando: Boolean,
    onRevisar: () -> Unit,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    // el cliente ya filtra, pero el ADMIN no debe fiarse: se recalcula
    val nombreCoincide = Preverificacion.nombreCoincide(solicitud.nombreCuenta, solicitud.nombreCedula)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colores.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(colores.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = solicitud.nombreCuenta.firstOrNull()?.uppercase().orEmpty(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colores.primary,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = solicitud.nombreCuenta,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleMedium,
                        color = colores.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(if (solicitud.esEntrenador) R.string.admin_etiqueta_entrenador else R.string.admin_etiqueta_pendiente),
                        modifier = Modifier.border(1.dp, colores.outline, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                        color = colores.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    text = solicitud.correo,
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (solicitud.esEntrenador) {
                // la credencial del entrenador no está en ningún registro público: el ADMIN la juzga por lo que escribió
                Text(
                    text = stringResource(R.string.admin_credencial, solicitud.credencial),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colores.onBackground,
                )
            } else {
                Text(
                    text = stringResource(R.string.admin_cedula, solicitud.cedula),
                    style = MaterialTheme.typography.titleLarge,
                    color = colores.onBackground,
                )
                Text(
                    text = stringResource(R.string.admin_nombre_cedula, solicitud.nombreCedula),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onBackground,
                )
                Text(
                    text = stringResource(if (nombreCoincide) R.string.admin_nombre_coincide else R.string.admin_nombre_distinto),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (nombreCoincide) colores.onSurfaceVariant else colores.error,
                )
            }
        }
        // primer paso: abre la consulta de la SEP para comparar la cédula y el nombre antes de resolver
        if (!solicitud.esEntrenador) {
            Button(
                onClick = onRevisar,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(21.dp),
            ) {
                Icon(painterResource(R.drawable.ic_buscar), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.admin_revisar))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAprobar,
                enabled = !trabajando,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
            ) { Text(stringResource(R.string.admin_aprobar)) }
            OutlinedButton(
                onClick = onRechazar,
                enabled = !trabajando,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(21.dp),
            ) { Text(stringResource(R.string.admin_rechazar)) }
        }
    }
}
