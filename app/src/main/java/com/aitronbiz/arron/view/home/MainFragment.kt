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
import java.text.SimpleDateFormat
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
import com.aitronbiz.arron.util.CustomUtil.selectedSubjectId

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
    private var toggleActivity = false
    private var onOff1 = false
    private var onOff2 = false
    private var onOff3 = false
    private var onOff4 = false
    private var weekOffset = 0

    // 날짜 포맷
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayDateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    private val dayNames = arrayOf("일", "월", "화", "수", "목", "금", "토")

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

        binding.btnToggleActivity.setOnClickListener {
            if(toggleActivity) {
                binding.toggleView.visibility = View.VISIBLE
                binding.tvToggleLabel.text = "간략히 보기"
                binding.btnToggleActivity.setImageResource(R.drawable.arrow_up)
            }else {
                binding.toggleView.visibility = View.GONE
                binding.tvToggleLabel.text = "자세히 보기"
                binding.btnToggleActivity.setImageResource(R.drawable.arrow_down)
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
            weekOffset--
            weeklyActivityView()
        }

        binding.btnNext.setOnClickListener {
            weekOffset++
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

        // LiveData 관찰
        viewModel.signal.observe(requireActivity(), androidx.lifecycle.Observer { signal ->
            if(signal) {
                getDailyData()
                activityView()
                detailDataView()
                weeklyActivityView()
            }
        })

        binding.btnAddTestData.setOnClickListener {
            if(selectedDevice.id > 0) {
                viewModel.sendSignal(selectedDate, selectedDevice.id)
            }else {
                Toast.makeText(requireActivity(), "등록된 기기가 없습니다.", Toast.LENGTH_SHORT).show()
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
            binding.recyclerView.visibility = View.VISIBLE
            subjectAdapter = SubjectAdapter(subjects)
            binding.recyclerView.adapter = subjectAdapter

            binding.tvSubjectCnt.text = "등록된 대상자 : ${subjects.size}명"
            selectedSubjectId = subjects[0].id
            roomListView(subjects[0].id)

            subjectAdapter.setOnItemClickListener(object : SubjectAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    subjectAdapter.setSelectedPosition(position)
                    selectedSubjectId = subjects[position].id
                    roomListView(subjects[position].id)
                }
            })
        }else {
            binding.recyclerView.visibility = View.GONE
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
            binding.tvDeviceCnt.text = "등록된 장소 : ${devices.size}개"
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

        val dataSet = BarDataSet(entries, "chart").apply {
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

    data class TestActivity(val value: Int, val date: String)

    // 예시 데이터
    val allActivities = listOf(
        TestActivity(11, "2025-05-26"), // 월요일
        TestActivity(23, "2025-05-28")  // 수요일
    )

    private fun weeklyActivityView() {
        val xAxisLabels = mutableListOf<String>()
        val displayLabels = mutableListOf<String>()

        val startCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            add(Calendar.WEEK_OF_YEAR, weekOffset)
        }
        val dateToIndex = mutableMapOf<String, Int>()

        for(i in 0 until 7) {
            val dateStr = dateFormat.format(startCal.time)
            xAxisLabels.add(dateStr)
            displayLabels.add(displayDateFormat.format(startCal.time))
            dateToIndex[dateStr] = i
            startCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Map으로 빠르게 조회 가능하게 변환
        val activityMap = allActivities.associateBy { it.date }

        // 모든 요일에 대해 BarEntry 생성
        val entries = (0 until 7).map { i ->
            val dateStr = xAxisLabels[i]
            val value = activityMap[dateStr]?.value?.toFloat() ?: 0f
            BarEntry(i.toFloat(), value)
        }

        val dataSet = BarDataSet(entries, "일간 활동도").apply {
            color = Color.LTGRAY
            highLightAlpha = 255
            highLightColor = Color.BLUE
            valueTextSize = 12f
            valueTextColor = Color.BLACK

            // 숫자 정수로만 표시
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value == 0f) "" else value.toInt().toString()
                }
            }
        }

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
                            val date = xAxisLabels[index]
                            val cal = Calendar.getInstance().apply {
                                time = dateFormat.parse(date)!!
                            }

                            val dayName = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]

                            val dateLabel = if(dayName == "일") {
                                displayDateFormat.format(cal.time)
                            }else {
                                cal.get(Calendar.DAY_OF_MONTH).toString()
                            }

                            "$dayName $dateLabel"
                        } else ""
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