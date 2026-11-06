package com.example.nutrigajo.data.repository

// Caché LRU por tamaño: guarda lo más reciente sin pasar de `limite` (en la unidad de `tamano`; bytes, para un bitmap). Sincronizada porque
// la lee la composición y la llena un hilo de entrada/salida.
class CacheLru<V : Any>(private val limite: Long, private val tamano: (V) -> Long) {
    // con orden de acceso: el primero es el que hace más tiempo que nadie usa
    private val mapa = LinkedHashMap<String, V>(16, 0.75f, true)
    private var usado = 0L

    @Synchronized
    operator fun get(clave: String): V? = mapa[clave]

    @Synchronized
    operator fun set(clave: String, valor: V) {
        val peso = tamano(valor)
        // si no cabe ni solo, no vale la pena sacar a los demás para nada
        if (peso > limite) return
        mapa.put(clave, valor)?.let { usado -= tamano(it) }
        usado += peso
        val menosUsados = mapa.entries.iterator()
        while (usado > limite && menosUsados.hasNext()) {
            usado -= tamano(menosUsados.next().value)
            menosUsados.remove()
        }
    }
}
