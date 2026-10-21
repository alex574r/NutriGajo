package com.example.nutrigajo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.nutrigajo.data.local.entity.PacienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PacienteDao {
    @Upsert
    suspend fun upsert(paciente: PacienteEntity)

    @Query("SELECT * FROM paciente WHERE id = :id")
    fun observar(id: String): Flow<PacienteEntity?>

    // expediente del propio paciente (rol PACIENTE)
    @Query("SELECT * FROM paciente WHERE usuarioId = :usuarioId")
    fun observarPorUsuario(usuarioId: String): Flow<PacienteEntity?>

    // lista del nutriólogo: activos primero; busqueda vacía devuelve todos
    @Query(
        "SELECT * FROM paciente WHERE nutriologoId = :nutriologoId " +
            "AND instr(lower(nombre), lower(:busqueda)) > 0 " +
            "ORDER BY activo DESC, nombre",
    )
    fun observarDeNutriologo(nutriologoId: String, busqueda: String = ""): Flow<List<PacienteEntity>>

    @Query("SELECT * FROM paciente WHERE estadoSync = 'PENDIENTE'")
    suspend fun pendientes(): List<PacienteEntity>

    @Query("UPDATE paciente SET estadoSync = 'SINCRONIZADO' WHERE id = :id AND ultimaModificacion = :ultimaModificacion")
    suspend fun marcarSincronizado(id: String, ultimaModificacion: Long)
}
