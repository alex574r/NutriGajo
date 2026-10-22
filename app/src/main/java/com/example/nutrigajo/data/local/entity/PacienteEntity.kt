package com.example.nutrigajo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.nutrigajo.domain.model.EstadoSync
import com.example.nutrigajo.domain.model.Sexo

@Entity(
    tableName = "paciente",
    indices = [
        Index("nutriologoId", "correoVinculacion", unique = true),
        Index("usuarioId"),
    ],
)
data class PacienteEntity(
    @PrimaryKey val id: String,
    val nutriologoId: String,
    // null hasta que el paciente crea su cuenta con correoVinculacion
    val usuarioId: String? = null,
    val nombre: String,
    // los cuatro siguientes llegan con el expediente (opcional): el registro nace sin ellos
    val fechaNacimiento: String? = null,
    val sexo: Sexo? = null,
    val estaturaCm: Double? = null,
    val telefono: String? = null,
    // en minúsculas y sin espacios
    val correoVinculacion: String,
    val alergias: String? = null,
    val objetivo: String? = null,
    val pesoMetaKg: Double? = null,
    val proximaCita: Long? = null,
    // clave del objeto en R2
    val fotoPerfilKey: String? = null,
    // false = archivado
    val activo: Boolean = true,
    val ultimaModificacion: Long,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE,
)
