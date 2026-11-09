package com.example.nutrigajo.domain

import com.example.nutrigajo.domain.model.Rol

// Qué tanto necesita la app un permiso: OBLIGATORIO bloquea su función hasta que se concede (nunca toda la app), RECOMENDADO se pide con una
// explicación y se puede rechazar, OPCIONAL solo se pide cuando la persona lo quiere.
enum class NivelPermiso { OBLIGATORIO, RECOMENDADO, OPCIONAL }

// En qué punto está un permiso. BLOQUEADO es que la persona ya lo negó y Android no volverá a mostrar su cuadro: solo se arregla en los ajustes.
enum class EstadoPermiso { CONCEDIDO, FALTA, BLOQUEADO, NO_DISPONIBLE }

// Los permisos que la app pide, vistos por lo que hace cada uno. `grupo` es el permiso del sistema que hay detrás: dos usos del mismo
// permiso (las dos ubicaciones) comparten el «ya lo pidió». No renombrar `grupo`: se guarda en las preferencias del teléfono.
enum class PermisoApp(val nivel: NivelPermiso, val grupo: String) {
    // Modo carrera: el servicio en primer plano exige la ubicación precisa
    UBICACION_CARRERA(NivelPermiso.OBLIGATORIO, "ubicacion"),

    // el mapa abre igual sin ella; solo no se centra en la persona
    UBICACION_MAPA(NivelPermiso.OPCIONAL, "ubicacion"),
    NOTIFICACIONES(NivelPermiso.RECOMENDADO, "notificaciones"),

    // Health Connect: obligatorio para «Tu reloj»
    RELOJ(NivelPermiso.OBLIGATORIO, "reloj"),

    // instalar la actualización de la app (se pide al actualizar; en «Permisos» solo se muestra su estado)
    ACTUALIZACIONES(NivelPermiso.OPCIONAL, "actualizaciones"),
}

// El estado a partir de lo que se sabe. `yaSePidio` es que la app ya mostró el cuadro alguna vez; `elSistemaTodaviaExplica` es lo que contesta
// `shouldShowRequestPermissionRationale`: falso antes de pedir por primera vez y tras negar «para siempre», por eso solo vale junto con `yaSePidio`.
fun estadoDePermiso(concedido: Boolean, disponible: Boolean, yaSePidio: Boolean, elSistemaTodaviaExplica: Boolean): EstadoPermiso = when {
    !disponible -> EstadoPermiso.NO_DISPONIBLE
    concedido -> EstadoPermiso.CONCEDIDO
    !yaSePidio -> EstadoPermiso.FALTA
    elSistemaTodaviaExplica -> EstadoPermiso.FALTA
    else -> EstadoPermiso.BLOQUEADO
}

// El permiso se ofrece solo (al abrir una pantalla) una única vez: faltando y sin que la app se lo haya ofrecido nunca a la persona, sea con la
// hoja que lo explica o con el cuadro del sistema. Después solo sale si ella lo pide con el botón. `yaSeOfrecio` no es lo mismo que «ya se
// pidió»: una hoja que se cerró con «Ahora no» ya se ofreció, pero el sistema todavía no ha mostrado su cuadro (ver estadoDePermiso).
fun debePedirseSolo(estado: EstadoPermiso, yaSeOfrecio: Boolean): Boolean = estado == EstadoPermiso.FALTA && !yaSeOfrecio

// Las notificaciones piden permiso desde Android 13 (API 33); antes cuentan como concedidas.
fun notificacionesPidenPermiso(sdk: Int): Boolean = sdk >= 33

// Los permisos que muestra la pantalla «Permisos» a cada rol. El paciente tiene Modo carrera y el reloj; los demás solo el mapa del consultorio.
fun permisosDelRol(rol: Rol): List<PermisoApp> = when (rol) {
    Rol.PACIENTE -> listOf(PermisoApp.NOTIFICACIONES, PermisoApp.UBICACION_CARRERA, PermisoApp.RELOJ, PermisoApp.ACTUALIZACIONES)
    else -> listOf(PermisoApp.NOTIFICACIONES, PermisoApp.UBICACION_MAPA, PermisoApp.ACTUALIZACIONES)
}
