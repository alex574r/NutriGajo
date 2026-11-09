package com.example.nutrigajo.data.actualizacion

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File

// Cómo terminó una instalación, para decirle al usuario qué pasó.
enum class ResultadoInstalacion { EXITO, CANCELADA, FIRMA_DISTINTA, SIN_ESPACIO, OTRO }

// Traduce el estado que manda el instalador de Android. Una actualización firmada con otra llave (la de debug, por ejemplo) llega como
// conflicto («signatures do not match»); incompatible es del teléfono (ABI, versión de Android), no de la firma.
internal fun resultadoDe(estado: Int, mensaje: String?): ResultadoInstalacion = when {
    estado == PackageInstaller.STATUS_SUCCESS -> ResultadoInstalacion.EXITO
    estado == PackageInstaller.STATUS_FAILURE_ABORTED -> ResultadoInstalacion.CANCELADA
    estado == PackageInstaller.STATUS_FAILURE_STORAGE -> ResultadoInstalacion.SIN_ESPACIO
    estado == PackageInstaller.STATUS_FAILURE_CONFLICT || mensaje?.contains("signature", ignoreCase = true) == true -> ResultadoInstalacion.FIRMA_DISTINTA
    else -> ResultadoInstalacion.OTRO
}

// Instala un APK ya descargado y comprobado con una sesión de PackageInstaller. Android solo lo acepta si está firmado con la misma llave
// que la app instalada (esa es la defensa de fondo) y es de este paquete; la primera vez pide el permiso de instalar apps desconocidas y
// cada instalación puede mostrar su ventana de confirmación. El resultado llega a InstalacionReceiver.
object InstaladorApk {
    fun puedeInstalar(contexto: Context): Boolean = contexto.packageManager.canRequestPackageInstalls()

    // La pantalla del sistema donde se da el permiso «Instalar apps desconocidas» a NutriGajo.
    fun intentDePermiso(contexto: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${contexto.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun instalar(contexto: Context, apk: File) {
        val instalador = contexto.packageManager.packageInstaller
        val parametros = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            // solo puede ser una actualización de esta misma app
            setAppPackageName(contexto.packageName)
            setSize(apk.length())
            // desde Android 12 las actualizaciones pueden no pedir confirmación; si el sistema la quiere, la pide igual
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = instalador.createSession(parametros)
        instalador.openSession(id).use { sesion ->
            apk.inputStream().use { entrada ->
                sesion.openWrite("nutrigajo.apk", 0, apk.length()).use { salida ->
                    entrada.copyTo(salida)
                    sesion.fsync(salida)
                }
            }
            // el instalador agrega datos a este aviso: tiene que ser mutable
            val aviso = PendingIntent.getBroadcast(contexto, id, Intent(contexto, InstalacionReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            sesion.commit(aviso.intentSender)
        }
    }
}
