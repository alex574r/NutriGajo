package com.example.nutrigajo.data.remote.firebase.firestore

/**
 * Espejo de Room en Cloud Firestore. El id del documento es el id de la entidad.
 *
 * usuarios/{uid}                      rol, verificado, activo… (ADMIN se crea a mano en la consola)
 * codigosVinculacion/{codigo}         nutriologoId, expiraEn, usado, canjeadoPor… (el id es el código)
 * pacientes/{pacienteId}              nutriologoId, usuarioId, correoVinculacion…
 *   ├─ mediciones/{id}
 *   ├─ planes/{id}                    comidas[] embebidas
 *   ├─ rutinas/{id}                   ejercicios[] embebidos
 *   ├─ fotos/{id}                     solo metadatos; el archivo vive en R2
 *   └─ cumplimientos/{id}
 *
 * Acceso (firestore.rules): ADMIN lee y suspende usuarios; un subdocumento hereda el acceso de su
 * paciente (nutriologoId == uid o usuarioId == uid). Nadie puede asignarse ADMIN al registrarse.
 */
object FirestorePaths {
    const val USUARIOS = "usuarios"
    const val CODIGOS_VINCULACION = "codigosVinculacion"
    const val PACIENTES = "pacientes"
    const val MEDICIONES = "mediciones"
    const val PLANES = "planes"
    const val RUTINAS = "rutinas"
    const val FOTOS = "fotos"
    const val CUMPLIMIENTOS = "cumplimientos"
}
