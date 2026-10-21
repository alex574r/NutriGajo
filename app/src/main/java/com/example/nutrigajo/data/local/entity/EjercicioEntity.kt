package com.example.nutrigajo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Sin campos de sync propios: viaja embebido en el documento de su rutina.
@Entity(
    tableName = "ejercicio",
    foreignKeys = [
        ForeignKey(
            entity = RutinaEntity::class,
            parentColumns = ["id"],
            childColumns = ["rutinaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("rutinaId")],
)
data class EjercicioEntity(
    @PrimaryKey val id: String,
    val rutinaId: String,
    // java.time.DayOfWeek.value: 1 = lunes … 7 = domingo
    val dia: Int,
    val nombre: String,
    val series: Int,
    // repeticiones o duración: uno de los dos
    val repeticiones: Int? = null,
    val duracionSeg: Int? = null,
    val descansoSeg: Int,
    val orden: Int,
)
