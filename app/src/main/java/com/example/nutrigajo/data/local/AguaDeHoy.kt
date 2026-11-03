package com.example.nutrigajo.data.local

import android.content.Context
import java.time.LocalDate

// El agua que lleva el paciente hoy, guardada en el teléfono (por cuenta) para que no se pierda al cerrar la app. Vale solo el día en
// que se guardó: al día siguiente se empieza en 0.
// ponytail: no se sincroniza con Firestore ni entre teléfonos; el dato es de hoy y el nutriólogo no lo ve.
object AguaDeHoy {
    private fun prefs(contexto: Context) = contexto.getSharedPreferences("agua_de_hoy", Context.MODE_PRIVATE)

    fun leer(contexto: Context, uid: String, hoy: LocalDate = LocalDate.now()): Int {
        val p = prefs(contexto)
        return if (p.getLong("$uid.dia", -1) == hoy.toEpochDay()) p.getInt("$uid.ml", 0) else 0
    }

    fun guardar(contexto: Context, uid: String, ml: Int, hoy: LocalDate = LocalDate.now()) {
        prefs(contexto).edit().putLong("$uid.dia", hoy.toEpochDay()).putInt("$uid.ml", ml).apply()
    }
}
