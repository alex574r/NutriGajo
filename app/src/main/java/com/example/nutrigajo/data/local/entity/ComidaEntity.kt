package com.example.nutrigajo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.nutrigajo.domain.model.TipoComida

// Sin campos de sync propios: viaja embebida en el documento de su plan.
@Entity(
    tableName = "comida",
    foreignKeys = [
        ForeignKey(
            entity = PlanDietaEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planId")],
)
data class ComidaEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val tipo: TipoComida,
    // HH:mm
    val horario: String,
    val nombre: String,
    val descripcion: String? = null,
    val ingredientes: String? = null,
    val kcal: Int,
    val orden: Int,
)
