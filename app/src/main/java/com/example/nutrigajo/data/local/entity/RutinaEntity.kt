package com.example.nutrigajo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.nutrigajo.domain.model.EstadoSync

// Mismo criterio de versiones que PlanDietaEntity.
@Entity(
    tableName = "rutina",
    foreignKeys = [
        ForeignKey(
            entity = PacienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["pacienteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("pacienteId", "version", unique = true)],
)
data class RutinaEntity(
    @PrimaryKey val id: String,
    val pacienteId: String,
    val nombre: String,
    val version: Int,
    val enviadoPor: String,
    val fechaEnvio: Long,
    val ultimaModificacion: Long,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE,
)
