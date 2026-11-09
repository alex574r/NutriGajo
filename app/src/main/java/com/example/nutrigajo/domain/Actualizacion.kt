package com.example.nutrigajo.domain

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// Lo más que puede pesar un APK publicado (el manifiesto que diga más se descarta) y cada cuánto se busca una versión nueva.
const val MAX_APK_BYTES = 200L * 1024 * 1024
const val CADA_MS = 12L * 3_600_000

// Cuánto se espera, tras empezar a instalar, a que llegue el resultado antes de darla por abandonada al volver a la app.
const val ESPERA_INSTALACION_MS = 5_000L

private const val MAX_NOMBRE = 40
private const val MAX_NOTAS = 10
private const val MAX_NOTA = 200
private val SHA256 = Regex("^[0-9a-f]{64}$")

// La última versión publicada, tal como la deja backend/scripts/publicar-apk.mjs en apk/ultima.json. `minVersionCode` (0 si no se
// quiere forzar a nadie): una versión instalada menor que él tiene que actualizarse.
data class Actualizacion(
    val versionCode: Long,
    val versionName: String,
    val sha256: String,
    val bytes: Long,
    val notas: List<String>,
    val minVersionCode: Long,
    val publicadaEn: Long,
)

enum class EstadoActualizacion { AL_DIA, DISPONIBLE, OBLIGATORIA }

// Lee el manifiesto con rigor: lo que no cumple al pie de la letra (tipos, SHA-256 de 64 hexadecimales en minúsculas, tamaño entre 1 byte y
// MAX_APK_BYTES, hasta 10 notas de 200 caracteres, la mínima no mayor que la versión) se ignora entero y devuelve null.
fun actualizacionDeJson(texto: String): Actualizacion? {
    val o = try {
        JSONObject(texto)
    } catch (_: JSONException) {
        return null
    }
    val versionCode = o.entero("versionCode")?.takeIf { it > 0 } ?: return null
    val versionName = (o.opt("versionName") as? String)?.takeIf { it.isNotBlank() && it.length <= MAX_NOMBRE } ?: return null
    val sha256 = (o.opt("sha256") as? String)?.takeIf { SHA256.matches(it) } ?: return null
    val bytes = o.entero("bytes")?.takeIf { it in 1..MAX_APK_BYTES } ?: return null
    val arreglo = o.opt("notas") as? JSONArray ?: return null
    if (arreglo.length() > MAX_NOTAS) return null
    val notas = (0 until arreglo.length()).map { (arreglo.opt(it) as? String)?.takeIf { nota -> nota.length <= MAX_NOTA } ?: return null }
    val minVersionCode = if (o.has("minVersionCode")) o.entero("minVersionCode")?.takeIf { it in 0..versionCode } ?: return null else 0L
    val publicadaEn = o.entero("publicadaEn")?.takeIf { it >= 0 } ?: return null
    return Actualizacion(versionCode, versionName, sha256, bytes, notas, minVersionCode, publicadaEn)
}

// El manifiesto como texto, para guardarlo en el teléfono: se vuelve a leer con actualizacionDeJson.
fun jsonDe(a: Actualizacion): String = JSONObject()
    .put("versionCode", a.versionCode)
    .put("versionName", a.versionName)
    .put("sha256", a.sha256)
    .put("bytes", a.bytes)
    .put("notas", JSONArray(a.notas))
    .put("minVersionCode", a.minVersionCode)
    .put("publicadaEn", a.publicadaEn)
    .toString()

// La actualización obligatoria que ya se conocía (el último manifiesto guardado) si esta versión sigue por debajo de su mínima; null si no hay
// obligación. Así cerrar y volver a abrir la app, o que Android la mate, no deja saltarse una actualización obligatoria dentro de las 12 horas.
fun obligatoriaGuardada(versionActual: Long, guardado: String?): Actualizacion? =
    guardado?.let(::actualizacionDeJson)?.takeIf { estadoDe(versionActual, it) == EstadoActualizacion.OBLIGATORIA }

// Empezó a instalarse y, al volver a la app, no llegó ningún resultado: el usuario salió de la ventana de Android sin confirmar ni cancelar.
fun instalacionSinNoticias(desde: Long, ahora: Long): Boolean = ahora - desde > ESPERA_INSTALACION_MS

// Un entero de verdad: 2.5 o "2" no valen.
private fun JSONObject.entero(clave: String): Long? = when (val v = opt(clave)) {
    is Int -> v.toLong()
    is Long -> v
    else -> null
}

// Nunca se ofrece retroceder ni repetir la misma versión; la obligatoria es la que deja por debajo de la mínima publicada.
fun estadoDe(versionActual: Long, ultima: Actualizacion): EstadoActualizacion = when {
    ultima.versionCode <= versionActual -> EstadoActualizacion.AL_DIA
    versionActual < ultima.minVersionCode -> EstadoActualizacion.OBLIGATORIA
    else -> EstadoActualizacion.DISPONIBLE
}

// Pasaron las 12 horas desde la última consulta; sin consulta previa, o con el reloj atrasado, se consulta.
fun tocaConsultar(ahora: Long, ultimaConsulta: Long): Boolean =
    ultimaConsulta <= 0 || ultimaConsulta > ahora || ahora - ultimaConsulta >= CADA_MS

// La obligatoria siempre se enseña; la opcional, salvo que ya se pospuso esa misma versión (otra versión nueva vuelve a avisar).
fun debeMostrar(estado: EstadoActualizacion, versionCode: Long, aplazada: Long): Boolean = when (estado) {
    EstadoActualizacion.AL_DIA -> false
    EstadoActualizacion.OBLIGATORIA -> true
    EstadoActualizacion.DISPONIBLE -> versionCode != aplazada
}
