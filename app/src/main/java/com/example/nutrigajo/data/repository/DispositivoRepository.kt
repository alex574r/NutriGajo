package com.example.nutrigajo.data.repository

import android.util.Log
import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.time.ZoneId

// El teléfono de la cuenta para los avisos: su token de FCM y su zona horaria viven en usuarios/{uid}, que solo lee su dueño
// y el Worker (con la cuenta de servicio). Con ellos el Worker le manda, por ejemplo, que su cita quedó confirmada y la hora en su zona.
// ponytail: un solo teléfono por cuenta (el último donde se abrió la app).
class DispositivoRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val mensajeria: FirebaseMessaging = FirebaseMessaging.getInstance(),
) {
    // Al abrir la app con sesión: guarda el token actual de este teléfono en el perfil.
    suspend fun registrar() {
        if (auth.currentUser == null) return
        guardarSiHaySesion(mensajeria.token.await())
    }

    suspend fun guardarSiHaySesion(token: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(FirestorePaths.USUARIOS).document(uid)
            .update(mapOf("fcmToken" to token, "fcmZona" to ZoneId.systemDefault().id))
            .await()
        Log.i("Dispositivo", "Teléfono registrado para los avisos (cuenta ${uid.take(4)}…)")
    }

    // Al cerrar la sesión: este teléfono deja de recibir los avisos de la cuenta. Se borra del perfil y se invalida el token
    // (así, aunque no se pueda escribir en el perfil, el token viejo ya no sirve para quien entre después en este teléfono).
    suspend fun olvidar() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                db.collection(FirestorePaths.USUARIOS).document(uid).update("fcmToken", FieldValue.delete()).await()
            } finally {
                mensajeria.deleteToken().await()
            }
        } else {
            mensajeria.deleteToken().await()
        }
    }
}
