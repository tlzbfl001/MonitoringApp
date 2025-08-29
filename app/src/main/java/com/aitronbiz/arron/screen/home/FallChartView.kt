package com.aitronbiz.arron.screen.home

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.*

data class FallChartPoint(
    val timeLabel: String,
    val value: Float
)

class FallChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {
    private var onIndexChange: ((Int) -> Unit)? = null
    fun setOnIndexChangeListener(l: ((Int) -> Unit)?) { onIndexChange = l }

    fun setData(data: List<FallChartPoint>, date: LocalDate, selectedMinute: Int) {
        this.date = date
        this.selectedMinute = selectedMinute

        minuteToValue.clear()
        for (p in data) {
            val m = parseMinuteOfDay(p.timeLabel) ?: continue
            if (p.value > 0f) {
                minuteToValue[m] = (minuteToValue[m] ?: 0f) + p.value
            }
        }

        hasAnyDataCached = minuteToValue.isNotEmpty()

        // 오늘
        drawUntil = if (date == LocalDate.now()) {
            val now = LocalTime.now()
            min(1439, now.hour * 60 + now.minute)
        } else 1439

        if (this.selectedMinute < 0 && hasAnyDataCached) {
            this.selectedMinute = minuteToValue.keys.filter { it <= drawUntil }.maxOrNull() ?: -1
        } else {
            if (this.selectedMinute > drawUntil) {
                this.selectedMinute = minuteToValue.keys.filter { it <= drawUntil }.maxOrNull() ?: -1
            }
        }

        if (content.width() > 0f && content.height() > 0f) rebuildBars()
        requestLayout()
        invalidate()
    }

    fun setGridConfig(horizontalOnly: Boolean, gridColor: Int) {
        axisPaint.color = gridColor
        invalidate()
    }

    private fun selectMinute(minuteOfDay: Int, notify: Boolean = true) {
        if (!hasAnyDataCached) return
        val targetMinute = minuteToValue.closestMinute(minuteOfDay) ?: return
        selectedMinute = targetMinute
        invalidate()

        if (notify) onIndexChange?.invoke(selectedMinute)
    }

    fun showTooltipForLastData() {
        if (!hasAnyDataCached) return
        val last = minuteToValue.keys.filter { it <= drawUntil }.maxOrNull() ?: return
        selectMinute(last, notify = true)
    }

    // 내부 상태/데이터
    private var date: LocalDate = LocalDate.now()
    private var selectedMinute: Int = -1
    private val minuteToValue = LinkedHashMap<Int, Float>()

    private data class BarRect(
        val minute: Int,
        val value: Float,
        var cx: Float = 0f,
        var left: Float = 0f,
        var right: Float = 0f,
        var top: Float = 0f,
        var bottom: Float = 0f
    )
    private val barsByMinute = HashMap<Int, BarRect>()

    private val content = RectF()
    private var stepX = 1f
    private var hasAnyDataCached = false
    private var drawUntil = 1439
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
        color = Color.argb(170, 255, 255, 255)
        textSize = 10f * sp
        textAlign = Paint.Align.LEFT
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A89E0")
        style = Paint.Style.FILL
    }
    private val selectedBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9116D3")
        style = Paint.Style.FILL
    }

    // 툴팁 & 포커스 점
    private val tooltipBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 8, 20, 35)
        style = Paint.Style.FILL
    }
    private val tooltipText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 9f * sp
    }

    // 바 스타일
    private val minBarWidthPx = 2f * dp
    private val barCorner = 3f * dp

    private val hitRadiusPx = 16f * dp

    private fun parseMinuteOfDay(hhmm: String): Int? {
        val parts = hhmm.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    private fun formatHHmm(minute: Int): String {
        val h = minute / 60
        val m = minute % 60
        return String.format("%02d:%02d", h, m)
    }

    private fun LinkedHashMap<Int, Float>.closestMinute(target: Int): Int? {
        if (isEmpty()) return null
        var bestKey = -1
        var bestDist = Int.MAX_VALUE
        for (k in keys) {
            val d = abs(k - target)
            if (d < bestDist) { bestKey = k; bestDist = d }
        }
        return if (bestKey >= 0) bestKey else null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val minH = (180f * dp).toInt()
        val h = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            else -> max(minH, suggestedMinimumHeight)
        }
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateContentRect(w, h)
        rebuildBars()
    }

    private fun updateContentRect(w: Int = width, h: Int = height) {
        val left = paddingLeft + padL
        val top = paddingTop + padT
        val right = max(0f, w - paddingRight - padR)
        val bottom = max(0f, h - paddingBottom - padB)
        content.set(left, top, right, bottom)
    }

    private fun rebuildBars() {
        barsByMinute.clear()
        if (content.width() <= 0f || content.height() <= 0f) return

        stepX = content.width() / 1439f

        val maxVal = (minuteToValue.values.maxOrNull() ?: 0f).coerceAtLeast(1f)
        val sy = content.height() / maxVal
        val yBottom = content.bottom
        val barW = max(minBarWidthPx, stepX * 0.6f)
        val half = barW / 2f

        fun xOfMinute(m: Int) = content.left + (m * stepX)
        fun yOfValue(v: Float) = yBottom - (v.coerceAtLeast(0f) * sy)

        for ((m, v) in minuteToValue) {
            if (m > drawUntil) continue
            val cx = xOfMinute(m)
            val top = yOfValue(v)
            barsByMinute[m] = BarRect(
                minute = m,
                value = v,
                cx = cx,
                left = cx - half,
                right = cx + half,
                top = top,
                bottom = yBottom
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (content.width() <= 0f || content.height() <= 0f) return

        // X축 하단 라인
        canvas.drawLine(content.left, content.bottom, content.right, content.bottom, axisPaint)

        // X축 라벨
        val baselineY = height - 2f * dp
        val anchors = arrayOf(0 to "12AM", 360 to "6AM", 720 to "12PM", 1080 to "6PM")
        for ((m, label) in anchors) {
            val x = content.left + (m / 1439f) * content.width()
            canvas.drawText(label, x, baselineY, xText)
        }

        // 데이터 없으면 막대 없음
        if (!hasAnyDataCached || barsByMinute.isEmpty()) return

        val selMinute = selectedMinute.takeIf { barsByMinute.containsKey(it) }
            ?: minuteToValue.keys.filter { it <= drawUntil }.maxOrNull()

        // 막대
        for ((m, b) in barsByMinute) {
            val r = RectF(b.left, b.top, b.right, b.bottom)
            val paint = if (m == selMinute) selectedBarPaint else barPaint
            canvas.drawRoundRect(r, barCorner, barCorner, paint)
        }

        // 툴팁
        if (selMinute != null) {
            val b = barsByMinute[selMinute]!!
            val px = b.cx
            val py = b.top
            val t1 = "${b.value.roundToInt()} 회"
            val t2 = formatHHmm(b.minute)
            drawTooltip(canvas, px, py, t1, t2)
        }
    }

    private fun drawTooltip(canvas: Canvas, px: Float, py: Float, t1: String, t2: String) {
        val padH = 6f * dp
        val padV = 4f * dp
        val gap = 2f * dp

        val boxW = max(tooltipText.measureText(t1), tooltipText.measureText(t2)) + padH * 2
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
        val textX = rect.left + padH
        var textY = rect.top + padV + tooltipText.textSize
        canvas.drawText(t1, textX, textY, tooltipText)
        textY += gap + tooltipText.textSize
        canvas.drawText(t2, textX, textY, tooltipText)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!hasAnyDataCached || barsByMinute.isEmpty()) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP -> {
                val bx = event.x
                val by = event.y
                val hit = findNearestBar(bx, by)
                if (hit != null) {
                    if (hit.minute != selectedMinute) {
                        selectedMinute = hit.minute
                        onIndexChange?.invoke(selectedMinute)
                        invalidate()
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findNearestBar(x: Float, y: Float): BarRect? {
        var best: BarRect? = null
        var bestDist = Float.MAX_VALUE
        for ((_, b) in barsByMinute) {
            val dx = abs(b.cx - x)
            val dy = if (y in b.top..b.bottom) 0f else min(abs(b.top - y), abs(b.bottom - y))
            val d = dx + dy * 0.25f
            if (dx <= hitRadiusPx && d < bestDist) { best = b; bestDist = d }
        }
        if (best == null) {
            for ((_, b) in barsByMinute) {
                val dx = abs(b.cx - x)
                if (dx < bestDist) { best = b; bestDist = dx }
            }
        }
        return best
    }
}
