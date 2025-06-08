package com.aitronbiz.arron.view.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.MainViewModel
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.entity.Light
import com.aitronbiz.arron.entity.Temperature
import com.aitronbiz.arron.util.CustomUtil.getFormattedDate
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.ai.StressPrediction
import com.aitronbiz.arron.databinding.FragmentDetailBinding
import com.aitronbiz.arron.view.device.DeviceFragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.prolificinteractive.materialcalendarview.format.DateFormatTitleFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

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
            DateFormatTitleFormatter(DateTimeFormatter.ofPattern("yyyy년 MM월").withLocale(Locale.KOREA))
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

        if(deviceId != 0) {
            binding.noDevice.visibility = View.GONE
            binding.detailView.visibility = View.VISIBLE

            val getData = dataManager.getActivityNowData()
            if(getData == "") {
                viewModel.sendDailyData(subjectId, deviceId)
            }

            viewModel.dailyActivityUpdated.observe(requireActivity(), androidx.lifecycle.Observer { signal ->
                if(signal) {
                    getDailyData()
                }
            })
        }
    }

    private fun getDailyData() {
        val formattedDate = getFormattedDate(selectedDate)
        lifecycleScope.launch(Dispatchers.IO) {
            dailyActivityData = dataManager.getDailyActivity(deviceId, formattedDate)
            dailyTemperatureData = dataManager.getDailyTemperature(deviceId, formattedDate)
            dailyLightData = dataManager.getDailyLight(deviceId, formattedDate)
            withContext(Dispatchers.Main) {
                if(dailyActivityData.size > 0) {
                    binding.noData.visibility = View.GONE
                    binding.scrollView.visibility = View.VISIBLE
                    setupChart(binding.chart1, 1)
                    setupChart(binding.chart2, 2)
                    setupChart(binding.chart3, 3)

                    // 활동량, 온도, 조명 데이터를 제공하여 스트레스 지수 예측
                    stressToPercentage()
                }else {
                    binding.noData.visibility = View.VISIBLE
                    binding.scrollView.visibility = View.GONE
                }
            }
        }
    }

    private fun setupChart(chart: BarChart, type: Int) {
        val entries = ArrayList<BarEntry>()
        var max = 0f
        var dataSize = 0

        when(type) {
            1 -> {
                if(dailyActivityData.isNotEmpty()) {
                    binding.container1.visibility = View.VISIBLE
                    dailyActivityData.forEachIndexed { index, value ->
                        entries.add(BarEntry(index.toFloat(), value.activity.toFloat()))
                    }
                    max = dailyActivityData.maxOf { it.activity.toFloat() }
                    dataSize = dailyActivityData.size
                }else {
                    binding.container1.visibility = View.GONE
                }
            }
            2 -> {
                if(dailyTemperatureData.isNotEmpty()) {
                    binding.container2.visibility = View.VISIBLE
                    dailyTemperatureData.forEachIndexed { index, value ->
                        entries.add(BarEntry(index.toFloat(), value.temperature.toFloat()))
                    }
                    max = dailyTemperatureData.maxOf { it.temperature.toFloat() }
                    dataSize = dailyTemperatureData.size
                }else {
                    binding.container2.visibility = View.GONE
                }
            }
            else -> {
                if(dailyLightData.isNotEmpty()) {
                    binding.container3.visibility = View.VISIBLE
                    dailyLightData.forEachIndexed { index, value ->
                        entries.add(BarEntry(index.toFloat(), value.light.toFloat()))
                    }
                    max = dailyLightData.maxOf { it.light.toFloat() }
                    dataSize = dailyLightData.size
                }else {
                    binding.container3.visibility = View.GONE
                }
            }
        }

        val dataSet = BarDataSet(entries, "DailyChart").apply {
            setDrawValues(false)
            highLightAlpha = 0
            colors = List(dataSize) { Color.LTGRAY }
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.9f
        }

        chart.apply {
            data = barData
            setFitBars(true)
            setScaleEnabled(false)
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        if (index in dailyActivityData.indices) {
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                            val dateTime = org.threeten.bp.LocalDateTime.parse(dailyActivityData[index].createdAt, formatter)
                            val hour = dateTime.hour

                            return when(hour) {
                                0 -> "오전 12"
                                6 -> "오전 6"
                                12 -> "오후 12"
                                18 -> "오후 6"
                                23 -> "(시)"
                                else -> ""
                            }
                        }
                        return ""
                    }
                }
            }

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = max
                granularity = 20f
                setDrawGridLines(false)
            }

            axisRight.isEnabled = false

            invalidate()
        }
    }

    private fun stressToPercentage() {
        val activity = dailyActivityData[dailyActivityData.size - 1].activity.toFloat()
        val temperature = dailyTemperatureData[dailyTemperatureData.size - 1].temperature.toFloat()
        val lighting = dailyLightData[dailyLightData.size - 1].light.toFloat()
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