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

    // X축 값(시간), 현재 심박수, 저장용 리스트
    private var xValue = 0f
    private var currentHeartRate = 90
    private val heartRates = mutableListOf<Int>()

    private var lastUpdateTime = 0L
    private val dataInterval = 2000L // 실제 데이터 생성 주기 (2초)
    private val updateInterval = 500L // 그래프 업데이트 주기 (0.5초)
    private val handler = Handler(Looper.getMainLooper())

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

        // 차트 설정 및 데이터 업데이트 시작
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

    // 차트 데이터 업데이트 (심박수 생성 및 그래프에 추가)
    private fun updateChartData() {
        val now = System.currentTimeMillis()

        // 데이터 생성은 2초마다 수행
        if (now - lastUpdateTime >= dataInterval) {
            // 심박수 랜덤 생성 (60~120)
            currentHeartRate = Random.nextInt(60, 160)
            heartRates.add(currentHeartRate)

            // 데이터 300개 이상이면 오래된 것 제거
            if (heartRates.size > 300) heartRates.removeAt(0)

            val avg = heartRates.average().toInt()
            val max = heartRates.maxOrNull() ?: 0

            binding.tvCurrent.text = "$currentHeartRate BPM"
            binding.tvAvg.text = "$avg BPM"
            binding.tvMax.text = "$max BPM"

            lastUpdateTime = now
        }

        val entry = Entry(xValue, currentHeartRate.toFloat())
        lineData.addEntry(entry, 0)
        if (dataSet.entryCount > 300) dataSet.removeFirst()

        // 차트 갱신
        binding.lineChart.apply {
            notifyDataSetChanged()
            setVisibleXRangeMaximum(30f) // X축에 보일 최대 항목 수
            moveViewToX(xValue - 25f) // 자동 스크롤
            invalidate() // 다시 그리기
        }

        xValue += 1f
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateChartData()
            handler.postDelayed(this, updateInterval) // 0.5초마다 반복 실행
        }
    }

    // 업데이트 시작
    private fun startUpdating() {
        handler.post(updateRunnable)
    }

    // 프래그먼트 비활성화 시 업데이트 중지
    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    // 뷰 소멸 시 업데이트 중지 및 바인딩 해제
    override fun onDestroyView() {
        handler.removeCallbacks(updateRunnable)
        _binding = null
        super.onDestroyView()
    }
}
