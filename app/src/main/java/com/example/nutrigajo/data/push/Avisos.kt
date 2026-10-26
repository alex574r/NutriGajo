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

// Avisos del teléfono. FCM muestra solo los que llegan con la app en segundo plano o cerrada; los que llegan con ella
// abierta los muestra `mostrar`, con el mismo aspecto.
object Avisos {
    // el mismo canal que pide el Worker (channel_id) y el manifiesto (default_notification_channel_id)
    const val CANAL_CITAS = "citas"

    fun crearCanales(contexto: Context) {
        val canal = NotificationChannel(CANAL_CITAS, contexto.getString(R.string.aviso_canal_citas), NotificationManager.IMPORTANCE_HIGH)
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun mostrar(contexto: Context, titulo: String, texto: String) {
        // desde Android 13 el permiso se pide al agendar la primera cita; sin él no se avisa
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        crearCanales(contexto)
        val abrir = PendingIntent.getActivity(
            contexto,
            0,
            Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val aviso = NotificationCompat.Builder(contexto, CANAL_CITAS)
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
