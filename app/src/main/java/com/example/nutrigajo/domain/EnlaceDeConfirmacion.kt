package com.example.nutrigajo.domain

private val ENLACE = Regex("""https://\S+""")

// El enlace del correo de Firebase dentro de lo que la persona pegó: el primer `https://…` hasta el siguiente espacio o salto de línea.
// Va entero (con `&` y `%`): Firebase lo valida y lo rechaza si falta algo. null si no hay ninguno.
fun extraerEnlace(textoPegado: String): String? = ENLACE.find(textoPegado)?.value
