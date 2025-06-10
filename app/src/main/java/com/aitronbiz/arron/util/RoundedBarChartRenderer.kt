package com.aitronbiz.arron.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Log
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.buffer.BarBuffer
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler

class RoundedBarChartRenderer(
    chart: BarDataProvider,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : BarChartRenderer(chart, animator, viewPortHandler) {

    override fun initBuffers() {
        if (mChart.barData != null) {
            mBarBuffers = Array(mChart.barData.dataSetCount) { i ->
                BarBuffer(
                    mChart.barData.getDataSetByIndex(i).entryCount * 4,
                    mChart.barData.dataSetCount,
                    mChart.barData.getDataSetByIndex(i).isStacked
                )
            }
        }
    }

    override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
        val trans = mChart.getTransformer(dataSet.axisDependency)
        val buffer = mBarBuffers[index]

        mRenderPaint.color = dataSet.color

        buffer.setPhases(mAnimator.phaseX, mAnimator.phaseY)
        buffer.setDataSet(index)
        buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
        buffer.setBarWidth(mChart.barData.barWidth)

        buffer.feed(dataSet)

        for (j in 0 until buffer.size() step 4) {
            val left = buffer.buffer[j]
            val top = buffer.buffer[j + 1]
            val right = buffer.buffer[j + 2]
            val bottom = buffer.buffer[j + 3]

            val rect = RectF(left, top, right, bottom)
            trans.rectToPixelPhase(rect, mAnimator.phaseY)

            val radius = 20f
            val path = Path().apply {
                addRoundRect(
                    rect,
                    floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f),
                    Path.Direction.CW
                )
            }

            c.drawPath(path, mRenderPaint)
        }
    }
}


