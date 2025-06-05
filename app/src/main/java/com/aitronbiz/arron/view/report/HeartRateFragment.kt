package com.aitronbiz.arron.view.report

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentHeartRateBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.random.Random
import androidx.core.graphics.toColorInt

class HeartRateFragment : Fragment() {

    private var _binding: FragmentHeartRateBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var dataSet: LineDataSet
    private lateinit var lineData: LineData

    private var xValue = 0f
    private var currentHeartRate = 90

    private val heartRates = mutableListOf<Int>()
    private var lastUpdateTime = 0L
    private val dataInterval = 2000L
    private val updateInterval = 500L
    private val handler = Handler(Looper.getMainLooper())

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateChartData()
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHeartRateBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireContext())

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, ReportFragment())
        }

        setupChart()
        startUpdating()

        return binding.root
    }

    private fun setupChart() {
        dataSet = LineDataSet(null, "").apply {
            color = "#7A5FFF".toColorInt()
            setDrawCircles(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f

            setDrawFilled(true)
            fillAlpha = 255
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.fade_purple)

            isHighlightEnabled = false
            setDrawValues(false)
        }

        lineData = LineData(dataSet)
        binding.lineChart.data = lineData

        binding.lineChart.apply {
            description.isEnabled = false
            setNoDataText("데이터를 수집 중입니다...")

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                isGranularityEnabled = true
                textSize = 8f
                labelRotationAngle = 0f
                setAvoidFirstLastClipping(true)
                setDrawAxisLine(true)
                axisLineColor = Color.BLACK
                axisLineWidth = 1.1f
            }

            axisRight.isEnabled = false

            axisLeft.apply {
                axisMinimum = 40f
                axisMaximum = 160f
                setDrawGridLines(true)
                setDrawAxisLine(false)
                textSize = 10f
                gridColor = Color.LTGRAY
                gridLineWidth = 0.5f
            }

            legend.isEnabled = false
            setTouchEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawBorders(false)
        }
    }

    private fun updateChartData() {
        val now = System.currentTimeMillis()

        if (now - lastUpdateTime >= dataInterval) {
            currentHeartRate = Random.nextInt(60, 120)
            heartRates.add(currentHeartRate)
            if (heartRates.size > 300) heartRates.removeAt(0)

            val avg = heartRates.average().toInt()
            val max = heartRates.maxOrNull() ?: 0

            binding.tvCurrent.text = "$currentHeartRate BPM"
            binding.tvAvg.text = "$avg BPM"
            binding.tvMax.text = "$max BPM"

            lastUpdateTime = now
        }

        // 그래프에는 항상 추가
        val entry = Entry(xValue, currentHeartRate.toFloat())
        lineData.addEntry(entry, 0)
        if (dataSet.entryCount > 300) dataSet.removeFirst()

        binding.lineChart.apply {
            notifyDataSetChanged()
            setVisibleXRangeMaximum(30f)
            moveViewToX(xValue - 25f)
            invalidate()
        }

        xValue += 1f
    }

    private fun startUpdating() {
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onDestroyView() {
        handler.removeCallbacks(updateRunnable)
        _binding = null
        super.onDestroyView()
    }
}
