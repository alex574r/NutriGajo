package com.example.nutrigajo.domain

import java.text.Normalizer

// Filtro automático previo a la revisión del ADMIN: descarta lo evidente (formato y nombre ajeno)
// para que el ADMIN solo vea solicitudes que merecen abrir la consulta de la SEP.
// ponytail: el nombre debe contener todas las palabras del de la cuenta; un "Dra." en la cuenta lo haría fallar.
object Preverificacion {
    private val FORMATO_CEDULA = Regex("[0-9]{6,8}")
    private val MARCAS = Regex("\\p{M}+")
    private val SEPARADORES = Regex("[^a-z]+")

    // La cédula sin espacios, o null si no son 6 a 8 dígitos.
    fun cedula(escrita: String): String? =
        escrita.filter { !it.isWhitespace() }.takeIf { FORMATO_CEDULA.matches(it) }

    // Sin acentos ni mayúsculas: "ANA MARÍA LÓPEZ" contiene a "Ana López".
    fun nombreCoincide(cuenta: String, enCedula: String): Boolean {
        val palabrasCuenta = palabras(cuenta)
        return palabrasCuenta.isNotEmpty() && palabras(enCedula).containsAll(palabrasCuenta)
    }

    private fun palabras(texto: String): Set<String> =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(MARCAS, "")
            .lowercase()
            .split(SEPARADORES)
            .filter { it.isNotEmpty() }
            .toSet()
}
