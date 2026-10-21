package com.example.nutrigajo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.nutrigajo.data.local.entity.ComidaEntity
import com.example.nutrigajo.data.local.entity.PlanDietaEntity
import com.example.nutrigajo.data.local.relation.PlanConComidas
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDietaDao {
    @Upsert
    suspend fun upsertPlan(plan: PlanDietaEntity)

    @Upsert
    suspend fun upsertComidas(comidas: List<ComidaEntity>)

    @Query("DELETE FROM comida WHERE planId = :planId")
    suspend fun borrarComidas(planId: String)

    // reemplaza las comidas completas: sirve tanto al crear como al bajar un plan de Firestore
    @Transaction
    suspend fun guardar(plan: PlanDietaEntity, comidas: List<ComidaEntity>) {
        upsertPlan(plan)
        borrarComidas(plan.id)
        upsertComidas(comidas)
    }

    @Transaction
    @Query("SELECT * FROM plan_dieta WHERE pacienteId = :pacienteId ORDER BY version DESC LIMIT 1")
    fun observarVigente(pacienteId: String): Flow<PlanConComidas?>

    @Query("SELECT * FROM plan_dieta WHERE pacienteId = :pacienteId ORDER BY version DESC")
    fun observarHistorial(pacienteId: String): Flow<List<PlanDietaEntity>>

    @Query("SELECT COALESCE(MAX(version), 0) + 1 FROM plan_dieta WHERE pacienteId = :pacienteId")
    suspend fun siguienteVersion(pacienteId: String): Int

    @Transaction
    @Query("SELECT * FROM plan_dieta WHERE estadoSync = 'PENDIENTE'")
    suspend fun pendientes(): List<PlanConComidas>

    @Query("UPDATE plan_dieta SET estadoSync = 'SINCRONIZADO' WHERE id = :id AND ultimaModificacion = :ultimaModificacion")
    suspend fun marcarSincronizado(id: String, ultimaModificacion: Long)
}
