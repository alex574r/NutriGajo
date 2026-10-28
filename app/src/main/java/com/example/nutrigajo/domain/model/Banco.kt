package com.example.nutrigajo.domain.model

// No renombrar valores: Firestore guarda el nombre del enum como texto.
enum class TipoBanco { COMIDA, EJERCICIO }

enum class MomentoComida { DESAYUNO, COMIDA, CENA }

// Algo que el nutriólogo guarda para reutilizar en sus planes. Comida: `nombre` es el platillo, `texto` su preparación y
// `momento` el tiempo de comida. Ejercicio: `nombre` es el enfoque de la sesión y `texto` la rutina (sin momento).
data class ItemBanco(
    val id: String = "",
    val tipo: TipoBanco,
    val nombre: String,
    val texto: String,
    val momento: MomentoComida? = null,
    val creadoEn: Long = 0,
) {
    // Lo que se escribe en el plan al elegir una comida: su nombre va al principio si el texto aún no lo trae.
    fun textoParaPlan(): String {
        val inicio = nombre.removeSuffix("…")
        return if (tipo == TipoBanco.COMIDA && !texto.startsWith(inicio, ignoreCase = true)) "$nombre: $texto" else texto
    }

    companion object {
        const val MAX_NOMBRE = 80
    }
}

// Nombre con el que se guarda un texto escrito en el plan: lo que va antes de «:» ("Tortilla española: corta 1 papa…") o,
// si no hay, el principio de la primera línea, cortado en una palabra.
fun nombreSugerido(texto: String): String {
    val primera = texto.trim().lineSequence().first().trim()
    val antes = primera.substringBefore(':', "").trim()
    val base = if (antes.isNotEmpty() && antes.length <= ItemBanco.MAX_NOMBRE) antes else primera
    if (base.length <= 40) return base
    val corte = base.take(40)
    return corte.substringBeforeLast(' ', corte).trimEnd() + "…"
}

// Suma `nuevo` a lo que ya hay en un campo: si está vacío lo reemplaza; si no, va en otra línea. Respeta el tope del campo.
fun String.agregando(nuevo: String, max: Int): String = (if (isBlank()) nuevo else "${trimEnd()}\n$nuevo").take(max)

fun OpcionMenu.de(momento: MomentoComida): String = when (momento) {
    MomentoComida.DESAYUNO -> desayuno
    MomentoComida.COMIDA -> comida
    MomentoComida.CENA -> cena
}

fun OpcionMenu.con(momento: MomentoComida, texto: String): OpcionMenu = when (momento) {
    MomentoComida.DESAYUNO -> copy(desayuno = texto)
    MomentoComida.COMIDA -> copy(comida = texto)
    MomentoComida.CENA -> copy(cena = texto)
}
