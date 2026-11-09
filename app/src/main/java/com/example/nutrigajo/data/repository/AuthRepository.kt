package com.example.nutrigajo.data.repository

import com.example.nutrigajo.data.remote.firebase.firestore.FirestorePaths
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
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
        return Perfil(
            uid = uid,
            nombre = doc.getString("nombre").orEmpty(),
            correo = doc.getString("correo").orEmpty(),
            rol = rol,
            fechaAlta = doc.getLong("fechaAlta"),
            portadaVersion = doc.getLong("portadaVersion"),
            avatarVersion = doc.getLong("avatarVersion"),
            nombres = doc.getString("nombres").orEmpty(),
            apellidos = doc.getString("apellidos").orEmpty(),
        )
    }

    // Cambia el nombre de la cuenta: `nombre` es nombres y apellidos juntos, como al registrarse. Los nombres que ya se copiaron a
    // otros documentos (la lista de pacientes de un nutriólogo, por ejemplo) se quedan como estaban.
    suspend fun actualizarNombre(nombres: String, apellidos: String) {
        val uid = auth.currentUser?.uid ?: error("Sin sesión")
        db.collection(FirestorePaths.USUARIOS).document(uid).update(
            mapOf(
                "nombre" to "$nombres $apellidos".trim(),
                "nombres" to nombres,
                "apellidos" to apellidos,
                "ultimaModificacion" to System.currentTimeMillis(),
            ),
        ).await()
    }

    // Firebase manda el correo con el enlace para elegir una contraseña nueva.
    suspend fun restablecerContrasena(correo: String) {
        auth.sendPasswordResetEmail(correo).await()
    }

    // Para una acción delicada (borrar la cuenta) Firebase manda al correo de la cuenta un enlace de confirmación. No es un código: Firebase solo
    // manda enlaces. Requiere «Enlace de correo» activado en Authentication.
    // ponytail: sin enlaces de la app (App Links) todavía: la persona abre el correo, copia el enlace y lo pega en la app; si la prueba con el
    // proyecto real muestra que los enlaces de Hosting abren la app, aquí se agrega el `linkDomain` y el filtro del manifiesto.
    suspend fun enviarEnlaceDeConfirmacion() {
        val correo = auth.currentUser?.email ?: error("Sin sesión")
        val ajustes = ActionCodeSettings.newBuilder()
            .setUrl("https://${FirebaseApp.getInstance().options.projectId}.firebaseapp.com/confirmar")
            .setHandleCodeInApp(true)
            .build()
        auth.sendSignInLinkToEmail(correo, ajustes).await()
    }

    // Vuelve a confirmar la identidad con el enlace del correo: sube la hora de confirmación de la sesión (`auth_time`) y el Worker acepta el
    // borrado. Lanza `EnlaceInvalidoException` si el texto no es un enlace de Firebase y la de Firebase si es de otra cuenta o ya venció.
    suspend fun confirmarConEnlace(enlace: String) {
        val usuario = auth.currentUser ?: error("Sin sesión")
        val correo = usuario.email ?: error("Sin correo")
        if (!auth.isSignInWithEmailLink(enlace)) throw EnlaceInvalidoException()
        usuario.reauthenticate(EmailAuthProvider.getCredentialWithLink(correo, enlace)).await()
    }

    fun cerrarSesion() = auth.signOut()
}

// Lo que se pegó no es un enlace de confirmación de Firebase.
class EnlaceInvalidoException : Exception("El enlace no es de Firebase")
