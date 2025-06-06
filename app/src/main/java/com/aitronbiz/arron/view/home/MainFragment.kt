package com.aitronbiz.arron.view.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import com.aitronbiz.arron.R
import com.aitronbiz.arron.databinding.FragmentMainBinding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.adapter.DeviceAdapter
import com.aitronbiz.arron.adapter.MenuAdapter
import com.aitronbiz.arron.adapter.SectionAdapter
import com.aitronbiz.arron.adapter.SubjectAdapter
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.MenuItem
import com.aitronbiz.arron.entity.SectionItem
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.util.OnStartDragListener
import com.aitronbiz.arron.view.device.AddDeviceFragment
import com.aitronbiz.arron.view.device.DeviceFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFragment : Fragment(), OnStartDragListener {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var sectionAdapter: SectionAdapter
    private lateinit var dataManager: DataManager
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var subjectId = 0
    private var deviceId = 0

    private val menuItems = mutableListOf(
        MenuItem("활동도", true),
        MenuItem("주간 활동량", true),
        MenuItem("연속 거주 시간", true),
        MenuItem("스마트 절전", true)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        dataManager = DataManager.getInstance(requireContext())

        initUI()
        loadInitialData()

        return binding.root
    }

    private fun initUI() {
        setStatusBar(requireActivity(), binding.mainLayout)

        binding.recyclerSubject.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding.recyclerDevice.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding.sectionRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        sectionAdapter = SectionAdapter(requireContext(), subjectId, deviceId, mutableListOf())
        binding.sectionRecyclerView.adapter = sectionAdapter

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                rv: RecyclerView,
                from: RecyclerView.ViewHolder,
                to: RecyclerView.ViewHolder
            ): Boolean {
                sectionAdapter.moveItem(from.adapterPosition, to.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = true
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.sectionRecyclerView)

        binding.btnAddSubject.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
        }

        binding.btnAddDevice.setOnClickListener {
            if(subjectId != 0) {
                replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
            }else {
                Toast.makeText(requireActivity(), "대상자를 등록해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEditMenu.setOnClickListener {
            showMenuEditSheet()
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            val subjects = withContext(Dispatchers.IO) {
                dataManager.getSubjects(AppController.prefs.getUID())
            }

            if (subjects.isNotEmpty()) {
                subjectId = subjects[0].id
                setupSubjectList(subjects)

                val devices = withContext(Dispatchers.IO) {
                    dataManager.getDevices(subjectId)
                }

                if (devices.isNotEmpty()) {
                    deviceId = devices[0].id
                    setupDeviceList(devices)
                    updateSectionList()
                } else {
                    binding.recyclerDevice.visibility = View.GONE
                    binding.btnAddDevice.visibility = View.VISIBLE
                }
            } else {
                binding.recyclerSubject.visibility = View.GONE
                binding.btnAddSubject.visibility = View.VISIBLE
            }

            updateSectionList()
        }
    }

    private fun setupSubjectList(subjects: List<Subject>) {
        val adapter = SubjectAdapter(subjects.toMutableList())
        binding.recyclerSubject.adapter = adapter

        adapter.setOnItemClickListener { pos ->
            subjectId = subjects[pos].id
            adapter.setSelectedPosition(pos)
            loadDevices(subjectId)
        }

        adapter.setOnAddClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
        }

        binding.recyclerSubject.visibility = View.VISIBLE
        binding.btnAddSubject.visibility = View.GONE
        binding.tvSubject.text = "등록된 대상자 : ${subjects.size}"
    }

    private fun loadDevices(subjectId: Int) {
        lifecycleScope.launch {
            val devices = withContext(Dispatchers.IO) {
                dataManager.getDevices(subjectId)
            }

            if (devices.isNotEmpty()) {
                deviceId = devices[0].id
                setupDeviceList(devices)
                updateSectionList()
            } else {
                binding.recyclerDevice.visibility = View.GONE
                binding.btnAddDevice.visibility = View.VISIBLE
            }
        }
    }

    private fun setupDeviceList(devices: List<Device>) {
        val adapter = DeviceAdapter(devices)
        binding.recyclerDevice.adapter = adapter

        adapter.setOnItemClickListener { pos ->
            deviceId = devices[pos].id
            adapter.setSelectedPosition(pos)
            updateSectionList()
        }

        adapter.setOnAddClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
        }

        binding.recyclerDevice.visibility = View.VISIBLE
        binding.btnAddDevice.visibility = View.GONE
        binding.tvDevice.text = "등록된 기기 : ${devices.size}"
    }

    private fun updateSectionList() {
        val newSections = menuItems.filter { it.visible }.mapNotNull {
            when (it.title) {
                "활동도" -> SectionItem.TodayActivity(subjectId, deviceId)
                "주간 활동량" -> SectionItem.WeeklyActivity
                "연속 거주 시간" -> SectionItem.ResidenceTime
                "스마트 절전" -> SectionItem.SmartEnergy
                else -> null
            }
        }
        sectionAdapter.updateSections(newSections)
    }

    private fun showMenuEditSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_menu_edit, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.menuRecyclerView)
        val btnConfirm = view.findViewById<CardView>(R.id.btnConfirm)

        val adapter = MenuAdapter(menuItems.map { it.copy() }.toMutableList(), this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                rv: RecyclerView,
                from: RecyclerView.ViewHolder,
                to: RecyclerView.ViewHolder
            ): Boolean {
                adapter.moveItem(from.adapterPosition, to.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = false
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        btnConfirm.setOnClickListener {
            menuItems.clear()
            menuItems.addAll(adapter.getMenuItems())
            dialog.dismiss()
            updateSectionList()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/*class TestFragment : Fragment(), OnStartDragListener {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var dataManager: DataManager
    private lateinit var subjectAdapter: SubjectAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var dailyActivityData = ArrayList<Activity>()
    private var dailyTemperatureData = ArrayList<Temperature>()
    private var dailyLightData = ArrayList<Light>()
    private var selectedDate = CalendarDay.today()
    private var selectedDevice = Device()
    private lateinit var startOfWeek: LocalDate
    private var subjectId = 0
    private var onOff1 = false
    private var onOff2 = false
    private var onOff3 = false
    private var onOff4 = false

    // 날짜 포맷
    private val dataFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayFormatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd")
    private val dayNames = arrayOf("일", "월", "화", "수", "목", "금", "토")

    // 메뉴 항목 (초기값)
    private val menuItems = mutableListOf(
        MenuItem("재실", true),
        MenuItem("활동도", true),
        MenuItem("일간 활동량", false),
        MenuItem("연속 거주 시간", true),
        MenuItem("스마트 절전", false)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(layoutInflater)

        setStatusBar(requireActivity(), binding.mainLayout)

        // 싱글톤 DataManager 인스턴스 얻기
        dataManager = DataManager.getInstance(requireContext())

        setupUI()
        subjectListView() // 등록 대상자 조회
        todayView() // 활동도 조회
        weeklyActivityView() // 주별 활동도 조회

        return binding.root
    }

    private fun setupUI() {
        val today = LocalDate.now() // 오늘 날짜
        startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) // 이번 주 일요일

        binding.btnNotification.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, NotificationFragment())
        }

        binding.btnActivity.setOnClickListener {
            val bundle = Bundle()
            bundle.putInt("subjectId", subjectId)
            replaceFragment2(requireActivity().supportFragmentManager, ActivityFragment(), bundle)
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
            switchButtonStyle(
                onOff1,
                binding.btnTelevision,
                binding.ivTelevision,
                binding.energyType1,
                binding.energyStatus1
            )
        }

        binding.btnAirConditioner.setOnClickListener {
            onOff2 = !onOff2
            switchButtonStyle(
                onOff2,
                binding.btnAirConditioner,
                binding.ivAirConditioner,
                binding.energyType2,
                binding.energyStatus2
            )
        }

        binding.btnLight.setOnClickListener {
            onOff3 = !onOff3
            switchButtonStyle(
                onOff3,
                binding.btnLight,
                binding.ivLight,
                binding.energyType3,
                binding.energyStatus3
            )
        }

        binding.btnMicrowave.setOnClickListener {
            onOff4 = !onOff4
            switchButtonStyle(
                onOff4,
                binding.btnMicrowave,
                binding.ivMicrowave,
                binding.energyType4,
                binding.energyStatus4
            )
        }

        binding.btnEditMenu.setOnClickListener {
            showMenuEditSheet()
        }

        viewModel.dailyActivityUpdated.observe(
            requireActivity(),
            androidx.lifecycle.Observer { signal ->
                if (signal) {
                    getTodayData()
                    todayView()
                    weeklyActivityView()
                }
            })
    }

    private fun getTodayData() {
        val formattedDate = getFormattedDate(selectedDate)
        lifecycleScope.launch(Dispatchers.IO) {
            dailyActivityData = dataManager.getDailyActivity(selectedDevice.id, formattedDate)
            dailyTemperatureData = dataManager.getDailyTemperature(selectedDevice.id, formattedDate)
            dailyLightData = dataManager.getDailyLight(selectedDevice.id, formattedDate)
            withContext(Dispatchers.Main) {
                todayView()
                weeklyActivityView()
            }
        }
    }

    private fun subjectListView() {
        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerSubject.layoutManager = layoutManager

        lifecycleScope.launch(Dispatchers.IO) {
            val subjects = dataManager.getSubjects(AppController.prefs.getUID())
            withContext(Dispatchers.Main) {
                subjectAdapter = SubjectAdapter(subjects)
                binding.recyclerSubject.adapter = subjectAdapter

                if (subjects.isNotEmpty()) {
                    binding.btnAddSubject.visibility = View.GONE
                    binding.recyclerSubject.visibility = View.VISIBLE
                    binding.tvSubject.text = "등록된 대상자 : ${subjects.size}"

                    // 첫 번째 subjectId 자동 선택
                    subjectId = subjects[0].id

                    subjectAdapter.setOnItemClickListener(object :
                        SubjectAdapter.OnItemClickListener {
                        override fun onItemClick(position: Int) {
                            subjectAdapter.setSelectedPosition(position)
                            subjectId = subjects[position].id
                        }
                    })

                    subjectAdapter.setOnAddClickListener {
                        replaceFragment1(
                            requireActivity().supportFragmentManager,
                            AddSubjectFragment()
                        )
                    }
                } else {
                    binding.btnAddSubject.visibility = View.VISIBLE
                    binding.recyclerSubject.visibility = View.GONE
                    binding.tvSubject.text = "등록된 대상자 : 0"
                }
            }
        }
    }

    private fun todayView() {
        val data = dataManager.getDailyActivity(selectedDevice.id, LocalDate.now().toString())
        if (data.isNotEmpty()) {
            binding.weeklyView.visibility = View.VISIBLE
            binding.noData2.visibility = View.GONE
            binding.residenceView.visibility = View.VISIBLE
            binding.noData1.visibility = View.GONE

            val pct = dataManager.getDailyData(selectedDevice.id, LocalDate.now().toString())
            binding.circularProgress.setProgressWithAnimation(pct.toFloat(), 2000)
            binding.progressLabel.text = "${pct}%"

            when (pct) {
                in 0..30 -> setTextStyle(
                    binding.tvActiveSt1,
                    binding.tvActiveSt2,
                    binding.tvActiveSt3,
                    1
                )

                in 31..70 -> setTextStyle(
                    binding.tvActiveSt1,
                    binding.tvActiveSt3,
                    binding.tvActiveSt2,
                    1
                )

                else -> setTextStyle(
                    binding.tvActiveSt2,
                    binding.tvActiveSt3,
                    binding.tvActiveSt1,
                    1
                )
            }

            binding.progressData1.text = "2시간 / 4시간"
            binding.progressData2.text = "5시간 / 7시간"
            binding.progressBar.progress = 2
            binding.progressBar.max = 4
            binding.progressBar2.progress = 5
            binding.progressBar2.max = 7
        } else {
            binding.circularProgress.progress = 0f
            binding.progressLabel.text = "0%"
            binding.weeklyView.visibility = View.GONE
            binding.noData2.visibility = View.VISIBLE
            binding.residenceView.visibility = View.GONE
            binding.noData1.visibility = View.VISIBLE
            setTextStyle(binding.tvActiveSt2, binding.tvActiveSt3, binding.tvActiveSt1, 2)
        }

        if (selectedDevice.room == 1) {
            binding.tvActiveSt1.visibility = View.VISIBLE
            binding.tvActiveSt2.visibility = View.VISIBLE
            binding.tvActiveSt3.visibility = View.VISIBLE
            binding.tvActiveAbsent.visibility = View.GONE
        } else {
            binding.tvActiveSt1.visibility = View.GONE
            binding.tvActiveSt2.visibility = View.GONE
            binding.tvActiveSt3.visibility = View.GONE
            binding.tvActiveAbsent.visibility = View.VISIBLE
        }
    }

    private fun weeklyActivityView() {
        // 7일간 날짜 목록(일 ~ 토)
        val weekDates = (0L until 7L).map { startOfWeek.plusDays(it) }

        val activities = dataManager.getAllDailyData(
            selectedDevice.id,
            startOfWeek.toString(),
            startOfWeek.plusDays(6).toString()
        )
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
                            val dayName = dayNames[date.dayOfWeek.value % 7]
                            val dateLabel = if (dayName == "일") {
                                date.format(displayFormatter)
                            } else {
                                String.format("%02d", date.dayOfMonth)
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

    private fun showMenuEditSheet() {
        val ctx = context ?: return
        val dialog = BottomSheetDialog(ctx)
        val view = layoutInflater.inflate(R.layout.dialog_menu_edit, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.menuRecyclerView)
        val btnConfirm = view.findViewById<CardView>(R.id.btnConfirm)

        val adapter = MenuAdapter(menuItems, { index, isVisible ->
            menuItems[index].visible = isVisible
        }, this)

        recyclerView.layoutManager = LinearLayoutManager(ctx)
        recyclerView.adapter = adapter

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                rv: RecyclerView,
                from: RecyclerView.ViewHolder,
                to: RecyclerView.ViewHolder
            ): Boolean {
                adapter.moveItem(from.adapterPosition, to.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = false
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        btnConfirm.setOnClickListener {
            val reorderedItems = adapter.getMenuItems()
            menuItems.clear()
            menuItems.addAll(reorderedItems)
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun setTextStyle(none1: TextView, none2: TextView, active: TextView, type: Int) {
        none1.setTextColor("#CCCCCC".toColorInt())
        none2.setTextColor("#CCCCCC".toColorInt())
        none1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
        none2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
        when (type) {
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

    private fun switchButtonStyle(
        onOff: Boolean,
        container: ConstraintLayout,
        image: ImageView,
        title: TextView,
        status: TextView
    ) {
        if (onOff) {
            container.setBackgroundResource(R.drawable.rec_12_gradient)
            image.imageTintList = ColorStateList.valueOf(Color.WHITE)
            title.setTextColor(Color.WHITE)
            status.setTextColor(Color.WHITE)
            status.text = "사용함"
        } else {
            container.setBackgroundResource(R.drawable.rec_12_border_gradient)
            image.imageTintList = ColorStateList.valueOf(Color.BLACK)
            title.setTextColor(Color.BLACK)
            status.setTextColor(Color.BLACK)
            status.text = "사용안함"
        }
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}*/
