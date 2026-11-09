package com.example.nutrigajo.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.EjercicioAsignado
import com.example.nutrigajo.domain.model.GrupoAlimento
import com.example.nutrigajo.domain.model.PlanContenido
import com.example.nutrigajo.domain.model.RangoImc
import com.example.nutrigajo.domain.model.dosis
import com.example.nutrigajo.domain.model.opcionDelDia
import com.example.nutrigajo.domain.model.rangoImc
import com.example.nutrigajo.ui.theme.color
import com.example.nutrigajo.ui.theme.titulo
import java.io.File
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// Datos que no están en el contenido del plan: quién lo recibe, quién lo envía, cuándo y cómo contactarlo.
data class DatosPlanPdf(
    val paciente: String,
    val nutriologo: String,
    // ya escrita: «25 de octubre de 2026»
    val fecha: String,
    val contenido: PlanContenido,
    val telefono: String = "",
    val direccion: String = "",
)

// Página tamaño carta horizontal, como el formato de ejemplo de los planes.
private const val ANCHO = 792
private const val ALTO = 612
private const val MARGEN = 40f
private const val ANCHO_UTIL = ANCHO - 2 * MARGEN
private const val Y_CONTENIDO = 108f
private const val Y_LIMITE = ALTO - 46f

// Colores del sistema de diseño (ui/theme): naranja solo como acento, grises para texto y superficies.
private val NARANJA = 0xFFF97316.toInt()
private val TEXTO = 0xFF111214.toInt()
private val SECUNDARIO = 0xFF676C75.toInt()
private val SUPERFICIE = 0xFFF3F3F4.toInt()
private val BORDE = 0xFFD7D8D9.toInt()
private val ETIQUETA = 0xFFFFEDD5.toInt()
private val BLANCO = 0xFFFFFFFF.toInt()

// Colores del medidor del IMC, en el orden de RangoImc.
private val COLORES_IMC = listOf(0xFF7BA7D1, 0xFF16A34A, 0xFFEAB308, 0xFFF97316, 0xFFEF4444).map { it.toInt() }

// Arma el PDF del plan con la API de Android (PdfDocument), sin librerías: portada, bienvenida con IMC y datos clave, metas,
// plan nutricional (tabla de equivalentes), opciones de menú, plan de ejercicio y cierre. Solo salen las secciones con contenido.
// Las filas de la tabla «Ejercicios asignados»: [día, ejercicio, dosis, nota], de lunes a domingo y, dentro de cada día, en el orden del plan.
internal fun filasAsignadas(asignados: List<EjercicioAsignado>, nombreDia: (Int) -> String): List<List<String>> =
    asignados.sortedBy { it.dia }.map { listOf(nombreDia(it.dia), it.titulo, it.dosis(), it.nota) }

// Las filas de «Menú por día» de un plan con duración: [día, opción] de cada día que tiene menú, en orden; sin duración o sin menú no hay filas.
internal fun filasMenuPorDia(plan: PlanContenido, etiquetaDia: (Int) -> String, etiquetaOpcion: (Int) -> String): List<List<String>> {
    if (plan.duracionDias == null) return emptyList()
    return plan.menuDias.indices.mapNotNull { i ->
        plan.opcionDelDia(i + 1)?.let { listOf(etiquetaDia(i + 1), etiquetaOpcion(it + 1)) }
    }
}

class GeneradorPlanPdf(private val contexto: Context) {
    private val base: Typeface = ResourcesCompat.getFont(contexto, R.font.inter) ?: Typeface.SANS_SERIF

    // `avance` recibe de 0 a 1 cuánto va, una vez por etapa (cada sección del PDF y, al final, la escritura del archivo); se
    // llama desde el hilo que genera.
    fun generar(datos: DatosPlanPdf, destino: File, avance: (Float) -> Unit = {}) {
        val documento = PdfDocument()
        try {
            val lienzo = Lienzo(documento, datos.paciente)
            val plan = datos.contenido
            val etapas = buildList<() -> Unit> {
                add { portada(lienzo, datos) }
                add { bienvenida(lienzo, datos) }
                if (plan.metas.isNotEmpty()) add { metas(lienzo, plan) }
                if (plan.calorias != null || plan.equivalentes.isNotEmpty()) add { planNutricional(lienzo, plan) }
                if (plan.opciones.isNotEmpty()) add { menu(lienzo, plan) }
                if (plan.sesiones.isNotEmpty() || plan.ejercicioNotas.isNotBlank() || plan.ejerciciosAsignados.isNotEmpty()) add { ejercicio(lienzo, plan) }
                if (plan.notas.isNotBlank() || datos.telefono.isNotBlank() || datos.direccion.isNotBlank()) add { cierre(lienzo, datos) }
                add {
                    lienzo.terminar()
                    destino.outputStream().use { documento.writeTo(it) }
                }
            }
            etapas.forEachIndexed { i, etapa ->
                etapa()
                avance((i + 1f) / etapas.size)
            }
        } finally {
            documento.close()
        }
    }

    // ---- herramientas de dibujo

    private fun s(@StringRes id: Int, vararg argumentos: Any): String = contexto.getString(id, *argumentos)

    // Inter es una fuente variable: el peso se pide por su eje; si la fuente no lo tiene (respaldo del sistema) se simula.
    private fun pincel(tamano: Float, peso: Int = 400, color: Int = TEXTO) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = base
        textSize = tamano
        this.color = color
        if (!setFontVariationSettings("'wght' $peso")) isFakeBoldText = peso >= 600
    }

    private fun relleno(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; this.color = color }

    private fun trazo(color: Int, grosor: Float) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; this.color = color; strokeWidth = grosor; strokeCap = Paint.Cap.ROUND }

    private fun texto(t: String, p: TextPaint, ancho: Float, alinear: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL, interlineado: Float = 1.28f): StaticLayout =
        StaticLayout.Builder.obtain(t, 0, t.length, p, ancho.toInt().coerceAtLeast(1))
            .setAlignment(alinear)
            .setLineSpacing(0f, interlineado)
            .setIncludePad(false)
            .build()

    private fun Canvas.dibujar(l: StaticLayout, x: Float, y: Float) {
        save()
        translate(x, y)
        l.draw(this)
        restore()
    }

    private fun Canvas.caja(x: Float, y: Float, ancho: Float, alto: Float, color: Int, radio: Float = 14f) =
        drawRoundRect(RectF(x, y, x + ancho, y + alto), radio, radio, relleno(color))

    private fun logo(c: Canvas, x: Float, y: Float, lado: Float) {
        ContextCompat.getDrawable(contexto, R.drawable.ic_logo)?.apply {
            setBounds(x.toInt(), y.toInt(), (x + lado).toInt(), (y + lado).toInt())
            draw(c)
        }
    }

    // La foto escalada para no pesar de más en el PDF.
    private fun foto(@DrawableRes id: Int, ladoMax: Int): Bitmap? {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(contexto.resources, id, medidas)
        var muestra = 1
        while (max(medidas.outWidth, medidas.outHeight) / (muestra * 2) >= ladoMax) muestra *= 2
        return BitmapFactory.decodeResource(contexto.resources, id, BitmapFactory.Options().apply { inSampleSize = muestra })
    }

    // Una página a la vez con cabecera, pie y salto automático cuando el contenido no cabe.
    private inner class Lienzo(private val documento: PdfDocument, private val paciente: String) {
        private var pagina: PdfDocument.Page? = null
        private var numero = 0
        private var titulo = ""
        lateinit var c: Canvas
        var y = Y_CONTENIDO

        // `titulo` null es la portada: sin cabecera ni pie.
        fun nueva(titulo: String?) {
            terminar()
            numero++
            val nueva = documento.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero).create())
            pagina = nueva
            c = nueva.canvas
            if (titulo != null) {
                this.titulo = titulo
                cabecera(titulo)
            }
            y = Y_CONTENIDO
        }

        fun continuar() = nueva(s(R.string.pdf_continuacion, titulo))

        // Cabe `alto` más en esta página; si no, sigue en una nueva.
        fun asegurar(alto: Float) {
            if (y + alto > Y_LIMITE) continuar()
        }

        fun terminar() {
            val actual = pagina ?: return
            if (numero > 1) pie()
            documento.finishPage(actual)
            pagina = null
        }

        private fun cabecera(titulo: String) {
            logo(c, MARGEN, 22f, 26f)
            c.drawText("NutriGajo", MARGEN + 34f, 41f, pincel(14f, 700))
            val quien = pincel(10f, 500, SECUNDARIO)
            c.drawText(paciente, ANCHO - MARGEN - quien.measureText(paciente), 40f, quien)
            c.drawText(titulo, MARGEN, 84f, pincel(22f, 700))
            c.caja(MARGEN, 92f, 36f, 4f, NARANJA, 2f)
        }

        private fun pie() {
            c.drawRect(0f, ALTO - 10f, ANCHO.toFloat(), ALTO.toFloat(), relleno(NARANJA))
            val p = pincel(8.5f, 400, SECUNDARIO)
            c.drawText(s(R.string.pdf_pie, paciente), MARGEN, ALTO - 20f, p)
            val n = s(R.string.pdf_pagina, numero)
            c.drawText(n, ANCHO - MARGEN - p.measureText(n), ALTO - 20f, p)
        }
    }

    // ---- secciones

    private fun portada(lz: Lienzo, d: DatosPlanPdf) {
        lz.nueva(null)
        val c = lz.c
        // la foto a la derecha, recortada al centro y con esquinas redondeadas
        val marco = RectF(436f, 36f, ANCHO - 36f, ALTO - 56f)
        // el PDF guarda el mapa de bits sin recompresión con pérdida: se recorta y se reduce a ~1.3 px por punto para que el archivo pese poco
        foto(R.drawable.onboarding_alimentacion, 700)?.let { f ->
            val escala = max(marco.width() / f.width, marco.height() / f.height)
            val origen = android.graphics.Rect(
                ((f.width - marco.width() / escala) / 2).toInt(),
                ((f.height - marco.height() / escala) / 2).toInt(),
                ((f.width + marco.width() / escala) / 2).toInt(),
                ((f.height + marco.height() / escala) / 2).toInt(),
            )
            val reducida = Bitmap.createBitmap((marco.width() * 1.3f).toInt(), (marco.height() * 1.3f).toInt(), Bitmap.Config.ARGB_8888)
            Canvas(reducida).drawBitmap(f, origen, RectF(0f, 0f, reducida.width.toFloat(), reducida.height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
            f.recycle()
            c.save()
            c.clipPath(Path().apply { addRoundRect(marco, 30f, 30f, Path.Direction.CW) })
            c.drawBitmap(reducida, null, marco, Paint(Paint.FILTER_BITMAP_FLAG))
            c.restore()
            reducida.recycle()
        }
        logo(c, MARGEN, 40f, 34f)
        c.drawText("NutriGajo", MARGEN + 44f, 64f, pincel(18f, 700))

        val titulo = texto(s(R.string.pdf_titulo_portada), pincel(36f, 800), 350f, interlineado = 1.12f)
        c.dibujar(titulo, MARGEN, 150f)
        var y = 150f + titulo.height + 14f
        c.caja(MARGEN, y, 44f, 5f, NARANJA, 2.5f)
        y += 30f
        val nombre = texto(d.paciente, pincel(22f, 700), 350f, interlineado = 1.15f)
        c.dibujar(nombre, MARGEN, y)
        y += nombre.height + 6f
        c.drawText(s(R.string.pdf_subtitulo_portada, d.fecha), MARGEN, y + 14f, pincel(13f, 500, SECUNDARIO))
        c.drawText(s(R.string.pdf_con_nutriologo, d.nutriologo), MARGEN, y + 34f, pincel(13f, 500, SECUNDARIO))
        // un plan con duración lo dice en la portada
        d.contenido.duracionDias?.let { c.drawText(s(R.string.pdf_duracion_portada, it), MARGEN, y + 56f, pincel(13f, 700, NARANJA)) }

        // el lema, en una barra como la del formato de ejemplo
        c.caja(MARGEN, ALTO - 124f, 350f, 44f, SUPERFICIE, 22f)
        val lema = pincel(11.5f, 500, SECUNDARIO)
        c.drawText(s(R.string.pdf_lema), MARGEN + 22f, ALTO - 124f + 27f, lema)
        c.drawRect(0f, ALTO - 10f, ANCHO.toFloat(), ALTO.toFloat(), relleno(NARANJA))
    }

    private fun bienvenida(lz: Lienzo, d: DatosPlanPdf) {
        val plan = d.contenido
        lz.nueva(s(R.string.pdf_bienvenida, d.paciente.trim().substringBefore(' ')))
        val c = lz.c
        val ancho = 384f
        val xDerecha = MARGEN + ancho + 28f
        val anchoDerecha = ANCHO - MARGEN - xDerecha
        var y = lz.y

        if (plan.mensaje.isNotBlank()) {
            val mensaje = texto(plan.mensaje, pincel(11.5f), ancho, interlineado = 1.36f)
            c.dibujar(mensaje, MARGEN, y)
            y += mensaje.height + 20f
        }

        // datos clave
        val datos = buildList {
            plan.pesoKg?.let { add(s(R.string.pdf_dato_peso) to s(R.string.pdf_unidad_kg, numero(it))) }
            plan.tallaCm?.let { add(s(R.string.pdf_dato_talla) to s(R.string.pdf_unidad_m, "%.2f".format(Locale.US, it / 100))) }
            plan.cinturaCm?.let { add(s(R.string.pdf_dato_cintura) to s(R.string.pdf_unidad_cm, numero(it))) }
            plan.caderaCm?.let { add(s(R.string.pdf_dato_cadera) to s(R.string.pdf_unidad_cm, numero(it))) }
            plan.grasaPct?.let { add(s(R.string.pdf_dato_grasa) to "${numero(it)} %") }
            if (plan.seguimiento.isNotBlank()) add(s(R.string.pdf_dato_seguimiento) to plan.seguimiento)
        }
        if (datos.isNotEmpty()) {
            val etiqueta = pincel(10.5f, 400, SECUNDARIO)
            val valor = pincel(11.5f, 700)
            val alto = 46f + datos.size * 22f
            val yCaja = if (y + alto > Y_LIMITE) Y_LIMITE - alto else y
            c.caja(MARGEN, yCaja, ancho, alto, SUPERFICIE, 18f)
            c.drawText(s(R.string.pdf_datos_clave), MARGEN + 20f, yCaja + 28f, pincel(13f, 700))
            datos.forEachIndexed { i, (nombre, v) ->
                val fila = yCaja + 54f + i * 22f
                c.drawText(nombre, MARGEN + 20f, fila, etiqueta)
                c.drawText(v, MARGEN + ancho - 20f - valor.measureText(v), fila, valor)
            }
        }

        // IMC
        val imc = plan.imc
        if (imc != null) {
            val alto = 300f
            c.caja(xDerecha, lz.y, anchoDerecha, alto, SUPERFICIE, 18f)
            c.drawText(s(R.string.pdf_imc), xDerecha + 20f, lz.y + 28f, pincel(13f, 700))
            val cx = xDerecha + anchoDerecha / 2
            val cy = lz.y + 170f
            medidor(c, cx, cy, 100f, imc)
            val rango = rangoImc(imc)
            val chip = pincel(11.5f, 700)
            val frase = "${"%.1f".format(Locale.US, imc)}  ·  ${nombreRango(rango)}"
            val anchoChip = chip.measureText(frase) + 32f
            c.caja(cx - anchoChip / 2, lz.y + 188f, anchoChip, 28f, ETIQUETA, 14f)
            c.drawText(frase, cx - anchoChip / 2 + 16f, lz.y + 207f, chip)
            // leyenda de rangos
            val leyenda = pincel(8.5f, 400, SECUNDARIO)
            val paso = (anchoDerecha - 24f) / 5
            RangoImc.entries.forEachIndexed { i, r ->
                val x = xDerecha + 12f + i * paso
                c.drawCircle(x + paso / 2, lz.y + 238f, 4.5f, relleno(COLORES_IMC[i]))
                val nombre = nombreRango(r)
                val l = texto(nombre, leyenda, paso - 2f, Layout.Alignment.ALIGN_CENTER, 1.1f)
                c.dibujar(l, x + 1f, lz.y + 248f)
            }
        }
    }

    // Semicírculo de cinco sectores con una aguja en el valor del IMC.
    private fun medidor(c: Canvas, cx: Float, cy: Float, r: Float, imc: Double) {
        val caja = RectF(cx - r, cy - r, cx + r, cy + r)
        COLORES_IMC.forEachIndexed { i, color -> c.drawArc(caja, 180f + i * 36f, 36f, true, relleno(color)) }
        COLORES_IMC.indices.forEach { i -> c.drawArc(caja, 180f + i * 36f, 36f, true, trazo(SUPERFICIE, 2f)) }
        c.drawCircle(cx, cy, r * 0.5f, relleno(SUPERFICIE))
        // posición dentro de su sector: cada rango ocupa 36° y el IMC avanza por él de forma lineal
        val (sector, desde, hasta) = when (rangoImc(imc)) {
            RangoImc.BAJO_PESO -> Triple(0, 15.0, 18.5)
            RangoImc.SALUDABLE -> Triple(1, 18.5, 25.0)
            RangoImc.SOBREPESO -> Triple(2, 25.0, 30.0)
            RangoImc.OBESIDAD_I -> Triple(3, 30.0, 35.0)
            RangoImc.OBESIDAD_II -> Triple(4, 35.0, 40.0)
        }
        val fraccion = ((imc - desde) / (hasta - desde)).coerceIn(0.0, 1.0)
        val grados = Math.toRadians(180.0 + sector * 36 + fraccion * 36)
        c.drawLine(cx, cy, cx + (r * 0.92f * cos(grados)).toFloat(), cy + (r * 0.92f * sin(grados)).toFloat(), trazo(TEXTO, 3f))
        c.drawCircle(cx, cy, 7f, relleno(TEXTO))
    }

    private fun nombreRango(r: RangoImc): String = s(
        when (r) {
            RangoImc.BAJO_PESO -> R.string.pdf_imc_bajo
            RangoImc.SALUDABLE -> R.string.pdf_imc_saludable
            RangoImc.SOBREPESO -> R.string.pdf_imc_sobrepeso
            RangoImc.OBESIDAD_I -> R.string.pdf_imc_obesidad1
            RangoImc.OBESIDAD_II -> R.string.pdf_imc_obesidad2
        },
    )

    private fun metas(lz: Lienzo, plan: PlanContenido) {
        lz.nueva(s(R.string.pdf_metas))
        val ayuda = texto(s(R.string.pdf_metas_ayuda), pincel(11.5f, 400, SECUNDARIO), ANCHO_UTIL, interlineado = 1.36f)
        lz.c.dibujar(ayuda, MARGEN, lz.y)
        lz.y += ayuda.height + 22f
        val anchoTarjeta = (ANCHO_UTIL - 16f) / 2
        val estiloMeta = pincel(12.5f, 600)
        plan.metas.chunked(2).forEach { fila ->
            val capas = fila.map { texto(it, estiloMeta, anchoTarjeta - 72f, interlineado = 1.25f) }
            val alto = max(58f, capas.maxOf { it.height } + 32f)
            lz.asegurar(alto + 12f)
            fila.indices.forEach { i ->
                val x = MARGEN + i * (anchoTarjeta + 16f)
                lz.c.caja(x, lz.y, anchoTarjeta, alto, SUPERFICIE, 18f)
                lz.c.drawCircle(x + 34f, lz.y + alto / 2, 14f, relleno(ETIQUETA))
                val n = (plan.metas.indexOf(fila[i]) + 1).toString()
                val pn = pincel(12f, 700)
                lz.c.drawText(n, x + 34f - pn.measureText(n) / 2, lz.y + alto / 2 + 4.5f, pn)
                lz.c.dibujar(capas[i], x + 60f, lz.y + (alto - capas[i].height) / 2)
            }
            lz.y += alto + 12f
        }
    }

    private fun planNutricional(lz: Lienzo, plan: PlanContenido) {
        lz.nueva(s(R.string.pdf_plan_nutricional))
        val c = lz.c
        val calorias = plan.calorias
        val intro = texto(
            if (calorias != null) s(R.string.pdf_plan_intro_calorias, calorias) else s(R.string.pdf_plan_intro),
            pincel(11.5f), ANCHO_UTIL, interlineado = 1.36f,
        )
        c.dibujar(intro, MARGEN, lz.y)
        lz.y += intro.height + 18f
        if (plan.equivalentes.isEmpty()) return

        val titulo = if (calorias != null) s(R.string.pdf_distribucion_calorias, calorias) else s(R.string.pdf_distribucion)
        c.drawText(titulo, MARGEN, lz.y + 14f, pincel(15f, 700))
        lz.y += 26f

        val pesos = listOf(2.3f, 1.2f, 1.2f, 1.2f, 1.2f)
        val suma = pesos.sum()
        val columnas = pesos.map { ANCHO_UTIL * it / suma }
        val encabezados = listOf(R.string.pdf_col_grupo, R.string.pdf_col_equivalentes, R.string.pdf_col_desayuno, R.string.pdf_col_comida, R.string.pdf_col_cena).map { s(it) }
        val alto = 28f
        fun fila(y: Float, celdas: List<String>, fondo: Int, colorTexto: Int, peso: Int) {
            var x = MARGEN
            celdas.forEachIndexed { i, t ->
                val p = pincel(10.5f, peso, colorTexto)
                c.drawRect(x, y, x + columnas[i], y + alto, relleno(fondo))
                c.drawText(t, if (i == 0) x + 12f else x + (columnas[i] - p.measureText(t)) / 2, y + alto / 2 + 3.8f, p)
                x += columnas[i]
            }
        }
        fila(lz.y, encabezados, TEXTO, BLANCO, 700)
        lz.y += alto
        GrupoAlimento.entries.filter { plan.equivalentes[it]?.total?.let { t -> t > 0 } == true }.forEachIndexed { i, grupo ->
            val e = plan.equivalentes.getValue(grupo)
            val fondo = if (i % 2 == 0) BLANCO else SUPERFICIE
            fila(lz.y, listOf("", e.total.toString(), e.desayuno.guion(), e.comida.guion(), e.cena.guion()), fondo, TEXTO, 400)
            // la etiqueta del grupo lleva su color
            c.drawRect(MARGEN, lz.y, MARGEN + columnas[0], lz.y + alto, relleno(grupo.color))
            c.drawText(s(grupo.titulo), MARGEN + 12f, lz.y + alto / 2 + 3.8f, pincel(10.5f, 700, BLANCO))
            c.drawLine(MARGEN, lz.y + alto, MARGEN + ANCHO_UTIL, lz.y + alto, trazo(BORDE, 0.6f))
            lz.y += alto
        }
        lz.y += 12f
        c.drawText(s(R.string.pdf_equivalente_nota), MARGEN, lz.y + 8f, pincel(9.5f, 500, SECUNDARIO))
    }

    private fun Int.guion(): String = if (this == 0) "-" else toString()

    private fun menu(lz: Lienzo, plan: PlanContenido) {
        lz.nueva(s(R.string.pdf_menu))
        tabla(
            lz,
            columnas = listOf(s(R.string.pdf_col_opcion) to 0.9f, s(R.string.pdf_col_desayuno) to 2f, s(R.string.pdf_col_comida) to 2f, s(R.string.pdf_col_cena) to 2f),
            filas = plan.opciones.mapIndexed { i, o -> listOf(s(R.string.pdf_opcion, i + 1), o.desayuno, o.comida, o.cena) },
        )
        // con duración, qué opción toca cada día
        val porDia = filasMenuPorDia(plan, { s(R.string.plan_dia_n, it) }, { s(R.string.pdf_opcion, it) })
        if (porDia.isEmpty()) return
        lz.y += 22f
        val titulo = texto(s(R.string.plan_menu_por_dia), pincel(13f, 700), ANCHO_UTIL)
        lz.c.dibujar(titulo, MARGEN, lz.y)
        lz.y += titulo.height + 10f
        tabla(lz, columnas = listOf(s(R.string.pdf_col_dia) to 1f, s(R.string.pdf_col_opcion) to 2f), filas = porDia)
    }

    private fun ejercicio(lz: Lienzo, plan: PlanContenido) {
        lz.nueva(s(R.string.pdf_ejercicio))
        if (plan.ejercicioNotas.isNotBlank()) {
            val notas = texto(plan.ejercicioNotas, pincel(11.5f), ANCHO_UTIL, interlineado = 1.36f)
            lz.c.dibujar(notas, MARGEN, lz.y)
            lz.y += notas.height + 18f
        }
        if (plan.sesiones.isNotEmpty()) {
            tabla(
                lz,
                columnas = listOf(s(R.string.pdf_col_dia) to 1f, s(R.string.pdf_col_enfoque) to 1.6f, s(R.string.pdf_col_detalle) to 4.4f),
                filas = plan.sesiones.map { listOf(it.dia, it.enfoque, it.detalle) },
            )
        }
        if (plan.ejerciciosAsignados.isEmpty()) return
        // los ejercicios concretos del banco, por día; un plan sin ellos no lleva esta tabla
        if (plan.sesiones.isNotEmpty()) lz.y += 22f
        val titulo = texto(s(R.string.pdf_asignados), pincel(13f, 700), ANCHO_UTIL)
        lz.c.dibujar(titulo, MARGEN, lz.y)
        lz.y += titulo.height + 10f
        val dias = contexto.resources.getStringArray(R.array.plan_dias)
        tabla(
            lz,
            columnas = listOf(s(R.string.pdf_col_dia) to 1.3f, s(R.string.pdf_col_ejercicio) to 2.7f, s(R.string.pdf_col_dosis) to 1.5f, s(R.string.pdf_col_nota) to 2f),
            // con duración los días son «Día 1…N»; sin ella, los de la semana
            filas = filasAsignadas(plan.ejerciciosAsignados) { if (plan.duracionDias != null) s(R.string.plan_dia_n, it) else dias[it - 1] },
        )
    }

    // Tabla de texto libre: encabezado oscuro que se repite en cada página, primera columna en etiqueta y filas alternadas.
    private fun tabla(lz: Lienzo, columnas: List<Pair<String, Float>>, filas: List<List<String>>) {
        val suma = columnas.sumOf { it.second.toDouble() }.toFloat()
        val anchos = columnas.map { ANCHO_UTIL * it.second / suma }
        val margenCelda = 9f
        val normal = pincel(10.5f)
        val negrita = pincel(10.5f, 700)

        fun encabezado() {
            val alto = 28f
            var x = MARGEN
            columnas.forEachIndexed { i, (titulo, _) ->
                lz.c.drawRect(x, lz.y, x + anchos[i], lz.y + alto, relleno(TEXTO))
                val p = pincel(11f, 700, BLANCO)
                lz.c.drawText(titulo, x + (anchos[i] - p.measureText(titulo)) / 2, lz.y + alto / 2 + 4f, p)
                x += anchos[i]
            }
            lz.y += alto
        }
        encabezado()
        filas.forEachIndexed { n, fila ->
            val capas = fila.mapIndexed { i, t -> texto(t, if (i == 0) negrita else normal, anchos[i] - 2 * margenCelda, interlineado = 1.25f) }
            val alto = max(38f, capas.maxOf { it.height } + 2 * margenCelda)
            if (lz.y + alto > Y_LIMITE) {
                lz.continuar()
                encabezado()
            }
            var x = MARGEN
            capas.forEachIndexed { i, capa ->
                val fondo = if (i == 0) ETIQUETA else if (n % 2 == 0) BLANCO else SUPERFICIE
                lz.c.drawRect(x, lz.y, x + anchos[i], lz.y + alto, relleno(fondo))
                val yTexto = if (i == 0) lz.y + (alto - capa.height) / 2 else lz.y + margenCelda
                lz.c.dibujar(capa, x + margenCelda, yTexto)
                x += anchos[i]
            }
            lz.c.drawLine(MARGEN, lz.y + alto, MARGEN + ANCHO_UTIL, lz.y + alto, trazo(BORDE, 0.6f))
            lz.y += alto
        }
    }

    private fun cierre(lz: Lienzo, d: DatosPlanPdf) {
        lz.nueva(s(R.string.pdf_cierre))
        if (d.contenido.notas.isNotBlank()) {
            val notas = texto(d.contenido.notas, pincel(11.5f), ANCHO_UTIL, interlineado = 1.36f)
            lz.c.dibujar(notas, MARGEN, lz.y)
            lz.y += notas.height + 24f
        }
        val lineas = listOfNotNull(
            s(R.string.pdf_dudas_texto, d.nutriologo),
            d.telefono.takeIf { it.isNotBlank() }?.let { s(R.string.pdf_telefono, it) },
            d.direccion.takeIf { it.isNotBlank() }?.let { s(R.string.pdf_direccion, it) },
        )
        val capas = lineas.mapIndexed { i, t -> texto(t, pincel(11.5f, if (i == 0) 500 else 400), ANCHO_UTIL - 48f, interlineado = 1.3f) }
        val alto = capas.sumOf { it.height + 8.0 }.toFloat() + 50f
        lz.asegurar(alto)
        lz.c.caja(MARGEN, lz.y, ANCHO_UTIL, alto, SUPERFICIE, 18f)
        lz.c.drawText(s(R.string.pdf_dudas), MARGEN + 24f, lz.y + 30f, pincel(14f, 700))
        var y = lz.y + 42f
        capas.forEach {
            lz.c.dibujar(it, MARGEN + 24f, y)
            y += it.height + 8f
        }
    }

    // 84 y 84.5, sin ceros de más
    private fun numero(valor: Double): String = if (valor % 1.0 == 0.0) valor.toInt().toString() else "%.1f".format(Locale.US, valor)
}
