package com.aitronbiz.arron.view.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentMainBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.prolificinteractive.materialcalendarview.format.DateFormatTitleFormatter
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import com.aitronbiz.arron.util.EventDecorator
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
import com.github.mikephil.charting.components.AxisBase
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitronbiz.arron.adapter.DeviceAdapter
import com.aitronbiz.arron.adapter.SubjectAdapter

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var selectedDay: CalendarDay? = null
    private var events: MutableMap<CalendarDay, List<Event>> = mutableMapOf()
    data class Event(val name: String, val average: Int)
    private var toggleActivity = false
    private var isInRoom = true
    private var weekOffset = 0
    private val activityVal = List(24) { (0..100).random().toFloat() }
    private val temperatureVal = List(24) { (0..50).random().toFloat() }
    private val brightVal = List(24) { (0..1000).random().toFloat() }
    private var dailyActivityVal: List<Float> = emptyList()
    private var onOff1 = false
    private var onOff2 = false
    private var onOff3 = false
    private var onOff4 = false
    private lateinit var subjectAdapter: SubjectAdapter
    private lateinit var deviceAdapter: DeviceAdapter
    private var selectedDevice = 1

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

        binding.btnAddSubject.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
        }

        binding.btnAddDevice.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, AddDeviceFragment())
        }

        binding.btnToggleInRoom.setOnClickListener {
            isInRoom = !isInRoom
        }

        binding.btnToggleActivity.setOnClickListener {
            if(toggleActivity) {
                binding.toggleActivityView.visibility = View.VISIBLE
                binding.tvToggleLabel.text = "간략히 보기"
                binding.btnToggleActivity.setImageResource(R.drawable.arrow_up)
            }else {
                binding.toggleActivityView.visibility = View.GONE
                binding.tvToggleLabel.text = "자세히 보기"
                binding.btnToggleActivity.setImageResource(R.drawable.arrow_down)
            }
            toggleActivity = !toggleActivity
        }

        binding.btnPrev.setOnClickListener {
            weekOffset--
            dailyActivityView()
        }

        binding.btnNext.setOnClickListener {
            weekOffset++
            dailyActivityView()
        }

        binding.btnEdit.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, EditResidenceTimeFragment())
        }

        binding.btnTelevision.setOnClickListener {
            onOff1 = !onOff1
            switchEnergyButtonStyle(onOff1, binding.btnTelevision, binding.ivTelevision, binding.energyStatus1, binding.energyType1)
        }

        binding.btnAirConditioner.setOnClickListener {
            onOff2 = !onOff2
            switchEnergyButtonStyle(onOff2, binding.btnAirConditioner, binding.ivAirConditioner, binding.energyStatus2, binding.energyType2)
        }

        binding.btnLight.setOnClickListener {
            onOff3 = !onOff3
            switchEnergyButtonStyle(onOff3, binding.btnLight, binding.ivLight, binding.energyStatus3, binding.energyType3)
        }

        binding.btnMicrowave.setOnClickListener {
            onOff4 = !onOff4
            switchEnergyButtonStyle(onOff4, binding.btnMicrowave, binding.ivMicrowave, binding.energyStatus4, binding.energyType4)
        }

//        setupProfileView()
        subjectView()
        activityView()
        toggleActivityView()
        dailyActivityView()
        residenceTimeView()

        return binding.root
    }

    private fun subjectView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val subjects = dataManager.getSubjects(AppController.prefs.getUserPrefs())

        if(subjects.isNotEmpty()) {
            binding.recyclerView.visibility = View.VISIBLE

            subjectAdapter = SubjectAdapter(subjects)
            binding.recyclerView.adapter = subjectAdapter

            subjectAdapter.notifyDataSetChanged()

            subjectAdapter.setOnItemClickListener(object : SubjectAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    subjectAdapter.setSelectedPosition(position)
                    roomStatusView(subjects[position].id)
                }
            })

            roomStatusView(subjects[0].id)
        }else {
            binding.recyclerView.visibility = View.GONE
        }
    }

    private fun roomStatusView(subjectId: Int) {
        binding.recyclerView2.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val devices = dataManager.getDevices(subjectId)

        if(devices.isNotEmpty()) {
            binding.recyclerView2.visibility = View.VISIBLE

            deviceAdapter = DeviceAdapter(devices)
            binding.recyclerView2.adapter = deviceAdapter

            deviceAdapter.notifyDataSetChanged()

            deviceAdapter.setOnItemClickListener(object : DeviceAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    deviceAdapter.setSelectedPosition(position)
                    selectedDevice = devices[position].id
                    activityView()
                    toggleActivityView()
                    dailyActivityView()
                    residenceTimeView()
                }
            })
        }else {
            binding.recyclerView2.visibility = View.GONE
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

    private fun toggleActivityView() {
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

        setupChart(binding.chart1, 1)
        setupChart(binding.chart2, 2)
        setupChart(binding.chart3, 3)
    }

    private fun setupChart(chart: BarChart, type: Int) {
        val entries = ArrayList<BarEntry>()
        var max = 0f

        when(type) {
            1 -> {
                activityVal.forEachIndexed { index, value ->
                    entries.add(BarEntry(index.toFloat(), value))
                }
                max = activityVal.max()
            }
            2 -> {
                temperatureVal.forEachIndexed { index, value ->
                    entries.add(BarEntry(index.toFloat(), value))
                }
                max = temperatureVal.max()
            }
            else -> {
                brightVal.forEachIndexed { index, value ->
                    entries.add(BarEntry(index.toFloat(), value))
                }
                max = brightVal.max()
            }
        }

        val dataSet = BarDataSet(entries, "chart").apply {
            setDrawValues(false)
            highLightAlpha = 0 // 하이라이트 색상 비활성화
            colors = List(activityVal.size) { Color.LTGRAY } // 초기엔 회색
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
                                newColors = activityVal.mapIndexed { index, v ->
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
                                newColors = temperatureVal.mapIndexed { index, v ->
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
                                newColors = brightVal.mapIndexed { index, v ->
                                    if (index == selectedIndex) {
                                        when {
                                            v < 500 -> "#333333".toColorInt()
                                            else -> Color.WHITE
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
                        List(activityVal.size) { Color.LTGRAY }
                    invalidate()
                }
            })

            invalidate()
        }
    }

    private fun dailyActivityView() {
        dailyActivityVal = List(7) { (Math.random() * 90 + 10).toFloat() }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            add(Calendar.WEEK_OF_YEAR, weekOffset)
        }

        val weekStart = calendar.time
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val weekEnd = calendar.time

        val rangeFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
//        tvWeekRange.text = "${rangeFormat.format(weekStart)} ~ ${rangeFormat.format(weekEnd)}"

        val entries = dailyActivityVal.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value)
        }

        val dataSet = BarDataSet(entries, "일간 활동량").apply {
            color = Color.LTGRAY
            highLightAlpha = 255
            highLightColor = Color.BLUE
            valueTextSize = 12f
            valueTextColor = Color.BLACK
        }

        binding.chart4.apply {
            data = BarData(dataSet).apply {
                barWidth = 0.5f
            }

            description.isEnabled = false
            legend.isEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            axisRight.isEnabled = false

            axisLeft.apply {
                setDrawAxisLine(true)
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 20f
                setDrawGridLines(false)
                textColor = Color.DKGRAY
                textSize = 10f
            }

            xAxis.apply {
                axisLineWidth = 0.8f
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = Color.DKGRAY
                textSize = 10f
                valueFormatter = object : ValueFormatter() {
                    private val dayNames = arrayOf("일", "월", "화", "수", "목", "금", "토")

                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        val index = value.toInt()
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                            add(Calendar.WEEK_OF_YEAR, weekOffset)
                            add(Calendar.DAY_OF_MONTH, index)
                        }

                        val dayName = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
                        val dateFormat = if (index == 0 || cal.get(Calendar.DAY_OF_MONTH) == 1)
                            SimpleDateFormat("MM/dd", Locale.getDefault())
                        else
                            SimpleDateFormat("dd", Locale.getDefault())

                        return "$dayName\n${dateFormat.format(cal.time)}"
                    }
                }
            }

            setTouchEnabled(true)
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        dataSet.color = Color.LTGRAY
                        dataSet.setColor(Color.LTGRAY)
                        dataSet.highLightColor = Color.BLUE
                        highlightValue(h)
                        binding.chart4.invalidate()
                    }
                }

                override fun onNothingSelected() {
                    highlightValue(null)
                }
            })

            invalidate()
        }
    }

    private fun residenceTimeView() {
        val devices = ArrayList<String>()
        devices.add("1")

        if(devices.isEmpty()) {
            binding.noDevice.visibility = View.VISIBLE
            binding.residenceView.visibility = View.GONE
        }else {
            binding.noDevice.visibility = View.GONE
            binding.residenceView.visibility = View.VISIBLE
            binding.progressData1.text = "2시간 / 4시간"
            binding.progressData2.text = "5시간 / 7시간"
            binding.progressBar.progress = 2
            binding.progressBar.max = 4
            binding.progressBar2.progress = 5
            binding.progressBar2.max = 7
        }

        binding.btnEdit.setOnClickListener {
        }
    }

    private fun switchEnergyButtonStyle(onOff: Boolean, container: ConstraintLayout, image: ImageView, status: TextView, type: TextView) {
        if(onOff) {
            container.setBackgroundDrawable(resources.getDrawable(R.drawable.smart_item_on))
            image.imageTintList = ColorStateList.valueOf("#333333".toColorInt())
            status.text = "사용함"
            status.setTextColor("#333333".toColorInt())
            type.setTextColor("#333333".toColorInt())
        }else {
            container.setBackgroundDrawable(resources.getDrawable(R.drawable.smart_item_off))
            image.imageTintList = ColorStateList.valueOf("#CCCCCC".toColorInt())
            status.text = "사용안함"
            status.setTextColor("#CCCCCC".toColorInt())
            type.setTextColor("#CCCCCC".toColorInt())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}