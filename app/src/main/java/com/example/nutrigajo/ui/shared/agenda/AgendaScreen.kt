package com.example.nutrigajo.ui.shared.agenda

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Cita
import com.example.nutrigajo.domain.model.EstadoCita
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.AvatarInicial
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.SelectorPestanas
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.shared.citas.BotonCita
import com.example.nutrigajo.ui.shared.citas.CitasEstado
import com.example.nutrigajo.ui.shared.citas.CitasScreen
import com.example.nutrigajo.ui.shared.citas.DialogoCambio
import com.example.nutrigajo.ui.shared.citas.EtiquetaEstado
import com.example.nutrigajo.ui.shared.citas.aDia
import com.example.nutrigajo.ui.shared.citas.diaLargo
import com.example.nutrigajo.ui.shared.citas.horaCorta
import java.time.LocalDate

private val VISTAS = listOf(R.string.agenda_mes, R.string.agenda_semana, R.string.agenda_lista)
private const val UNA_HORA_MS = 3_600_000L

// Las citas de cada rol en un calendario del mes, en una semana con sus horas y en la lista de siempre; el día elegido se comparte
// entre el mes y la semana. Tocar una cita abre su detalle: el nutriólogo confirma, rechaza o cancela, y el paciente reagenda
// (`onReagendar`, que abre la agenda de su nutriólogo) o cancela.
@Composable
fun AgendaScreen(
    rol: Rol,
    estado: CitasEstado,
    onActualizar: () -> Unit,
    onCambiar: (Cita, EstadoCita) -> Unit,
    onVolver: () -> Unit,
    onReagendar: ((Cita) -> Unit)? = null,
) {
    BackHandler(onBack = onVolver)
    val colores = MaterialTheme.colorScheme
    val hoy = remember { LocalDate.now() }
    var vista by rememberSaveable { mutableIntStateOf(0) }
    var diaElegido by rememberSaveable { mutableLongStateOf(hoy.toEpochDay()) }
    var citaAbierta by rememberSaveable { mutableStateOf<String?>(null) }
    val dia = LocalDate.ofEpochDay(diaElegido)
    val ahora = remember(estado.citas) { System.currentTimeMillis() }
    val porDia = remember(estado.citas) { estado.citas.groupBy { it.inicio.aDia() } }

    Column(Modifier.fillMaxSize().background(colores.background).statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.align(Alignment.CenterStart).size(40.dp).clip(CircleShape).background(colores.surfaceVariant).alPulsar(onVolver),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_chevron_izquierda), stringResource(R.string.mapa_volver), tint = colores.onSurfaceVariant)
            }
            Text(
                text = stringResource(if (rol == Rol.NUTRIOLOGO) R.string.agenda_titulo else R.string.citas_titulo_paciente),
                style = MaterialTheme.typography.titleMedium,
                color = colores.onBackground,
            )
            // llevar el calendario a hoy
            Text(
                text = stringResource(R.string.agenda_hoy),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colores.surfaceVariant)
                    .alPulsar({ diaElegido = hoy.toEpochDay() }, minimo = 0.94f)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                style = MaterialTheme.typography.labelLarge,
                color = colores.onBackground,
            )
        }
        SelectorPestanas(VISTAS, vista, { vista = it }, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        estado.error?.let { AlertaError(stringResource(it), Modifier.padding(top = 8.dp)) }
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
            when (vista) {
                0 -> VistaMes(rol, dia, hoy, porDia, ahora, estado.cargando, onActualizar, { diaElegido = it.toEpochDay() }, { citaAbierta = it.id }, Modifier.fillMaxSize())
                1 -> VistaSemana(rol, dia, hoy, porDia, ahora, estado.cargando, onActualizar, { diaElegido = it.toEpochDay() }, { citaAbierta = it.id }, Modifier.fillMaxSize())
                else -> CitasScreen(rol, estado, onActualizar, onCambiar, onVolver, onReagendar, conCabecera = false)
            }
        }
    }

    // la cita se busca por su id en cada vuelta: así el detalle refleja lo último que se leyó
    citaAbierta?.let { id ->
        estado.citas.firstOrNull { it.id == id }?.let { cita ->
            DetalleCita(
                rol = rol,
                cita = cita,
                ahora = ahora,
                trabajando = estado.trabajando == cita.id,
                onCambiar = { nuevo ->
                    onCambiar(cita, nuevo)
                    citaAbierta = null
                },
                onReagendar = onReagendar?.let { reagendar ->
                    {
                        citaAbierta = null
                        reagendar(cita)
                    }
                },
                onCerrar = { citaAbierta = null },
            )
        }
    }
}

// El detalle de una cita en una hoja: quién es, cuándo y lo que se puede hacer con ella. Rechazar y cancelar piden confirmar antes
// porque no se deshacen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalleCita(
    rol: Rol,
    cita: Cita,
    ahora: Long,
    trabajando: Boolean,
    onCambiar: (EstadoCita) -> Unit,
    onReagendar: (() -> Unit)?,
    onCerrar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val vigente = cita.vigente(ahora)
    var pendiente by remember { mutableStateOf<EstadoCita?>(null) }
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colores.background,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val nombre = if (rol == Rol.NUTRIOLOGO) cita.pacienteNombre else cita.nutriologoNombre
                AvatarInicial(nombre = nombre, tamano = 56.dp, estilo = MaterialTheme.typography.titleLarge)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(nombre, style = MaterialTheme.typography.titleLarge, color = colores.onBackground, maxLines = 2)
                    Row { EtiquetaEstado(cita.estado, vigente) }
                }
            }
            FilaAjuste(R.drawable.ic_calendario, titulo = diaLargo(cita.inicio.aDia()), etiqueta = stringResource(R.string.agenda_fecha))
            FilaAjuste(R.drawable.ic_reloj, titulo = "${horaCorta(cita.inicio)} – ${horaCorta(cita.inicio + UNA_HORA_MS)}", etiqueta = stringResource(R.string.agenda_hora))
            if (vigente) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    if (rol == Rol.NUTRIOLOGO && cita.estado == EstadoCita.PENDIENTE) {
                        BotonCita(R.string.cita_rechazar, principal = false, enabled = !trabajando) { pendiente = EstadoCita.RECHAZADA }
                        BotonCita(R.string.cita_confirmar, principal = true, enabled = !trabajando) { onCambiar(EstadoCita.CONFIRMADA) }
                    } else {
                        // el paciente puede pasar su cita a otra hora o cancelarla
                        if (rol == Rol.PACIENTE && onReagendar != null) {
                            BotonCita(R.string.cita_reagendar, principal = false, enabled = !trabajando) { onReagendar() }
                        }
                        BotonCita(R.string.cita_cancelar, principal = false, enabled = !trabajando) { pendiente = EstadoCita.CANCELADA }
                    }
                }
            }
        }
    }
    pendiente?.let { nuevo ->
        DialogoCambio(
            rechazar = nuevo == EstadoCita.RECHAZADA,
            onConfirmar = {
                pendiente = null
                onCambiar(nuevo)
            },
            onCerrar = { pendiente = null },
        )
    }
}
