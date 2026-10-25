package com.example.nutrigajo.ui.shared.citas

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES = Locale.forLanguageTag("es-MX")
private val DIA_LARGO = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES)
private val DIA_CORTO = DateTimeFormatter.ofPattern("EEE", ES)
private val HORA = DateTimeFormatter.ofPattern("HH:mm", ES)

private fun Long.aFecha() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())

// «Miércoles 4 de noviembre»
fun diaLargo(dia: LocalDate): String = DIA_LARGO.format(dia).replaceFirstChar { it.uppercase(ES) }

// «mié»
fun diaCorto(dia: LocalDate): String = DIA_CORTO.format(dia)

// «11:00»
fun horaCorta(inicio: Long): String = HORA.format(inicio.aFecha())

// «Miércoles 4 de noviembre · 11:00»
fun diaYHora(inicio: Long): String = "${diaLargo(inicio.aFecha().toLocalDate())} · ${horaCorta(inicio)}"
