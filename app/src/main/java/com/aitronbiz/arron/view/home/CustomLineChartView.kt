package com.aitronbiz.arron.view.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.aitronbiz.arron.R

class CustomLineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    var hourlyData: List<Float> = emptyList()

    private val linePaint = Paint().apply {
        color = "#4357F0".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 10f * context.resources.displayMetrics.density
        isAntiAlias = true
    }

    private val textPaint2 = Paint().apply {
        color = Color.BLACK
        textSize = 10f * context.resources.displayMetrics.density
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT
    }

    private val axisPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }

    private val markerView = LayoutInflater.from(context).inflate(R.layout.marker_view1, null)
    private val markerText = markerView.findViewById<TextView>(R.id.tvContent)
    private var markerX: Float? = null
    private var markerY: Float? = null
    private var markerValue: Float? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (hourlyData.isEmpty()) return

        val paddingLeft = 70f
        val paddingBottom = 60f
        val paddingTop = 55f
        val chartWidth = width - paddingLeft
        val chartHeight = height - paddingBottom - paddingTop

        val widthStep = chartWidth / 24f

        val yLabels = listOf(0, 20, 40, 60, 80, 100)
        val yStep = chartHeight / (yLabels.size - 1)

        val fm = textPaint.fontMetrics
        val baselineOffset = (fm.descent + fm.ascent) / 2

        val labelGap = 15f
        yLabels.forEachIndexed { index, label ->
            val y = paddingTop + chartHeight - (index * yStep)
            canvas.drawText(label.toString(), paddingLeft - labelGap, y - baselineOffset, textPaint2)
        }

        val xLabels = mapOf(
            0 to "오전12",
            6 to "오전6",
            12 to "오후12",
            18 to "오후6"
        )
        for (i in 0..23) {
            val x = paddingLeft + i * widthStep
            val label = xLabels[i] ?: ""
            if (label.isNotEmpty()) {
                canvas.drawText(label, x, paddingTop + chartHeight + 40f, textPaint)
            }
        }

        // Y축 기준선
        canvas.drawLine(
            paddingLeft,
            paddingTop,
            paddingLeft,
            paddingTop + chartHeight,
            axisPaint
        )

        // X축 기준선
        canvas.drawLine(
            paddingLeft,
            paddingTop + chartHeight,
            paddingLeft + chartWidth,
            paddingTop + chartHeight,
            axisPaint
        )

        val path = Path()
        for (i in hourlyData.indices) {
            val value = hourlyData[i].coerceAtMost(100f)
            val yRatio = value / 100f
            val x = paddingLeft + i * widthStep
            val y = paddingTop + chartHeight - (yRatio * chartHeight)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                val prevValue = hourlyData[i - 1].coerceAtMost(100f)
                val prevYRatio = prevValue / 100f
                val prevX = paddingLeft + (i - 1) * widthStep
                val prevY = paddingTop + chartHeight - (prevYRatio * chartHeight)

                val midX = (prevX + x) / 2
                path.cubicTo(midX, prevY, midX, y, x, y)
            }
        }

        val fillPath = Path(path).apply {
            lineTo(paddingLeft + 24 * widthStep, paddingTop + chartHeight)
            lineTo(paddingLeft, paddingTop + chartHeight)
            close()
        }

        val shader = LinearGradient(
            0f, paddingTop, 0f, height.toFloat(),
            Color.parseColor("#DDE1F8"),
            Color.parseColor("#FFFFFF"),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = shader
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        markerX?.let { x ->
            markerY?.let { y ->
                markerValue?.let { value ->
                    markerText.text = value.toInt().toString()
                    val specWidth = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                    val specHeight = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                    markerView.measure(specWidth, specHeight)
                    markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

                    val bitmap = Bitmap.createBitmap(
                        markerView.measuredWidth,
                        markerView.measuredHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val markerCanvas = Canvas(bitmap)
                    markerView.draw(markerCanvas)

                    val markerPosX = x - markerView.measuredWidth / 2
                    var markerPosY = y - markerView.measuredHeight - 20f
                    if (markerPosY < paddingTop) {
                        markerPosY = y + 20f
                    }

                    canvas.drawBitmap(bitmap, markerPosX, markerPosY, null)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val paddingLeft = 70f
                val paddingBottom = 60f
                val paddingTop = 55f
                val chartWidth = width - paddingLeft
                val chartHeight = height - paddingBottom - paddingTop
                val widthStep = chartWidth / 24f

                val xTouch = event.x
                val indexFloat = ((xTouch - paddingLeft) / widthStep)
                val index = indexFloat.toInt().coerceIn(0, hourlyData.size - 1)
                val value = hourlyData[index]

                val yRatio = value / 100f
                val y = paddingTop + chartHeight - (yRatio * chartHeight)

                markerX = paddingLeft + index * widthStep
                markerY = y
                markerValue = value

                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}