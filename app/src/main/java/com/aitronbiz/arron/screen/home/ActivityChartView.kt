package com.aitronbiz.arron.screen.home

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import com.aitronbiz.arron.model.ChartPoint
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.*

class ActivityChartView @JvmOverloads constructor(
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
    private var padL = 15f * dp
    private var padR = 20f * dp
    private var padT = 10f * dp
    private var padB = 16f * dp

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 255, 255, 255)
        strokeWidth = 1f * dp
        style = Paint.Style.STROKE
    }
    private val xText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 255, 255, 255)
        textSize = 10f * sp
        textAlign = Paint.Align.LEFT
    }

    // 막대 스타일
    private val barColor = "#5FA3FF".toColorInt()
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = barColor
        style = Paint.Style.FILL
    }

    // 툴팁
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
    private val bars = ArrayList<BarSlot>(144)

    private var drawUntil = -1 // 오늘이면 현재 시각까지만 그리기
    private var stepX = 1f
    private var hasAnyDataCached = false

    fun setChart(
        raw: List<ChartPoint>,
        selectedDate: LocalDate,
        selectedIndex: Int,
        fixedMaxY: Int
    ) {
        this.fullData = ensure144(raw)
        this.selectedDate = selectedDate
        this.maxY = max(1, fixedMaxY)

        drawUntil = if (selectedDate == LocalDate.now()) {
            val now = LocalTime.now()
            ((now.hour * 60 + now.minute) / 10).coerceIn(0, this.fullData.lastIndex)
        } else 143.coerceAtMost(this.fullData.lastIndex)

        hasAnyDataCached = detectHasAnyData()
        updateContentRect(hasAnyDataCached, width, height)

        // 초기 선택 인덱스
        this.selectedIndex = if (selectedIndex >= 0) {
            selectedIndex.coerceIn(0, max(0, drawUntil))
        } else {
            (0..max(0, drawUntil))
                .lastOrNull { i -> this.fullData.getOrNull(i)?.value?.let { it > 0f } == true }
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

        stepX = if (143 > 0) content.width() / 143f else content.width()

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

        // X축 라벨
        drawXAxisLabels(canvas)

        if (bars.isEmpty() || !hasAnyDataCached) return

        // 막대
        for (slot in bars) {
            val paint = barPaint
            canvas.drawRect(slot.rect, paint)
        }

        // 선택 인덱스 툴팁
        val idx = selectedIndex.coerceIn(0, min(drawUntil, fullData.lastIndex))
        val v = fullData[idx].value
        if (v > 0f) {
            val x = content.left + (idx * stepX)
            val sy = content.height() / max(1f, maxY.toFloat())
            val y = content.bottom - (v.coerceAtLeast(0f) * sy)
            drawTooltip(canvas, x, y, fullData[idx].timeLabel, "${v.roundToInt()} 점")
        }
    }

    private fun drawXAxisLabels(canvas: Canvas) {
        val baselineY = height - 2f * dp
        val anchors = arrayOf(
            0   to "12 AM",
            36  to "6 AM",
            72  to "12 PM",
            108 to "6 PM"
        )
        for ((slot, label) in anchors) {
            val x = content.left + slot * stepX
            canvas.drawText(label, x, baselineY, xText)
        }
    }

    private fun drawTooltip(canvas: Canvas, px: Float, py: Float, topText: String, bottomText: String) {
        // 상단 박스크기
        val padH = 6f * dp
        val minW = 50f * dp

        val padTop = 4f * dp
        val padBottom = 8f * dp
        val gap = 2f * dp

        val textW = max(tooltipText.measureText(topText), tooltipText.measureText(bottomText))
        val boxW = max(textW + padH * 2, minW)
        val boxH = tooltipText.textSize * 2 + gap + padTop + padBottom

        // 하단 삼각형
        val arrowH = 4f * dp
        val arrowHalfW = 3f * dp
        val anchorGap = 2f * dp
        val r = 6f * dp

        // 박스는 항상 포인트 위에, 삼각형은 아래로
        var bx = px - boxW / 2f
        var by = py - (boxH + arrowH + anchorGap)

        if (bx < content.left) bx = content.left
        if (bx + boxW > content.right) bx = content.right - boxW
        if (by < content.top) by = content.top

        val rect = RectF(bx, by, bx + boxW, by + boxH)

        val EPS = 0.5f * dp
        val baseY = rect.bottom - EPS
        val tipY = baseY + arrowH

        val minArrowX = rect.left + r + arrowHalfW
        val maxArrowX = rect.right - r - arrowHalfW
        val arrowX = px.coerceIn(minArrowX, maxArrowX)

        // 본체
        canvas.drawRoundRect(rect, r, r, tooltipBg)

        // 삼각형
        val tail = Path().apply {
            moveTo(arrowX - arrowHalfW, baseY)
            lineTo(arrowX + arrowHalfW, baseY)
            lineTo(arrowX, tipY)
            close()
        }
        canvas.drawPath(tail, tooltipBg)

        val textX = rect.centerX()
        var textY = rect.top + padTop + tooltipText.textSize
        canvas.drawText(topText, textX, textY, tooltipText)
        textY += gap + tooltipText.textSize
        canvas.drawText(bottomText, textX, textY, tooltipText)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (fullData.isEmpty() || drawUntil < 0) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val i = ((event.x - content.left) / stepX).roundToInt()
                val clamp = i.coerceIn(0, min(drawUntil, fullData.lastIndex))
                val target = nearestNonZeroIndex(clamp) ?: selectedIndex
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

    private fun ensure144(list: List<ChartPoint>): List<ChartPoint> {
        if (list.size >= 144) return list.take(144)
        val out = ArrayList<ChartPoint>(144)
        out.addAll(list)
        for (i in list.size until 144) {
            val totalM = i * 10
            val h = totalM / 60
            val m = totalM % 60
            out.add(ChartPoint(String.format("%02d:%02d", h, m), 0f))
        }
        return out
    }
}
