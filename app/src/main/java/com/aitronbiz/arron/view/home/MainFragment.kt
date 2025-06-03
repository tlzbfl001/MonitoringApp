package com.aitronbiz.arron.view.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
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
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.getFormattedDate
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import org.w3c.dom.Text
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

        setStatusBar(requireActivity(), binding.mainLayout)

        dataManager = DataManager(requireActivity())
        dataManager.open()

        val today = LocalDate.now() // 오늘 날짜
        startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) // 이번 주 일요일

        binding.btnNotification.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, NotificationFragment())
        }

        binding.tvName.setOnClickListener {
        }

        binding.btnAdd.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
        }

        binding.btnSetting.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, SettingsFragment())
        }

        binding.btnAddSubject.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
        }

        binding.btnAddDevice.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
        }

        binding.toggleLabel.setOnClickListener {
            if(toggleActivity) {
                binding.activityView.visibility = View.VISIBLE
                binding.toggleLabel.text = "간략히 보기"
            }else {
                binding.activityView.visibility = View.GONE
                binding.toggleLabel.text = "자세히 보기"
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
            dailyView()
            detailActivityView()
            weeklyActivityView()
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
            switchButtonStyle(onOff1, binding.btnTelevision, binding.ivTelevision, binding.energyStatus1)
        }

        binding.btnAirConditioner.setOnClickListener {
            onOff2 = !onOff2
            switchButtonStyle(onOff2, binding.btnAirConditioner, binding.ivAirConditioner, binding.energyStatus2)
        }

        binding.btnLight.setOnClickListener {
            onOff3 = !onOff3
            switchButtonStyle(onOff3, binding.btnLight, binding.ivLight, binding.energyStatus3)
        }

        binding.btnMicrowave.setOnClickListener {
            onOff4 = !onOff4
            switchButtonStyle(onOff4, binding.btnMicrowave, binding.ivMicrowave, binding.energyStatus4)
        }

        viewModel.dailyActivityUpdated.observe(requireActivity(), androidx.lifecycle.Observer { signal ->
            if(signal) {
                getDailyData()
                dailyView()
                detailActivityView()
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
        dailyView() // 활동도 조회
        detailActivityView() // 일별 활동량, 온도, 조명 데이터 조회
        weeklyActivityView() // 주별 활동도 조회

        return binding.root
    }

    private fun getDailyData() {
        val formattedDate = getFormattedDate(selectedDate)
        dailyActivityData = dataManager.getDailyActivity(selectedDevice.id, formattedDate)
        dailyTemperatureData = dataManager.getDailyTemperature(selectedDevice.id, formattedDate)
        dailyLightData = dataManager.getDailyLight(selectedDevice.id, formattedDate)
    }

    private fun subjectListView() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerSubject.layoutManager = layoutManager

        val subjects = dataManager.getSubjects(AppController.prefs.getUserPrefs()).toMutableList()
        subjectAdapter = SubjectAdapter(subjects)
        binding.recyclerSubject.adapter = subjectAdapter

        if (subjects.isNotEmpty()) {
            binding.btnAddSubject.visibility = View.GONE
            binding.recyclerSubject.visibility = View.VISIBLE

            // 첫 번째 항목 자동 선택
            subjectId = subjects[0].id
            roomListView()

            subjectAdapter.setOnItemClickListener(object : SubjectAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    subjectAdapter.setSelectedPosition(position)
                    subjectId = subjects[position].id
                    roomListView()
                }
            })

            subjectAdapter.setOnAddClickListener {
                replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
            }

            // 리스트 오른쪽으로 이동
            binding.btnNextSubject.setOnClickListener {
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val next = (lastVisible + 3).coerceAtMost(subjectAdapter.itemCount - 1)
                binding.recyclerSubject.smoothScrollToPosition(next)
            }
        } else {
            binding.btnAddSubject.visibility = View.VISIBLE
            binding.recyclerSubject.visibility = View.GONE
        }
    }

    private fun roomListView() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerDevice.layoutManager = layoutManager

        val devices = dataManager.getDevices(subjectId)

        if (devices.isNotEmpty()) {
            binding.recyclerDevice.visibility = View.VISIBLE
            binding.btnAddDevice.visibility = View.GONE

            deviceAdapter = DeviceAdapter(devices,
                onAddClick = {
                    // 추가 버튼 클릭 시 동작
                    replaceFragment1(parentFragmentManager, AddDeviceFragment())
                }
            )

            binding.recyclerDevice.adapter = deviceAdapter

            selectedDevice = devices[0]
            getDailyData()
            dailyView()
            detailActivityView()
            weeklyActivityView()

            deviceAdapter.setOnItemClickListener(object : DeviceAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    deviceAdapter.setSelectedPosition(position)
                    selectedDevice = devices[position]
                    getDailyData()
                    dailyView()
                    detailActivityView()
                    weeklyActivityView()
                }
            })

            deviceAdapter.setOnAddClickListener {
                replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
            }

            // 리스트 오른쪽으로 이동
            binding.btnNextDevice.setOnClickListener {
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val next = (lastVisible + 3).coerceAtMost(deviceAdapter.itemCount - 1)
                binding.recyclerDevice.smoothScrollToPosition(next)
            }
        } else {
            binding.recyclerDevice.visibility = View.GONE
            binding.btnAddDevice.visibility = View.VISIBLE
        }
    }

    private fun dailyView() {
        val data = dataManager.getDailyActivity(selectedDevice.id, LocalDate.now().toString())
        if(data.isNotEmpty()) {
            binding.weeklyView.visibility = View.VISIBLE
            binding.noData2.visibility = View.GONE
            binding.residenceView.visibility = View.VISIBLE
            binding.noData1.visibility = View.GONE

            val pct = dataManager.getDailyData(selectedDevice.id, LocalDate.now().toString())
            binding.circularProgress.setProgressWithAnimation(pct.toFloat(), 2000)
            binding.progressLabel.text = "${pct}%"

            when(pct) {
                in 0..30 -> setTextStyle(binding.tvActiveSt1, binding.tvActiveSt2, binding.tvActiveSt3, 1)
                in 31..70 -> setTextStyle(binding.tvActiveSt1, binding.tvActiveSt3, binding.tvActiveSt2, 1)
                else -> setTextStyle(binding.tvActiveSt2, binding.tvActiveSt3, binding.tvActiveSt1, 1)
            }

            binding.progressData1.text = "2시간 / 4시간"
            binding.progressData2.text = "5시간 / 7시간"
            binding.progressBar.progress = 2
            binding.progressBar.max = 4
            binding.progressBar2.progress = 5
            binding.progressBar2.max = 7
        }else {
            binding.circularProgress.progress = 0f
            binding.progressLabel.text = "0%"
            binding.weeklyView.visibility = View.GONE
            binding.noData2.visibility = View.VISIBLE
            binding.residenceView.visibility = View.GONE
            binding.noData1.visibility = View.VISIBLE
            setTextStyle(binding.tvActiveSt2, binding.tvActiveSt3, binding.tvActiveSt1, 2)
        }

        if(selectedDevice.room == 1) {
            binding.tvActiveSt1.visibility = View.VISIBLE
            binding.tvActiveSt2.visibility = View.VISIBLE
            binding.tvActiveSt3.visibility = View.VISIBLE
            binding.tvActiveAbsent.visibility = View.GONE
        }else {
            binding.tvActiveSt1.visibility = View.GONE
            binding.tvActiveSt2.visibility = View.GONE
            binding.tvActiveSt3.visibility = View.GONE
            binding.tvActiveAbsent.visibility = View.VISIBLE
        }
    }

    private fun detailActivityView() {
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

        val activities = dataManager.getAllDailyData(selectedDevice.id, startOfWeek.toString(), startOfWeek.plusDays(6).toString())
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

    private fun setTextStyle(none1: TextView, none2: TextView, active: TextView, type: Int) {
        none1.setTextColor("#CCCCCC".toColorInt())
        none2.setTextColor("#CCCCCC".toColorInt())
        none1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
        none2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
        when(type) {
            1 -> {
                active.setTextColor(Color.BLACK)
                active.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
            }
            else -> {
                active.setTextColor("#CCCCCC".toColorInt())
                active.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
            }
        }
    }

    private fun switchButtonStyle(onOff: Boolean, container: ConstraintLayout, image: ImageView, status: TextView) {
        if(onOff) {
            container.setBackgroundResource(R.drawable.rec_12_gradient)
            image.imageTintList = ColorStateList.valueOf(Color.BLACK)
            status.text = "사용함"
            status.setTextColor(Color.BLACK)
        }else {
            container.setBackgroundResource(R.drawable.rec_12_border_gradient)
            image.imageTintList = ColorStateList.valueOf("#CCCCCC".toColorInt())
            status.text = "사용안함"
            status.setTextColor("#CCCCCC".toColorInt())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}