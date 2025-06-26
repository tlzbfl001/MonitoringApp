package com.aitronbiz.arron.view.home

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.aitronbiz.arron.util.CustomUtil.getFormattedDate
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.device.DeviceFragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt
import com.aitronbiz.arron.util.CustomMarkerView
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.buffer.BarBuffer
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import java.time.LocalDate
import kotlin.math.roundToInt

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
                            Log.d(TAG, "일간 선택됨")
                            menuType = 1
                        }
                        R.id.btnWeekly -> {
                            Log.d(TAG, "주간 선택됨")
                            menuType = 2
//                            setupRoundedBarChart(binding.chart1)
                        }
                        R.id.btnMonthly -> {
                            Log.d(TAG, "월간 선택됨")
                            menuType = 3
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
    }

    private fun getDailyData() {
        dailyActivityData = dataManager.getDailyActivities(deviceId, date.toString())
        dailyTemperatureData = dataManager.getDailyTemperature(deviceId, date.toString())
        dailyLightData = dataManager.getDailyLight(deviceId, date.toString())

        if(dailyActivityData.isNotEmpty()) {
            binding.noData.visibility = View.GONE
            binding.view.visibility = View.VISIBLE
//            setupChart(binding.chart1, 1)
//            setupChart(binding.chart2, 2)
//            setupChart(binding.chart3, 3)
//            setupRoundedBarChart(binding.chart1)
            stressToPercentage()
        }else {
            binding.noData.visibility = View.VISIBLE
            binding.view.visibility = View.GONE
        }
    }

    private fun setupChart(chart: BarChart, type: Int) {
        val entries = ArrayList<BarEntry>()
        val hourlyData = FloatArray(24) { 0f }

        when(type) {
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

        for(hour in 0..23) {
            entries.add(BarEntry(hour.toFloat(), hourlyData[hour]))
        }

        val defaultColor = "#A4A4FF".toColorInt()
        val highlightColor = "#5558FF".toColorInt()
        val colors = MutableList(24) { defaultColor }

        val dataSet = BarDataSet(entries, "")
        dataSet.colors = colors
        dataSet.setDrawValues(false)

        val barData = BarData(dataSet)
        barData.barWidth = 0.8f
        chart.data = barData

        chart.setExtraOffsets(0f, 0f, 0f, 0f)
        chart.axisLeft.apply {
            setDrawGridLines(false)
            setDrawLabels(false)
            setDrawAxisLine(false)
            axisMinimum = 0f
        }

        chart.axisRight.isEnabled = false
        chart.description = null
        chart.legend.isEnabled = false
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.isHighlightPerTapEnabled = true

        chart.xAxis.apply {
            setDrawAxisLine(true)
            setDrawLabels(true)
            setDrawGridLines(true)
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter((0..23).map { "${it}시" })
        }

        val markerView = CustomMarkerView(requireContext(), R.layout.custom_marker_view)
        markerView.chartView = chart
        chart.marker = markerView

        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val selectedIndex = e?.x?.toInt() ?: return
                for (i in colors.indices) {
                    colors[i] = if (i == selectedIndex) highlightColor else defaultColor
                }
                dataSet.colors = colors
                chart.data = BarData(dataSet) // 새로 적용
                chart.notifyDataSetChanged()
                chart.invalidate()
            }

            override fun onNothingSelected() {
                for (i in colors.indices) {
                    colors[i] = defaultColor
                }
                dataSet.colors = colors
                chart.data = BarData(dataSet)
                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        })

        chart.invalidate()
    }

    private fun setupRoundedBarChart(chart: BarChart) {
        val values = listOf(88f, 72f, 85f, 90f, 75f, 82f, 86f)
        val labels = listOf("16", "17", "18", "19", "20", "21", "22")

        val defaultColor = "#C2C2FF".toColorInt()
        val selectedColor = "#5856FF".toColorInt()
        val entries = values.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
        val dataSet = BarDataSet(entries, "").apply {
            colors = List(entries.size) { defaultColor }
            setDrawValues(false)
        }
        chart.data = BarData(dataSet).apply { barWidth = 0.5f }
        chart.renderer = RoundedBarChartRenderer(chart, chart.animator, chart.viewPortHandler)
        chart.description = Description().apply { isEnabled = false }
        chart.axisLeft.apply { axisMinimum = 0f; setDrawLabels(false); setDrawAxisLine(false); setDrawGridLines(false) }
        chart.axisRight.isEnabled = false
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            labelCount = labels.size
        }
        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val selectedIndex = e?.x?.toInt() ?: return
                dataSet.colors = List(values.size) { i -> if (i == selectedIndex) selectedColor else defaultColor }
                chart.invalidate()
            }
            override fun onNothingSelected() {
                dataSet.colors = List(values.size) { defaultColor }
                chart.invalidate()
            }
        })
        chart.data.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    class RoundedBarChartRenderer(
        chart: BarDataProvider,
        animator: ChartAnimator,
        viewPortHandler: ViewPortHandler
    ) : BarChartRenderer(chart, animator, viewPortHandler) {
        private val barRadius = 30f
        override fun initBuffers() {
            mBarBuffers = Array(mChart.barData.dataSetCount) { i ->
                val set = mChart.barData.getDataSetByIndex(i)
                BarBuffer(set.entryCount * 4, mChart.barData.dataSetCount, set.isStacked)
            }
        }
        override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
            val buffer = mBarBuffers.getOrNull(index) ?: return
            val paint = mRenderPaint
            for (j in buffer.buffer.indices step 4) {
                val left = buffer.buffer[j]
                val top = buffer.buffer[j + 1]
                val right = buffer.buffer[j + 2]
                val bottom = buffer.buffer[j + 3]
                paint.color = dataSet.getColor(j / 4)
                val rectF = RectF(left, top, right, bottom)
                c.drawRoundRect(rectF, barRadius, barRadius, paint)
                if (bottom > top) {
                    c.drawRect(RectF(left, top + (right - left) / 2, right, bottom), paint)
                }
            }
        }
    }

    private fun getHourFromCreatedAt(createdAt: String): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val date = dateFormat.parse(createdAt)
        return date?.hours ?: 0
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
