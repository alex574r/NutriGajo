package com.example.nutrigajo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.nutrigajo.domain.model.EstadoSync
import com.example.nutrigajo.domain.model.TipoCumplimiento

@Entity(
    tableName = "cumplimiento",
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
data class CumplimientoEntity(
    // determinista (ver idDe): dos dispositivos marcando lo mismo no duplican filas
    @PrimaryKey val id: String,
    val pacienteId: String,
    val fecha: String,
    val tipo: TipoCumplimiento,
    // id de la ComidaEntity o EjercicioEntity marcada
    val refId: String,
    val cumplido: Boolean,
    val ultimaModificacion: Long,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE,
) {
    companion object {
        fun idDe(fecha: String, tipo: TipoCumplimiento, refId: String) = "${fecha}_${tipo.name}_$refId"
    }
}
