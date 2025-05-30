package com.aitronbiz.arron.view.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentMainBinding
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.prolificinteractive.materialcalendarview.format.DateFormatTitleFormatter
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
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
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.components.AxisBase
import kotlin.collections.ArrayList
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitronbiz.arron.MainViewModel
import com.aitronbiz.arron.adapter.DeviceAdapter
import com.aitronbiz.arron.adapter.SubjectAdapter
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.Light
import com.aitronbiz.arron.entity.Temperature
import com.aitronbiz.arron.util.CustomUtil.getFormattedDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var dataManager: DataManager
    private lateinit var subjectAdapter: SubjectAdapter
    private lateinit var deviceAdapter: DeviceAdapter
    private var dailyActivityData = ArrayList<Activity>()
    private var dailyTemperatureData = ArrayList<Temperature>()
    private var dailyLightData = ArrayList<Light>()
    private var selectedDate = CalendarDay.today()
    private var selectedDevice = Device()
    private lateinit var startOfWeek: LocalDate
    private var subjectId = 0
    private var toggleActivity = false
    private var onOff1 = false
    private var onOff2 = false
    private var onOff3 = false
    private var onOff4 = false

    // 날짜 포맷
    private val dataFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayFormatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd")
    private val dayNames = arrayOf("일", "월", "화", "수", "목", "금", "토")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(layoutInflater)

        dataManager = DataManager(requireActivity())
        dataManager.open()

        val today = LocalDate.now() // 오늘 날짜
        startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) // 이번 주 일요일

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

        binding.toggleLabel.setOnClickListener {
            if(toggleActivity) {
                binding.activityView.visibility = View.VISIBLE
                binding.toggleLabel.text = "간략히 보기"
                binding.btnToggle.setImageResource(R.drawable.arrow_up)
            }else {
                binding.activityView.visibility = View.GONE
                binding.toggleLabel.text = "자세히 보기"
                binding.btnToggle.setImageResource(R.drawable.arrow_down)
            }
            toggleActivity = !toggleActivity
        }

        binding.btnToggle.setOnClickListener {
            if(toggleActivity) {
                binding.activityView.visibility = View.VISIBLE
                binding.toggleLabel.text = "간략히 보기"
                binding.btnToggle.setImageResource(R.drawable.arrow_up)
            }else {
                binding.activityView.visibility = View.GONE
                binding.toggleLabel.text = "자세히 보기"
                binding.btnToggle.setImageResource(R.drawable.arrow_down)
            }
            toggleActivity = !toggleActivity
        }

        val topBar = binding.calendarView.getChildAt(0) as ViewGroup
        val titleTextView = topBar.getChildAt(1) as TextView
        titleTextView.textSize = 16f
        titleTextView.setTextColor(Color.GRAY)

        binding.calendarView.state().edit().setCalendarDisplayMode(CalendarMode.WEEKS).commit()
        binding.calendarView.setSelectedDate(selectedDate)
        binding.calendarView.setTitleFormatter(
            DateFormatTitleFormatter(DateTimeFormatter.ofPattern("yyyy년 MM월").withLocale(Locale.KOREA))
        )

        binding.calendarView.setOnDateChangedListener(OnDateSelectedListener { widget, date, selected ->
            selectedDate = date
            getDailyData()
            activityView()
            detailDataView()
            weeklyActivityView()
            consecutiveTimeView()
        })

        binding.btnPrev.setOnClickListener {
            startOfWeek = startOfWeek.minusWeeks(1)
            weeklyActivityView()
        }

        binding.btnNext.setOnClickListener {
            startOfWeek = startOfWeek.plusWeeks(1)
            weeklyActivityView()
        }

        binding.btnEdit.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, EditResidenceTimeFragment())
        }

        binding.btnTelevision.setOnClickListener {
            onOff1 = !onOff1
            switchButtonStyle(onOff1, binding.btnTelevision, binding.ivTelevision, binding.energyStatus1, binding.energyType1)
        }

        binding.btnAirConditioner.setOnClickListener {
            onOff2 = !onOff2
            switchButtonStyle(onOff2, binding.btnAirConditioner, binding.ivAirConditioner, binding.energyStatus2, binding.energyType2)
        }

        binding.btnLight.setOnClickListener {
            onOff3 = !onOff3
            switchButtonStyle(onOff3, binding.btnLight, binding.ivLight, binding.energyStatus3, binding.energyType3)
        }

        binding.btnMicrowave.setOnClickListener {
            onOff4 = !onOff4
            switchButtonStyle(onOff4, binding.btnMicrowave, binding.ivMicrowave, binding.energyStatus4, binding.energyType4)
        }

        viewModel.dailyDataUpdated.observe(requireActivity(), androidx.lifecycle.Observer { signal ->
            if(signal) {
                getDailyData()
                activityView()
                detailDataView()
                weeklyActivityView()
            }
        })

        binding.btnAddTestData.setOnClickListener {
            if(selectedDevice.id > 0) {
                viewModel.sendDailyData(selectedDate, subjectId, selectedDevice.id)
            }else {
                Toast.makeText(requireActivity(), "기기를 먼저 등록해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        subjectListView() // 등록 대상자 조회
        activityView() // 활동도 조회
        detailDataView() // 일별 활동량, 온도, 조명 데이터 조회
        weeklyActivityView() // 주별 활동도 조회
        consecutiveTimeView() // 연속 거주 시간 조회

        return binding.root
    }

    private fun subjectListView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val subjects = dataManager.getSubjects(AppController.prefs.getUserPrefs())
        if(subjects.isNotEmpty()) {
            binding.btnAddSubject.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            subjectAdapter = SubjectAdapter(subjects)
            binding.recyclerView.adapter = subjectAdapter

            binding.tvSubjectCnt.text = "등록된 대상자 : ${subjects.size}명"
            subjectId = subjects[0].id
            roomListView(subjects[0].id)

            subjectAdapter.setOnItemClickListener(object : SubjectAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    subjectAdapter.setSelectedPosition(position)
                    subjectId = subjects[position].id
                    roomListView(subjects[position].id)
                }
            })
        }else {
            binding.recyclerView.visibility = View.GONE
            binding.btnAddSubject.visibility = View.VISIBLE
        }
    }

    private fun roomListView(subjectId: Int) {
        binding.recyclerView2.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val devices = dataManager.getDevices(subjectId)
        if(devices.isNotEmpty()) {
            binding.recyclerView2.visibility = View.VISIBLE
            deviceAdapter = DeviceAdapter(devices)
            binding.recyclerView2.adapter = deviceAdapter

            selectedDevice = devices[0]
            getDailyData()

            deviceAdapter.setOnItemClickListener(object : DeviceAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    deviceAdapter.setSelectedPosition(position)
                    selectedDevice = devices[position]
                    getDailyData()
                    activityView()
                    detailDataView()
                    weeklyActivityView()
                    consecutiveTimeView()
                }
            })
        }else {
            binding.recyclerView2.visibility = View.GONE
        }
    }

    private fun activityView() {
        if(dailyActivityData.isNotEmpty() && selectedDevice.room == 1) {
            var total = 0
            for(i in dailyActivityData.indices) {
                total += dailyActivityData[i].activity
            }

            val pct = (total * 100) / (dailyActivityData.size * 100)
            binding.circularProgress.setProgressWithAnimation(pct.toFloat(), 2000)
            binding.progressLabel.text = "${pct}%"

            when(pct) {
                in 0..30 -> {
                    binding.tvActiveSt1.setTextColor("#CCCCCC".toColorInt())
                    binding.tvActiveSt2.setTextColor("#CCCCCC".toColorInt())
                    binding.tvActiveSt3.setTextColor(Color.RED)
                }
                in 31..70 -> {
                    binding.tvActiveSt1.setTextColor("#CCCCCC".toColorInt())
                    binding.tvActiveSt2.setTextColor(Color.BLUE)
                    binding.tvActiveSt3.setTextColor("#CCCCCC".toColorInt())
                }
                else -> {
                    binding.tvActiveSt1.setTextColor(Color.GREEN)
                    binding.tvActiveSt2.setTextColor("#CCCCCC".toColorInt())
                    binding.tvActiveSt3.setTextColor("#CCCCCC".toColorInt())
                }
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

    private fun detailDataView() {
        setupChart(binding.chart1, 1)
        setupChart(binding.chart2, 2)
        setupChart(binding.chart3, 3)
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
            highLightAlpha = 0 // 하이라이트 색상 비활성화
            colors = List(dataSize) { Color.LTGRAY } // 초기엔 회색
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

    private fun weeklyActivityView() {
        // 7일간 날짜 목록(일 ~ 토)
        val weekDates = (0L until 7L).map { startOfWeek.plusDays(it) }

        val activities = dataManager.getWeeklyData(selectedDevice.id, startOfWeek.toString(), startOfWeek.plusDays(6).toString())

        if(activities.isEmpty()) {
            binding.noData1.visibility = View.VISIBLE
            binding.chart4.visibility = View.GONE
        }else {
            binding.noData1.visibility = View.GONE
            binding.chart4.visibility = View.VISIBLE
        }

        // 데이터 맵(key: "yyyy-MM-dd")
        val activityMap = activities.associateBy { it.createdAt }

        // BarEntry 생성
        val entries = weekDates.mapIndexed { index, date ->
            val key = date.format(dataFormatter)
            val value = activityMap[key]?.activityRate?.toFloat() ?: 0f
            BarEntry(index.toFloat(), value)
        }

        val dataSet = BarDataSet(entries, "WeeklyChart").apply {
            color = Color.LTGRAY
            highLightAlpha = 255
            highLightColor = Color.BLUE
            valueTextSize = 12f
            valueTextColor = Color.BLACK
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value == 0f) "" else value.toInt().toString()
                }
            }
        }

        // 차트 설정
        binding.chart4.apply {
            data = BarData(dataSet).apply { barWidth = 0.5f }

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
                position = XAxis.XAxisPosition.BOTTOM
                axisLineWidth = 0.8f
                granularity = 1f
                labelCount = 7
                setDrawGridLines(false)
                textColor = Color.DKGRAY
                textSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        val index = value.toInt()
                        return if (index in 0..6) {
                            val date = weekDates[index]
                            val dayName = dayNames[date.dayOfWeek.value % 7] // 일~토
                            val dateLabel = if (dayName == "일") {
                                date.format(displayFormatter)
                            }else {
                                String.format("%02d", date.dayOfMonth)
                            }
                            "$dayName $dateLabel"
                        }else ""
                    }
                }
            }

            setTouchEnabled(true)
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    highlightValue(h)
                    invalidate()
                }

                override fun onNothingSelected() {
                    highlightValue(null)
                }
            })

            invalidate()
        }
    }

    private fun consecutiveTimeView() {
        binding.progressData1.text = "2시간 / 4시간"
        binding.progressData2.text = "5시간 / 7시간"
        binding.progressBar.progress = 2
        binding.progressBar.max = 4
        binding.progressBar2.progress = 5
        binding.progressBar2.max = 7

        binding.btnEdit.setOnClickListener {
        }
    }

    private fun getDailyData() {
        val formattedDate = getFormattedDate(selectedDate)
        dailyActivityData = dataManager.getDailyActivity(selectedDevice.id, formattedDate)
        dailyTemperatureData = dataManager.getDailyTemperature(selectedDevice.id, formattedDate)
        dailyLightData = dataManager.getDailyLight(selectedDevice.id, formattedDate)
    }

    private fun switchButtonStyle(onOff: Boolean, container: ConstraintLayout, image: ImageView, status: TextView, type: TextView) {
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