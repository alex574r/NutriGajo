package com.example.nutrigajo.data.remote.firebase.firestore

/**
 * Espejo de Room en Cloud Firestore. El id del documento es el id de la entidad.
 *
 * usuarios/{uid}                      rol, verificado, activo… (ADMIN se crea a mano en la consola)
 * codigosVinculacion/{codigo}         nutriologoId, expiraEn, usado, canjeadoPor… (el id es el código)
 * verificaciones/{uid}                solicitud de verificación del nutriólogo (cédula, estado PENDIENTE|APROBADA|RECHAZADA)
 * cedulas/{numero}                    reserva de una cédula por un nutriólogo (una cédula, una cuenta)
 * directorio/{uid}                    consultorio de un nutriólogo verificado en el mapa (nombre, lat, lng)
 *   └─ ocupados/{inicio}              hora ya pedida (citaId, inicio); sin datos del paciente, la ve cualquier sesión
 * citas/{citaId}                      cita que un paciente pide a un nutriólogo (estado PENDIENTE|CONFIRMADA|RECHAZADA|CANCELADA)
 * pacientes/{pacienteId}              nutriologoId, usuarioId, correoVinculacion…
 *   ├─ mediciones/{id}
 *   ├─ planes/{id}                    plan de alimentación y ejercicio enviado (enviadoPor, enviadoEn, contenido…); su PDF vive en R2
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
    const val VERIFICACIONES = "verificaciones"
    const val CEDULAS = "cedulas"
    const val DIRECTORIO = "directorio"
    const val OCUPADOS = "ocupados"
    const val CITAS = "citas"
    const val PACIENTES = "pacientes"
    const val MEDICIONES = "mediciones"
    const val PLANES = "planes"
    const val RUTINAS = "rutinas"
    const val FOTOS = "fotos"
    const val CUMPLIMIENTOS = "cumplimientos"
}
