package com.aitronbiz.arron

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt

class SleepQualityChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val values = listOf(88, 72, 85, 95, 70, 84, 89)
    private val days = listOf("일", "월", "화", "수", "목", "금", "토")
    private var selectedIndex: Int? = null

    private val barPaint = Paint().apply {
        color = Color.parseColor("#8F99FB")
        isAntiAlias = true
    }

    private val selectedBarPaint = Paint().apply {
        color = Color.parseColor("#4B56F1")
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private var markerView: View? = null

    fun setMarkerView(view: View) {
        markerView = view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val maxVal = 100f
        val barCount = values.size

        val sidePadding = 12f * context.resources.displayMetrics.density // dp → px
        val unitCount = barCount * 2 + (barCount - 1)
        val availableWidth = width - sidePadding * 2
        val unitWidth = availableWidth / unitCount

        val barWidth = unitWidth * 2
        val barSpacing = unitWidth

        val chartBottom = height.toFloat() - textPaint.textSize - 10f
        val chartHeight = chartBottom

        var currentX = sidePadding

        for (i in values.indices) {
            val left = currentX
            val right = left + barWidth
            val barHeight = (values[i] / maxVal) * chartHeight
            val top = chartBottom - barHeight
            val bottom = chartBottom

            val isSelected = selectedIndex == i
            val paint = if (isSelected) selectedBarPaint else barPaint

            val path = Path().apply {
                moveTo(left, bottom)
                lineTo(left, top + barWidth / 2)
                quadTo(left, top, left + barWidth / 2, top)
                quadTo(right, top, right, top + barWidth / 2)
                lineTo(right, bottom)
                close()
            }

            canvas.drawPath(path, paint)

            // X축 요일
            canvas.drawText(days[i], left + barWidth / 2, height.toFloat() - 4f, textPaint)

            // 마커 뷰 그리기
            if (isSelected && markerView != null) {
                val marker = markerView!!
                marker.measure(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                )
                val markerWidth = marker.measuredWidth
                val markerHeight = marker.measuredHeight

                val cx = (left + barWidth / 2).toInt()
                val markerX = cx - markerWidth / 2
                val markerY = (top - markerHeight - 20f).toInt().coerceAtLeast(0)

                marker.layout(markerX, markerY, markerX + markerWidth, markerY + markerHeight)
                marker.draw(canvas)
            }

            currentX += barWidth + barSpacing
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val barCount = values.size
            val sidePadding = 32f * context.resources.displayMetrics.density
            val unitCount = barCount * 2 + (barCount - 1)
            val availableWidth = width - sidePadding * 2
            val unitWidth = availableWidth / unitCount
            val barWidth = unitWidth * 2
            val barSpacing = unitWidth

            var currentX = sidePadding
            for (i in values.indices) {
                val left = currentX
                val right = left + barWidth
                if (event.x in left..right) {
                    selectedIndex = i
                    invalidate()
                    break
                }
                currentX += barWidth + barSpacing
            }
        }
        return true
    }
}