package com.example.nutrigajo.data.salud

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.nutrigajo.domain.model.ActividadHoy
import com.example.nutrigajo.domain.model.BarraPulso
import com.example.nutrigajo.domain.model.NocheSueno
import com.example.nutrigajo.domain.model.Oxigeno
import com.example.nutrigajo.domain.model.PeriodoPulso
import com.example.nutrigajo.domain.model.ResumenReloj
import com.example.nutrigajo.domain.model.SesionReloj
import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class Disponibilidad { DISPONIBLE, INSTALAR, NO_DISPONIBLE }

private val ESPANOL = Locale.forLanguageTag("es-MX")

// El reloj no habla con la app: su propia app (Mi Fitness, Samsung Health…) manda el pulso y los entrenamientos a Health Connect, el
// almacén de salud de Android, y aquí solo se lee (con el permiso que el usuario concede). Antes de Android 14 Health Connect es una
// app aparte que hay que instalar.
class RelojRepository(private val contexto: Context) {
    private val permisoPulso = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val permisoEjercicio = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val permisoPasos = HealthPermission.getReadPermission(StepsRecord::class)
    private val permisoDistancia = HealthPermission.getReadPermission(DistanceRecord::class)
    private val permisoCalorias = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val permisoSueno = HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val permisoOxigeno = HealthPermission.getReadPermission(OxygenSaturationRecord::class)

    // lo que se pide al usuario
    val permisos = setOf(permisoPulso, permisoEjercicio, permisoPasos, permisoDistancia, permisoCalorias, permisoSueno, permisoOxigeno)

    private val cliente by lazy { HealthConnectClient.getOrCreate(contexto) }

    fun disponibilidad(): Disponibilidad = when (HealthConnectClient.getSdkStatus(contexto)) {
        HealthConnectClient.SDK_AVAILABLE -> Disponibilidad.DISPONIBLE
        // sin la app de Health Connect (o desactualizada) se manda a la tienda
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Disponibilidad.INSTALAR
        else -> Disponibilidad.NO_DISPONIBLE
    }

    // Con el permiso del pulso ya se puede mostrar lo principal; el de los entrenamientos es opcional.
    suspend fun conPermiso(): Boolean = permisoPulso in cliente.permissionController.getGrantedPermissions()

    suspend fun resumen(): ResumenReloj {
        val ahora = Instant.now()
        val concedidos = cliente.permissionController.getGrantedPermissions()
        val ultima = cliente.readRecords(
            ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(ahora.minus(7, ChronoUnit.DAYS), ahora), ascendingOrder = false, pageSize = 1),
        ).records.firstOrNull()
        val muestra = ultima?.samples?.maxByOrNull { it.time }
        return ResumenReloj(
            ultimoBpm = muestra?.beatsPerMinute?.toInt(),
            ultimaLectura = muestra?.time,
            origen = ultima?.metadata?.dataOrigin?.packageName?.let(::nombreDeApp),
            // cada métrica por su lado: sin su permiso (o si falla) solo ella falta, no todo el resumen
            sesiones = if (permisoEjercicio in concedidos) leer { sesiones(ahora) }.orEmpty() else emptyList(),
            actividad = leer { actividadHoy(ahora, concedidos) },
            sueno = if (permisoSueno in concedidos) leer { sueno(ahora) }.orEmpty() else emptyList(),
            oxigeno = if (permisoOxigeno in concedidos) leer { oxigeno(ahora) } else null,
            faltanPermisos = !concedidos.containsAll(permisos),
        )
    }

    private suspend fun <T> leer(bloque: suspend () -> T): T? =
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    // Lo de hoy desde las 0:00. Health Connect suma sin repetir lo que dos apps cuentan dos veces; solo se piden las métricas con permiso.
    private suspend fun actividadHoy(ahora: Instant, concedidos: Set<String>): ActividadHoy? {
        val metricas = buildSet {
            if (permisoPasos in concedidos) add(StepsRecord.COUNT_TOTAL)
            if (permisoDistancia in concedidos) add(DistanceRecord.DISTANCE_TOTAL)
            if (permisoCalorias in concedidos) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            if (permisoEjercicio in concedidos) add(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL)
        }
        if (metricas.isEmpty()) return null
        val inicio = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val r = cliente.aggregate(AggregateRequest(metricas, TimeRangeFilter.between(inicio, ahora)))
        return ActividadHoy(
            pasos = (r[StepsRecord.COUNT_TOTAL] ?: 0L).toInt(),
            km = ((r[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0) / 1000).toFloat(),
            kcal = (r[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0).toInt(),
            minutos = (r[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes() ?: 0L).toInt(),
        )
    }

    // Las noches de la última semana: cada sesión de sueño cuenta en el día en que terminó y solo suma el tiempo dormido (sin los
    // ratos despierto); si el reloj no manda etapas se cuenta la sesión entera.
    private suspend fun sueno(ahora: Instant): List<NocheSueno> {
        val zona = ZoneId.systemDefault()
        val dormido = setOf(
            SleepSessionRecord.STAGE_TYPE_SLEEPING,
            SleepSessionRecord.STAGE_TYPE_LIGHT,
            SleepSessionRecord.STAGE_TYPE_DEEP,
            SleepSessionRecord.STAGE_TYPE_REM,
        )
        return cliente.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(ahora.minus(8, ChronoUnit.DAYS), ahora), ascendingOrder = false, pageSize = 30),
        ).records
            .groupBy { it.endTime.atZone(zona).toLocalDate() }
            .map { (dia, sesiones) ->
                val minutos = sesiones.sumOf { s ->
                    if (s.stages.isEmpty()) Duration.between(s.startTime, s.endTime).toMinutes()
                    else s.stages.filter { it.stage in dormido }.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
                }
                NocheSueno(dia, minutos.toInt())
            }
            .filter { it.minutos > 0 }
            .sortedByDescending { it.fecha }
            .take(7)
    }

    private suspend fun oxigeno(ahora: Instant): Oxigeno? {
        val lecturas = cliente.readRecords(
            ReadRecordsRequest(OxygenSaturationRecord::class, TimeRangeFilter.between(ahora.minus(24, ChronoUnit.HOURS), ahora), ascendingOrder = false, pageSize = 200),
        ).records
        val ultima = lecturas.firstOrNull() ?: return null
        return Oxigeno(
            porcentaje = ultima.percentage.value.toInt(),
            promedio = lecturas.map { it.percentage.value }.average().toInt(),
            lectura = ultima.time,
        )
    }

    private suspend fun sesiones(ahora: Instant): List<SesionReloj> =
        cliente.readRecords(
            ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(ahora.minus(30, ChronoUnit.DAYS), ahora), ascendingOrder = false, pageSize = 10),
        ).records.map { s ->
            // el pulso de ese rato; sin lecturas (o sin permiso para leerlas) la sesión se muestra sin él
            val pulso = runCatching {
                cliente.aggregate(AggregateRequest(setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MAX), TimeRangeFilter.between(s.startTime, s.endTime)))
            }.getOrNull()
            SesionReloj(
                titulo = s.title?.takeIf { it.isNotBlank() } ?: nombreDeEjercicio(s.exerciseType),
                inicio = s.startTime.toEpochMilli(),
                minutos = Duration.between(s.startTime, s.endTime).toMinutes().toInt(),
                promedio = pulso?.get(HeartRateRecord.BPM_AVG)?.toInt(),
                maximo = pulso?.get(HeartRateRecord.BPM_MAX)?.toInt(),
            )
        }

    // Hoy: 24 barras, una por hora. 7 días: una por día, con hoy al final. Las horas y los días sin lecturas llevan bpm null.
    suspend fun barras(periodo: PeriodoPulso): List<BarraPulso> {
        val zona = ZoneId.systemDefault()
        return if (periodo == PeriodoPulso.HOY) {
            val inicio = LocalDate.now(zona).atStartOfDay(zona).toInstant()
            val porHora = cliente.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(setOf(HeartRateRecord.BPM_AVG), TimeRangeFilter.between(inicio, Instant.now()), Duration.ofHours(1)),
            ).associate { ZonedDateTime.ofInstant(it.startTime, zona).hour to it.result[HeartRateRecord.BPM_AVG]?.toInt() }
            (0..23).map { h -> BarraPulso(if (h % 6 == 0) "$h" else "", String.format(Locale.US, "%02d:00", h), porHora[h]) }
        } else {
            val hoy = LocalDate.now(zona)
            val primero = hoy.minusDays(6)
            val porDia = cliente.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    setOf(HeartRateRecord.BPM_AVG),
                    TimeRangeFilter.between(primero.atStartOfDay(), LocalDateTime.now(zona)),
                    Period.ofDays(1),
                ),
            ).associate { it.startTime.toLocalDate() to it.result[HeartRateRecord.BPM_AVG]?.toInt() }
            (0L..6L).map { i ->
                val dia = primero.plusDays(i)
                BarraPulso(
                    etiqueta = dia.dayOfWeek.getDisplayName(TextStyle.NARROW, ESPANOL).uppercase(ESPANOL),
                    detalle = DateTimeFormatter.ofPattern("EEE d MMM", ESPANOL).format(dia),
                    bpm = porDia[dia],
                )
            }
        }
    }

    // ponytail: las apps de reloj más comunes; el resto se muestra con su identificador
    private fun nombreDeApp(paquete: String): String = when (paquete) {
        "com.mi.healthglobal" -> "Mi Fitness"
        "com.xiaomi.wearable" -> "Xiaomi Wear"
        "com.sec.android.app.shealth" -> "Samsung Health"
        "com.google.android.apps.fitness" -> "Google Fit"
        "com.fitbit.FitbitMobile" -> "Fitbit"
        "com.garmin.android.apps.connectmobile" -> "Garmin Connect"
        "com.huawei.health" -> "Huawei Health"
        else -> paquete.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    private fun nombreDeEjercicio(tipo: Int): String = when (tipo) {
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Caminata"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Carrera"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING, ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Ciclismo"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Natación"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Senderismo"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING, ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Pesas"
        else -> "Entrenamiento"
    }
}
