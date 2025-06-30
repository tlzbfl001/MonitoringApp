package com.aitronbiz.arron.view.home

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.aitronbiz.arron.MainViewModel
import com.aitronbiz.arron.R
import com.aitronbiz.arron.util.StressPrediction
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentDetailBinding
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.entity.Light
import com.aitronbiz.arron.entity.Temperature
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.device.DeviceFragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt
import com.aitronbiz.arron.util.CustomMarkerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.button.MaterialButton
import java.time.LocalDate

class DetailFragment : Fragment() {
    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var stressPrediction: StressPrediction
    private var dailyActivityData = ArrayList<Activity>()
    private var dailyTemperatureData = ArrayList<Temperature>()
    private var dailyLightData = ArrayList<Light>()
    private var menuType = 1
    private var subjectId = 0
    private var deviceId = 0
    private var date = LocalDate.now()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)

        setupUI()
        getDailyData()

        return binding.root
    }

    private fun setupUI() {
        setStatusBar(requireActivity(), binding.mainLayout)

        dataManager = DataManager.getInstance(requireActivity())

        arguments?.let {
            subjectId = it.getInt("subjectId", 0)
            deviceId = it.getInt("deviceId", 0)
        }

        stressPrediction = StressPrediction(requireActivity())

        if(deviceId != 0) {
            binding.noDevice.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE

            val getData = dataManager.getActivityNowData(deviceId)
            if (getData == "") {
                viewModel.sendDailyData(date, subjectId, deviceId)
            }

            viewModel.dailyActivityUpdated.observe(viewLifecycleOwner) { signal ->
                if (signal) {
                    getDailyData()
                }
            }
        }

        binding.tvDate.text = date.toString()

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.btnAddDevice.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
        }

        binding.toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if(!isChecked) return@addOnButtonCheckedListener

            for(i in 0 until group.childCount) {
                val btn = group.getChildAt(i) as MaterialButton
                if (btn.id == checkedId) {
                    btn.shapeAppearanceModel =
                        btn.shapeAppearanceModel.toBuilder()
                            .setAllCornerSizes(14f)
                            .build()
                    btn.backgroundTintList = ColorStateList.valueOf("#0D5EDD".toColorInt())
                    btn.setTextColor(Color.WHITE)

                    when (checkedId) {
                        R.id.btnDaily -> {
                            menuType = 1
                            getDailyData()
                        }
                        R.id.btnWeekly -> {
                            menuType = 2
                            getDailyData()
                        }
                        R.id.btnMonthly -> {
                            menuType = 3
                            getDailyData()
                        }
                    }
                }else {
                    btn.shapeAppearanceModel =
                        btn.shapeAppearanceModel.toBuilder()
                            .setAllCornerSizes(0f)
                            .build()
                    btn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                    btn.setTextColor(Color.BLACK)
                }
            }
        }

        binding.btnDaily.post {
            binding.btnDaily.shapeAppearanceModel =
                binding.btnDaily.shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(14f)
                    .build()
            binding.btnDaily.backgroundTintList = ColorStateList.valueOf("#0D5EDD".toColorInt())
            binding.btnDaily.setTextColor(Color.WHITE)
            binding.btnDaily.isChecked = true
        }

        binding.btnPrev.setOnClickListener {
            date = date.minusDays(1)
            binding.tvDate.text = date.toString()
            getDailyData()
        }

        binding.btnNext.setOnClickListener {
            date = date.plusDays(1)
            binding.tvDate.text = date.toString()
            getDailyData()
        }

        // 마커 설정
        val marker1 = LayoutInflater.from(requireContext()).inflate(R.layout.custom_marker_view, null)
        binding.weeklyChart1.setMarkerView(marker1)

        val marker2 = LayoutInflater.from(requireContext()).inflate(R.layout.custom_marker_view, null)
        binding.weeklyChart2.setMarkerView(marker2)

        val marker3 = LayoutInflater.from(requireContext()).inflate(R.layout.custom_marker_view, null)
        binding.weeklyChart3.setMarkerView(marker3)
    }

    private fun getDailyData() {
        dailyActivityData = dataManager.getDailyActivities(deviceId, date.toString())
        dailyTemperatureData = dataManager.getDailyTemperature(deviceId, date.toString())
        dailyLightData = dataManager.getDailyLight(deviceId, date.toString())

        if(dailyActivityData.isNotEmpty()) {
            binding.noData.visibility = View.GONE
            binding.view.visibility = View.VISIBLE

            when(menuType) {
                1 -> {
                    binding.stressContainer.visibility = View.VISIBLE
                    binding.dailyChart1.visibility = View.VISIBLE
                    binding.dailyChart2.visibility = View.VISIBLE
                    binding.dailyChart3.visibility = View.VISIBLE
                    binding.weeklyChart1.visibility = View.GONE
                    binding.weeklyChart2.visibility = View.GONE
                    binding.weeklyChart3.visibility = View.GONE
                    binding.monthlyChart1.visibility = View.GONE
                    binding.monthlyChart2.visibility = View.GONE
                    binding.monthlyChart3.visibility = View.GONE
                    setupDailyChart(binding.dailyChart1, 1)
                    setupDailyChart(binding.dailyChart2, 2)
                    setupDailyChart(binding.dailyChart3, 3)
                }
                2 -> {
                    binding.stressContainer.visibility = View.GONE
                    binding.dailyChart1.visibility = View.GONE
                    binding.dailyChart2.visibility = View.GONE
                    binding.dailyChart3.visibility = View.GONE
                    binding.monthlyChart1.visibility = View.GONE
                    binding.monthlyChart2.visibility = View.GONE
                    binding.monthlyChart3.visibility = View.GONE
                    binding.weeklyChart1.setData(listOf(60, 50, 80, 90, 40, 88, 75))
                    binding.weeklyChart2.setData(listOf(23, 22, 24, 29, 21, 26, 22))
                    binding.weeklyChart3.setData(listOf(800, 600, 400, 670, 480, 490, 685))
                    binding.weeklyChart1.visibility = View.VISIBLE
                    binding.weeklyChart2.visibility = View.VISIBLE
                    binding.weeklyChart3.visibility = View.VISIBLE
                }
                else -> {
                    binding.stressContainer.visibility = View.GONE
                    binding.dailyChart1.visibility = View.GONE
                    binding.dailyChart2.visibility = View.GONE
                    binding.dailyChart3.visibility = View.GONE
                    binding.weeklyChart1.visibility = View.GONE
                    binding.weeklyChart2.visibility = View.GONE
                    binding.weeklyChart3.visibility = View.GONE
                    binding.monthlyChart1.visibility = View.VISIBLE
                    binding.monthlyChart2.visibility = View.VISIBLE
                    binding.monthlyChart3.visibility = View.VISIBLE
                    setupMonthlyChart(binding.monthlyChart1, 1)
                    setupMonthlyChart(binding.monthlyChart2, 2)
                    setupMonthlyChart(binding.monthlyChart3, 3)
                }
            }
            stressToPercentage()
        }else {
            binding.noData.visibility = View.VISIBLE
            binding.view.visibility = View.GONE
        }
    }

    private fun setupDailyChart(chart: BarChart, type: Int) {
        val entries = ArrayList<BarEntry>()
        val hourlyData = FloatArray(24) { 0f }

        when (type) {
            1 -> {
                if (dailyActivityData.isNotEmpty()) {
                    binding.cv1.visibility = View.VISIBLE
                    dailyActivityData.forEach {
                        val hour = getHourFromCreatedAt(it.createdAt!!)
                        hourlyData[hour] += it.activity.toFloat()
                    }
                } else binding.cv1.visibility = View.GONE
            }
            2 -> {
                if (dailyTemperatureData.isNotEmpty()) {
                    binding.cv2.visibility = View.VISIBLE
                    dailyTemperatureData.forEach {
                        val hour = getHourFromCreatedAt(it.createdAt!!)
                        hourlyData[hour] += it.temperature.toFloat()
                    }
                } else binding.cv2.visibility = View.GONE
            }
            3 -> {
                if (dailyLightData.isNotEmpty()) {
                    binding.cv3.visibility = View.VISIBLE
                    dailyLightData.forEach {
                        val hour = getHourFromCreatedAt(it.createdAt!!)
                        hourlyData[hour] += it.light.toFloat()
                    }
                } else binding.cv3.visibility = View.GONE
            }
        }

        for (hour in 0..23) {
            entries.add(BarEntry(hour.toFloat(), hourlyData[hour]))
        }

        val barColor = "#A4A4FF".toColorInt()
        val dataSet = BarDataSet(entries, "").apply {
            color = barColor
            setDrawValues(false)
            highLightAlpha = 0  // ✅ 하이라이트 색상 변화 없앰
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.6f
        }

        chart.data = barData

        chart.setExtraOffsets(0f, 0f, 0f, 0f)

        // Y축
        chart.axisLeft.apply {
            setDrawGridLines(false)
            setDrawLabels(true)
            setDrawAxisLine(true)
            axisLineWidth = 1f
            axisMinimum = 0f
        }

        chart.axisRight.isEnabled = false

        chart.description = null
        chart.legend.isEnabled = false
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.isDoubleTapToZoomEnabled = false

        // ✅ 클릭 시 마커뷰는 뜨고 바 색상은 그대로 유지
        chart.isHighlightPerTapEnabled = true

        chart.xAxis.apply {
            setDrawAxisLine(true)
            axisLineWidth = 1f
            setDrawLabels(true)
            setDrawGridLines(false)
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter((0..23).map { "${it}시" })
            axisMinimum = -1.1f
            axisMaximum = 24.2f
        }

        val markerView = CustomMarkerView(requireContext(), R.layout.custom_marker_view).apply {
            chartView = chart
        }
        chart.marker = markerView

        chart.invalidate()
    }

    private fun getHourFromCreatedAt(createdAt: String): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val date = dateFormat.parse(createdAt)
        return date?.hours ?: 0
    }

    private fun setupMonthlyChart(lineChart: LineChart, type: Int) {
        val months = listOf(
            "1월", "2월", "3월", "4월", "5월", "6월",
            "7월", "8월", "9월", "10월", "11월", "12월"
        )

        val data = List(12) { index ->
            val value = when (type) {
                1 -> (0..100).random().toFloat()
                2 -> (0..50).random().toFloat()
                3 -> (0..1000).random().toFloat()
                else -> 0f
            }
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(data, "").apply {
            val lineColor = "#5558FF".toColorInt()

            color = lineColor
            lineWidth = 2.7f
            circleRadius = 5f
            setCircleColor(lineColor)
            circleHoleRadius = 3f
            circleHoleColor = Color.WHITE

            setDrawFilled(true)
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf("#885558FF".toColorInt(), Color.TRANSPARENT)
            )

            setHighlightEnabled(true)
            highLightColor = Color.TRANSPARENT
        }

        val lineData = LineData(dataSet).apply {
            setDrawValues(false)
        }

        lineChart.data = lineData

        // X축
        lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(months)
            granularity = 1f
            setLabelCount(months.size, true)

            setDrawGridLines(false)
            setDrawAxisLine(true)
            axisLineWidth = 1f
        }

        // Y축
        lineChart.axisLeft.apply {
            setDrawGridLines(false)
            setDrawAxisLine(true)
            axisLineWidth = 1f
            axisMinimum = 0f
        }
        lineChart.axisRight.isEnabled = false

        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setScaleEnabled(false)
        lineChart.isDoubleTapToZoomEnabled = false
        lineChart.isHighlightPerTapEnabled = true

        // 마커뷰
        val mv = CustomMarkerView(requireContext(), R.layout.custom_marker_view)
        mv.chartView = lineChart
        lineChart.marker = mv

        lineChart.invalidate()
    }

    class CustomMarkerView(context: Context) : MarkerView(context, R.layout.custom_marker_view) {
        private val textView: TextView = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            e?.let {
                val hours = it.y.toInt()
                val minutes = ((it.y - hours) * 60).toInt()
                textView.text = "${hours}h ${minutes}m"
            }
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2).toFloat(), -height.toFloat())
        }
    }

    private fun stressToPercentage() {
        if (dailyActivityData.isNotEmpty() &&
            dailyTemperatureData.isNotEmpty() &&
            dailyLightData.isNotEmpty()
        ) {
            val activity = dailyActivityData.last().activity.toFloat()
            val temperature = dailyTemperatureData.last().temperature.toFloat()
            val lighting = dailyLightData.last().light.toFloat()
            val predictedStress = stressPrediction.predict(activity, temperature, lighting)
            val stressPercentage = predictedStress.toInt() * 10
            val status = when {
                stressPercentage <= 20 -> "거의 스트레스를 받지 않는 상태"
                stressPercentage <= 40 -> "약간 스트레스를 받는 상태"
                stressPercentage <= 60 -> "스트레스를 조금 받는 상태"
                stressPercentage <= 80 -> "스트레스가 꽤 심한 상태"
                else -> "매우 스트레스를 받는 상태"
            }

            binding.tvStressValue.text = "스트레스 지수: $stressPercentage%"
            binding.tvStressStatus.text = status
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}