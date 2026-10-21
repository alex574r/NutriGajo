package com.example.nutrigajo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.nutrigajo.domain.model.EstadoSync

// Un plan enviado no se edita: cada cambio es una versión nueva; la vigente es la mayor.
@Entity(
    tableName = "plan_dieta",
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
data class PlanDietaEntity(
    @PrimaryKey val id: String,
    val pacienteId: String,
    val titulo: String,
    val caloriasObjetivo: Int,
    // reparto de macronutrientes; los tres suman 100
    val proteinasPct: Int,
    val carbohidratosPct: Int,
    val grasasPct: Int,
    val version: Int,
    val enviadoPor: String,
    val fechaEnvio: Long,
    val ultimaModificacion: Long,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE,
)
