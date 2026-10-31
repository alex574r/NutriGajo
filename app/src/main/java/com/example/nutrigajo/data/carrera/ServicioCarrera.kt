package com.example.nutrigajo.data.carrera

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.nutrigajo.MainActivity
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.PuntoRuta

// Servicio en primer plano que pide el GPS mientras se corre: con la pantalla apagada y la app en segundo plano el sistema deja de
// dar ubicaciones a quien no tiene uno, y la ruta saldría con huecos. Solo registra: los puntos van al RastreoRuta.global, que es lo
// que lee la pantalla. Se arranca al comenzar o reanudar y se detiene al pausar o terminar (la pausa apaga el GPS).
class ServicioCarrera : Service() {
    private val escucha = LocationListener { ubicacion ->
        RastreoRuta.global.ubicar(
            punto = PuntoRuta(ubicacion.latitude, ubicacion.longitude),
            tiempoMs = ubicacion.elapsedRealtimeNanos / 1_000_000,
            precisionM = if (ubicacion.hasAccuracy()) ubicacion.accuracy else null,
            velocidadMs = if (ubicacion.hasSpeed()) ubicacion.speed else null,
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission") // quien arranca el servicio ya comprobó el permiso (ver `iniciarServicio`)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val gestor = getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannel(NotificationChannel(CANAL, getString(R.string.carrera_canal), NotificationManager.IMPORTANCE_LOW))
        val abrir = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val aviso = NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_logo_mono)
            .setColor(getColor(R.color.pulse_orange_50))
            .setContentTitle(getString(R.string.carrera_aviso_titulo))
            .setContentText(getString(R.string.carrera_aviso_texto))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(abrir)
            .build()
        startForeground(ID_AVISO, aviso, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        val ubicaciones = getSystemService(LocationManager::class.java)
        ubicaciones.removeUpdates(escucha)
        // un fix por segundo, sin distancia mínima: el filtro de pasos es el de Ruta.paso
        runCatching { ubicaciones.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, escucha, Looper.getMainLooper()) }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        getSystemService(LocationManager::class.java).removeUpdates(escucha)
        super.onDestroy()
    }

    companion object {
        private const val CANAL = "carrera"
        private const val ID_AVISO = 7001

        fun iniciarServicio(contexto: Context) =
            ContextCompat.startForegroundService(contexto, Intent(contexto, ServicioCarrera::class.java))

        fun detenerServicio(contexto: Context) {
            contexto.stopService(Intent(contexto, ServicioCarrera::class.java))
        }
    }
}
