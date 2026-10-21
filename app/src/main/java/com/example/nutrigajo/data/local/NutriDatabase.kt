package com.example.nutrigajo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.nutrigajo.data.local.dao.CumplimientoDao
import com.example.nutrigajo.data.local.dao.FotoProgresoDao
import com.example.nutrigajo.data.local.dao.MedicionDao
import com.example.nutrigajo.data.local.dao.PacienteDao
import com.example.nutrigajo.data.local.dao.PlanDietaDao
import com.example.nutrigajo.data.local.dao.RutinaDao
import com.example.nutrigajo.data.local.dao.UsuarioDao
import com.example.nutrigajo.data.local.entity.ComidaEntity
import com.example.nutrigajo.data.local.entity.CumplimientoEntity
import com.example.nutrigajo.data.local.entity.EjercicioEntity
import com.example.nutrigajo.data.local.entity.FotoProgresoEntity
import com.example.nutrigajo.data.local.entity.MedicionEntity
import com.example.nutrigajo.data.local.entity.PacienteEntity
import com.example.nutrigajo.data.local.entity.PlanDietaEntity
import com.example.nutrigajo.data.local.entity.RutinaEntity
import com.example.nutrigajo.data.local.entity.UsuarioEntity

/**
 * Convenciones del modelo:
 * - Ids: UUID generado en el cliente (se puede crear sin red). Usuario usa el uid de Firebase.
 * - Fechas sin hora: texto ISO yyyy-MM-dd. Marcas de tiempo: epoch en milisegundos.
 * - Toda entidad sincronizable lleva ultimaModificacion y estadoSync. Comida y Ejercicio no:
 *   viajan embebidas en el documento de su plan/rutina.
 * - Sin borrado duro en lo sincronizable (no hay tombstones): el paciente se archiva con activo = false.
 */
@Database(
    entities = [
        UsuarioEntity::class,
        PacienteEntity::class,
        MedicionEntity::class,
        PlanDietaEntity::class,
        ComidaEntity::class,
        RutinaEntity::class,
        EjercicioEntity::class,
        FotoProgresoEntity::class,
        CumplimientoEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NutriDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun pacienteDao(): PacienteDao
    abstract fun medicionDao(): MedicionDao
    abstract fun planDietaDao(): PlanDietaDao
    abstract fun rutinaDao(): RutinaDao
    abstract fun fotoProgresoDao(): FotoProgresoDao
    abstract fun cumplimientoDao(): CumplimientoDao

    companion object {
        const val NOMBRE = "nutrigajo.db"
    }
}
