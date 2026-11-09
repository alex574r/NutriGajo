package com.example.nutrigajo.data.repository

import android.content.Context
import com.example.nutrigajo.widgets.AlmacenWidget
import com.example.nutrigajo.widgets.actualizarWidgets
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.ZoneId

// Reconstruye la racha desde lo que el paciente tiene guardado en la nube (entrenamientos y caminatas o carreras): los días del teléfono se
// pierden al reinstalar o al entrar con otra cuenta, y antes solo se rehacían si abría el historial o el Modo carrera. Sin red no pasa nada:
// queda lo que ya había en el teléfono. Devuelve cuántos días leyó.
suspend fun sincronizarDiasRacha(contexto: Context): Int {
    val dias = mutableSetOf<Long>()
    val zona = ZoneId.systemDefault()
    try {
        EntrenamientosRepository().listar(LECTURAS).filter { it.cuentaParaLaRacha }.forEach { dias += it.dia() }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
    }
    try {
        CarrerasRepository().listar(LECTURAS).forEach { dias += Instant.ofEpochMilli(it.inicio).atZone(zona).toLocalDate().toEpochDay() }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
    }
    if (dias.isNotEmpty()) {
        AlmacenWidget.agregarDiasCarrera(contexto, dias)
        actualizarWidgets(contexto)
    }
    return dias.size
}

// Cuántos de cada uno se leen: más de un año de actividad diaria alcanza para cualquier racha razonable.
private const val LECTURAS = 400L
