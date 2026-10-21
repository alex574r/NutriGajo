package com.example.nutrigajo.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.nutrigajo.data.local.entity.ComidaEntity
import com.example.nutrigajo.data.local.entity.PlanDietaEntity

// Room no ordena las relaciones: ordenar comidas por `orden` al mapear.
data class PlanConComidas(
    @Embedded val plan: PlanDietaEntity,
    @Relation(parentColumn = "id", entityColumn = "planId")
    val comidas: List<ComidaEntity>,
)
