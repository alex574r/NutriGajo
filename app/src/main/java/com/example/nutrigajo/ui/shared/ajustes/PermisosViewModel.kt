package com.example.nutrigajo.ui.shared.ajustes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrigajo.data.actualizacion.InstaladorApk
import com.example.nutrigajo.data.local.AjustesApp
import com.example.nutrigajo.data.salud.Disponibilidad
import com.example.nutrigajo.data.salud.RelojRepository
import com.example.nutrigajo.domain.EstadoPermiso
import com.example.nutrigajo.domain.PermisoApp
import com.example.nutrigajo.domain.estadoDePermiso
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Lo que la pantalla «Permisos» no puede calcular sola: Health Connect (se lee con una consulta que suspende) y el permiso de instalar
// actualizaciones. `reloj` es null mientras se lee; `relojInstalable` dice si Health Connect falta pero se puede instalar desde la tienda.
data class PermisosExtra(
    val reloj: EstadoPermiso? = null,
    val relojInstalable: Boolean = false,
    val actualizaciones: EstadoPermiso = EstadoPermiso.CONCEDIDO,
)

// AndroidViewModel porque Health Connect y el instalador piden un Context.
class PermisosViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = RelojRepository(app)
    private val _estado = MutableStateFlow(PermisosExtra())
    val estado: StateFlow<PermisosExtra> = _estado.asStateFlow()

    // los permisos de Health Connect que se piden, para lanzar su cuadro desde la pantalla
    val permisosReloj get() = repo.permisos

    // Se llama al abrir y cada vez que se vuelve a la app (de los ajustes o de otra pantalla).
    fun cargar() {
        val contexto = getApplication<Application>()
        val actualizaciones = if (InstaladorApk.puedeInstalar(contexto)) EstadoPermiso.CONCEDIDO else EstadoPermiso.FALTA
        viewModelScope.launch {
            val disponibilidad = repo.disponibilidad()
            val reloj = try {
                val disponible = disponibilidad == Disponibilidad.DISPONIBLE
                estadoDePermiso(
                    concedido = disponible && repo.conPermiso(),
                    disponible = disponible,
                    yaSePidio = AjustesApp.yaSePidio(contexto, PermisoApp.RELOJ.grupo),
                    // Health Connect no dice cuándo dejó de mostrar su cuadro: tras pedirlo una vez se manda a sus ajustes (en «Tu reloj» están los dos botones)
                    elSistemaTodaviaExplica = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            _estado.value = PermisosExtra(reloj, relojInstalable = disponibilidad == Disponibilidad.INSTALAR, actualizaciones = actualizaciones)
        }
    }
}
