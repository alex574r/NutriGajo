package com.example.nutrigajo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.nutrigajo.data.local.entity.EjercicioEntity
import com.example.nutrigajo.data.local.entity.RutinaEntity
import com.example.nutrigajo.data.local.relation.RutinaConEjercicios
import kotlinx.coroutines.flow.Flow

@Dao
interface RutinaDao {
    @Upsert
    suspend fun upsertRutina(rutina: RutinaEntity)

    @Upsert
    suspend fun upsertEjercicios(ejercicios: List<EjercicioEntity>)

    @Query("DELETE FROM ejercicio WHERE rutinaId = :rutinaId")
    suspend fun borrarEjercicios(rutinaId: String)

    @Transaction
    suspend fun guardar(rutina: RutinaEntity, ejercicios: List<EjercicioEntity>) {
        upsertRutina(rutina)
        borrarEjercicios(rutina.id)
        upsertEjercicios(ejercicios)
    }

    @Transaction
    @Query("SELECT * FROM rutina WHERE pacienteId = :pacienteId ORDER BY version DESC LIMIT 1")
    fun observarVigente(pacienteId: String): Flow<RutinaConEjercicios?>

    @Query("SELECT * FROM rutina WHERE pacienteId = :pacienteId ORDER BY version DESC")
    fun observarHistorial(pacienteId: String): Flow<List<RutinaEntity>>

    @Query("SELECT COALESCE(MAX(version), 0) + 1 FROM rutina WHERE pacienteId = :pacienteId")
    suspend fun siguienteVersion(pacienteId: String): Int

    @Transaction
    @Query("SELECT * FROM rutina WHERE estadoSync = 'PENDIENTE'")
    suspend fun pendientes(): List<RutinaConEjercicios>

    @Query("UPDATE rutina SET estadoSync = 'SINCRONIZADO' WHERE id = :id AND ultimaModificacion = :ultimaModificacion")
    suspend fun marcarSincronizado(id: String, ultimaModificacion: Long)
}
