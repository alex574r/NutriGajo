package com.example.nutrigajo.data.actualizacion

import android.content.pm.PackageInstaller
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultadoInstalacionTest {
    @Test
    fun cada_estado_del_instalador_se_traduce_a_su_resultado() {
        assertEquals(ResultadoInstalacion.EXITO, resultadoDe(PackageInstaller.STATUS_SUCCESS, null))
        assertEquals(ResultadoInstalacion.CANCELADA, resultadoDe(PackageInstaller.STATUS_FAILURE_ABORTED, "INSTALL_FAILED_ABORTED: User rejected permissions"))
        assertEquals(ResultadoInstalacion.SIN_ESPACIO, resultadoDe(PackageInstaller.STATUS_FAILURE_STORAGE, null))
        // Android manda las firmas distintas como conflicto: la app instalada y el APK no son de la misma llave
        assertEquals(ResultadoInstalacion.FIRMA_DISTINTA, resultadoDe(PackageInstaller.STATUS_FAILURE_CONFLICT, null))
        assertEquals(ResultadoInstalacion.OTRO, resultadoDe(PackageInstaller.STATUS_FAILURE, null))
        assertEquals(ResultadoInstalacion.OTRO, resultadoDe(PackageInstaller.STATUS_FAILURE_BLOCKED, null))
        assertEquals(ResultadoInstalacion.OTRO, resultadoDe(PackageInstaller.STATUS_FAILURE_INVALID, null))
        // incompatible es del teléfono (ABI, versión de Android), no de la firma
        assertEquals(ResultadoInstalacion.OTRO, resultadoDe(PackageInstaller.STATUS_FAILURE_INCOMPATIBLE, "INSTALL_FAILED_CPU_ABI_INCOMPATIBLE"))
        assertEquals(ResultadoInstalacion.OTRO, resultadoDe(99, null))
    }

    @Test
    fun el_mensaje_de_firmas_distintas_se_reconoce_aunque_el_estado_sea_otro() {
        val mensaje = "INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.example.nutrigajo signatures do not match newer version; ignoring!"
        assertEquals(ResultadoInstalacion.FIRMA_DISTINTA, resultadoDe(PackageInstaller.STATUS_FAILURE, mensaje))
        assertEquals(ResultadoInstalacion.FIRMA_DISTINTA, resultadoDe(PackageInstaller.STATUS_FAILURE_INCOMPATIBLE, mensaje))
    }
}
