package com.example.nutrigajo.ui.actualizacion

import com.example.nutrigajo.R
import com.example.nutrigajo.data.actualizacion.ResultadoInstalacion
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class MensajeActualizacionTest {
    @Test
    fun un_archivo_que_no_coincide_se_dice_aparte_del_resto_de_fallos_de_descarga() {
        assertEquals(R.string.actualiza_error_no_coincide, mensajeDeErrorDescarga(IOException("El archivo no coincide con lo publicado")))
        assertEquals(R.string.actualiza_error_no_coincide, mensajeDeErrorDescarga(IOException("El archivo es más grande de lo publicado")))
        assertEquals(R.string.actualiza_error_espacio, mensajeDeErrorDescarga(IOException("write failed: ENOSPC (No space left on device)")))
        assertEquals(R.string.actualiza_error_descarga, mensajeDeErrorDescarga(IOException("Unable to resolve host \"nutrigajo-api.alex574r.workers.dev\"")))
        assertEquals(R.string.actualiza_error_descarga, mensajeDeErrorDescarga(IOException("El Worker respondió 502")))
        assertEquals(R.string.actualiza_error_descarga, mensajeDeErrorDescarga(IOException()))
    }

    @Test
    fun cada_resultado_de_la_instalacion_tiene_su_mensaje() {
        assertEquals(R.string.actualiza_error_firma, mensajeDeResultado(ResultadoInstalacion.FIRMA_DISTINTA))
        assertEquals(R.string.actualiza_error_cancelada, mensajeDeResultado(ResultadoInstalacion.CANCELADA))
        assertEquals(R.string.actualiza_error_espacio, mensajeDeResultado(ResultadoInstalacion.SIN_ESPACIO))
        assertEquals(R.string.actualiza_error_instalar, mensajeDeResultado(ResultadoInstalacion.OTRO))
        assertEquals(R.string.actualiza_error_instalar, mensajeDeResultado(ResultadoInstalacion.EXITO))
    }
}
