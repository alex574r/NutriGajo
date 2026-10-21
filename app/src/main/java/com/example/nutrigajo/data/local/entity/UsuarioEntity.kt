package com.example.nutrigajo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.nutrigajo.domain.model.EstadoSync
import com.example.nutrigajo.domain.model.Rol

@Entity(tableName = "usuario", indices = [Index("rol")])
data class UsuarioEntity(
    @PrimaryKey val uid: String,
    val nombre: String,
    val correo: String,
    val rol: Rol,
    val cedulaProfesional: String? = null,
    // solo NUTRIOLOGO: false hasta que un ADMIN valide la cédula
    val verificado: Boolean = false,
    // un ADMIN puede suspender cuentas
    val activo: Boolean = true,
    // evidencia del consentimiento del aviso de privacidad
    val avisoPrivacidadAceptadoEn: Long,
    val fechaAlta: Long,
    val ultimaModificacion: Long,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE,
)
