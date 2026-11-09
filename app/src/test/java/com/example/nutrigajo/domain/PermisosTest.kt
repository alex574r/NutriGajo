package com.example.nutrigajo.domain

import com.example.nutrigajo.domain.model.Rol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Qué permisos hay, cuáles son obligatorios, en qué estado está cada uno y cuándo se pide solo.
class PermisosTest {
    @Test
    fun sin_disponible_es_no_disponible_aunque_se_haya_pedido() {
        assertEquals(EstadoPermiso.NO_DISPONIBLE, estadoDePermiso(concedido = false, disponible = false, yaSePidio = true, elSistemaTodaviaExplica = true))
        assertEquals(EstadoPermiso.NO_DISPONIBLE, estadoDePermiso(concedido = false, disponible = false, yaSePidio = false, elSistemaTodaviaExplica = false))
    }

    @Test
    fun concedido_y_disponible_es_concedido() {
        assertEquals(EstadoPermiso.CONCEDIDO, estadoDePermiso(concedido = true, disponible = true, yaSePidio = true, elSistemaTodaviaExplica = false))
        assertEquals(EstadoPermiso.CONCEDIDO, estadoDePermiso(concedido = true, disponible = true, yaSePidio = false, elSistemaTodaviaExplica = false))
    }

    @Test
    fun sin_conceder_y_nunca_pedido_falta() {
        assertEquals(EstadoPermiso.FALTA, estadoDePermiso(concedido = false, disponible = true, yaSePidio = false, elSistemaTodaviaExplica = false))
    }

    @Test
    fun ya_pedido_y_el_sistema_aun_explica_todavia_se_puede_pedir() {
        assertEquals(EstadoPermiso.FALTA, estadoDePermiso(concedido = false, disponible = true, yaSePidio = true, elSistemaTodaviaExplica = true))
    }

    @Test
    fun ya_pedido_y_el_sistema_ya_no_explica_esta_bloqueado() {
        assertEquals(EstadoPermiso.BLOQUEADO, estadoDePermiso(concedido = false, disponible = true, yaSePidio = true, elSistemaTodaviaExplica = false))
    }

    @Test
    fun solo_se_pide_solo_lo_que_falta_y_nunca_se_ha_pedido() {
        assertTrue(debePedirseSolo(EstadoPermiso.FALTA, yaSeOfrecio = false))
        assertFalse(debePedirseSolo(EstadoPermiso.FALTA, yaSeOfrecio = true))
        assertFalse(debePedirseSolo(EstadoPermiso.BLOQUEADO, yaSeOfrecio = true))
        assertFalse(debePedirseSolo(EstadoPermiso.BLOQUEADO, yaSeOfrecio = false))
        assertFalse(debePedirseSolo(EstadoPermiso.CONCEDIDO, yaSeOfrecio = false))
        assertFalse(debePedirseSolo(EstadoPermiso.NO_DISPONIBLE, yaSeOfrecio = false))
    }

    @Test
    fun las_notificaciones_piden_permiso_desde_android_13() {
        assertFalse(notificacionesPidenPermiso(32))
        assertTrue(notificacionesPidenPermiso(33))
        assertTrue(notificacionesPidenPermiso(36))
    }

    @Test
    fun el_paciente_ve_notificaciones_ubicacion_de_carrera_reloj_y_actualizaciones() {
        assertEquals(
            listOf(PermisoApp.NOTIFICACIONES, PermisoApp.UBICACION_CARRERA, PermisoApp.RELOJ, PermisoApp.ACTUALIZACIONES),
            permisosDelRol(Rol.PACIENTE),
        )
    }

    @Test
    fun los_demas_roles_no_ven_reloj_ni_modo_carrera() {
        val esperado = listOf(PermisoApp.NOTIFICACIONES, PermisoApp.UBICACION_MAPA, PermisoApp.ACTUALIZACIONES)
        for (rol in listOf(Rol.NUTRIOLOGO, Rol.ENTRENADOR, Rol.ADMIN)) assertEquals("rol $rol", esperado, permisosDelRol(rol))
    }

    @Test
    fun solo_la_ubicacion_de_carrera_y_el_reloj_son_obligatorios() {
        assertEquals(
            setOf(PermisoApp.UBICACION_CARRERA, PermisoApp.RELOJ),
            PermisoApp.entries.filter { it.nivel == NivelPermiso.OBLIGATORIO }.toSet(),
        )
        assertEquals(NivelPermiso.RECOMENDADO, PermisoApp.NOTIFICACIONES.nivel)
        assertEquals(NivelPermiso.OPCIONAL, PermisoApp.UBICACION_MAPA.nivel)
    }

    @Test
    fun las_dos_ubicaciones_comparten_el_ya_lo_pidio() {
        assertEquals(PermisoApp.UBICACION_CARRERA.grupo, PermisoApp.UBICACION_MAPA.grupo)
        assertEquals(PermisoApp.entries.map { it.grupo }.toSet().size, PermisoApp.entries.size - 1)
    }
}
