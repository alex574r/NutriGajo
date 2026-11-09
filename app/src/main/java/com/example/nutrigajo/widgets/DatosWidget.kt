package com.example.nutrigajo.widgets

import android.content.Context
import android.content.SharedPreferences
import com.example.nutrigajo.domain.model.unirDiasRacha
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

// Lo último que la app supo del reloj y de las metas: de ahí se dibujan los widgets de la pantalla de inicio del teléfono, sin abrir
// la app ni leer Health Connect. Se guarda cada vez que la app lee el reloj, así que un widget nunca está más al día que la última vez
// que se abrió Nutri Gajo.
data class DatosWidget(
    // el día (LocalDate.toEpochDay) al que pertenece lo diario: pasos, calorías, minutos y el rango del pulso
    val dia: Long,
    val ultimoBpm: Int?,
    // milisegundos desde 1970
    val ultimaLectura: Long?,
    val bpmMin: Int?,
    val bpmMax: Int?,
    val pasos: Int?,
    val kcal: Int?,
    val minutos: Int?,
    // minutos dormidos por noche, con el día (toEpochDay) en que se despertó
    val sueno: Map<Long, Int>,
    // los pasos de cada uno de los últimos 60 días, para la racha
    val pasosPorDia: Map<Long, Int> = emptyMap(),
    val metaPasos: Int = META_PASOS,
    val metaKcal: Int = META_KCAL,
    val metaMinutos: Int = META_MINUTOS,
) {
    // Lo diario solo vale el día en que se guardó: al día siguiente el widget no enseña los pasos de ayer como si fueran de hoy.
    fun delDia(hoy: Long): DatosWidget =
        if (dia == hoy) this else copy(bpmMin = null, bpmMax = null, pasos = null, kcal = null, minutos = null)

    companion object {
        // las mismas metas por defecto que MetasDiarias
        const val META_PASOS = 8000
        const val META_KCAL = 1750
        const val META_MINUTOS = 30
    }
}

// Quién es y qué le espera: su nombre, los planes y los mensajes de su nutriólogo que aún no ha visto.
data class PerfilWidget(val nombre: String, val uid: String, val planesNuevos: Int, val mensajes: Int)

// Todo lo que un widget puede necesitar: los datos del reloj (null si la app aún no los leyó), el perfil, los días con caminata o
// carrera y la meta de pasos de la racha.
class EntornoWidget(
    val datos: DatosWidget?,
    val perfil: PerfilWidget?,
    val diasCarrera: Set<Long>,
    val metaPasos: Int,
)

object AlmacenWidget {
    private const val PREFS = "widgets_reloj"
    private const val DATOS = "datos"
    private const val PERFIL = "perfil"
    private const val DIAS_CARRERA = "diasCarrera"
    private const val META_PASOS = "metaPasos"
    private const val META_KCAL = "metaKcal"
    private const val META_MINUTOS = "metaMinutos"
    // de la racha solo importa el pasado reciente
    private const val DIAS_GUARDADOS = 400

    private fun prefs(contexto: Context) = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun entorno(contexto: Context) = EntornoWidget(leer(contexto), leerPerfil(contexto), diasCarrera(contexto), metaPasos(contexto))

    fun metaPasos(contexto: Context): Int = prefs(contexto).getInt(META_PASOS, DatosWidget.META_PASOS)

    // null si la app nunca leyó el reloj (o se cerró la sesión): los widgets piden abrir la app.
    fun leer(contexto: Context): DatosWidget? {
        val p = prefs(contexto)
        val texto = p.getString(DATOS, null) ?: return null
        return try {
            val j = JSONObject(texto)
            fun entero(clave: String) = if (j.isNull(clave)) null else j.getInt(clave)
            fun mapa(clave: String): Map<Long, Int> {
                val a = j.optJSONArray(clave) ?: return emptyMap()
                return (0 until a.length()).associate { a.getJSONArray(it).let { n -> n.getLong(0) to n.getInt(1) } }
            }
            DatosWidget(
                dia = j.getLong("dia"),
                ultimoBpm = entero("ultimoBpm"),
                ultimaLectura = if (j.isNull("ultimaLectura")) null else j.getLong("ultimaLectura"),
                bpmMin = entero("bpmMin"),
                bpmMax = entero("bpmMax"),
                pasos = entero("pasos"),
                kcal = entero("kcal"),
                minutos = entero("minutos"),
                sueno = mapa("sueno"),
                pasosPorDia = mapa("pasosPorDia"),
                metaPasos = p.getInt(META_PASOS, DatosWidget.META_PASOS),
                metaKcal = p.getInt(META_KCAL, DatosWidget.META_KCAL),
                metaMinutos = p.getInt(META_MINUTOS, DatosWidget.META_MINUTOS),
            )
        } catch (_: Exception) {
            null
        }
    }

    // Guarda lo leído del reloj; las metas (que llegan de otro lado) no se tocan.
    fun guardar(contexto: Context, d: DatosWidget) {
        fun pares(m: Map<Long, Int>) = JSONArray().also { a -> m.forEach { (dia, valor) -> a.put(JSONArray().put(dia).put(valor)) } }
        val j = JSONObject()
            .put("dia", d.dia)
            .put("ultimoBpm", d.ultimoBpm ?: JSONObject.NULL)
            .put("ultimaLectura", d.ultimaLectura ?: JSONObject.NULL)
            .put("bpmMin", d.bpmMin ?: JSONObject.NULL)
            .put("bpmMax", d.bpmMax ?: JSONObject.NULL)
            .put("pasos", d.pasos ?: JSONObject.NULL)
            .put("kcal", d.kcal ?: JSONObject.NULL)
            .put("minutos", d.minutos ?: JSONObject.NULL)
            .put("sueno", pares(d.sueno))
            .put("pasosPorDia", pares(d.pasosPorDia))
        prefs(contexto).edit().putString(DATOS, j.toString()).apply()
    }

    fun guardarMetas(contexto: Context, pasos: Int, kcal: Int, minutos: Int) {
        prefs(contexto).edit().putInt(META_PASOS, pasos).putInt(META_KCAL, kcal).putInt(META_MINUTOS, minutos).apply()
    }

    fun leerPerfil(contexto: Context): PerfilWidget? {
        val texto = prefs(contexto).getString(PERFIL, null) ?: return null
        return try {
            val j = JSONObject(texto)
            PerfilWidget(j.getString("nombre"), j.getString("uid"), j.getInt("planesNuevos"), j.getInt("mensajes"))
        } catch (_: Exception) {
            null
        }
    }

    fun guardarPerfil(contexto: Context, p: PerfilWidget) {
        val j = JSONObject().put("nombre", p.nombre).put("uid", p.uid).put("planesNuevos", p.planesNuevos).put("mensajes", p.mensajes)
        prefs(contexto).edit().putString(PERFIL, j.toString()).apply()
    }

    // Quién es: la sesión de Firebase o, si ya se cerró (al borrar), el perfil que quedó guardado.
    private fun uidActual(contexto: Context): String? =
        (try { FirebaseAuth.getInstance().currentUser?.uid } catch (_: Exception) { null }) ?: leerPerfil(contexto)?.uid

    private fun claveDias(uid: String) = "$DIAS_CARRERA.$uid"

    private fun leerDias(p: SharedPreferences, clave: String): Set<Long> =
        p.getStringSet(clave, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    // Antes los días eran de «quien tuviera abierta la sesión» y se borraban al salir: los de esa clave vieja pasan a la cuenta que los pida.
    private fun adoptarDiasViejos(contexto: Context, uid: String) {
        val p = prefs(contexto)
        if (!p.contains(DIAS_CARRERA)) return
        val unidos = unirDiasRacha(leerDias(p, claveDias(uid)), leerDias(p, DIAS_CARRERA), DIAS_GUARDADOS)
        p.edit().putStringSet(claveDias(uid), unidos.map { it.toString() }.toSet()).remove(DIAS_CARRERA).apply()
    }

    // Los días (toEpochDay) en que se cumplió algo (pasos, caminata, carrera o rutina) de esa cuenta; se van sumando y nunca se quitan (borrar una
    // actividad o subir la meta no rompe una racha ya ganada). Van por cuenta y sobreviven a cerrar sesión: solo son fechas.
    fun diasCarrera(contexto: Context, uid: String? = uidActual(contexto)): Set<Long> {
        if (uid == null) return emptySet()
        adoptarDiasViejos(contexto, uid)
        return leerDias(prefs(contexto), claveDias(uid))
    }

    fun agregarDiasCarrera(contexto: Context, dias: Collection<Long>, uid: String? = uidActual(contexto)) {
        if (uid == null) return
        val todos = unirDiasRacha(diasCarrera(contexto, uid), dias, DIAS_GUARDADOS)
        prefs(contexto).edit().putStringSet(claveDias(uid), todos.map { it.toString() }.toSet()).apply()
    }

    // Al cerrar sesión: los widgets no deben seguir enseñando la salud ni el nombre de quien salió. Los días de la racha se conservan (por cuenta):
    // si no, cerrar sesión o cambiar de cuenta dejaba la racha en cero.
    fun borrar(contexto: Context) {
        val p = prefs(contexto)
        leerPerfil(contexto)?.uid?.let { adoptarDiasViejos(contexto, it) }
        val dias = p.all.filterKeys { it.startsWith("$DIAS_CARRERA.") }
        val editor = p.edit().clear()
        dias.forEach { (clave, valor) -> (valor as? Set<*>)?.let { editor.putStringSet(clave, it.filterIsInstance<String>().toSet()) } }
        editor.apply()
    }
}
