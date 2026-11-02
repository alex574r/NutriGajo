package com.example.nutrigajo.data.salud

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.nutrigajo.domain.model.BarraPulso
import com.example.nutrigajo.domain.model.PeriodoPulso
import com.example.nutrigajo.domain.model.ResumenReloj
import com.example.nutrigajo.domain.model.SesionReloj
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

    // lo que se pide al usuario
    val permisos = setOf(permisoPulso, permisoEjercicio)

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
        val ultima = cliente.readRecords(
            ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(ahora.minus(7, ChronoUnit.DAYS), ahora), ascendingOrder = false, pageSize = 1),
        ).records.firstOrNull()
        val muestra = ultima?.samples?.maxByOrNull { it.time }
        val sesiones = if (permisoEjercicio in cliente.permissionController.getGrantedPermissions()) sesiones(ahora) else emptyList()
        return ResumenReloj(
            ultimoBpm = muestra?.beatsPerMinute?.toInt(),
            ultimaLectura = muestra?.time,
            origen = ultima?.metadata?.dataOrigin?.packageName?.let(::nombreDeApp),
            sesiones = sesiones,
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
