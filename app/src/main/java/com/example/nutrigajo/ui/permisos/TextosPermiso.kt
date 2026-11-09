package com.example.nutrigajo.ui.permisos

import androidx.annotation.StringRes
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.EstadoPermiso
import com.example.nutrigajo.domain.NivelPermiso
import com.example.nutrigajo.domain.PermisoApp

// Los textos de cada permiso y de cada estado: los usan el panel que explica un permiso y la pantalla «Permisos».

@StringRes
fun PermisoApp.titulo(): Int = when (this) {
    PermisoApp.UBICACION_CARRERA -> R.string.permiso_ubicacion_carrera_titulo
    PermisoApp.UBICACION_MAPA -> R.string.permiso_ubicacion_mapa_titulo
    PermisoApp.NOTIFICACIONES -> R.string.permiso_notificaciones_titulo
    PermisoApp.RELOJ -> R.string.permiso_reloj_titulo
    PermisoApp.ACTUALIZACIONES -> R.string.permiso_actualizaciones_titulo
}

// Para qué se pide.
@StringRes
fun PermisoApp.paraQue(): Int = when (this) {
    PermisoApp.UBICACION_CARRERA -> R.string.permiso_ubicacion_carrera_para
    PermisoApp.UBICACION_MAPA -> R.string.permiso_ubicacion_mapa_para
    PermisoApp.NOTIFICACIONES -> R.string.permiso_notificaciones_para
    PermisoApp.RELOJ -> R.string.permiso_reloj_para
    PermisoApp.ACTUALIZACIONES -> R.string.permiso_actualizaciones_para
}

// Qué pasa sin él: los obligatorios dicen que lo necesitan para abrir su función.
@StringRes
fun PermisoApp.sinEl(): Int = when (this) {
    PermisoApp.UBICACION_CARRERA -> R.string.permiso_ubicacion_carrera_sin
    PermisoApp.UBICACION_MAPA -> R.string.permiso_ubicacion_mapa_sin
    PermisoApp.NOTIFICACIONES -> R.string.permiso_notificaciones_sin
    PermisoApp.RELOJ -> R.string.permiso_reloj_sin
    PermisoApp.ACTUALIZACIONES -> R.string.permiso_actualizaciones_sin
}

// «Obligatorio para Modo carrera», «Recomendado» u «Opcional».
@StringRes
fun PermisoApp.textoNivel(): Int = when {
    this == PermisoApp.UBICACION_CARRERA -> R.string.permiso_nivel_obligatorio_carrera
    this == PermisoApp.RELOJ -> R.string.permiso_nivel_obligatorio_reloj
    nivel == NivelPermiso.RECOMENDADO -> R.string.permiso_nivel_recomendado
    else -> R.string.permiso_nivel_opcional
}

@StringRes
fun EstadoPermiso.texto(): Int = when (this) {
    EstadoPermiso.CONCEDIDO -> R.string.permiso_estado_concedido
    EstadoPermiso.FALTA -> R.string.permiso_estado_falta
    EstadoPermiso.BLOQUEADO -> R.string.permiso_estado_bloqueado
    EstadoPermiso.NO_DISPONIBLE -> R.string.permiso_estado_no_disponible
}

// El botón que arregla ese estado; null cuando no hay nada que arreglar.
@StringRes
fun EstadoPermiso.textoAccion(): Int? = when (this) {
    EstadoPermiso.CONCEDIDO -> null
    EstadoPermiso.FALTA -> R.string.permiso_accion_dar
    EstadoPermiso.BLOQUEADO -> R.string.permiso_accion_ajustes
    EstadoPermiso.NO_DISPONIBLE -> R.string.permiso_accion_instalar
}
