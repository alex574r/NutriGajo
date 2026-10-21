package com.example.nutrigajo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.nutrigajo.data.local.entity.CumplimientoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CumplimientoDao {
    @Upsert
    suspend fun upsert(cumplimiento: CumplimientoEntity)

    @Query("SELECT * FROM cumplimiento WHERE pacienteId = :pacienteId AND fecha = :fecha")
    fun observarDelDia(pacienteId: String, fecha: String): Flow<List<CumplimientoEntity>>

    @Query("SELECT * FROM cumplimiento WHERE estadoSync = 'PENDIENTE'")
    suspend fun pendientes(): List<CumplimientoEntity>

    @Query("UPDATE cumplimiento SET estadoSync = 'SINCRONIZADO' WHERE id = :id AND ultimaModificacion = :ultimaModificacion")
    suspend fun marcarSincronizado(id: String, ultimaModificacion: Long)
}
