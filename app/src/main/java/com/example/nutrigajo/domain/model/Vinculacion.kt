package com.example.nutrigajo.domain.model

// Código de 4 dígitos con el que un nutriólogo invita a un paciente; si algún día es más largo, se muestra en grupos de 4 con guion.
data class CodigoVinculacion(val codigo: String, val expiraEn: Long) {
    val formateado: String get() = codigo.chunked(4).joinToString("-")
}

// Quién cerró un vínculo (pacientes/{id} con `activo = false`). `CUENTA_ELIMINADA` solo lo escribe el Worker al borrar una cuenta.
// No renombrar valores: Firestore guarda el nombre del enum como texto y las reglas lo comparan.
enum class CerradoPor { PACIENTE, NUTRIOLOGO, CUENTA_ELIMINADA }

// Un vínculo ya cerrado visto desde una de sus dos partes: con quién fue (`nombre`: el nutriólogo para el paciente, el paciente para el
// nutriólogo), cuándo se cerró (0 si no lo dice) y quién lo cerró. Es solo de lectura: el registro, sus planes y su chat se conservan.
data class RegistroCerrado(val pacienteId: String, val nombre: String, val cerradoEn: Long, val cerradoPor: CerradoPor?) {
    val porCuentaEliminada: Boolean get() = cerradoPor == CerradoPor.CUENTA_ELIMINADA
}

// Del cierre más reciente al más viejo; los que no dicen cuándo se cerraron van al final.
fun List<RegistroCerrado>.masRecientesPrimero(): List<RegistroCerrado> = sortedByDescending { it.cerradoEn }

// El motivo de cierre que guarda Firestore; null si falta o no se conoce.
fun cerradoPorDeTexto(texto: String?): CerradoPor? = CerradoPor.entries.firstOrNull { it.name == texto }

// Fila de la lista de pacientes de un nutriólogo.
// `usuarioId` es la cuenta del paciente: con él se encuentra su foto de perfil.
// `ultimoMensaje` es el último del chat con él, para el punto de «sin leer»; `metas` son sus metas diarias.
data class PacienteResumen(
    val id: String,
    val usuarioId: String,
    val nombre: String,
    val correo: String,
    val fechaAlta: Long,
    val ultimoMensaje: UltimoMensaje? = null,
    val metas: MetasDiarias = MetasDiarias(),
)

// Lo que el paciente necesita saber de su nutriólogo; `ultimoMensaje` es el último del chat con él y `metas` sus metas diarias.
// `nutriologoId` es la cuenta del nutriólogo: con él se encuentra su foto de perfil.
data class Vinculo(
    val pacienteId: String,
    val nutriologoNombre: String,
    val ultimoMensaje: UltimoMensaje? = null,
    val metas: MetasDiarias = MetasDiarias(),
    val nutriologoId: String = "",
)
