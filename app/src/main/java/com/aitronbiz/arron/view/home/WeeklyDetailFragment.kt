package com.aitronbiz.arron.view.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentWeeklyDetailBinding
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.util.CustomMarkerView
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.getWeekDates
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import org.threeten.bp.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.ArrayList
import java.util.Calendar
import java.util.Locale

class WeeklyDetailFragment : Fragment() {
    private var _binding: FragmentWeeklyDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private val days = arrayOf("일", "월", "화", "수", "목", "금", "토")
    private var selectedDate = CalendarDay.today()
    private var deviceId = 0
    private var type = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeeklyDetailBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        dataManager = DataManager.getInstance(requireActivity())

        arguments?.let {
            deviceId = it.getInt("deviceId", 0)
        }

        setupUI()
        setupCalendarView()
        loadData()

        return binding.root
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.toggleGroup.check(R.id.btn1)

        binding.toggleGroup.addOnButtonCheckedListener { toggleButtonGroup, chekedId, isChecked ->
            if(isChecked) {
                when(chekedId) {
                    R.id.btn1 -> {
                        type = 1
                        loadData()
                    }
                    R.id.btn2 -> {
                        type = 2
                        loadData()
                    }
                    R.id.btn3 -> {
                        type = 3
                        loadData()
                    }
                }
            }
        }
    }

    private fun setupCalendarView() {
        binding.calendarView.topbarVisible = false
        binding.calendarView.setLeftArrow(R.drawable.oval)
        binding.calendarView.setRightArrow(R.drawable.oval)
        binding.calendarView.state().edit().setCalendarDisplayMode(CalendarMode.WEEKS).commit()
        binding.calendarView.setSelectedDate(selectedDate)
        binding.calendarTitle.text = selectedDate.date.format(org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        binding.calendarView.setOnDateChangedListener(OnDateSelectedListener { widget, date, selected ->
            selectedDate = date
            binding.calendarTitle.text = selectedDate.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            loadData()
        })
    }

    private fun loadData() {
        binding.tvActivity.text = "0"

        when(type) {
            1 -> {
                setupChart()
            }
            2 -> {
                setupWeeklyChart()
            }
            3 -> {

            }
        }
    }

    private fun setupChart() {
        val entries = ArrayList<BarEntry>()
        val hourlyData = FloatArray(24) { 0f }

        val activities = dataManager.getDailyActivities(deviceId, selectedDate.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        if (activities.isNotEmpty()) {
            activities.forEach { value ->
                val hour = getHourFromCreatedAt(value.createdAt!!)
                hourlyData[hour] += value.activity.toFloat()
            }
        }

        for (hour in 0..23) {
            entries.add(BarEntry(hour + 0.25f, hourlyData[hour]))
        }

        val dataSet = BarDataSet(entries, "")
        dataSet.color = "#3F51B5".toColorInt()
        dataSet.setDrawValues(false)

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f
        binding.chart.data = barData

        // 왼쪽 Y축: 숨김
        binding.chart.axisLeft.apply {
            isEnabled = true
            setDrawLabels(false)
            setDrawAxisLine(false)
            setDrawGridLines(false)
            axisMinimum = 0f
        }

        binding.chart.axisRight.apply {
            isEnabled = true
            axisMinimum = 0f
            axisMaximum = 100f
            setLabelCount(4, true)
            setDrawGridLines(true)
            setDrawAxisLine(true)
            setDrawLabels(true)
            axisLineColor = "#CCCCCC".toColorInt()
            gridColor = "#CCCCCC".toColorInt()
            textColor = "#AAAAAA".toColorInt()
        }

        // X축
        binding.chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawAxisLine(false)
            setDrawGridLines(true)
            setDrawLabels(true)
            enableGridDashedLine(10f, 5f, 0f)

            axisLineColor = "#CCCCCC".toColorInt()
            gridColor = "#CCCCCC".toColorInt()
            textColor = "#AAAAAA".toColorInt()

            granularity = 1f
            isGranularityEnabled = true
            setCenterAxisLabels(false)

            axisMinimum = 0f
            axisMaximum = 23.99f

            val xAxisLabels = ArrayList<String>()
            for (i in 0..23) {
                xAxisLabels.add("${i}시")
            }
            valueFormatter = IndexAxisValueFormatter(xAxisLabels)
        }

        // 마커 뷰
        val markerView = CustomMarkerView(requireActivity(), R.layout.custom_marker_view)
        markerView.chartView = binding.chart
        binding.chart.marker = markerView

        // 기타 설정
        binding.chart.apply {
            description = null
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            legend.isEnabled = false
            isHighlightPerTapEnabled = true
            setExtraOffsets(0f, 0f, 0f, 0f)
            invalidate()
        }

        binding.chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let {
                    val value = it.y.toInt()
                    binding.tvActivity.text = "$value"
                }
            }

            override fun onNothingSelected() {
                binding.tvActivity.text = "0"
            }
        })
    }

    private fun setupWeeklyChart() {
        val dayOfWeekData = FloatArray(7) { 0f }
        val entries = ArrayList<BarEntry>()

        val weekDates = getWeekDates(selectedDate, DayOfWeek.SUNDAY)

        for (i in weekDates.indices) {
            val weeklyActivity = dataManager.getWeeklyActivity(deviceId, weekDates[i])
            dayOfWeekData[i] += weeklyActivity.toFloat()
        }

        for (i in 0..6) {
            entries.add(BarEntry(i + 0.5f, dayOfWeekData[i])) // 막대를 중심 정렬로 설정
        }

        val dataSet = BarDataSet(entries, "")
        dataSet.color = "#3F51B5".toColorInt()
        dataSet.setDrawValues(false)

        val barData = BarData(dataSet)
        barData.barWidth = 0.2f
        binding.chart.data = barData

        // 왼쪽 Y축: 숨김
        binding.chart.axisLeft.apply {
            isEnabled = true
            setDrawLabels(false)
            setDrawAxisLine(false)
            setDrawGridLines(false)
            axisMinimum = 0f
        }

        // 오른쪽 Y축
        binding.chart.axisRight.apply {
            isEnabled = true
            axisMinimum = 0f
            axisMaximum = 2400f
            setLabelCount(4, true)
            setDrawGridLines(true)
            setDrawAxisLine(true)
            setDrawLabels(true)
            axisLineColor = "#CCCCCC".toColorInt()
            gridColor = "#CCCCCC".toColorInt()
            textColor = "#AAAAAA".toColorInt()
        }

        // X축 설정
        binding.chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawAxisLine(false)
            setDrawGridLines(true)
            enableGridDashedLine(10f, 5f, 0f)

            axisLineColor = "#CCCCCC".toColorInt()
            gridColor = "#CCCCCC".toColorInt()
            textColor = "#AAAAAA".toColorInt()

            granularity = 1f
            isGranularityEnabled = true
            setCenterAxisLabels(true)

            axisMinimum = 0f
            axisMaximum = 7f

            valueFormatter = IndexAxisValueFormatter(days.toList())
        }

        // 마커 뷰 설정
        val markerView = CustomMarkerView(requireActivity(), R.layout.custom_marker_view)
        markerView.chartView = binding.chart
        binding.chart.marker = markerView

        // 차트 공통 설정
        binding.chart.apply {
            description = null
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            legend.isEnabled = false
            isHighlightPerTapEnabled = true
            setExtraOffsets(0f, 0f, 0f, 0f)
            invalidate()
        }

        // 클릭 이벤트 처리
        binding.chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let {
                    val value = it.y.toInt()
                    binding.tvActivity.text = "$value"
                }
            }

            override fun onNothingSelected() {
                binding.tvActivity.text = "0"
            }
        })
    }

    private fun getHourFromCreatedAt(createdAt: String): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val date = dateFormat.parse(createdAt)
        return date?.hours ?: 0  // 시간을 추출하여 반환 (0~23)
    }
}