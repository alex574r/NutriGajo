package com.example.nutrigajo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.nutrigajo.data.local.entity.UsuarioEntity
import com.example.nutrigajo.domain.model.Rol
import kotlinx.coroutines.flow.Flow

// Todos los DAO usan @Upsert, nunca REPLACE: REPLACE borra la fila y el CASCADE se lleva a sus hijos.
@Dao
interface UsuarioDao {
    @Upsert
    suspend fun upsert(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuario WHERE uid = :uid")
    fun observar(uid: String): Flow<UsuarioEntity?>

    // panel del ADMIN: nutriólogos por validar, cuentas por suspender
    @Query("SELECT * FROM usuario WHERE rol = :rol ORDER BY nombre")
    fun observarPorRol(rol: Rol): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuario WHERE estadoSync = 'PENDIENTE'")
    suspend fun pendientes(): List<UsuarioEntity>

    // solo si no se editó durante el envío; si cambió, sigue pendiente
    @Query("UPDATE usuario SET estadoSync = 'SINCRONIZADO' WHERE uid = :uid AND ultimaModificacion = :ultimaModificacion")
    suspend fun marcarSincronizado(uid: String, ultimaModificacion: Long)
}
