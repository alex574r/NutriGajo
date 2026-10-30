package com.example.nutrigajo.data.push

import com.example.nutrigajo.data.repository.DispositivoRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Recibe lo que manda FCM: el token nuevo de este teléfono y los avisos que llegan con la app abierta.
class NutriMessagingService : FirebaseMessagingService() {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // FCM renueva el token de vez en cuando; si hay una sesión, el perfil guarda el nuevo (si no, se guarda al iniciar sesión).
    override fun onNewToken(token: String) {
        alcance.launch { runCatching { DispositivoRepository().guardarSiHaySesion(token) } }
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        // un mensaje del chat que se está viendo ya está en pantalla: no se avisa
        if (mensaje.data["tipo"] == Avisos.TIPO_MENSAJE_NUEVO && mensaje.data["pacienteId"] == Avisos.chatAbierto) return
        mensaje.notification?.let { Avisos.mostrar(this, it.title.orEmpty(), it.body.orEmpty(), mensaje.data) }
    }

    override fun onDestroy() {
        alcance.cancel()
        super.onDestroy()
    }
}
