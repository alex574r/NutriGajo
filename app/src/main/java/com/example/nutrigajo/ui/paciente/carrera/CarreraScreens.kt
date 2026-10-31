package com.example.nutrigajo.ui.paciente.carrera

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.nutrigajo.R
import com.example.nutrigajo.data.carrera.Fase
import com.example.nutrigajo.data.carrera.RastreoRuta
import com.example.nutrigajo.data.repository.PlanElegido
import com.example.nutrigajo.domain.model.Carrera
import com.example.nutrigajo.domain.model.DIA_MS
import com.example.nutrigajo.domain.model.PlanCaminata
import com.example.nutrigajo.domain.model.TipoActividad
import com.example.nutrigajo.domain.model.progreso
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

// «Modo carrera»: registra con el GPS la ruta de una carrera o una caminata y la dibuja en el mapa, guarda un historial y propone
// planes de caminata. Según la fase del rastreo se ve el inicio, la carrera en curso o el resumen. `pesoKg` (del expediente) afina
// las calorías; sin él se supone 70 kg.
@Composable
fun CarreraFlujo(pesoKg: Double?, onSalir: () -> Unit, vm: CarreraViewModel) {
    val rastreo by RastreoRuta.global.estado.collectAsState()
    val datos by vm.datos.collectAsState()
    var abierta by rememberSaveable { mutableStateOf<String?>(null) }
    val peso = pesoKg ?: PESO_POR_DEFECTO_KG
    LaunchedEffect(Unit) { vm.cargar() }
    val detalle = datos.carreras.firstOrNull { it.id == abierta }
    when {
        rastreo.fase == Fase.CORRIENDO || rastreo.fase == Fase.PAUSADA ->
            CarreraEnCurso(rastreo, peso, vm::pausar, vm::reanudar, vm::terminar, vm::descartar)
        rastreo.fase == Fase.RESUMEN ->
            ResumenCarrera(rastreo, peso, datos.guardando, datos.error, onGuardar = { vm.guardar(peso) }, onDescartar = vm::descartar)
        detalle != null -> DetalleCarrera(detalle, onVolver = { abierta = null }, onEliminar = { vm.eliminar(detalle.id) { abierta = null } })
        else -> CarreraInicio(
            datos = datos,
            onComenzar = vm::comenzar,
            onAbrir = { abierta = it },
            onElegirPlan = vm::elegirPlan,
            onQuitarPlan = vm::quitarPlan,
            onVolver = onSalir,
        )
    }
}

@Composable
private fun CarreraInicio(
    datos: CarreraDatos,
    onComenzar: (TipoActividad) -> Unit,
    onAbrir: (String) -> Unit,
    onElegirPlan: (PlanCaminata) -> Unit,
    onQuitarPlan: () -> Unit,
    onVolver: () -> Unit,
) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    PantallaMetrica(stringResource(R.string.carrera_titulo), onVolver) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SelectorPestanas(listOf(R.string.carrera_pestana_actividad, R.string.carrera_pestana_planes), pestana, { pestana = it })
            datos.error?.let { AlertaError(stringResource(it)) }
            if (pestana == 0) PestanaActividad(datos, onComenzar, onAbrir) else PestanaPlanes(datos, onElegirPlan, onQuitarPlan)
        }
    }
}

// ---- Actividad: qué se va a hacer, lo de la semana, el botón y el historial

@Composable
private fun PestanaActividad(datos: CarreraDatos, onComenzar: (TipoActividad) -> Unit, onAbrir: (String) -> Unit) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    var tipo by rememberSaveable { mutableStateOf(TipoActividad.CAMINAR) }
    @StringRes var aviso by remember { mutableStateOf<Int?>(null) }

    // El servicio en primer plano de ubicación exige el permiso preciso; el GPS del teléfono tiene que estar encendido.
    fun empezar() {
        val gps = contexto.getSystemService(LocationManager::class.java)?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        if (gps) {
            aviso = null
            onComenzar(tipo)
        } else {
            aviso = R.string.carrera_gps_apagado
        }
    }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { concedidos ->
        if (concedidos[Manifest.permission.ACCESS_FINE_LOCATION] == true) empezar() else aviso = R.string.carrera_permiso
    }
    val preciso = { ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OpcionActividad(R.drawable.ic_caminar, R.string.carrera_caminar, R.string.carrera_caminar_ayuda, tipo == TipoActividad.CAMINAR, { tipo = TipoActividad.CAMINAR }, Modifier.weight(1f))
        OpcionActividad(R.drawable.ic_correr, R.string.carrera_correr, R.string.carrera_correr_ayuda, tipo == TipoActividad.CORRER, { tipo = TipoActividad.CORRER }, Modifier.weight(1f))
    }
    aviso?.let { AlertaError(stringResource(it)) }
    BotonPrimario(
        texto = stringResource(R.string.carrera_comenzar),
        onClick = {
            if (preciso()) empezar() else pedir.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        },
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

// ---- Planes de caminata

@Composable
private fun PestanaPlanes(datos: CarreraDatos, onElegir: (PlanCaminata) -> Unit, onQuitar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    datos.plan?.let { PlanActivo(it, datos.carreras, onQuitar) }
    PlanCaminata.entries.forEach { plan -> TarjetaPlan(plan, activo = datos.plan?.plan == plan, onElegir = { onElegir(plan) }) }
    Text(stringResource(R.string.carrera_plan_nota), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
}

// El plan que sigue, en la tarjeta oscura: la semana en que va y un círculo por cada caminata que pide (lleno si ya la hizo).
@Composable
private fun PlanActivo(elegido: PlanElegido, carreras: List<Carrera>, onQuitar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val progreso = elegido.plan.progreso(elegido.inicio, carreras, System.currentTimeMillis())
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(colores.onBackground).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.carrera_tu_plan), style = MaterialTheme.typography.bodySmall, color = colores.background.copy(alpha = 0.7f))
        Text(stringResource(elegido.plan.titulo), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = colores.background)
        if (progreso.terminado) {
            Text(stringResource(R.string.carrera_plan_terminado), style = MaterialTheme.typography.bodyMedium, color = colores.background)
        } else {
            Text(
                stringResource(R.string.carrera_plan_semana, progreso.semana + 1, elegido.plan.semanas.size),
                style = MaterialTheme.typography.titleMedium,
                color = colores.background,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(progreso.meta.sesiones) { i ->
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(if (i < progreso.hechas) colores.primary else colores.background.copy(alpha = 0.2f)),
                )
            }
        }
        Text(
            stringResource(R.string.carrera_plan_avance, progreso.hechas.coerceAtMost(progreso.meta.sesiones), progreso.meta.sesiones, progreso.meta.minutos),
            style = MaterialTheme.typography.bodySmall,
            color = colores.background.copy(alpha = 0.7f),
        )
        TextButton(onClick = onQuitar) {
            Text(stringResource(R.string.carrera_plan_dejar), style = MaterialTheme.typography.titleMedium, color = colores.background)
        }
    }
}

@Composable
private fun TarjetaPlan(plan: PlanCaminata, activo: Boolean, onElegir: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val sesiones = plan.semanas.map { it.sesiones }
    val minutos = plan.semanas.map { it.minutos }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colores.surfaceVariant).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(plan.titulo), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = colores.onBackground)
            if (activo) {
                // el tag del diseño: fondo naranja claro y texto naranja
                Text(
                    text = stringResource(R.string.carrera_plan_en_curso),
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(colores.primaryContainer).padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = colores.primary,
                )
            }
        }
        Text(stringResource(plan.ayuda), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
        Text(
            stringResource(R.string.carrera_plan_resumen, plan.semanas.size, rango(sesiones.min(), sesiones.max()), rango(minutos.min(), minutos.max())),
            style = MaterialTheme.typography.bodySmall,
            color = colores.onSurfaceVariant,
        )
        if (!activo) BotonPrimario(stringResource(R.string.carrera_plan_elegir), onElegir, Modifier.fillMaxWidth(), altura = 48.dp)
    }
}

private fun rango(minimo: Int, maximo: Int) = if (minimo == maximo) "$minimo" else "$minimo a $maximo"

@get:StringRes
private val PlanCaminata.titulo: Int
    get() = when (this) {
        PlanCaminata.PRIMEROS_PASOS -> R.string.carrera_plan_primeros_pasos
        PlanCaminata.RITMO_CONSTANTE -> R.string.carrera_plan_ritmo_constante
        PlanCaminata.RESISTENCIA -> R.string.carrera_plan_resistencia
    }

@get:StringRes
private val PlanCaminata.ayuda: Int
    get() = when (this) {
        PlanCaminata.PRIMEROS_PASOS -> R.string.carrera_plan_primeros_pasos_ayuda
        PlanCaminata.RITMO_CONSTANTE -> R.string.carrera_plan_ritmo_constante_ayuda
        PlanCaminata.RESISTENCIA -> R.string.carrera_plan_resistencia_ayuda
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
