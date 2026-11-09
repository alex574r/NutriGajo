package com.example.nutrigajo.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.nutrigajo.MainActivity
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.notificacionesPidenPermiso

// Avisos del teléfono. FCM muestra solo los que llegan con la app en segundo plano o cerrada; los que llegan con ella
// abierta los muestra `mostrar`, con el mismo aspecto.
object Avisos {
    // los mismos canales que pide el Worker (channel_id); el de citas es también el del manifiesto (default_notification_channel_id)
    const val CANAL_CITAS = "citas"
    const val CANAL_PLANES = "planes"
    const val CANAL_MENSAJES = "mensajes"

    // `tipo` de los datos del aviso de un plan nuevo; al tocarlo, la app abre el plan
    const val TIPO_PLAN_NUEVO = "PLAN_NUEVO"

    // los avisos de cita llevan un `tipo` que empieza así (CITA_CONFIRMADA, CITA_AGENDADA, CITA_CANCELADA); al tocarlos, el paciente abre «Mis citas»
    const val PREFIJO_TIPO_CITA = "CITA_"

    // `tipo` del aviso de un mensaje nuevo del chat; lleva el `pacienteId` de la conversación y, al tocarlo, la app abre ese chat
    const val TIPO_MENSAJE_NUEVO = "MENSAJE_NUEVO"

    // La conversación que está a la vista (su pacienteId): lo que llega a ese chat no se avisa, ya se está viendo.
    @Volatile
    var chatAbierto: String? = null

    fun crearCanales(contexto: Context) {
        val gestor = contexto.getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannel(NotificationChannel(CANAL_CITAS, contexto.getString(R.string.aviso_canal_citas), NotificationManager.IMPORTANCE_HIGH))
        gestor.createNotificationChannel(NotificationChannel(CANAL_PLANES, contexto.getString(R.string.aviso_canal_planes), NotificationManager.IMPORTANCE_HIGH))
        gestor.createNotificationChannel(NotificationChannel(CANAL_MENSAJES, contexto.getString(R.string.aviso_canal_mensajes), NotificationManager.IMPORTANCE_HIGH))
    }

    // Desde Android 13 los avisos piden permiso y la app no lo trae concedido: sin él el sistema descarta tanto los que muestra FCM
    // como los de `mostrar`. Se pide al abrir el inicio del paciente y al solicitar una cita.
    fun sinPermiso(contexto: Context): Boolean =
        notificacionesPidenPermiso(Build.VERSION.SDK_INT) &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    // `datos` son los de la notificación de FCM: viajan en la intención, como cuando FCM muestra el aviso por su cuenta.
    fun mostrar(contexto: Context, titulo: String, texto: String, datos: Map<String, String> = emptyMap()) {
        if (sinPermiso(contexto)) return
        crearCanales(contexto)
        val abrir = PendingIntent.getActivity(
            contexto,
            0,
            Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).apply { datos.forEach { (k, v) -> putExtra(k, v) } },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val canal = when (datos["tipo"]) {
            TIPO_PLAN_NUEVO -> CANAL_PLANES
            TIPO_MENSAJE_NUEVO -> CANAL_MENSAJES
            else -> CANAL_CITAS
        }
        val aviso = NotificationCompat.Builder(contexto, canal)
            .setSmallIcon(R.drawable.ic_logo_mono)
            .setColor(contexto.getColor(R.color.pulse_orange_50))
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        NotificationManagerCompat.from(contexto).notify(System.currentTimeMillis().toInt(), aviso)
    }
}
