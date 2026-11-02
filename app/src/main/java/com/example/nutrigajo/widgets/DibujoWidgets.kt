package com.example.nutrigajo.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.racha
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Los widgets son una sola imagen que se dibuja aquí con el Canvas de Android: así se ven como el diseño (anillos, degradados,
// barras) sin librerías ni vistas remotas, y el diseño de cada uno cabe en una función. Las medidas van en dp y se escalan a la
// densidad; las posiciones verticales son proporciones del alto para que el widget se estire sin deformar el texto.

// el tamaño (dp) para el que se pensó cada diseño y hasta dónde se deja encoger o crecer el contenido
private const val ANCHO_DISENO = 250f
private const val ALTO_DISENO = 110f
private const val ESCALA_MIN = 0.55f
private const val ESCALA_MAX = 1.6f

// la paleta de la app (el naranja y el negro de Color.kt) más los verdes y azules de las tarjetas de métricas
private val NEGRO = 0xFF111214.toInt()
private val BLANCO = 0xFFFFFFFF.toInt()
private val NARANJA = 0xFFF97316.toInt()
private val GRIS = 0xFF9AA0AB.toInt()
private val VERDE_CLARO = 0xFF4ADE80.toInt()
private val AZUL_CLARO = 0xFF7DB0FF.toInt()

private val ESPANOL = Locale.forLanguageTag("es-MX")

private fun conAlfa(color: Int, alfa: Float): Int = (color and 0x00FFFFFF) or ((alfa * 255).roundToInt() shl 24)

// «7h 12m»
private fun hm(minutos: Int) = "${minutos / 60}h ${String.format(Locale.US, "%02d", minutos % 60)}m"

private fun numero(n: Int) = String.format(Locale.getDefault(), "%,d", n)

// El lienzo de un widget: el bitmap, sus medidas en dp y las herramientas de dibujo.
class Tela(val contexto: Context, ancho: Int, alto: Int) {
    val bitmap: Bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
    val c = Canvas(bitmap)
    private val densidadReal = contexto.resources.displayMetrics.density

    // Los diseños están pensados para 250 × 110 dp. En un widget más chico o más grande el contenido se escala con la medida que
    // limita (con topes), así el texto nunca se encima ni se corta al redimensionarlo; el resto del espacio lo absorben las
    // posiciones proporcionales de cada diseño. Todas las medidas en dp pasan por aquí.
    private val escala = min(ancho / densidadReal / ANCHO_DISENO, alto / densidadReal / ALTO_DISENO).coerceIn(ESCALA_MIN, ESCALA_MAX)
    private val densidad = densidadReal * escala
    private val inter: Typeface = ResourcesCompat.getFont(contexto, R.font.inter) ?: Typeface.SANS_SERIF

    // ancho y alto en dp
    val w = ancho / densidad
    val h = alto / densidad

    fun px(dp: Float) = dp * densidad

    // El fondo de la tarjeta: esquinas redondas y un color o un degradado en diagonal.
    fun fondo(desde: Int, hasta: Int = desde) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        if (desde != hasta) p.shader = LinearGradient(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), desde, hasta, Shader.TileMode.CLAMP) else p.color = desde
        c.drawRoundRect(RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()), px(22f), px(22f), p)
    }

    // Un texto con su baseline en (x, y) dp; `peso` es el eje wght de Inter. Devuelve su ancho en dp.
    fun texto(
        s: String,
        x: Float,
        y: Float,
        tamano: Float,
        color: Int,
        peso: Int = 600,
        alinear: Paint.Align = Paint.Align.LEFT,
        // si el texto es más ancho (en dp) se corta con puntos suspensivos
        maxAncho: Float = Float.MAX_VALUE,
    ): Float {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = inter
            textSize = px(tamano)
            this.color = color
            textAlign = alinear
            if (!setFontVariationSettings("'wght' $peso")) isFakeBoldText = peso >= 600
        }
        val dibujado = if (maxAncho < Float.MAX_VALUE) TextUtils.ellipsize(s, TextPaint(p), px(maxAncho), TextUtils.TruncateAt.END).toString() else s
        c.drawText(dibujado, px(x), px(y), p)
        return p.measureText(dibujado) / densidad
    }

    // La foto de fondo, recortada al centro para llenar la tarjeta, con las mismas esquinas redondas.
    fun fondoFoto(foto: Bitmap) {
        val escala = max(bitmap.width / foto.width.toFloat(), bitmap.height / foto.height.toFloat())
        val m = Matrix().apply {
            setScale(escala, escala)
            postTranslate((bitmap.width - foto.width * escala) / 2, (bitmap.height - foto.height * escala) / 2)
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(foto, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).also { it.setLocalMatrix(m) } }
        c.drawRoundRect(RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()), px(22f), px(22f), p)
    }

    // Un velo oscuro de izquierda a derecha sobre la foto, para que el texto blanco se lea.
    fun velo(desde: Int, hasta: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, bitmap.width.toFloat(), 0f, desde, hasta, Shader.TileMode.CLAMP) }
        c.drawRoundRect(RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()), px(22f), px(22f), p)
    }

    // El avatar: su foto en un círculo o, sin ella, la inicial sobre naranja; con un aro blanco.
    fun avatar(cx: Float, cy: Float, radio: Float, foto: Bitmap?, nombre: String) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        if (foto != null) {
            val escala = max(px(2 * radio) / foto.width, px(2 * radio) / foto.height)
            val m = Matrix().apply {
                setScale(escala, escala)
                postTranslate(px(cx) - foto.width * escala / 2, px(cy) - foto.height * escala / 2)
            }
            p.shader = BitmapShader(foto, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).also { it.setLocalMatrix(m) }
            c.drawCircle(px(cx), px(cy), px(radio), p)
        } else {
            circulo(cx, cy, radio, NARANJA)
            texto(nombre.trim().firstOrNull()?.uppercase() ?: "", cx, cy + radio * 0.33f, radio * 0.9f, BLANCO, 700, Paint.Align.CENTER)
        }
        val aro = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = px(2f)
            color = conAlfa(BLANCO, 0.9f)
        }
        c.drawCircle(px(cx), px(cy), px(radio), aro)
    }

    // Lo que ocupa una píldora con icono y texto, en dp.
    fun anchoPildora(s: String): Float {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = inter
            textSize = px(11f)
            if (!setFontVariationSettings("'wght' 700")) isFakeBoldText = true
        }
        return 10f + 14f + 5f + p.measureText(s) / densidad + 11f
    }

    // Una píldora de 24 dp: un icono y un texto cortos sobre `fondo`.
    fun pildora(x: Float, y: Float, @DrawableRes icono: Int, s: String, fondo: Int) {
        val ancho = anchoPildora(s)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fondo }
        c.drawRoundRect(RectF(px(x), px(y), px(x + ancho), px(y + 24f)), px(12f), px(12f), p)
        icono(icono, x + 10f, y + 5f, 14f, BLANCO)
        texto(s, x + 29f, y + 16f, 11f, BLANCO, 700)
    }

    fun icono(@DrawableRes id: Int, x: Float, y: Float, tamano: Float, color: Int) {
        val d = DrawableCompat.wrap(ContextCompat.getDrawable(contexto, id)!!.mutate())
        DrawableCompat.setTint(d, color)
        d.setBounds(px(x).roundToInt(), px(y).roundToInt(), px(x + tamano).roundToInt(), px(y + tamano).roundToInt())
        d.draw(c)
    }

    fun barra(x: Float, y: Float, ancho: Float, alto: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        c.drawRoundRect(RectF(px(x), px(y), px(x + ancho), px(y + alto)), px(min(ancho, alto) / 2), px(min(ancho, alto) / 2), p)
    }

    // Un anillo de `avance` (0 a 1) de su meta: la pista tenue y encima el arco, que arranca arriba.
    fun anillo(cx: Float, cy: Float, radio: Float, grosor: Float, avance: Float, color: Int) {
        val caja = RectF(px(cx - radio), px(cy - radio), px(cx + radio), px(cy + radio))
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = px(grosor)
            strokeCap = Paint.Cap.ROUND
        }
        p.color = conAlfa(BLANCO, 0.14f)
        c.drawArc(caja, 0f, 360f, false, p)
        if (avance > 0f) {
            p.color = color
            c.drawArc(caja, -90f, 360f * avance.coerceIn(0.02f, 1f), false, p)
        }
    }

    fun circulo(cx: Float, cy: Float, radio: Float, color: Int) {
        c.drawCircle(px(cx), px(cy), px(radio), Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    // Un óvalo con texto centrado (los valores mínimo y máximo del widget de pulso).
    fun pastilla(x: Float, y: Float, ancho: Float, alto: Float, color: Int, s: String, colorTexto: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        c.drawRoundRect(RectF(px(x), px(y), px(x + ancho), px(y + alto)), px(alto / 2), px(alto / 2), p)
        texto(s, x + ancho / 2, y + alto / 2 + 4.2f, 12f, colorTexto, 700, Paint.Align.CENTER)
    }
}

enum class TipoWidget { RESUMEN, PULSO, PASOS, SUENO, SALUDO }

// Dibuja el widget en la tela. Sin datos (la app aún no leyó el reloj, o no hay perfil) todos piden abrir la app.
fun dibujarWidget(tipo: TipoWidget, t: Tela, e: EntornoWidget, hoy: LocalDate, ahora: Instant = Instant.now()) {
    if (tipo == TipoWidget.SALUDO) {
        if (e.perfil == null) sinDatos(t, "Abre la app para ver tu resumen") else saludo(t, e.perfil, e, hoy)
        return
    }
    val d = e.datos?.delDia(hoy.toEpochDay())
    when {
        d == null -> sinDatos(t, "Abre la app y conecta tu reloj")
        tipo == TipoWidget.RESUMEN -> resumen(t, d, hoy)
        tipo == TipoWidget.PULSO -> pulso(t, d, ahora)
        tipo == TipoWidget.PASOS -> pasos(t, d)
        else -> sueno(t, d, hoy)
    }
}

private fun sinDatos(t: Tela, mensaje: String) {
    t.fondo(NEGRO)
    t.icono(R.drawable.ic_smartwatch, 18f, t.h / 2 - 16f, 32f, NARANJA)
    t.texto("Nutri Gajo", 62f, t.h / 2 - 3f, 16f, BLANCO, 700)
    t.texto(mensaje, 62f, t.h / 2 + 15f, 12f, GRIS, 500, maxAncho = t.w - 62f - 12f)
}

// El saludo: la portada de su perfil (o la foto del inicio) de fondo, su avatar y su nombre, la fecha y, abajo, las píldoras de la
// racha y de lo que le espera (planes y mensajes nuevos de su nutriólogo).
private fun saludo(t: Tela, perfil: PerfilWidget, e: EntornoWidget, hoy: LocalDate) {
    val contexto = t.contexto
    t.fondoFoto(foto(contexto, archivoEnCache(contexto, "portada-${perfil.uid}-"), R.drawable.inicio_cabecera, t.bitmap.width, t.bitmap.height))
    t.velo(0xB8000000.toInt(), 0x66000000.toInt())
    val avatar = archivoEnCache(contexto, "avatar-${perfil.uid}-")?.let { BitmapFactory.decodeFile(it.path) }
    t.avatar(36f, 34f, 20f, avatar, perfil.nombre)
    val primerNombre = perfil.nombre.trim().substringBefore(' ')
    t.texto(contexto.getString(R.string.inicio_hola, primerNombre), 66f, 32f, 18f, BLANCO, 700, maxAncho = t.w - 66f - 16f)
    val fecha = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ESPANOL).format(hoy).replaceFirstChar { it.uppercase() }
    t.texto(fecha, 66f, 48f, 11f, conAlfa(BLANCO, 0.85f), 500, maxAncho = t.w - 66f - 16f)

    // las píldoras, de izquierda a derecha por orden de importancia. Si no caben todas con su texto, la racha lo conserva y las demás se
    // enseñan solo con el icono y el número; la que ni así cabe se omite.
    val racha = racha(hoy, e.datos?.pasosPorDia.orEmpty(), e.diasCarrera, e.metaPasos)
    val recursos = contexto.resources
    val traslucido = conAlfa(BLANCO, 0.24f)
    val pildoras = buildList {
        add(Triple(R.drawable.ic_llama_solida, recursos.getQuantityString(R.plurals.racha_dias, racha.dias, racha.dias) to racha.dias.toString(), if (racha.dias > 0) NARANJA else traslucido))
        if (perfil.planesNuevos > 0) {
            add(Triple(R.drawable.ic_campana, recursos.getQuantityString(R.plurals.widget_planes, perfil.planesNuevos, perfil.planesNuevos) to perfil.planesNuevos.toString(), traslucido))
        }
        if (perfil.mensajes > 0) {
            add(Triple(R.drawable.ic_mensaje, recursos.getQuantityString(R.plurals.widget_mensajes, perfil.mensajes, perfil.mensajes) to perfil.mensajes.toString(), traslucido))
        }
        if (perfil.planesNuevos == 0 && perfil.mensajes == 0) {
            val alDia = contexto.getString(R.string.widget_al_dia)
            add(Triple(R.drawable.ic_verificado, alDia to alDia, traslucido))
        }
    }
    val completas = pildoras.sumOf { t.anchoPildora(it.second.first).toDouble() } + 8f * (pildoras.size - 1)
    val caben = completas <= t.w - 32f
    val y = t.h - 38f
    var x = 16f
    pildoras.forEachIndexed { i, (icono, textos, fondo) ->
        val texto = if (caben || i == 0) textos.first else textos.second
        val ancho = t.anchoPildora(texto)
        if (x + ancho > t.w - 16f) return@forEachIndexed
        t.pildora(x, y, icono, texto, fondo)
        x += ancho + 8f
    }
}

// Un archivo de la caché del teléfono que empieza por ese prefijo (las fotos de perfil que la app ya bajó).
private fun archivoEnCache(contexto: Context, prefijo: String): File? =
    contexto.cacheDir.listFiles { f -> f.name.startsWith(prefijo) && f.name.endsWith(".jpg") }?.maxByOrNull { it.lastModified() }

// La foto de ese archivo, o la del recurso si no hay; reducida a lo que cabe en la tarjeta para no gastar memoria.
private fun foto(contexto: Context, archivo: File?, @DrawableRes defecto: Int, ancho: Int, alto: Int): Bitmap {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    if (archivo != null) BitmapFactory.decodeFile(archivo.path, medidas) else BitmapFactory.decodeResource(contexto.resources, defecto, medidas)
    var muestra = 1
    while (medidas.outWidth / (muestra * 2) >= ancho && medidas.outHeight / (muestra * 2) >= alto) muestra *= 2
    val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
    return archivo?.let { BitmapFactory.decodeFile(it.path, opciones) } ?: BitmapFactory.decodeResource(contexto.resources, defecto, opciones)
}

// Hoy: el sueño y el pulso a la izquierda y, a la derecha, los anillos de pasos, calorías activas y minutos de entrenamiento.
private fun resumen(t: Tela, d: DatosWidget, hoy: LocalDate) {
    t.fondo(NEGRO)
    val fecha = DateTimeFormatter.ofPattern("d MMM", ESPANOL).format(hoy).trimEnd('.')
    t.texto("Hoy · $fecha", 18f, t.h * 0.26f, 17f, BLANCO, 700)
    val noche = d.sueno.maxByOrNull { it.key }?.value
    t.icono(R.drawable.ic_luna, 18f, t.h * 0.47f - 15f, 20f, AZUL_CLARO)
    t.texto(noche?.let(::hm) ?: "--", 46f, t.h * 0.47f, 16f, BLANCO, 600)
    t.texto("Sueño", 46f, t.h * 0.59f, 11f, GRIS, 500)
    val promedio = if (d.bpmMin != null && d.bpmMax != null) (d.bpmMin + d.bpmMax) / 2 else d.ultimoBpm
    t.icono(R.drawable.ic_corazon, 18f, t.h * 0.76f - 15f, 20f, NARANJA)
    t.texto(d.ultimoBpm?.let { "$it bpm" } ?: "--", 46f, t.h * 0.76f, 16f, BLANCO, 600)
    t.texto(if (promedio != null && d.ultimoBpm != null) "Ahora" else "Pulso", 46f, t.h * 0.88f, 11f, GRIS, 500)

    val radio = min(t.h / 2 - 12f, 46f)
    val cx = t.w - radio - 22f
    val cy = t.h / 2
    val avances = listOf(
        (d.pasos ?: 0) / d.metaPasos.toFloat(),
        (d.kcal ?: 0) / d.metaKcal.toFloat(),
        (d.minutos ?: 0) / d.metaMinutos.toFloat(),
    )
    listOf(NARANJA, BLANCO, VERDE_CLARO).forEachIndexed { i, color -> t.anillo(cx, cy, radio - i * 11f, 8f, avances[i], color) }
    val pct = if (d.pasos != null) "${(avances[0] * 100).roundToInt().coerceAtMost(999)}%" else "--"
    t.texto(pct, cx, cy + 4.5f, 12f, BLANCO, 700, android.graphics.Paint.Align.CENTER)
}

// El pulso de ahora en grande, con su mínimo y su máximo del día en las esquinas.
private fun pulso(t: Tela, d: DatosWidget, ahora: Instant) {
    t.fondo(0xFFFB923C.toInt(), 0xFFC2410C.toInt())
    // unas burbujas tenues, como en el modelo
    t.circulo(t.w * 0.78f, t.h * 0.30f, 26f, conAlfa(BLANCO, 0.10f))
    t.circulo(t.w * 0.90f, t.h * 0.62f, 14f, conAlfa(BLANCO, 0.12f))
    t.texto("AHORA", 18f, t.h * 0.26f, 12f, conAlfa(BLANCO, 0.85f), 700)
    t.icono(R.drawable.ic_corazon, t.w - 18f - 22f, t.h * 0.10f, 22f, BLANCO)
    val valor = d.ultimoBpm?.toString() ?: "--"
    val ancho = t.texto(valor, t.w / 2 - 14f, t.h * 0.64f, 46f, BLANCO, 800, android.graphics.Paint.Align.CENTER)
    t.texto("BPM", t.w / 2 - 14f + ancho / 2 + 6f, t.h * 0.64f, 14f, conAlfa(BLANCO, 0.85f), 700)
    t.pastilla(18f, t.h - 34f, 46f, 22f, conAlfa(BLANCO, 0.22f), d.bpmMin?.toString() ?: "--", BLANCO)
    t.pastilla(t.w - 18f - 46f, t.h - 34f, 46f, 22f, conAlfa(BLANCO, 0.22f), d.bpmMax?.toString() ?: "--", BLANCO)
    d.ultimaLectura?.let { t.texto(haceCuanto(it, ahora), t.w / 2, t.h - 18f, 11f, conAlfa(BLANCO, 0.8f), 500, android.graphics.Paint.Align.CENTER) }
}

// Los pasos de hoy en grande, con una barra de lo que lleva de su meta.
private fun pasos(t: Tela, d: DatosWidget) {
    t.fondo(0xFF15803D.toInt(), 0xFF22C55E.toInt())
    t.texto("PASOS", 18f, t.h * 0.24f, 12f, conAlfa(BLANCO, 0.85f), 700)
    t.icono(R.drawable.ic_corredor, t.w - 18f - 30f, t.h * 0.12f, 30f, BLANCO)
    t.texto(d.pasos?.let(::numero) ?: "--", 18f, t.h * 0.62f, 44f, BLANCO, 800)
    val avance = ((d.pasos ?: 0) / d.metaPasos.toFloat()).coerceIn(0f, 1f)
    val ancho = t.w - 36f
    t.barra(18f, t.h - 32f, ancho, 8f, conAlfa(BLANCO, 0.25f))
    if (avance > 0f) t.barra(18f, t.h - 32f, max(8f, ancho * avance), 8f, BLANCO)
    val pct = ((d.pasos ?: 0) * 100f / d.metaPasos).roundToInt()
    t.texto("de ${numero(d.metaPasos)} · $pct%", 18f, t.h - 12f, 11f, conAlfa(BLANCO, 0.85f), 500)
}

// El sueño de la última noche y las barras de la semana (una por día de despertar; la última, la más brillante).
private fun sueno(t: Tela, d: DatosWidget, hoy: LocalDate) {
    t.fondo(0xFF1E3A8A.toInt(), 0xFF2563EB.toInt())
    val ultima = d.sueno.maxByOrNull { it.key }
    t.texto(ultima?.value?.let(::hm) ?: "--", 18f, t.h * 0.34f, 28f, BLANCO, 800)
    val etiqueta = when {
        ultima == null -> "Sueño"
        ultima.key == hoy.toEpochDay() -> "Anoche"
        else -> "Última noche"
    }
    t.texto(etiqueta, 18f, t.h * 0.34f + 15f, 11f, conAlfa(BLANCO, 0.8f), 500)
    t.icono(R.drawable.ic_luna, t.w - 18f - 22f, t.h * 0.10f, 22f, conAlfa(BLANCO, 0.9f))
    val dias = (6L downTo 0L).map { hoy.minusDays(it) }
    val valores = dias.map { d.sueno[it.toEpochDay()] }
    val tope = max(480, valores.maxOf { it ?: 0 }).toFloat()
    val hueco = (t.w - 36f) / 7
    val grosor = 12f
    val base = t.h - 22f
    val altoMax = t.h * 0.30f
    dias.forEachIndexed { i, dia ->
        val x = 18f + i * hueco + (hueco - grosor) / 2
        val v = valores[i]
        val alto = if (v == null) grosor else max(grosor, altoMax * v / tope)
        val color = if (i == 6) BLANCO else conAlfa(BLANCO, 0.42f)
        t.barra(x, base - alto, grosor, alto, if (v == null) conAlfa(BLANCO, 0.2f) else color)
        t.texto(dia.dayOfWeek.getDisplayName(TextStyle.NARROW, ESPANOL).uppercase(ESPANOL), x + grosor / 2, t.h - 7f, 10f, conAlfa(BLANCO, 0.8f), 600, android.graphics.Paint.Align.CENTER)
    }
}

// «hace 5 min», «hace 2 h», «hace 3 d»
private fun haceCuanto(desde: Long, ahora: Instant): String =
    com.example.nutrigajo.domain.model.haceCuanto(Instant.ofEpochMilli(desde), ahora)
