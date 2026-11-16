package com.example.zipstats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

// Donut chart: الحلقة 140dp والفتحة 100dp (نسبة 0.357) مع نصين في المنتصف
class PieView(
    ctx: Context,
    private val values: List<Float>,
    private val colors: List<Int>,
    private val hole: Int,
    private val centerTop: String = "",
    private val centerBottom: String = "",
    private val topColor: Int = 0,
    private val bottomColor: Int = 0
) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val d = ctx.resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val total = values.sum()
        if (total <= 0f) return
        val s = minOf(width, height).toFloat()
        rect.set(2f, 2f, s - 2f, s - 2f)
        var start = -90f
        for ((i, v) in values.withIndex()) {
            val sweep = v / total * 360f
            p.style = Paint.Style.FILL
            p.color = colors[i % colors.size]
            c.drawArc(rect, start, sweep, true, p)
            start += sweep
        }
        p.color = hole
        c.drawCircle(s / 2f, s / 2f, s * 0.357f, p)
        p.textAlign = Paint.Align.CENTER
        if (centerTop.isNotEmpty()) {
            p.color = topColor
            p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 14f * d
            c.drawText(centerTop, s / 2f, s / 2f - 1f * d, p)
        }
        if (centerBottom.isNotEmpty()) {
            p.color = bottomColor
            p.typeface = Typeface.DEFAULT
            p.textSize = 10f * d
            c.drawText(centerBottom, s / 2f, s / 2f + 13f * d, p)
        }
    }
}
