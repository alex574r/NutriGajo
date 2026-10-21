package com.example.nutrigajo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.nutrigajo.data.local.entity.MedicionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicionDao {
    @Upsert
    suspend fun upsert(medicion: MedicionEntity)

    @Query("SELECT * FROM medicion WHERE pacienteId = :pacienteId ORDER BY fecha DESC, ultimaModificacion DESC")
    fun observarDePaciente(pacienteId: String): Flow<List<MedicionEntity>>

    // lista del nutriólogo (peso actual y variación): agrupar por pacienteId al mapear
    @Query(
        "SELECT m.* FROM medicion m JOIN paciente p ON p.id = m.pacienteId " +
            "WHERE p.nutriologoId = :nutriologoId ORDER BY m.fecha DESC",
    )
    fun observarDeNutriologo(nutriologoId: String): Flow<List<MedicionEntity>>

    @Query("SELECT * FROM medicion WHERE estadoSync = 'PENDIENTE'")
    suspend fun pendientes(): List<MedicionEntity>

    @Query("UPDATE medicion SET estadoSync = 'SINCRONIZADO' WHERE id = :id AND ultimaModificacion = :ultimaModificacion")
    suspend fun marcarSincronizado(id: String, ultimaModificacion: Long)
}
