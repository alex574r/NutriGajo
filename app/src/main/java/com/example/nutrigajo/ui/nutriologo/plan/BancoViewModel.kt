package com.example.nutrigajo.ui.nutriologo.plan

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.BancoRepository
import com.example.nutrigajo.domain.model.ItemBanco
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BancoEstado(
    // solo antes de la primera lectura; después se muestra lo ya leído mientras se vuelve a leer
    val cargando: Boolean = true,
    val items: List<ItemBanco> = emptyList(),
    val guardando: Boolean = false,
    @param:StringRes val error: Int? = null,
)

// El banco de comidas y ejercicios del nutriólogo, compartido por el panel del plan (que guarda y elige de él) y la pantalla
// donde se administra.
class BancoViewModel(private val banco: BancoRepository = BancoRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(BancoEstado())
    val estado: StateFlow<BancoEstado> = _estado.asStateFlow()

    fun cargar() {
        viewModelScope.launch { leer() }
    }

    private suspend fun leer() {
        try {
            val items = banco.listar()
            _estado.update { it.copy(cargando = false, items = items, error = null) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _estado.update { it.copy(cargando = false, error = R.string.banco_error_cargar) }
        }
    }

    // Guarda (crea o actualiza) y avisa si salió bien.
    fun guardar(item: ItemBanco, fin: (Boolean) -> Unit = {}) = escribir(R.string.banco_error_guardar, fin) { banco.guardar(item) }

    fun eliminar(id: String, fin: (Boolean) -> Unit = {}) = escribir(R.string.banco_error_eliminar, fin) { banco.eliminar(id) }

    private fun escribir(@StringRes fallo: Int, fin: (Boolean) -> Unit, accion: suspend () -> Unit) {
        if (_estado.value.guardando) return
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, error = null) }
            val bien = try {
                accion()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            _estado.update { it.copy(guardando = false, error = if (bien) null else fallo) }
            if (bien) leer()
            fin(bien)
        }
    }
}
