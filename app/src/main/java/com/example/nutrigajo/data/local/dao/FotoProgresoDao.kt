package com.example.nutrigajo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.nutrigajo.data.local.entity.FotoProgresoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FotoProgresoDao {
    @Upsert
    suspend fun upsert(foto: FotoProgresoEntity)

    @Query("SELECT * FROM foto_progreso WHERE pacienteId = :pacienteId ORDER BY fecha DESC, ultimaModificacion DESC")
    fun observarDePaciente(pacienteId: String): Flow<List<FotoProgresoEntity>>

    // pendiente = falta subir el archivo (rutaLocal != null) y/o sus metadatos
    @Query("SELECT * FROM foto_progreso WHERE estadoSync = 'PENDIENTE'")
    suspend fun pendientes(): List<FotoProgresoEntity>

    // archivo ya en R2: se conserva PENDIENTE hasta enviar los metadatos a Firestore
    @Query("UPDATE foto_progreso SET rutaStorage = :rutaStorage, rutaLocal = NULL WHERE id = :id")
    suspend fun marcarSubida(id: String, rutaStorage: String)

    @Query("UPDATE foto_progreso SET estadoSync = 'SINCRONIZADO' WHERE id = :id AND ultimaModificacion = :ultimaModificacion")
    suspend fun marcarSincronizado(id: String, ultimaModificacion: Long)
}
