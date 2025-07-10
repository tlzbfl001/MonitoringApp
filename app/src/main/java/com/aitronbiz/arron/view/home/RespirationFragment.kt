package com.aitronbiz.arron.view.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.aitronbiz.arron.R
import com.aitronbiz.arron.databinding.FragmentMainBinding
import com.aitronbiz.arron.databinding.FragmentRespirationBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlin.math.log
import kotlin.math.min
import kotlin.math.roundToInt

class RespirationFragment : Fragment() {
    private var _binding: FragmentRespirationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRespirationBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        setupChart()

        return binding.root
    }

    private fun setupChart() {
        val entries = mutableListOf<BarEntry>()
        for (minute in 0 until 1440) {
            val value = getDbValueForMinute(minute)
            if (value > 0f) {
                entries.add(BarEntry(minute.toFloat(), value))
            }
        }

        val dataSet = BarDataSet(entries, "Respiration").apply {
            color = "#4A60FF".toColorInt()
            setDrawValues(false)
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.4f
        binding.barChart.data = barData

        val markerView = object : MarkerView(binding.barChart.context, R.layout.marker_view1) {
            private val tvContent: TextView = findViewById(R.id.tvContent)
            override fun refreshContent(e: Entry?, highlight: Highlight?) {
                tvContent.text = "${e?.y?.toInt() ?: ""}"
                super.refreshContent(e, highlight)
            }

            override fun getOffset(): MPPointF {
                return MPPointF(-(width / 2).toFloat(), -height.toFloat())
            }
        }
        markerView.chartView = binding.barChart
        binding.barChart.marker = markerView

        val rawMaxY = entries.maxByOrNull { it.y }?.y ?: 40f
        val roundedMaxY = kotlin.math.ceil(rawMaxY / 10f) * 10f

        // X축
        binding.barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 15f
            isGranularityEnabled = true
            setDrawGridLines(false)
            setDrawAxisLine(true)
            axisLineColor = Color.BLACK
            textColor = Color.BLACK
            textSize = 10f
            yOffset = 6f

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val totalMinutes = value.toInt()
                    val hours = totalMinutes / 60
                    val minutes = totalMinutes % 60
                    return if (minutes % 15 == 0) {
                        String.format("%02d:%02d", hours, minutes)
                    } else {
                        ""
                    }
                }
            }
        }

        // Y축
        binding.barChart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = roundedMaxY
            granularity = 10f
            isGranularityEnabled = true
            setLabelCount(5, true)

            setDrawGridLines(false)
            setDrawAxisLine(true)
            axisLineColor = Color.BLACK
            textColor = Color.BLACK
            textSize = 10f
            xOffset = 6f

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}회"
                }
            }
        }

        binding.barChart.axisRight.isEnabled = false

        // 데이터 범위 계산
        val firstMinute = entries.minByOrNull { it.x }?.x?.toInt() ?: 0
        val lastMinute = entries.maxByOrNull { it.x }?.x?.toInt() ?: 0

        val leftPadding = barData.barWidth * 9f
        val rightPadding = barData.barWidth * 7f
        val shiftAmount = barData.barWidth * 0.5f

        binding.barChart.xAxis.axisMinimum = firstMinute.toFloat() + shiftAmount - leftPadding
        binding.barChart.xAxis.axisMaximum = lastMinute.toFloat() + rightPadding

        binding.barChart.setVisibleXRangeMaximum(60f)
        binding.barChart.setExtraOffsets(0f, 0f, 0f, 2f)
        binding.barChart.isDragEnabled = true
        binding.barChart.setScaleEnabled(false)
        binding.barChart.setPinchZoom(false)
        binding.barChart.isDoubleTapToZoomEnabled = false
        binding.barChart.description.isEnabled = false
        binding.barChart.legend.isEnabled = false
        binding.barChart.invalidate()

        val lastValue = entries.lastOrNull()?.y ?: 0f
        val averageValue = if (entries.isNotEmpty()) {
            entries.sumOf { it.y.toDouble() }.toFloat() / entries.size
        } else {
            0f
        }
        val minValue = entries.minByOrNull { it.y }?.y ?: 0f
        val maxValue = entries.maxByOrNull { it.y }?.y ?: 0f

        binding.tvCurrent.text = "${lastValue.toInt()}회"
        binding.tvAverage.text = "${averageValue.roundToInt()}회"
        binding.tvMin.text = "${minValue.toInt()}회"
        binding.tvMax.text = "${maxValue.toInt()}회"
    }

    private fun getDbValueForMinute(minute: Int): Float {
        return when (minute) {
            in 240..250 -> (10..30).random().toFloat()
            in 260..275 -> (10..30).random().toFloat()
            in 280..287 -> (10..30).random().toFloat()
            in 292..310 -> (10..30).random().toFloat()
            in 320..329 -> (10..30).random().toFloat()
            else -> 0f
        }
    }
}