package com.aitronbiz.arron.view.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.aitronbiz.arron.MainViewModel
import com.aitronbiz.arron.R
import com.aitronbiz.arron.ai.StressPrediction
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentDetailBinding
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.entity.Light
import com.aitronbiz.arron.entity.Temperature
import com.aitronbiz.arron.util.CustomUtil.TAG
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
import com.prolificinteractive.materialcalendarview.format.DateFormatTitleFormatter
import java.text.SimpleDateFormat
import java.util.*

class DetailFragment : Fragment() {
    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var stressPrediction: StressPrediction
    private var dailyActivityData = ArrayList<Activity>()
    private var dailyTemperatureData = ArrayList<Temperature>()
    private var dailyLightData = ArrayList<Light>()
    private var selectedDate = CalendarDay.today()
    private var subjectId = 0
    private var deviceId = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        dataManager = DataManager.getInstance(requireActivity())

        arguments?.let {
            subjectId = it.getInt("subjectId", 0)
            deviceId = it.getInt("deviceId", 0)
        }

        stressPrediction = StressPrediction(requireActivity())

        setupCalendarView()
        getDailyData()
        setupUI()

        return binding.root
    }

    private fun setupCalendarView() {
        val topBar = binding.calendarView.getChildAt(0) as ViewGroup
        val titleTextView = topBar.getChildAt(1) as TextView
        titleTextView.textSize = 16f
        titleTextView.setTextColor(Color.BLACK)

        binding.calendarView.setLeftArrow(R.drawable.oval)
        binding.calendarView.setRightArrow(R.drawable.oval)
        binding.calendarView.state().edit().setCalendarDisplayMode(CalendarMode.WEEKS).commit()
        binding.calendarView.setSelectedDate(selectedDate)
        binding.calendarView.setTitleFormatter(
            DateFormatTitleFormatter(org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy년 MM월").withLocale(Locale.KOREA))
        )

        binding.calendarView.setOnDateChangedListener(OnDateSelectedListener { widget, date, selected ->
            selectedDate = date
            getDailyData()
        })
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.btnAddDevice.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
        }

        if (deviceId != 0) {
            binding.noDevice.visibility = View.GONE
            binding.detailView.visibility = View.VISIBLE

            val getData = dataManager.getActivityNowData(deviceId)
            if (getData == "") {
                viewModel.sendDailyData(subjectId, deviceId)
            }

            viewModel.dailyActivityUpdated.observe(requireActivity(), androidx.lifecycle.Observer { signal ->
                if (signal) {
                    getDailyData()
                }
            })
        }
    }

    private fun getDailyData() {
        val formattedDate = getFormattedDate(selectedDate)
        dailyActivityData = dataManager.getDailyActivity(deviceId, formattedDate)
        dailyTemperatureData = dataManager.getDailyTemperature(deviceId, formattedDate)
        dailyLightData = dataManager.getDailyLight(deviceId, formattedDate)

        if (dailyActivityData.isNotEmpty()) {
            binding.noData.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE
            setupChart(binding.chart1, 1)
            setupChart(binding.chart2, 2)
            setupChart(binding.chart3, 3)

            // 활동량, 온도, 조명 데이터를 제공하여 스트레스 지수 예측
            stressToPercentage()
        } else {
            binding.noData.visibility = View.VISIBLE
            binding.scrollView.visibility = View.GONE
        }
    }

    private fun setupChart(chart: BarChart, type: Int) {
        val entries = ArrayList<BarEntry>()

        // 시간대별로 데이터가 들어있는 리스트를 초기화
        val hourlyData = FloatArray(24) { 0f }  // 0시부터 23시까지

        // 데이터 세팅
        when (type) {
            1 -> {
                if (dailyActivityData.isNotEmpty()) {
                    binding.container1.visibility = View.VISIBLE
                    dailyActivityData.forEachIndexed { index, value ->
                        val hour = getHourFromCreatedAt(value.createdAt!!)
                        hourlyData[hour] += value.activity.toFloat()  // 해당 시간대의 값에 추가
                    }
                } else {
                    binding.container1.visibility = View.GONE
                }
            }
            2 -> {
                if (dailyTemperatureData.isNotEmpty()) {
                    binding.container2.visibility = View.VISIBLE
                    dailyTemperatureData.forEachIndexed { index, value ->
                        val hour = getHourFromCreatedAt(value.createdAt!!)
                        hourlyData[hour] += value.temperature.toFloat()
                    }
                } else {
                    binding.container2.visibility = View.GONE
                }
            }
            else -> {
                if (dailyLightData.isNotEmpty()) {
                    binding.container3.visibility = View.VISIBLE
                    dailyLightData.forEachIndexed { index, value ->
                        val hour = getHourFromCreatedAt(value.createdAt!!)
                        hourlyData[hour] += value.light.toFloat()
                    }
                } else {
                    binding.container3.visibility = View.GONE
                }
            }
        }

        for (hour in 0..23) {
            entries.add(BarEntry(hour.toFloat(), hourlyData[hour]))
        }

        val dataSet = BarDataSet(entries, "")
        dataSet.color = Color.parseColor("#3F51B5")
        dataSet.setDrawValues(false)

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        chart.data = barData
        chart.invalidate() // 차트 갱신

        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.setDrawGridLines(false)
        chart.xAxis.setDrawGridLines(true)

        chart.xAxis.setDrawAxisLine(true)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisRight.setDrawAxisLine(false)

        chart.xAxis.setDrawLabels(true)
        chart.axisLeft.setDrawLabels(true)
        chart.axisRight.setDrawLabels(false)

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)

        chart.description = null
        chart.legend.isEnabled = false

        chart.setDrawMarkers(false) // 툴팁/마커 비활성화

        // X축 레이블에 시간대 추가 (0시부터 23시까지)
        val xAxisLabels = ArrayList<String>()
        for (i in 0..23) {
            xAxisLabels.add("${i}시")
        }
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels)

        chart.setExtraOffsets(0f, 0f, 0f, 0f)

        chart.axisLeft.axisMinimum = 0f
    }

    private fun getHourFromCreatedAt(createdAt: String): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val date = dateFormat.parse(createdAt)
        return date?.hours ?: 0  // 시간을 추출하여 반환 (0~23)
    }

    private fun stressToPercentage() {
        if(dailyActivityData.isNotEmpty() && dailyTemperatureData.isNotEmpty() && dailyLightData.isNotEmpty()) {
            val activity = dailyActivityData[dailyActivityData.lastIndex].activity.toFloat()
            val temperature = dailyTemperatureData[dailyTemperatureData.lastIndex].temperature.toFloat()
            val lighting = dailyLightData[dailyLightData.lastIndex].light.toFloat()
            val predictedStress = stressPrediction.predict(activity, temperature, lighting)

            val stressPercentage = predictedStress.toInt() * 10
            var status = "거의 스트레스를 받지 않는 상태"

            when {
                stressPercentage <= 20 -> {
                    status = "거의 스트레스를 받지 않는 상태"
                }
                stressPercentage <= 40 -> {
                    status = "약간 스트레스를 받는 상태"
                }
                stressPercentage <= 60 -> {
                    status = "스트레스를 조금 받는 상태"
                }
                stressPercentage <= 80 -> {
                    status = "스트레스가 꽤 심한 상태"
                }
                else -> {
                    status = "매우 스트레스를 받는 상태"
                }
            }

            binding.tvStressValue.text = "스트레스 지수: $stressPercentage%"
            binding.tvStressStatus.text = status
        }
    }
}
