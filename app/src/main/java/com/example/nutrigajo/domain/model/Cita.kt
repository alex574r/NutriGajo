package com.example.nutrigajo.domain.model

// No renombrar valores: Firestore guarda el nombre del enum como texto.
enum class EstadoCita { PENDIENTE, CONFIRMADA, RECHAZADA, CANCELADA }

// Quién creó la cita. Las de antes de que el nutriólogo pudiera agendar no lo guardan: se leen como del paciente.
// No renombrar valores: Firestore guarda el nombre del enum como texto.
enum class OrigenCita { PACIENTE, NUTRIOLOGO }

// Lo más que mide la nota que el nutriólogo deja en una cita; las reglas de Firestore lo exigen también.
const val NOTA_MAX = 120

// Cita entre un paciente y un nutriólogo del directorio; vive en citas/{id}. La pide el paciente (y el nutriólogo la confirma) o la
// agenda el nutriólogo a uno de sus pacientes (y nace confirmada).
data class Cita(
    val id: String,
    val nutriologoId: String,
    val nutriologoNombre: String,
    // uid de la cuenta del paciente: puede pedir la cita antes de vincularse, por eso no es un pacientes/{id}
    val usuarioId: String,
    val pacienteNombre: String,
    // milisegundos de la hora de inicio
    val inicio: Long,
    val estado: EstadoCita,
    val origen: OrigenCita = OrigenCita.PACIENTE,
    // el pacientes/{id} del registro del paciente; solo las citas que agenda el nutriólogo lo llevan
    val pacienteId: String? = null,
    val nota: String = "",
) {
    // sigue por atender: todavía no llega su hora y no se rechazó ni se canceló
    fun vigente(ahora: Long): Boolean =
        inicio > ahora && (estado == EstadoCita.PENDIENTE || estado == EstadoCita.CONFIRMADA)
}

// Lo que le pasa a esa cita cuando `por` (el paciente o su nutriólogo) cierra el vínculo: el paciente cancela lo pendiente y lo confirmado;
// el nutriólogo rechaza lo pendiente y cancela lo confirmado (es lo que dejan hacer las reglas de Firestore). null si la cita ya no
// cuenta (pasada, rechazada o cancelada) o si `por` no es una de las dos partes.
fun estadoAlCerrar(cita: Cita, por: Rol, ahora: Long): EstadoCita? {
    if (!cita.vigente(ahora)) return null
    return when (por) {
        Rol.PACIENTE -> EstadoCita.CANCELADA
        Rol.NUTRIOLOGO -> if (cita.estado == EstadoCita.PENDIENTE) EstadoCita.RECHAZADA else EstadoCita.CANCELADA
        else -> null
    }
}

// Las citas vigentes entre ese nutriólogo y esa cuenta de paciente: las que se cierran junto con su vínculo.
fun citasVigentesEntre(citas: List<Cita>, nutriologoId: String, usuarioId: String, ahora: Long): List<Cita> =
    citas.filter { it.nutriologoId == nutriologoId && it.usuarioId == usuarioId && it.vigente(ahora) }

// La cita vigente que llega primero, o null si no hay ninguna.
fun List<Cita>.proxima(ahora: Long): Cita? = filter { it.vigente(ahora) }.minByOrNull { it.inicio }
