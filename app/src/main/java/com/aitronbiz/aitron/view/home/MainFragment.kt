package com.aitronbiz.aitron.view.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitronbiz.aitron.AppController
import com.aitronbiz.aitron.R
import com.aitronbiz.aitron.adapter.DeviceAdapter
import com.aitronbiz.aitron.database.DataManager
import com.aitronbiz.aitron.databinding.FragmentMainBinding
import com.aitronbiz.aitron.util.CustomUtil.TAG
import com.aitronbiz.aitron.util.CustomUtil.replaceFragment1
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.prolificinteractive.materialcalendarview.format.DateFormatTitleFormatter
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import com.aitronbiz.aitron.util.EventDecorator
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import androidx.core.graphics.toColorInt

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var isInRoom = true
    private var selectedDay: CalendarDay? = null
    private var events: MutableMap<CalendarDay, List<Event>> = mutableMapOf()
    data class Event(val name: String, val average: Int) // 이벤트 모델 클래스
    private var toggleActivity = false
    private val activityValue = List(24) { (0..100).random().toFloat() }
    private val tempValue = List(24) { (0..50).random().toFloat() }
    private val brightValue = List(24) { (0..1000).random().toFloat() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(layoutInflater)

        dataManager = DataManager(requireActivity())
        dataManager.open()

        binding.tvNotification.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, NotificationFragment())
        }

        binding.tvManage.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
        }

        binding.tvSetting.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, SettingsFragment())
        }

        binding.btnToggleInRoom.setOnClickListener {
            isInRoom = !isInRoom
            setupInRoomStatus()
            activityView()
            calendarActivityView()
        }

        binding.btnToggleActivity.setOnClickListener {
            if(toggleActivity) {
                binding.toggleActivityView.visibility = View.VISIBLE
                binding.btnToggleActivity.setImageResource(R.drawable.arrow_up)
            }else {
                binding.toggleActivityView.visibility = View.GONE
                binding.btnToggleActivity.setImageResource(R.drawable.arrow_down)
            }
            toggleActivity = !toggleActivity
        }

        setupInRoomStatus()
        activityView()
        calendarActivityView()

        return binding.root
    }

    private fun setupInRoomStatus() {
        val devices = dataManager.getDevices(AppController.prefs.getUserPrefs())

        if(isInRoom) {
            binding.absentView.visibility = View.GONE
            binding.tvPresent.setTextColor(Color.BLUE)
            binding.tvAbsent.setTextColor(Color.GRAY)

            if(devices.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            }else {
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE

                binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
                binding.recyclerView.adapter = DeviceAdapter(devices)
            }
        }else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.GONE
            binding.absentView.visibility = View.VISIBLE
            binding.tvPresent.setTextColor(Color.GRAY)
            binding.tvAbsent.setTextColor(Color.BLUE)

            binding.tvAbsentTime.text = "2시간 30분"
        }
    }

    private fun activityView() {
        val pct = 75
        if(isInRoom) {
            binding.circularProgress.setProgressWithAnimation(pct.toFloat(), 2000)
            binding.progressLabel.text = "${pct}%"

            when(pct) {
                in 0..30 -> binding.tvActiveSt3.setTextColor(Color.RED)
                in 31..70 -> binding.tvActiveSt2.setTextColor(Color.BLUE)
                else -> binding.tvActiveSt1.setTextColor(Color.GREEN)
            }

            binding.tvActiveSt1.visibility = View.VISIBLE
            binding.tvActiveSt2.visibility = View.VISIBLE
            binding.tvActiveSt3.visibility = View.VISIBLE
            binding.tvActiveAbsent.visibility = View.GONE
        }else {
            binding.circularProgress.progress = 0f
            binding.progressLabel.text = "0%"

            binding.tvActiveSt1.visibility = View.GONE
            binding.tvActiveSt2.visibility = View.GONE
            binding.tvActiveSt3.visibility = View.GONE
            binding.tvActiveAbsent.visibility = View.VISIBLE
        }
    }

    private fun calendarActivityView() {
        val currentDay = LocalDate.now()
        val focusedDay = CalendarDay.from(currentDay)

        binding.calendarView.state().edit().setCalendarDisplayMode(CalendarMode.WEEKS).commit()

        binding.calendarView.setSelectedDate(focusedDay)
        binding.calendarView.setTitleFormatter(
            DateFormatTitleFormatter(DateTimeFormatter.ofPattern("yyyy년 MM월").withLocale(Locale.KOREA))
        )

        binding.calendarView.setOnDateChangedListener(OnDateSelectedListener { widget, date, selected ->
            selectedDay = date
            Log.d(TAG, "selectedDay: $selectedDay")
            // 날짜 선택 시 처리할 작업
        })

        // 예시 이벤트
        events[CalendarDay.from(LocalDate.of(2025, 5, 21))] = listOf(Event("state", 50))
        events[CalendarDay.from(LocalDate.of(2025, 4, 21))] = listOf(Event("state", 50))
        events[CalendarDay.from(LocalDate.of(2025, 3, 1))] = listOf(Event("state", 10))

        for ((day, eventList) in events) {
            val eventColor = when {
                eventList.any { it.average <= 20 } -> Color.RED
                eventList.any { it.average <= 70 } -> Color.BLUE
                else -> Color.GREEN
            }

            val eventDecorator = EventDecorator(eventColor, listOf(day))
            binding.calendarView.addDecorator(eventDecorator)
        }

        activityBarChart(binding.activityChart, 1)
        activityBarChart(binding.tempChart, 2)
        activityBarChart(binding.brightChart, 3)
    }

    private fun activityBarChart(chart: BarChart, type: Int) {
        val entries = ArrayList<BarEntry>()
        var max = 0f

        when(type) {
            1 -> {
                activityValue.forEachIndexed { index, value ->
                    entries.add(BarEntry(index.toFloat(), value))
                }
                max = activityValue.max()
            }
            2 -> {
                tempValue.forEachIndexed { index, value ->
                    entries.add(BarEntry(index.toFloat(), value))
                }
                max = tempValue.max()
            }
            else -> {
                brightValue.forEachIndexed { index, value ->
                    entries.add(BarEntry(index.toFloat(), value))
                }
                max = brightValue.max()
            }
        }

        val dataSet = BarDataSet(entries, "chart").apply {
            setDrawValues(false)
            highLightAlpha = 0 // 하이라이트 색상 비활성화
            colors = List(activityValue.size) { Color.LTGRAY } // 초기엔 회색
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
                        return when (value.toInt()) {
                            0 -> "오전 12"
                            6 -> "오전 6"
                            12 -> "오후 12"
                            18 -> "오후 6"
                            23 -> "(시)"
                            else -> ""
                        }
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

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        val selectedIndex = it.x.toInt()
                        val value = it.y.toInt()
                        val ampm = if (selectedIndex < 12) "오전" else "오후"
                        val time = if (selectedIndex == 0 || selectedIndex == 12) "12시" else "${selectedIndex % 12}시"
                        val newColors: List<Int>

                        when(type) {
                            1 -> {
                                binding.activityChartValue.text = "$ampm $time : ${value}%"

                                // 선택된 막대만 색 변경
                                newColors = activityValue.mapIndexed { index, v ->
                                    if (index == selectedIndex) {
                                        when {
                                            v <= 30 -> Color.RED
                                            v <= 70 -> Color.BLUE
                                            else -> Color.GREEN
                                        }
                                    } else {
                                        Color.LTGRAY
                                    }
                                }
                            }
                            2 -> {
                                binding.tempChartValue.text = "$ampm $time : ${value}°C"

                                // 선택된 막대만 색 변경
                                newColors = tempValue.mapIndexed { index, v ->
                                    if (index == selectedIndex) {
                                        when {
                                            v <= 18 -> Color.BLUE
                                            v <= 27 -> Color.GREEN
                                            else -> Color.RED
                                        }
                                    } else {
                                        Color.LTGRAY
                                    }
                                }
                            }
                            else -> {
                                binding.brightChartValue.text = "$ampm $time : ${value}lux"

                                // 선택된 막대만 색 변경
                                newColors = brightValue.mapIndexed { index, v ->
                                    if (index == selectedIndex) {
                                        when {
                                            v < 500 -> Color.DKGRAY
                                            else -> "#EEEEEE".toColorInt()
                                        }
                                    } else {
                                        Color.LTGRAY
                                    }
                                }
                            }
                        }

                        (data.getDataSetByIndex(0) as BarDataSet).colors = newColors
                        invalidate()
                    }
                }

                override fun onNothingSelected() {
                    binding.activityChartValue.text = ""
                    // 모두 회색으로 초기화
                    (data.getDataSetByIndex(0) as BarDataSet).colors =
                        List(activityValue.size) { Color.LTGRAY }
                    invalidate()
                }
            })

            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}