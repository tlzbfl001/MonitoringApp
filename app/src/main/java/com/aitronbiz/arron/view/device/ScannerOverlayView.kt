package com.aitronbiz.arron.view.device

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val overlayPaint = Paint().apply {
        color = 0xAA000000.toInt()
    }

    private val cornerPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val clearPath = Path()
    private val frameRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 스캔 사각형 계산
        val width = width * 0.6f
        val height = width
        val left = (this.width - width) / 2
        val top = (this.height - height) / 2
        val right = left + width
        val bottom = top + height
        frameRect.set(left, top, right, bottom)

        // 바깥 어두운 영역
        clearPath.reset()
        clearPath.addRect(0f, 0f, this.width.toFloat(), this.height.toFloat(), Path.Direction.CW)
        clearPath.addRect(frameRect, Path.Direction.CCW) // 중앙 사각형 구멍
        canvas.drawPath(clearPath, overlayPaint)

        val cornerLength = 50f

        // 왼쪽 위
        canvas.drawLine(left, top, left + cornerLength, top, cornerPaint)
        canvas.drawLine(left, top, left, top + cornerLength, cornerPaint)

        // 오른쪽 위
        canvas.drawLine(right, top, right - cornerLength, top, cornerPaint)
        canvas.drawLine(right, top, right, top + cornerLength, cornerPaint)

        // 왼쪽 아래
        canvas.drawLine(left, bottom, left + cornerLength, bottom, cornerPaint)
        canvas.drawLine(left, bottom, left, bottom - cornerLength, cornerPaint)

        // 오른쪽 아래
        canvas.drawLine(right, bottom, right - cornerLength, bottom, cornerPaint)
        canvas.drawLine(right, bottom, right, bottom - cornerLength, cornerPaint)
    }

    fun getScanRect(): Rect {
        return Rect(
            frameRect.left.toInt(),
            frameRect.top.toInt(),
            frameRect.right.toInt(),
            frameRect.bottom.toInt()
        )
    }
}

