package com.example.nutrigajo.ui.shared.ejercicios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.repository.CatalogoRepository
import com.example.nutrigajo.data.repository.esIdCatalogo
import com.example.nutrigajo.domain.model.Ejercicio
import com.example.nutrigajo.domain.model.EntradaCatalogo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// `indice`: todo el catálogo (solo título, zona, tipo y equipo); `fichas`: los ejercicios del catálogo cuya ficha completa ya se bajó, por id.
data class CatalogoEstado(
    val cargando: Boolean = true,
    val indice: List<EntradaCatalogo> = emptyList(),
    val fichas: Map<String, Ejercicio> = emptyMap(),
    // se están bajando fichas: lo asignado que aún no tiene la suya está en camino, no «ya no disponible»
    val cargandoFichas: Boolean = false,
)

// El catálogo de ejercicios en la nube para las pantallas que lo usan (elegir un ejercicio, el inicio del paciente). Nada lanza: sin red queda
// lo que haya en el teléfono, o un índice vacío.
class CatalogoViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CatalogoRepository(directorio = app.cacheDir)
    private val _estado = MutableStateFlow(CatalogoEstado())
    val estado: StateFlow<CatalogoEstado> = _estado.asStateFlow()

    // El índice: la copia del teléfono si ya es la última versión; si no, la nueva. Una segunda llamada con el índice ya cargado no hace nada.
    fun cargarIndice() {
        if (_estado.value.indice.isNotEmpty()) return
        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            val indice = repo.indice()
            _estado.update { it.copy(cargando = false, indice = indice) }
        }
    }

    // Las fichas de esos ejercicios (ids del catálogo) que aún no se tienen; una que no se pueda bajar simplemente no aparece en `fichas`.
    fun cargarFichas(ids: Collection<String>) {
        val faltan = ids.filter { esIdCatalogo(it) && it !in _estado.value.fichas }.distinct()
        if (faltan.isEmpty()) return
        _estado.update { it.copy(cargandoFichas = true) }
        viewModelScope.launch {
            val nuevas = faltan.mapNotNull { id -> repo.ficha(id)?.let { id to it } }.toMap()
            _estado.update { it.copy(fichas = it.fichas + nuevas, cargandoFichas = false) }
        }
    }
}
