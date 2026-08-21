package net.tawkit.mobile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Cercle de progression déterministe (0-100) pour l'écran de chargement natif
 * (splashOverlay, cf. activity_main.xml) : remplace l'ancien ProgressBar
 * indéterminé (tournait indéfiniment sans jamais donner d'indication réelle
 * d'avancement, retour utilisateur explicite du 19/08/2026). Vue custom
 * plutôt qu'une dépendance Material Components (CircularProgressIndicator)
 * pour un seul indicateur simple -- évite d'alourdir l'APK pour ça.
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** 0-100. Toute valeur hors bornes est ramenée dans [0, 100]. */
    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    private val strokeWidthPx = 5f * resources.displayMetrics.density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = 0x33DFCDB1 // même teinte beige que le texte basmala, tres attenuee
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        color = 0xFFDFCDB1.toInt() // même teinte que splashBasmala/indeterminateTint d'origine
    }

    private val arcRect = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = strokeWidthPx / 2f
        arcRect.set(inset, inset, w - inset, h - inset)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawOval(arcRect, trackPaint)
        val sweep = 360f * (progress / 100f)
        // Part du haut (-90°), sens horaire -- convention standard des
        // indicateurs de progression circulaires.
        canvas.drawArc(arcRect, -90f, sweep, false, progressPaint)
    }
}
