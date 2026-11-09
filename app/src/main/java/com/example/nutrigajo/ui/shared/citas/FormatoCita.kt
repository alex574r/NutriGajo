package com.example.nutrigajo.ui.shared.citas

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale.forLanguageTag("es-MX")
private val DIA_LARGO = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES)
private val DIA_CORTO = DateTimeFormatter.ofPattern("EEE", ES)
private val HORA = DateTimeFormatter.ofPattern("HH:mm", ES)
private val MES_Y_ANIO = DateTimeFormatter.ofPattern("LLLL yyyy", ES)
private val DIA_Y_MES = DateTimeFormatter.ofPattern("d MMM", ES)

private fun Long.aFecha() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())

// «Miércoles 4 de noviembre»
fun diaLargo(dia: LocalDate): String = DIA_LARGO.format(dia).replaceFirstChar { it.uppercase(ES) }

// «mié»
fun diaCorto(dia: LocalDate): String = DIA_CORTO.format(dia)

// «11:00»
fun horaCorta(inicio: Long): String = HORA.format(inicio.aFecha())

// «Miércoles 4 de noviembre · 11:00»
fun diaYHora(inicio: Long): String = "${diaLargo(inicio.aFecha().toLocalDate())} · ${horaCorta(inicio)}"

// «Vie 6 nov · 10:00»: lo que cabe en una línea de una fila (el formato largo se cortaba y la hora no se veía)
fun diaYHoraCorto(inicio: Long): String {
    val dia = inicio.aFecha().toLocalDate()
    return "${diaCorto(dia).replaceFirstChar { it.uppercase(ES) }} ${DIA_Y_MES.format(dia)} · ${horaCorta(inicio)}"
}

// El día de calendario (en la zona del teléfono) en que empieza una cita.
fun Long.aDia(): LocalDate = aFecha().toLocalDate()

// «Noviembre 2026»
fun mesYAnio(dia: LocalDate): String = MES_Y_ANIO.format(dia).replaceFirstChar { it.uppercase(ES) }

// «26 oct – 1 nov», la semana que empieza en ese lunes
fun rangoSemana(lunes: LocalDate): String = "${DIA_Y_MES.format(lunes)} – ${DIA_Y_MES.format(lunes.plusDays(6))}"

// «L», «M», «X»… la inicial del día de la semana
fun inicialDia(dia: LocalDate): String = dia.dayOfWeek.getDisplayName(TextStyle.NARROW, ES).uppercase(ES)
