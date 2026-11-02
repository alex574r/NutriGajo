package com.example.nutrigajo.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.RemoteViews
import com.example.nutrigajo.MainActivity
import com.example.nutrigajo.R
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Los cinco widgets de la pantalla de inicio del teléfono. Cada uno es una imagen (ver DibujoWidgets) dibujada con lo último que
// guardó la app (AlmacenWidget); tocarlo abre Nutri Gajo.
open class WidgetBase(private val tipo: TipoWidget) : AppWidgetProvider() {
    override fun onUpdate(contexto: Context, gestor: AppWidgetManager, ids: IntArray) {
        ids.forEach { pintar(contexto, gestor, it, tipo) }
    }

    // al estirarlo o girar el teléfono el widget cambia de tamaño: se dibuja otra vez a la medida
    override fun onAppWidgetOptionsChanged(contexto: Context, gestor: AppWidgetManager, id: Int, opciones: Bundle) {
        pintar(contexto, gestor, id, tipo)
    }
}

class WidgetResumen : WidgetBase(TipoWidget.RESUMEN)

class WidgetPulso : WidgetBase(TipoWidget.PULSO)

class WidgetPasos : WidgetBase(TipoWidget.PASOS)

class WidgetSueno : WidgetBase(TipoWidget.SUENO)

class WidgetSaludo : WidgetBase(TipoWidget.SALUDO)

private val CLASES = mapOf(
    TipoWidget.RESUMEN to WidgetResumen::class.java,
    TipoWidget.PULSO to WidgetPulso::class.java,
    TipoWidget.PASOS to WidgetPasos::class.java,
    TipoWidget.SUENO to WidgetSueno::class.java,
    TipoWidget.SALUDO to WidgetSaludo::class.java,
)

// el tamaño mínimo de cada widget (el de widget_*.xml), por si el lanzador no dice el que tiene
private const val ANCHO_DP = 250
private const val ALTO_DP = 110

// Dibuja otra vez todos los widgets que haya en la pantalla de inicio: se llama cuando la app guarda datos nuevos.
fun actualizarWidgets(contexto: Context) {
    val gestor = AppWidgetManager.getInstance(contexto)
    CLASES.forEach { (tipo, clase) ->
        gestor.getAppWidgetIds(ComponentName(contexto, clase)).forEach { pintar(contexto, gestor, it, tipo) }
    }
}

private fun pintar(contexto: Context, gestor: AppWidgetManager, id: Int, tipo: TipoWidget) {
    val opciones = gestor.getAppWidgetOptions(id)
    val vertical = contexto.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
    var anchoDp = opciones.getInt(if (vertical) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).takeIf { it > 0 } ?: ANCHO_DP
    var altoDp = opciones.getInt(if (vertical) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).takeIf { it > 0 } ?: ALTO_DP
    val densidad = contexto.resources.displayMetrics.density
    // una imagen demasiado grande no cabe en un widget: se achica a unos 3 MB
    val area = anchoDp * altoDp * densidad * densidad * 4f
    if (area > 3_000_000f) {
        val k = sqrt(3_000_000f / area)
        anchoDp = (anchoDp * k).roundToInt()
        altoDp = (altoDp * k).roundToInt()
    }
    val tela = Tela(contexto, (anchoDp * densidad).roundToInt(), (altoDp * densidad).roundToInt())
    dibujarWidget(tipo, tela, AlmacenWidget.entorno(contexto), LocalDate.now())
    val abrir = PendingIntent.getActivity(contexto, 0, Intent(contexto, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    val vistas = RemoteViews(contexto.packageName, R.layout.widget_imagen).apply {
        setImageViewBitmap(R.id.imagen, tela.bitmap)
        setOnClickPendingIntent(R.id.imagen, abrir)
    }
    gestor.updateAppWidget(id, vistas)
}
