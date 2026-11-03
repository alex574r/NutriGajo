package com.example.nutrigajo.ui.nutriologo.inicio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrigajo.R
import com.example.nutrigajo.ui.shared.perfil.rememberFotoAvatar
import com.example.nutrigajo.domain.model.CodigoVinculacion
import com.example.nutrigajo.domain.model.EstadoCita
import com.example.nutrigajo.domain.model.PacienteResumen
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.components.ALTO_BARRA_INFERIOR
import com.example.nutrigajo.ui.components.TransicionPerfil
import com.example.nutrigajo.ui.components.AccionTarjeta
import com.example.nutrigajo.ui.components.AvisoInformativo
import com.example.nutrigajo.ui.components.CabeceraUsuario
import com.example.nutrigajo.ui.components.Etiqueta
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.TarjetaDestacada
import com.example.nutrigajo.ui.nutriologo.pacientes.PacientesEstado
import com.example.nutrigajo.ui.nutriologo.verificacion.TarjetaVerificacion
import com.example.nutrigajo.ui.nutriologo.verificacion.VerificacionEstado
import com.example.nutrigajo.ui.paciente.inicio.EncabezadoSeccion
import com.example.nutrigajo.ui.shared.citas.CitasEstado
import com.example.nutrigajo.ui.theme.Gray100

private val MARGEN = 20.dp
private const val MAX_RECIENTES = 6

// Inicio del nutriólogo con el diseño de las secciones del paciente: cabecera, atajos, aviso, el código de
// vinculación como tarjeta destacada, las citas por atender, los pacientes recientes y el mapa. Todo viene de datos reales.
@Composable
fun TabInicio(
    perfil: Perfil,
    estado: PacientesEstado,
    verificacion: VerificacionEstado,
    aprobada: Boolean,
    onPerfil: () -> Unit,
    onBuscar: () -> Unit,
    onPacientes: () -> Unit,
    onCodigo: () -> Unit,
    onGenerar: () -> Unit,
    onMapa: () -> Unit,
    onPlatillos: () -> Unit,
    citas: CitasEstado,
    onCitas: () -> Unit,
    onVerificar: () -> Unit,
    onActualizar: () -> Unit,
    onPaciente: (PacienteResumen) -> Unit,
    modifier: Modifier = Modifier,
    transicion: TransicionPerfil? = null,
) {
    val recientes = remember(estado.pacientes) { estado.pacientes.sortedByDescending { it.fechaAlta }.take(MAX_RECIENTES) }
    val ahora = remember(citas.citas) { System.currentTimeMillis() }
    val porConfirmar = remember(citas.citas) { citas.citas.count { it.vigente(ahora) && it.estado == EstadoCita.PENDIENTE } }
    val confirmadas = remember(citas.citas) { citas.citas.count { it.vigente(ahora) && it.estado == EstadoCita.CONFIRMADA } }
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
                saludo = stringResource(R.string.nutri_listo),
                nombre = perfil.nombre,
                verificado = aprobada,
                descripcionAvatar = stringResource(R.string.nutri_abrir_perfil),
                onAvatar = onPerfil,
                iconoAccion = R.drawable.ic_buscar,
                descripcionAccion = stringResource(R.string.nutri_buscar),
                onAccion = onBuscar,
                imagenAvatar = rememberFotoAvatar(perfil),
                transicion = transicion,
            )
        }
        item { Atajos(aprobada, onPacientes, onCodigo, onCitas, onMapa, onPlatillos) }
        item {
            AvisoInformativo(
                if (estado.pacientes.isEmpty()) stringResource(R.string.nutri_aviso_vacio)
                else pluralStringResource(R.plurals.nutri_aviso_pacientes, estado.pacientes.size, estado.pacientes.size),
            )
        }
        // aprobada la cuenta, la tarjeta de verificación ya no aporta: el estado se ve en el perfil
        if (!aprobada) item { TarjetaVerificacion(verificacion, onVerificar, Modifier.padding(horizontal = MARGEN), onActualizar) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EncabezadoSeccion(stringResource(R.string.nutri_codigo_titulo))
                CodigoDestacado(estado.codigo, estado.generando, onGenerar, Modifier.padding(horizontal = MARGEN))
            }
        }
        // las citas solo llegan a quien está en el mapa, es decir, con la cuenta verificada
        if (aprobada) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EncabezadoSeccion(stringResource(R.string.nutri_citas_titulo))
                    FilaAjuste(
                        icono = R.drawable.ic_calendario,
                        titulo = if (porConfirmar > 0) {
                            pluralStringResource(R.plurals.nutri_citas_pendientes, porConfirmar, porConfirmar)
                        } else {
                            stringResource(R.string.nutri_citas_sin_pendientes)
                        },
                        modifier = Modifier.padding(horizontal = MARGEN),
                        etiqueta = if (confirmadas > 0) {
                            pluralStringResource(R.plurals.nutri_citas_confirmadas, confirmadas, confirmadas)
                        } else {
                            stringResource(R.string.nutri_citas_sin_confirmadas)
                        },
                        onClick = onCitas,
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EncabezadoSeccion(
                    titulo = stringResource(R.string.nutri_pacientes_titulo),
                    contador = "(${estado.pacientes.size})",
                    verTodas = estado.pacientes.isNotEmpty(),
                    etiquetaVerTodas = R.string.nutri_ver_todos,
                    onVerTodas = onPacientes,
                )
                if (recientes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.pacientes_vacio),
                        modifier = Modifier.padding(horizontal = MARGEN),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = MARGEN),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(recientes, key = { it.id }) { TarjetaPaciente(it, onClick = { onPaciente(it) }) }
                    }
                }
            }
        }
        if (aprobada) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EncabezadoSeccion(stringResource(R.string.nutri_mapa_titulo))
                    TarjetaMapa(onMapa, Modifier.padding(horizontal = MARGEN))
                }
            }
        }
    }
}

// Atajos a lo que el nutriólogo usa más; los de citas, mapa y platillos solo aparecen con la cuenta verificada.
@Composable
private fun Atajos(aprobada: Boolean, onPacientes: () -> Unit, onCodigo: () -> Unit, onCitas: () -> Unit, onMapa: () -> Unit, onPlatillos: () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = MARGEN),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Atajo(R.drawable.ic_pacientes, R.string.nutri_atajo_pacientes, onPacientes)
        Atajo(R.drawable.ic_compartir, R.string.nutri_atajo_codigo, onCodigo)
        if (aprobada) {
            Atajo(R.drawable.ic_calendario, R.string.nutri_atajo_citas, onCitas)
            Atajo(R.drawable.ic_ubicacion, R.string.nutri_atajo_mapa, onMapa)
            Atajo(R.drawable.ic_documento, R.string.nutri_atajo_platillos, onPlatillos)
        }
    }
}

@Composable
private fun Atajo(icono: Int, texto: Int, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .clip(forma)
            .border(1.dp, colores.outline, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(18.dp), tint = colores.primary)
        Text(stringResource(texto), style = MaterialTheme.typography.labelLarge, color = colores.onBackground)
    }
}

// Muestra el código vigente; el botón naranja lo comparte (o genera uno si no hay) y el círculo genera uno nuevo.
@Composable
private fun CodigoDestacado(codigo: CodigoVinculacion?, generando: Boolean, onGenerar: () -> Unit, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val invitacion = codigo?.let { stringResource(R.string.pacientes_compartir_texto, it.formateado) }
    TarjetaDestacada(
        etiqueta = stringResource(if (codigo != null) R.string.nutri_codigo_vigente else R.string.nutri_codigo_sin),
        titulo = codigo?.formateado ?: stringResource(R.string.nutri_codigo_vacio_titulo),
        meta = if (codigo != null) {
            stringResource(R.string.pacientes_codigo_vigencia, remember(codigo.expiraEn) { fechaMedia(codigo.expiraEn) })
        } else {
            stringResource(R.string.nutri_codigo_vacio_ayuda)
        },
        modifier = modifier,
        espaciadoTitulo = if (codigo != null) 3.sp else (-1).sp,
        principal = AccionTarjeta(
            icono = if (codigo != null) R.drawable.ic_compartir else R.drawable.ic_agregar,
            descripcion = stringResource(if (codigo != null) R.string.pacientes_compartir else R.string.pacientes_generar),
            habilitada = !generando,
        ) { if (invitacion != null) compartirCodigo(contexto, invitacion) else onGenerar() },
        secundaria = if (codigo != null) {
            AccionTarjeta(R.drawable.ic_refrescar, stringResource(R.string.pacientes_generar_nuevo), !generando, onGenerar)
        } else {
            null
        },
    )
}

// Tarjeta oscura del carrusel de pacientes recientes; la flecha naranja abre su expediente.
@Composable
private fun TarjetaPaciente(paciente: PacienteResumen, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(width = 208.dp, height = 156.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(Gray100)
            .clickable(onClickLabel = stringResource(R.string.nutri_abrir_expediente, paciente.nombre), role = Role.Button, onClick = onClick),
    ) {
        Etiqueta(
            texto = stringResource(R.string.nutri_etiqueta_paciente),
            modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 14.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 14.dp, end = 62.dp)) {
            Text(
                text = paciente.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(painterResource(R.drawable.ic_calendario), contentDescription = null, modifier = Modifier.size(14.dp), tint = colores.outline)
                Text(
                    text = remember(paciente.fechaAlta) { fechaMedia(paciente.fechaAlta) },
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                    color = colores.outline,
                    maxLines = 1,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 14.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colores.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_flecha_adelante), contentDescription = null, modifier = Modifier.size(18.dp), tint = colores.onPrimary)
        }
    }
}

// Tarjeta naranja del mapa: el plato queda a la derecha y el texto a la izquierda.
@Composable
private fun TarjetaMapa(onMapa: () -> Unit, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(colores.primary)
            .clickable(onClickLabel = stringResource(R.string.ubicacion_tarjeta_boton), role = Role.Button, onClick = onMapa),
    ) {
        Image(
            painter = painterResource(R.drawable.inicio_comida),
            contentDescription = null,
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = 48.dp).size(190.dp),
            contentScale = ContentScale.Fit,
        )
        Etiqueta(
            texto = stringResource(R.string.nutri_verificado),
            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 16.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp).width(176.dp)) {
            Text(
                text = stringResource(R.string.ubicacion_titulo),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.ubicacion_tarjeta_boton),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}
