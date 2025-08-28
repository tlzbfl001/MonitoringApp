package com.aitronbiz.arron.screen.device

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class QrGuideView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density * 3f
        color = ContextCompat.getColor(context, android.R.color.white)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = paint.strokeWidth / 2f
        val rect = RectF(pad, pad, width - pad, height - pad)
        val radius = 28f * resources.displayMetrics.density
        canvas.drawRoundRect(rect, radius, radius, paint)
    }
}