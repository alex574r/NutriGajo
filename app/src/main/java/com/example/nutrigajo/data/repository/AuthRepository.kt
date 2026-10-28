package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Cuentas con Firebase Auth (correo y contraseña) y su perfil en usuarios/{uid}.
// ponytail: sin copia en Room todavía; el perfil se lee de Firestore cuando hace falta.
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    // Firebase conserva la sesión entre arranques
    val sesionIniciada: Boolean get() = auth.currentUser != null

    suspend fun ingresar(correo: String, contrasena: String) {
        auth.signInWithEmailAndPassword(correo, contrasena).await()
    }

    // Crea la cuenta y su perfil. El rol solo puede ser NUTRIOLOGO o PACIENTE (lo exigen también las reglas);
    // si el perfil falla se borra la cuenta para no dejar una sin rol.
    // `nombre` (nombres y apellidos juntos) es el que usan las demás pantallas y las reglas; nombres y apellidos también se
    // guardan por separado, porque después no se pueden separar con certeza («María José Hernández Maya»).
    suspend fun registrar(nombres: String, apellidos: String, correo: String, contrasena: String, rol: Rol) {
        require(rol != Rol.ADMIN) { "ADMIN se da de alta a mano" }
        val usuario = auth.createUserWithEmailAndPassword(correo, contrasena).await().user
            ?: error("Firebase no devolvió el usuario")
        val ahora = System.currentTimeMillis()
        val perfil = mapOf(
            "nombre" to "$nombres $apellidos",
            "nombres" to nombres,
            "apellidos" to apellidos,
            "correo" to correo,
            "rol" to rol.name,
            "verificado" to false,
            "activo" to true,
            "avisoPrivacidadAceptadoEn" to ahora,
            "fechaAlta" to ahora,
            "ultimaModificacion" to ahora,
        )
        try {
            db.collection(FirestorePaths.USUARIOS).document(usuario.uid).set(perfil).await()
        } catch (e: Exception) {
            runCatching { usuario.delete().await() }
            throw e
        }
    }

    suspend fun perfil(): Perfil? {
        val uid = auth.currentUser?.uid ?: return null
        val doc = db.collection(FirestorePaths.USUARIOS).document(uid).get().await()
        val rol = doc.getString("rol")?.let { runCatching { Rol.valueOf(it) }.getOrNull() } ?: return null
        return Perfil(uid, doc.getString("nombre").orEmpty(), doc.getString("correo").orEmpty(), rol, doc.getLong("fechaAlta"), doc.getLong("portadaVersion"), doc.getLong("avatarVersion"))
    }

    // Firebase manda el correo con el enlace para elegir una contraseña nueva.
    suspend fun restablecerContrasena(correo: String) {
        auth.sendPasswordResetEmail(correo).await()
    }

    fun cerrarSesion() = auth.signOut()
}
