package com.aitronbiz.arron.view.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.aitronbiz.arron.R

class WeeklyChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values = listOf<Int>()
    private val days = listOf("일", "월", "화", "수", "목", "금", "토")
    private var selectedIndex: Int? = null
    private var markerView: View? = null

    private var maxVal = 100f

    private val barPaint = Paint().apply {
        color = "#B3B9FF".toColorInt()
        isAntiAlias = true
    }

    private val selectedBarPaint = Paint().apply {
        color = "#535EF2".toColorInt()
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 10f * context.resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    private val axisPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 0.5f * context.resources.displayMetrics.density
        isAntiAlias = true
    }

    private val yValuePaint = Paint().apply {
        color = Color.BLACK
        textSize = 10f * context.resources.displayMetrics.density
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
    }

    fun setData(newValues: List<Int>) {
        values = newValues
        maxVal = (newValues.maxOrNull() ?: 100).toFloat()
        selectedIndex = null
        invalidate()
    }

    fun setMarkerView(view: View) {
        markerView = view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val density = context.resources.displayMetrics.density
        val barCount = values.size

        val baselineY = height - textPaint.textSize - 4f * density
        val chartTop = 15f * density
        val chartHeight = baselineY - chartTop

        val textIndent = 6f * density
        val yValueWidth = 30f * density
        val chartLeft = yValueWidth

        // Y축 선
        canvas.drawLine(chartLeft, chartTop, chartLeft, baselineY, axisPaint)

        // X축 선
        canvas.drawLine(chartLeft, baselineY, width.toFloat(), baselineY, axisPaint)

        // Y축 값
        val steps = 5
        val stepValue = (maxVal / steps).toInt()
        val fontMetrics = yValuePaint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        val centerOffset = textHeight / 2 - fontMetrics.descent
        val usableHeight = chartHeight - textHeight
        val verticalShift = textHeight / 2f

        for (i in 0..steps) {
            val y = baselineY - (i / steps.toFloat()) * usableHeight - verticalShift
            val label = (i * stepValue).toString()
            val baseline = y + centerOffset

            canvas.drawText(label, chartLeft - textIndent, baseline, yValuePaint)
        }

        val extraOffset = density
        val sidePadding = chartLeft + axisPaint.strokeWidth + 11f * density + extraOffset
        val rightPadding = 11f * density

        val spacingRatio = 0.4f
        val unitCount = barCount + (barCount - 1) * spacingRatio

        val unitWidth = (width - sidePadding - rightPadding) / unitCount
        val barWidth = unitWidth
        val barSpacing = unitWidth * spacingRatio

        val markerHeight = markerView?.let {
            it.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            it.measuredHeight
        } ?: 0

        var currentX = sidePadding
        for (i in values.indices) {
            val value = values[i]
            val left = currentX
            val right = left + barWidth
            val bottom = baselineY
            val barHeight = (value / maxVal) * usableHeight
            val top = bottom - barHeight - verticalShift

            val labelY = baselineY + textPaint.textSize + 2f * density
            canvas.drawText(days[i], left + barWidth / 2, labelY, textPaint)

            if (value > 0) {
                val paint = if (selectedIndex == i) selectedBarPaint else barPaint
                val barRadius = barWidth / 2f

                val path = Path().apply {
                    moveTo(left, bottom)
                    lineTo(left, top + barRadius)
                    quadTo(left, top, left + barRadius, top)
                    lineTo(right - barRadius, top)
                    quadTo(right, top, right, top + barRadius)
                    lineTo(right, bottom)
                    close()
                }
                canvas.drawPath(path, paint)
            }

            if (selectedIndex == i && markerView != null) {
                val marker = markerView!!
                marker.findViewById<TextView>(R.id.tvContent)?.text = "$value"

                val markerWidth = marker.measuredWidth
                val markerX = (left + barWidth / 2 - markerWidth / 2).toInt()

                val safeTop = chartTop
                val desiredY = (top - markerHeight - 3f * density).toInt()
                val markerY = if (desiredY < safeTop) safeTop.toInt() else desiredY

                marker.layout(markerX, markerY, markerX + markerWidth, markerY + markerHeight)
                canvas.save()
                canvas.translate(markerX.toFloat(), markerY.toFloat())
                marker.draw(canvas)
                canvas.restore()
            }

            currentX += barWidth + barSpacing
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val density = context.resources.displayMetrics.density
            val barCount = values.size

            val yValueWidth = 30f * density
            val chartLeft = yValueWidth
            val extraOffset = 4f * density
            val sidePadding = chartLeft + axisPaint.strokeWidth + 11f * density + extraOffset
            val rightPadding = 11f * density

            val spacingRatio = 0.4f
            val unitCount = barCount + (barCount - 1) * spacingRatio
            val unitWidth = (width - sidePadding - rightPadding) / unitCount
            val barWidth = unitWidth
            val barSpacing = unitWidth * spacingRatio
            val touchPadding = 10f * density

            var currentX = sidePadding
            for (i in values.indices) {
                val left = currentX
                val right = left + barWidth
                if (event.x in (left - touchPadding)..(right + touchPadding)) {
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