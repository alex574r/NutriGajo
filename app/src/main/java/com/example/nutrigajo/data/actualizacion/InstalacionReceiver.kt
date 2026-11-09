package com.example.nutrigajo.data.actualizacion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Recibe lo que dice el instalador de Android de una instalación: si pide que el usuario confirme, abre esa ventana; si terminó (bien o mal),
// publica el resultado en `resultado` para que la pantalla de la actualización lo cuente. Si la instalación sale bien, Android reinicia la app.
class InstalacionReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val estado = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (estado == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirmar = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_INTENT)
            }
            confirmar?.let { contexto.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            return
        }
        mutable.value = resultadoDe(estado, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
    }

    companion object {
        private val mutable = MutableStateFlow<ResultadoInstalacion?>(null)

        // el último resultado de una instalación (null: ninguno todavía); compartido por todo el proceso
        val resultado: StateFlow<ResultadoInstalacion?> = mutable.asStateFlow()

        fun limpiar() {
            mutable.value = null
        }
    }
}
