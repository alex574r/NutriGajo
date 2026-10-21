package com.example.nutrigajo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.nutrigajo.domain.model.EstadoSync

@Entity(
    tableName = "medicion",
    foreignKeys = [
        ForeignKey(
            entity = PacienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["pacienteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("pacienteId", "fecha")],
)
data class MedicionEntity(
    @PrimaryKey val id: String,
    val pacienteId: String,
    val fecha: String,
    val pesoKg: Double,
    val cinturaCm: Double? = null,
    val caderaCm: Double? = null,
    val grasaPct: Double? = null,
    // comentario del paciente o del nutriólogo
    val notas: String? = null,
    // uid de quien la capturó (nutriólogo o paciente)
    val registradaPor: String,
    val ultimaModificacion: Long,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE,
)
