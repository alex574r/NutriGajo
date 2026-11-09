package com.example.nutrigajo.ui.shared.citas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Cita
import com.example.nutrigajo.domain.model.EstadoCita
import com.example.nutrigajo.domain.model.OrigenCita
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.components.ActualizarAlDeslizar
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.AvatarInicial
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.paciente.inicio.EncabezadoSeccion

private val MARGEN = 20.dp

// Las citas del usuario, con el mismo diseño para los dos roles: el paciente ve a quién se las pidió y puede cancelarlas;
// el nutriólogo ve quién se las pidió y confirma, rechaza o cancela. Primero lo que sigue vigente, abajo el historial.
@Composable
fun CitasScreen(
    rol: Rol,
    estado: CitasEstado,
    onActualizar: () -> Unit,
    onCambiar: (Cita, EstadoCita) -> Unit,
    onVolver: () -> Unit,
    // solo el paciente: pasa su cita vigente a otra hora (abre la agenda de ese nutriólogo)
    onReagendar: ((Cita) -> Unit)? = null,
    // la agenda del nutriólogo trae la suya y usa esta lista como una de sus vistas
    conCabecera: Boolean = true,
) {
    val colores = MaterialTheme.colorScheme
    val ahora = remember(estado.citas) { System.currentTimeMillis() }
    val porConfirmar = remember(estado.citas) { estado.citas.filter { it.vigente(ahora) && it.estado == EstadoCita.PENDIENTE } }
    val confirmadas = remember(estado.citas) { estado.citas.filter { it.vigente(ahora) && it.estado == EstadoCita.CONFIRMADA } }
    val historial = remember(estado.citas) { estado.citas.filterNot { it.vigente(ahora) }.reversed() }
    // cambio que espera la confirmación del usuario (rechazar y cancelar no se deshacen)
    var pendiente by remember { mutableStateOf<Pair<Cita, EstadoCita>?>(null) }

    val tarjeta: @Composable (Cita) -> Unit = { cita ->
        TarjetaCita(
            cita = cita,
            rol = rol,
            ahora = ahora,
            trabajando = estado.trabajando == cita.id,
            onCambiar = { nuevo ->
                if (nuevo == EstadoCita.CONFIRMADA) onCambiar(cita, nuevo) else pendiente = cita to nuevo
            },
            onReagendar = onReagendar?.let { reagendar -> { reagendar(cita) } },
        )
    }

    ActualizarAlDeslizar(
        refrescando = estado.cargando,
        onActualizar = onActualizar,
        modifier = Modifier.fillMaxSize().background(colores.background),
    ) {
        Column(Modifier.fillMaxSize()) {
            if (conCabecera) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onVolver) { Text(stringResource(R.string.mapa_volver)) }
                    Text(
                        text = stringResource(if (rol == Rol.NUTRIOLOGO) R.string.citas_titulo_nutriologo else R.string.citas_titulo_paciente),
                        style = MaterialTheme.typography.titleLarge,
                        color = colores.onBackground,
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                estado.error?.let { item { AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN)) } }
                if (estado.cargando && estado.citas.isEmpty()) {
                    items(2) { Esqueleto(Modifier.padding(horizontal = MARGEN).fillMaxWidth().height(116.dp), RoundedCornerShape(22.dp)) }
                } else if (estado.citas.isEmpty() && estado.error == null) {
                    item {
                        Text(
                            text = stringResource(if (rol == Rol.NUTRIOLOGO) R.string.citas_vacio_nutriologo else R.string.citas_vacio_paciente),
                            modifier = Modifier.padding(horizontal = MARGEN),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colores.onSurfaceVariant,
                        )
                    }
                }
                seccion(R.string.citas_por_confirmar, porConfirmar, tarjeta)
                seccion(R.string.citas_confirmadas, confirmadas, tarjeta)
                seccion(R.string.citas_historial, historial, tarjeta)
            }
        }
    }

    pendiente?.let { (cita, nuevo) ->
        DialogoCambio(
            rechazar = nuevo == EstadoCita.RECHAZADA,
            onConfirmar = {
                onCambiar(cita, nuevo)
                pendiente = null
            },
            onCerrar = { pendiente = null },
        )
    }
}

private fun LazyListScope.seccion(titulo: Int, citas: List<Cita>, contenido: @Composable (Cita) -> Unit) {
    if (citas.isEmpty()) return
    item(key = "titulo-$titulo") { EncabezadoSeccion(titulo = stringResource(titulo), contador = "(${citas.size})") }
    items(citas, key = { it.id }) { contenido(it) }
}

@Composable
private fun TarjetaCita(cita: Cita, rol: Rol, ahora: Long, trabajando: Boolean, onCambiar: (EstadoCita) -> Unit, onReagendar: (() -> Unit)?) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(22.dp)
    val vigente = cita.vigente(ahora)
    val nombre = if (rol == Rol.NUTRIOLOGO) cita.pacienteNombre else cita.nutriologoNombre
    Column(
        modifier = Modifier
            .padding(horizontal = MARGEN)
            .fillMaxWidth()
            .background(colores.background, forma)
            .border(1.dp, colores.outlineVariant, forma)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AvatarInicial(nombre = nombre, tamano = 44.dp, estilo = MaterialTheme.typography.titleMedium)
            Text(
                text = nombre,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = colores.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            EtiquetaEstado(cita.estado, vigente)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_calendario), contentDescription = null, modifier = Modifier.size(20.dp), tint = colores.onBackground)
            Text(diaYHora(cita.inicio), style = MaterialTheme.typography.bodyMedium, color = colores.onBackground)
        }
        OrigenYNota(cita, rol)
        if (vigente) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rol == Rol.NUTRIOLOGO && cita.estado == EstadoCita.PENDIENTE) {
                    BotonCita(R.string.cita_rechazar, principal = false, enabled = !trabajando) { onCambiar(EstadoCita.RECHAZADA) }
                    BotonCita(R.string.cita_confirmar, principal = true, enabled = !trabajando) { onCambiar(EstadoCita.CONFIRMADA) }
                } else {
                    // el paciente puede pasar su cita a otra hora o cancelarla; solo tiene una vigente
                    if (rol == Rol.PACIENTE && onReagendar != null) BotonCita(R.string.cita_reagendar, principal = false, enabled = !trabajando) { onReagendar() }
                    BotonCita(R.string.cita_cancelar, principal = false, enabled = !trabajando) { onCambiar(EstadoCita.CANCELADA) }
                }
            }
        }
    }
}

// Quién la agendó, si fue el nutriólogo («Agendada por tu nutriólogo» al paciente y «Agendada por ti» a él), y la nota que dejó.
@Composable
internal fun OrigenYNota(cita: Cita, rol: Rol, modifier: Modifier = Modifier) {
    if (cita.origen != OrigenCita.NUTRIOLOGO && cita.nota.isEmpty()) return
    val colores = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (cita.origen == OrigenCita.NUTRIOLOGO) {
            Text(
                text = stringResource(if (rol == Rol.NUTRIOLOGO) R.string.cita_agendada_por_ti else R.string.cita_agendada_por_nutriologo),
                style = MaterialTheme.typography.labelLarge,
                color = colores.primary,
            )
        }
        if (cita.nota.isNotEmpty()) Text(cita.nota, style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
    }
}

// Naranja lleno si está confirmada, naranja suave si espera al nutriólogo y gris cuando ya no hay nada que atender.
@Composable
internal fun EtiquetaEstado(estado: EstadoCita, vigente: Boolean) {
    val colores = MaterialTheme.colorScheme
    val (texto, fondo, color) = when {
        estado == EstadoCita.CONFIRMADA && vigente -> Triple(R.string.cita_estado_confirmada, colores.primary, colores.onPrimary)
        estado == EstadoCita.PENDIENTE && vigente -> Triple(R.string.cita_estado_pendiente, colores.primaryContainer, colores.onPrimaryContainer)
        else -> Triple(
            when (estado) {
                EstadoCita.PENDIENTE -> R.string.cita_estado_vencida
                EstadoCita.CONFIRMADA -> R.string.cita_estado_confirmada
                EstadoCita.RECHAZADA -> R.string.cita_estado_rechazada
                EstadoCita.CANCELADA -> R.string.cita_estado_cancelada
            },
            colores.surfaceVariant,
            colores.onSurfaceVariant,
        )
    }
    Text(
        text = stringResource(texto),
        modifier = Modifier.background(fondo, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
        color = color,
    )
}

@Composable
internal fun RowScope.BotonCita(texto: Int, principal: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (principal) colores.primary else colores.surfaceVariant,
            contentColor = if (principal) colores.onPrimary else colores.onBackground,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) { Text(stringResource(texto), style = MaterialTheme.typography.titleSmall) }
}

@Composable
internal fun DialogoCambio(rechazar: Boolean, onConfirmar: () -> Unit, onCerrar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = colores.background,
        title = { Text(stringResource(if (rechazar) R.string.cita_dialogo_rechazar else R.string.cita_dialogo_cancelar)) },
        text = { Text(stringResource(R.string.cita_dialogo_ayuda)) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(stringResource(if (rechazar) R.string.cita_dialogo_si_rechazar else R.string.cita_dialogo_si_cancelar), color = colores.error)
            }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text(stringResource(R.string.cita_dialogo_volver)) } },
    )
}
