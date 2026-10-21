package com.example.nutrigajo.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.nutrigajo.data.local.entity.EjercicioEntity
import com.example.nutrigajo.data.local.entity.RutinaEntity

// Room no ordena las relaciones: agrupar por `dia` y ordenar por `orden` al mapear.
data class RutinaConEjercicios(
    @Embedded val rutina: RutinaEntity,
    @Relation(parentColumn = "id", entityColumn = "rutinaId")
    val ejercicios: List<EjercicioEntity>,
)
