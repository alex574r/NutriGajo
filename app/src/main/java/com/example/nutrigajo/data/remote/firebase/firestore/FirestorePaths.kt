package com.example.nutrigajo.data.remote.firebase.firestore

/**
 * Espejo de Room en Cloud Firestore. El id del documento es el id de la entidad.
 *
 * usuarios/{uid}                      rol, verificado, activo… (ADMIN se crea a mano en la consola)
 *   ├─ banco/{id}                     comida o ejercicio que el nutriólogo guarda para reutilizar en sus planes; solo lo ve él
 *   ├─ entrenamientos/{id}            rutina de «Entrenar ahora» terminada (fecha, enfoque, minutos, kcal, hechos, total); solo la ve su dueño
 *   ├─ carreras/{id}                  carrera o caminata del modo carrera (tipo, inicio, duracionSeg, distanciaM, kcal, ruta[]); solo la ve su dueño
 *   └─ modoCarrera/plan               plan de caminata que sigue (plan, inicio); solo lo ve su dueño
 * codigosVinculacion/{codigo}         nutriologoId, expiraEn, usado, canjeadoPor… (el id es el código)
 * verificaciones/{uid}                solicitud de verificación del nutriólogo (cédula) o del entrenador (credencial); estado PENDIENTE|APROBADA|RECHAZADA
 * ejercicios/{id}                    banco global: ejercicio que crea un entrenador verificado (estado BORRADOR|EN_REVISION|PUBLICADO|RECHAZADO|RETIRADO); lo publicado lo ve cualquier sesión, su foto vive en R2
 * cedulas/{numero}                    reserva de una cédula por un nutriólogo (una cédula, una cuenta)
 * directorio/{uid}                    consultorio de un nutriólogo verificado en el mapa (nombre, lat, lng)
 *   └─ ocupados/{inicio}              hora ya pedida (citaId, inicio); sin datos del paciente, la ve cualquier sesión
 * citas/{citaId}                      cita que un paciente pide a un nutriólogo (estado PENDIENTE|CONFIRMADA|RECHAZADA|CANCELADA)
 * citasActivas/{usuarioId}            candado de «una sola cita vigente por paciente» (citaId); se pone con la cita y se quita al cerrarla
 * comentarios/{id}                    comentario que alguien envía desde Configuración (usuarioId, rol, areas[], mensaje); solo lo lee el ADMIN
 * pacientes/{pacienteId}              nutriologoId, usuarioId, correoVinculacion…
 *   ├─ mediciones/{id}
 *   ├─ planes/{id}                    plan de alimentación y ejercicio enviado (enviadoPor, enviadoEn, contenido…); su PDF vive en R2
 *   ├─ mensajes/{id}                  chat con el nutriólogo (autorId, texto, foto, enviadoEn); la foto vive en R2. El último queda
 *   │                                 resumido en pacientes/{id}.ultimoMensaje para el punto de «sin leer»
 *   ├─ rutinas/{id}                   ejercicios[] embebidos
 *   ├─ fotos/{id}                     solo metadatos; el archivo vive en R2
 *   └─ cumplimientos/{id}
 *
 * Acceso (firestore.rules): ADMIN lee y suspende usuarios; un subdocumento hereda el acceso de su
 * paciente (nutriologoId == uid o usuarioId == uid). Nadie puede asignarse ADMIN al registrarse.
 */
object FirestorePaths {
    const val USUARIOS = "usuarios"
    const val BANCO = "banco"
    const val CARRERAS = "carreras"
    const val ENTRENAMIENTOS = "entrenamientos"
    const val MODO_CARRERA = "modoCarrera"
    const val CODIGOS_VINCULACION = "codigosVinculacion"
    const val VERIFICACIONES = "verificaciones"
    const val EJERCICIOS = "ejercicios"
    const val CEDULAS = "cedulas"
    const val DIRECTORIO = "directorio"
    const val OCUPADOS = "ocupados"
    const val CITAS = "citas"
    const val CITAS_ACTIVAS = "citasActivas"
    const val COMENTARIOS = "comentarios"
    const val PACIENTES = "pacientes"
    const val MEDICIONES = "mediciones"
    const val PLANES = "planes"
    const val MENSAJES = "mensajes"
    const val RUTINAS = "rutinas"
    const val FOTOS = "fotos"
    const val CUMPLIMIENTOS = "cumplimientos"
}
