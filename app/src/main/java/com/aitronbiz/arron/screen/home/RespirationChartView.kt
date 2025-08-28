package com.aitronbiz.arron.screen.home

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.aitronbiz.arron.model.ChartPoint
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.*
import androidx.core.graphics.toColorInt

class RespirationChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private var fullData: List<ChartPoint> = emptyList()
    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedIndex: Int = -1
    private var maxY: Int = 1

    private var onIndexChange: ((Int) -> Unit)? = null
    fun setOnIndexChangeListener(l: ((Int) -> Unit)?) { onIndexChange = l }

    private val dp = resources.displayMetrics.density
    private val sp = resources.displayMetrics.scaledDensity
    private var padL = 20f * dp
    private var padR = 20f * dp
    private var padT = 10f * dp
    private var padB = 16f * dp

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 255, 255, 255)
        strokeWidth = 1f * dp
        style = Paint.Style.STROKE
    }

    private val xText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 255, 255, 255) // X축 라벨
        textSize = 10f * sp
        textAlign = Paint.Align.LEFT
    }

    // 툴팁 막대
    private val barColor = "#5AAEFF".toColorInt()
    private val selectedBarColor = "#10B981".toColorInt()
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = barColor
        style = Paint.Style.FILL
    }
    private val selectedBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = selectedBarColor
        style = Paint.Style.FILL
    }

    // 툴팁 박스
    private val tooltipBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 8, 20, 35)
        style = Paint.Style.FILL
    }
    private val tooltipText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 9f * sp
        textAlign = Paint.Align.CENTER
    }

    private val content = RectF()
    private data class BarSlot(val index: Int, val rect: RectF)
    private val bars = ArrayList<BarSlot>(1440)
    private var drawUntil = -1
    private var stepX = 1f
    private var hasAnyDataCached = false

    fun setChart(
        raw: List<ChartPoint>,
        selectedDate: LocalDate,
        selectedIndex: Int,
        fixedMaxY: Int
    ) {
        this.fullData = raw
        this.selectedDate = selectedDate
        this.maxY = max(1, fixedMaxY)

        // 오늘이면 현재 시각까지만
        drawUntil = if (selectedDate == LocalDate.now()) {
            val now = LocalTime.now()
            min(raw.lastIndex, now.hour * 60 + now.minute)
        } else 1439.coerceAtMost(raw.lastIndex)

        hasAnyDataCached = detectHasAnyData()
        updateContentRect(hasAnyDataCached, width, height)

        // 초기 선택
        this.selectedIndex = if (selectedIndex >= 0) {
            selectedIndex
        } else {
            (0..max(0, drawUntil))
                .lastOrNull { i -> raw.getOrNull(i)?.value?.let { it > 0f } == true }
                ?: drawUntil.coerceAtLeast(0)
        }

        if (content.width() > 0f && content.height() > 0f) rebuildBars()
        requestLayout()
        invalidate()
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        val minH = (180f * dp).toInt()
        val h = when (MeasureSpec.getMode(hSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(hSpec)
            else -> max(minH, suggestedMinimumHeight)
        }
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        hasAnyDataCached = detectHasAnyData()
        updateContentRect(hasAnyDataCached, w, h)
        rebuildBars()
    }

    private fun detectHasAnyData(): Boolean {
        if (fullData.isEmpty() || drawUntil < 0) return false
        val end = min(drawUntil, fullData.lastIndex)
        for (i in 0..end) if (fullData[i].value > 0f) return true
        return false
    }

    private fun updateContentRect(@Suppress("UNUSED_PARAMETER") hasData: Boolean, w: Int = width, h: Int = height) {
        val left = paddingLeft + padL
        val top = paddingTop + padT
        val right = max(0f, w - paddingRight - padR)
        val bottom = max(0f, h - paddingBottom - padB)
        content.set(left, top, right, bottom)
    }

    private fun rebuildBars() {
        bars.clear()
        if (content.width() <= 0f || content.height() <= 0f || drawUntil < 0 || fullData.isEmpty()) return

        stepX = if (1439 > 0) content.width() / 1439f else content.width()

        val sy = content.height() / max(1f, maxY.toFloat())
        val yBottom = content.bottom
        val barW = max(2f * dp, stepX * 0.6f)
        val half = barW / 2f

        fun xOfIndex(i: Int) = content.left + (i * stepX)
        fun yOfValue(v: Float) = yBottom - (v.coerceAtLeast(0f) * sy)

        for (i in 0..drawUntil) {
            val v = fullData[i].value
            if (v <= 0f) continue
            val cx = xOfIndex(i)
            val top = yOfValue(v)
            bars += BarSlot(i, RectF(cx - half, top, cx + half, yBottom))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (content.width() <= 0f || content.height() <= 0f) return

        // X축 하단 라인
        canvas.drawLine(content.left, content.bottom, content.right, content.bottom, axisPaint)

        // X축
        canvas.drawLine(content.left, content.bottom, content.right, content.bottom, axisPaint)
        drawXAxisLabels(canvas)

        if (bars.isEmpty() || !hasAnyDataCached) return

        // 막대
        for (slot in bars) {
            val paint = if (slot.index == selectedIndex) selectedBarPaint else barPaint
            canvas.drawRect(slot.rect, paint)
        }

        // 선택 인덱스 기준 툴팁
        val idx = selectedIndex.coerceIn(0, min(drawUntil, fullData.lastIndex))
        val v = fullData[idx].value
        if (v > 0f) {
            val x = content.left + (idx * stepX)
            val y = content.bottom - (v.coerceAtLeast(0f) * (content.height() / max(1f, maxY.toFloat())))
            drawTooltip(canvas, x, y, fullData[idx].timeLabel, "${v.roundToInt()} bpm")
        }
    }

    private fun drawXAxisLabels(canvas: Canvas) {
        val baselineY = height - 2f * dp
        val anchors = arrayOf(
            0    to "12 AM",
            360  to "6 AM",
            720  to "12 PM",
            1080 to "6 PM"
        )
        for ((m, label) in anchors) {
            val x = content.left + (m / 1439f) * content.width()
            canvas.drawText(label, x, baselineY, xText)
        }
    }

    private fun drawTooltip(canvas: Canvas, px: Float, py: Float, topText: String, bottomText: String) {
        val padH = 6f * dp
        val padV = 4f * dp
        val gap = 2f * dp

        val boxW = max(tooltipText.measureText(topText), tooltipText.measureText(bottomText)) + padH * 2
        val boxH = tooltipText.textSize * 2 + padV * 2 + gap
        val anchorGap = 6f * dp

        var bx = px - boxW / 2f
        var by = py - (boxH + anchorGap)
        if (bx < content.left) bx = content.left
        if (bx + boxW > content.right) bx = content.right - boxW
        if (by < content.top) by = py + anchorGap

        val rect = RectF(bx, by, bx + boxW, by + boxH)
        val r = 8f * dp

        canvas.drawRoundRect(rect, r, r, tooltipBg)
        val textX = rect.centerX()
        var textY = rect.top + padV + tooltipText.textSize
        canvas.drawText(topText, textX, textY, tooltipText) // 위: 시간
        textY += gap + tooltipText.textSize
        canvas.drawText(bottomText, textX, textY, tooltipText) // 아래: bpm
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (fullData.isEmpty() || drawUntil < 0) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val i = ((event.x - content.left) / stepX).roundToInt()
                val clamp = i.coerceIn(0, min(drawUntil, fullData.lastIndex))
                val target = nearestNonZeroIndex(clamp) ?: selectedIndex // 유효값 없으면 유지
                if (target != selectedIndex) {
                    selectedIndex = target
                    onIndexChange?.invoke(selectedIndex)
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun nearestNonZeroIndex(from: Int): Int? {
        if (fullData.isEmpty() || drawUntil < 0) return null
        val end = min(drawUntil, fullData.lastIndex)
        if (from in 0..end && fullData[from].value > 0f) return from
        var off = 1
        while (true) {
            val l = from - off
            val r = from + off
            var hit: Int? = null
            if (l >= 0 && fullData[l].value > 0f) hit = l
            if (r <= end && fullData[r].value > 0f) return hit ?: r
            if (hit != null) return hit
            if (l < 0 && r > end) break
            off++
        }
        return null
    }
}