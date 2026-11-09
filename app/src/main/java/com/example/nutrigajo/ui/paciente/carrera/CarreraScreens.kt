package com.example.nutrigajo.ui.paciente.carrera

import android.location.LocationManager
import com.example.nutrigajo.ui.permisos.rememberPermiso
import com.example.nutrigajo.ui.permisos.PermisoUi
import com.example.nutrigajo.ui.permisos.PanelPermiso
import com.example.nutrigajo.domain.PermisoApp
import com.example.nutrigajo.domain.EstadoPermiso
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import android.provider.Settings
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.nutrigajo.R
import com.example.nutrigajo.data.carrera.Fase
import com.example.nutrigajo.data.carrera.RastreoRuta
import com.example.nutrigajo.domain.model.CALENTAMIENTO_SEG
import com.example.nutrigajo.domain.model.Carrera
import com.example.nutrigajo.domain.model.DIA_MS
import com.example.nutrigajo.domain.model.ENFRIAMIENTO_SEG
import com.example.nutrigajo.domain.model.PlanCaminata
import com.example.nutrigajo.domain.model.TipoActividad
import com.example.nutrigajo.domain.model.Tramo
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.BotonPrimario
import com.example.nutrigajo.ui.components.Esqueleto
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.SelectorPestanas
import com.example.nutrigajo.ui.components.alPulsar
import com.example.nutrigajo.ui.nutriologo.inicio.fechaMedia
import com.example.nutrigajo.ui.paciente.inicio.PantallaMetrica
import java.util.Locale

private const val PESO_POR_DEFECTO_KG = 70.0

// «Modo carrera»: registra con el GPS la ruta de una carrera o una caminata y la dibuja en el mapa, guarda un historial y ofrece
// planes: entrenamientos guiados que empiezan al elegirlos. Según la fase del rastreo se ve el inicio, la carrera en curso o el resumen. `pesoKg` (del expediente) afina
// las calorías; sin él se supone 70 kg.
@Composable
fun CarreraFlujo(pesoKg: Double?, onSalir: () -> Unit, vm: CarreraViewModel) {
    val rastreo by RastreoRuta.global.estado.collectAsState()
    val datos by vm.datos.collectAsState()
    val preparacion by vm.preparacion.collectAsState()
    var abierta by rememberSaveable { mutableStateOf<String?>(null) }
    val peso = pesoKg ?: PESO_POR_DEFECTO_KG
    LaunchedEffect(Unit) { vm.cargar() }
    val detalle = datos.carreras.firstOrNull { it.id == abierta }
    val preparando = preparacion
    when {
        rastreo.fase == Fase.CORRIENDO || rastreo.fase == Fase.PAUSADA ->
            CarreraEnCurso(rastreo, peso, vm::pausar, vm::reanudar, vm::terminar, vm::descartar)
        rastreo.fase == Fase.RESUMEN ->
            ResumenCarrera(rastreo, peso, datos.guardando, datos.error, onGuardar = { vm.guardar(peso) }, onDescartar = vm::descartar)
        preparando != null -> PrepararCarrera(preparando, onListo = vm::contar, onCancelar = vm::cancelar)
        detalle != null -> DetalleCarrera(detalle, onVolver = { abierta = null }, onEliminar = { vm.eliminar(detalle.id) { abierta = null } })
        else -> CarreraInicio(
            datos = datos,
            onComenzar = vm::preparar,
            onAbrir = { abierta = it },
            onVolver = onSalir,
        )
    }
}

@Composable
private fun CarreraInicio(
    datos: CarreraDatos,
    onComenzar: (TipoActividad, List<Tramo>) -> Unit,
    onAbrir: (String) -> Unit,
    onVolver: () -> Unit,
) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val arranque = rememberArranque(onComenzar)
    PantallaMetrica(stringResource(R.string.carrera_titulo), onVolver) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SelectorPestanas(listOf(R.string.carrera_pestana_actividad, R.string.carrera_pestana_planes), pestana, { pestana = it })
            datos.error?.let { AlertaError(stringResource(it)) }
            if (arranque.esperaPermiso) {
                PanelPermiso(
                    permiso = PermisoApp.UBICACION_CARRERA,
                    estado = arranque.permiso.estado,
                    onAccion = { if (arranque.permiso.estado == EstadoPermiso.BLOQUEADO) arranque.permiso.abrirAjustes() else arranque.permiso.pedir() },
                )
            }
            if (arranque.gpsApagado) {
                AlertaError(stringResource(R.string.carrera_gps_apagado))
                BotonPrimario(texto = stringResource(R.string.carrera_gps_encender), onClick = arranque.encenderGps, modifier = Modifier.fillMaxWidth(), altura = 48.dp)
            }
            if (pestana == 0) PestanaActividad(datos, arranque.empezar, onAbrir) else PestanaPlanes(arranque.empezar)
        }
    }
}

// Lo que hace falta antes de salir: el servicio en primer plano de ubicación exige el permiso preciso (obligatorio de Modo carrera, ver
// PermisoApp.UBICACION_CARRERA) y el GPS del teléfono tiene que estar encendido. `empezar` lo comprueba y solo entonces llama a `onComenzar`.
// Sin el permiso no lanza el cuadro de golpe: `esperaPermiso` hace que la pantalla muestre el panel que lo explica, y la actividad que se iba a
// empezar sigue sola en cuanto se concede (con el cuadro o desde los ajustes). Con el GPS apagado, `gpsApagado` muestra el aviso con su botón.
private class Arranque(
    val empezar: (TipoActividad, List<Tramo>) -> Unit,
    val permiso: PermisoUi,
    val esperaPermiso: Boolean,
    val gpsApagado: Boolean,
    val encenderGps: () -> Unit,
)

@Composable
private fun rememberArranque(onComenzar: (TipoActividad, List<Tramo>) -> Unit): Arranque {
    val contexto = LocalContext.current
    var gpsApagado by remember { mutableStateOf(false) }
    // lo que se quería empezar cuando faltaba el permiso, para seguir con ello en cuanto se concede
    var pendiente by remember { mutableStateOf<Pair<TipoActividad, List<Tramo>>?>(null) }
    val permiso = rememberPermiso(PermisoApp.UBICACION_CARRERA)

    fun gpsEncendido() = contexto.getSystemService(LocationManager::class.java)?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
    fun salir(tipo: TipoActividad, intervalos: List<Tramo>) {
        if (gpsEncendido()) {
            gpsApagado = false
            onComenzar(tipo, intervalos)
        } else {
            gpsApagado = true
        }
    }
    LaunchedEffect(permiso.estado, pendiente) {
        val actividad = pendiente
        if (permiso.estado == EstadoPermiso.CONCEDIDO && actividad != null) {
            pendiente = null
            salir(actividad.first, actividad.second)
        }
    }
    // al volver de los ajustes de ubicación el aviso se quita si ya se encendió
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (gpsApagado && gpsEncendido()) gpsApagado = false }
    return Arranque(
        empezar = { tipo, intervalos -> if (permiso.estado == EstadoPermiso.CONCEDIDO) salir(tipo, intervalos) else pendiente = tipo to intervalos },
        permiso = permiso,
        esperaPermiso = pendiente != null && permiso.estado != EstadoPermiso.CONCEDIDO,
        gpsApagado = gpsApagado,
        encenderGps = { contexto.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
    )
}

// ---- Actividad: qué se va a hacer, lo de la semana, el botón y el historial

@Composable
private fun PestanaActividad(datos: CarreraDatos, empezar: (TipoActividad, List<Tramo>) -> Unit, onAbrir: (String) -> Unit) {
    val colores = MaterialTheme.colorScheme
    var tipo by rememberSaveable { mutableStateOf(TipoActividad.CAMINAR) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OpcionActividad(R.drawable.ic_caminar, R.string.carrera_caminar, R.string.carrera_caminar_ayuda, tipo == TipoActividad.CAMINAR, { tipo = TipoActividad.CAMINAR }, Modifier.weight(1f))
        OpcionActividad(R.drawable.ic_correr, R.string.carrera_correr, R.string.carrera_correr_ayuda, tipo == TipoActividad.CORRER, { tipo = TipoActividad.CORRER }, Modifier.weight(1f))
    }
    BotonPrimario(
        texto = stringResource(R.string.carrera_comenzar),
        onClick = { empezar(tipo, emptyList()) },
        modifier = Modifier.fillMaxWidth(),
    )
    ResumenSemana(datos.carreras)
    Text(stringResource(R.string.carrera_historial), style = MaterialTheme.typography.titleLarge, color = colores.onBackground)
    when {
        datos.cargando && datos.carreras.isEmpty() -> repeat(3) { Esqueleto(Modifier.fillMaxWidth().height(72.dp), RoundedCornerShape(22.dp)) }
        datos.carreras.isEmpty() ->
            Text(stringResource(R.string.carrera_historial_vacio), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
        else -> datos.carreras.forEach { carrera ->
            val nombre = stringResource(if (carrera.tipo == TipoActividad.CORRER) R.string.carrera_correr else R.string.carrera_caminar)
            FilaAjuste(
                icono = if (carrera.tipo == TipoActividad.CORRER) R.drawable.ic_correr else R.drawable.ic_caminar,
                titulo = stringResource(R.string.carrera_fila_titulo, kilometros(carrera.distanciaM.toDouble()), duracion(carrera.duracionSeg.toLong())),
                etiqueta = stringResource(R.string.carrera_fila_etiqueta, fechaMedia(carrera.inicio), nombre),
                onClick = { onAbrir(carrera.id) },
            )
        }
    }
}

// Caminar o correr: una tarjeta que se marca con el borde naranja, como las opciones del expediente.
@Composable
private fun OpcionActividad(
    @androidx.annotation.DrawableRes icono: Int,
    @StringRes titulo: Int,
    @StringRes ayuda: Int,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .clip(forma)
            .background(if (seleccionada) colores.primaryContainer else colores.surfaceVariant)
            .border(2.dp, if (seleccionada) colores.primary else colores.surfaceVariant, forma)
            .alPulsar(onClick, role = Role.RadioButton)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(icono),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = if (seleccionada) colores.primary else colores.onSurfaceVariant,
        )
        Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
        Text(stringResource(ayuda), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
    }
}

@Composable
private fun ResumenSemana(carreras: List<Carrera>) {
    val colores = MaterialTheme.colorScheme
    // ponytail: la hora se lee al componer; se recalcula la próxima vez que algo recomponga la pantalla
    val semana = remember(carreras) { carreras.filter { it.inicio >= System.currentTimeMillis() - 7 * DIA_MS } }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(colores.surfaceVariant).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.carrera_semana), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
        Row(Modifier.fillMaxWidth()) {
            Cifra(kilometros(semana.sumOf { it.distanciaM }.toDouble()), stringResource(R.string.carrera_unidad_km), stringResource(R.string.carrera_distancia), Modifier.weight(1f))
            Cifra(duracion(semana.sumOf { it.duracionSeg }.toLong()), "", stringResource(R.string.carrera_tiempo), Modifier.weight(1f))
            Cifra(semana.size.toString(), "", stringResource(R.string.carrera_actividades), Modifier.weight(1f))
        }
    }
}

/// ---- Planes: entrenamientos guiados que se empiezan al elegirlos

@Composable
private fun PestanaPlanes(empezar: (TipoActividad, List<Tramo>) -> Unit) {
    val colores = MaterialTheme.colorScheme
    Text(
        text = stringResource(R.string.carrera_planes_intro, CALENTAMIENTO_SEG / 60, ENFRIAMIENTO_SEG / 60),
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.3.em),
        color = colores.onSurfaceVariant,
    )
    // primero los de caminata y luego los que llevan trote y carrera
    listOf(false, true).forEach { conTrote ->
        Text(
            stringResource(if (conTrote) R.string.carrera_planes_intervalos else R.string.carrera_planes_caminatas),
            style = MaterialTheme.typography.titleLarge,
            color = colores.onBackground,
        )
        PlanCaminata.entries.filter { it.conTrote == conTrote }.forEach { plan ->
            TarjetaPlan(plan, onComenzar = { empezar(plan.tipo, plan.tramos) })
        }
    }
}

// Un entrenamiento: qué es, el perfil de la sesión (una barra por tramo), cuánto dura y qué se repite en cada ronda. Elegirlo lo empieza.
@Composable
private fun TarjetaPlan(plan: PlanCaminata, onComenzar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colores.surfaceVariant).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(plan.titulo), style = MaterialTheme.typography.titleLarge, color = colores.onBackground)
        Text(stringResource(plan.ayuda), style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.3.em), color = colores.onSurfaceVariant)
        PerfilSesion(plan.tramos, Modifier.padding(top = 4.dp))
        Text(
            stringResource(R.string.carrera_plan_resumen, plan.minutos, plan.rondas, textoRonda(plan.ronda)),
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 1.3.em),
            color = colores.onSurfaceVariant,
        )
        BotonPrimario(stringResource(R.string.carrera_comenzar), onComenzar, Modifier.fillMaxWidth(), altura = 48.dp)
    }
}

@get:StringRes
private val PlanCaminata.titulo: Int
    get() = when (this) {
        PlanCaminata.PRIMEROS_PASOS -> R.string.carrera_plan_primeros_pasos
        PlanCaminata.RITMO_CONSTANTE -> R.string.carrera_plan_ritmo_constante
        PlanCaminata.RESISTENCIA -> R.string.carrera_plan_resistencia
        PlanCaminata.INTERVALOS_PRINCIPIANTE -> R.string.carrera_plan_intervalos_principiante
        PlanCaminata.INTERVALOS_INTERMEDIO -> R.string.carrera_plan_intervalos_intermedio
        PlanCaminata.INTERVALOS_AVANZADO -> R.string.carrera_plan_intervalos_avanzado
        PlanCaminata.QUEMAGRASA -> R.string.carrera_plan_quemagrasa
    }

@get:StringRes
private val PlanCaminata.ayuda: Int
    get() = when (this) {
        PlanCaminata.PRIMEROS_PASOS -> R.string.carrera_plan_primeros_pasos_ayuda
        PlanCaminata.RITMO_CONSTANTE -> R.string.carrera_plan_ritmo_constante_ayuda
        PlanCaminata.RESISTENCIA -> R.string.carrera_plan_resistencia_ayuda
        PlanCaminata.INTERVALOS_PRINCIPIANTE -> R.string.carrera_plan_intervalos_principiante_ayuda
        PlanCaminata.INTERVALOS_INTERMEDIO -> R.string.carrera_plan_intervalos_intermedio_ayuda
        PlanCaminata.INTERVALOS_AVANZADO -> R.string.carrera_plan_intervalos_avanzado_ayuda
        PlanCaminata.QUEMAGRASA -> R.string.carrera_plan_quemagrasa_ayuda
    }

// ---- Formatos: números con punto decimal, como en el resto de la app

internal fun duracion(segundos: Long): String {
    val h = segundos / 3600
    val m = segundos % 3600 / 60
    val s = segundos % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}

internal fun kilometros(metros: Double): String = String.format(Locale.US, "%.2f", metros / 1000)

// segundos por kilómetro como 6:05; sin dato, guiones
internal fun ritmo(segundosPorKm: Int?): String =
    if (segundosPorKm == null) "--:--" else String.format(Locale.US, "%d:%02d", segundosPorKm / 60, segundosPorKm % 60)
