package com.aitronbiz.arron.screen.home

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import com.aitronbiz.arron.viewmodel.EntryChartPoint
import kotlin.math.max
import kotlin.math.roundToInt

class EntryPatternChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var data: List<EntryChartPoint> = emptyList()
    private var selectedIndex: Int = -1
    private var maxY: Int = 1

    private var onIndexChange: ((Int) -> Unit)? = null
    fun setOnIndexChangeListener(l: ((Int) -> Unit)?) { onIndexChange = l }

    private val dp = resources.displayMetrics.density
    private val sp = resources.displayMetrics.scaledDensity

    private val padL = 15f * dp
    private val padR = 16f * dp
    private val padT = 10f * dp
    private val padB = 18f * dp

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 255, 255, 255)
        strokeWidth = 1f * dp
        style = Paint.Style.STROKE
    }

    private val xText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 255, 255, 255)
        textSize = 10f * sp
        textAlign = Paint.Align.LEFT
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#5FA3FF".toColorInt()
        style = Paint.Style.FILL
    }

    private val tooltipBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 8, 20, 35)
        style = Paint.Style.FILL
    }
    private val tooltipText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 10f * sp
        textAlign = Paint.Align.CENTER
    }

    private val content = RectF()
    private var stepX = 1f

    fun setChart(points: List<EntryChartPoint>, selectedIdx: Int = -1, maxYOverride: Int? = null) {
        data = points
        maxY = max(1, maxYOverride ?: (points.maxOfOrNull { it.total } ?: 1))
        selectedIndex = if (selectedIdx >= 0) selectedIdx
        else points.indexOfLast { it.total > 0 }.coerceAtLeast(0)

        rebuildContentRect(width, height)
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
        rebuildContentRect(w, h)
    }

    private fun rebuildContentRect(w: Int, h: Int) {
        val left = paddingLeft + padL
        val top = paddingTop + padT
        val right = max(0f, w - paddingRight - padR)
        val bottom = max(0f, h - paddingBottom - padB)
        content.set(left, top, right, bottom)
        stepX = if (content.width() > 0) content.width() / 24f else 1f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty() || content.width() <= 0 || content.height() <= 0) return

        val yBottom = content.bottom
        val sy = content.height() / maxY.toFloat()

        // X축 라인
        canvas.drawLine(content.left, yBottom, content.right, yBottom, axisPaint)

        // 시간 라벨(12 AM, 6 AM, 12 PM, 6 PM)
        drawXAxisLabels(canvas)

        // 막대
        val barW = stepX * 0.4f
        val half = barW / 2f

        data.forEachIndexed { i, p ->
            if (p.total <= 0) return@forEachIndexed
            val cx = content.left + i * stepX + stepX / 2f
            val top = yBottom - (p.total * sy)
            canvas.drawRect(cx - half, top, cx + half, yBottom, barPaint)
        }

        // 툴팁
        if (selectedIndex in data.indices) {
            val p = data[selectedIndex]
            val cx = content.left + selectedIndex * stepX + stepX / 2f
            val top = yBottom - (p.total * sy)
            drawTooltip(canvas, cx, top, p)
        }
    }

    private fun drawXAxisLabels(canvas: Canvas) {
        val baselineY = height - 2f * dp
        val anchors = intArrayOf(0, 6, 12, 18)
        anchors.forEach { h ->
            val x = content.left + h * stepX
            val label = when (h) {
                0  -> "12 AM"
                6  -> "6 AM"
                12 -> "12 PM"
                18 -> "6 PM"
                else -> "%02d:00".format(h)
            }
            canvas.drawText(label, x, baselineY, xText)
        }
    }

    private fun drawTooltip(canvas: Canvas, px: Float, py: Float, p: EntryChartPoint) {
        val line1 = p.hourLabel
        val line2 = "입실: ${p.enterCount}회"
        val line3 = "퇴실: ${p.exitCount}회"

        val padH = 8f * dp
        val padV = 6f * dp
        val gap = 3f * dp

        val r = 6f * dp
        val arrowH = 4f * dp
        val arrowHalfW = 3f * dp
        val anchorGap = 2f * dp
        val EPS = 0.5f * dp
        val minW = 50f * dp

        // 박스 크기 계산
        val maxTextW = listOf(line1, line2, line3).maxOf { tooltipText.measureText(it) }
        val boxW = kotlin.math.max(maxTextW + padH * 2, minW)
        val boxH = tooltipText.textSize * 3 + gap * 2 + padV * 2

        var bx = px - boxW / 2f
        var by = py - (boxH + arrowH + anchorGap)

        // 화면 경계 보정
        if (bx < content.left) bx = content.left
        if (bx + boxW > content.right) bx = content.right - boxW
        if (by < content.top) by = content.top

        val rect = RectF(bx, by, bx + boxW, by + boxH)

        val baseY = rect.bottom - EPS
        val tipY = baseY + arrowH

        // 둥근 모서리 내부 범위에 꼬리 배치되도록 X 제한
        val minArrowX = rect.left + r + arrowHalfW
        val maxArrowX = rect.right - r - arrowHalfW
        val arrowX = px.coerceIn(minArrowX, maxArrowX)

        canvas.drawRoundRect(rect, r, r, tooltipBg)

        val tail = Path().apply {
            moveTo(arrowX - arrowHalfW, baseY)
            lineTo(arrowX + arrowHalfW, baseY)
            lineTo(arrowX, tipY)
            close()
        }
        canvas.drawPath(tail, tooltipBg)

        val cx = rect.centerX()
        var ty = rect.top + padV + tooltipText.textSize
        canvas.drawText(line1, cx, ty, tooltipText)
        ty += gap + tooltipText.textSize
        canvas.drawText(line2, cx, ty, tooltipText)
        ty += gap + tooltipText.textSize
        canvas.drawText(line3, cx, ty, tooltipText)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (data.isEmpty() || content.width() <= 0) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val i = ((event.x - content.left) / stepX).roundToInt().coerceIn(0, 23)
                if (i != selectedIndex) {
                    selectedIndex = i
                    onIndexChange?.invoke(i)
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}