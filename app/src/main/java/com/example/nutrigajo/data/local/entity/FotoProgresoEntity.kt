package com.example.nutrigajo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.nutrigajo.domain.model.EstadoSync

@Entity(
    tableName = "foto_progreso",
    foreignKeys = [
        ForeignKey(
            entity = PacienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["pacienteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("pacienteId", "fecha"), Index("medicionId")],
)
data class FotoProgresoEntity(
    @PrimaryKey val id: String,
    val pacienteId: String,
    val medicionId: String? = null,
    val fecha: String,
    // clave del objeto en R2; null mientras el archivo no se haya subido
    val rutaStorage: String? = null,
    // archivo en el teléfono pendiente de subir; null una vez subido
    val rutaLocal: String? = null,
    val nota: String? = null,
    val subidaPor: String,
    val ultimaModificacion: Long,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE,
)
