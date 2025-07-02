package com.aitronbiz.arron.view.home

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.MainViewModel
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.DeviceDialogAdapter
import com.aitronbiz.arron.adapter.MenuAdapter
import com.aitronbiz.arron.adapter.SectionAdapter
import com.aitronbiz.arron.adapter.SelectRoomDialogAdapter
import com.aitronbiz.arron.adapter.WeekAdapter
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentMainBinding
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.entity.MenuItem
import com.aitronbiz.arron.entity.SectionItem
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.util.OnStartDragListener
import com.aitronbiz.arron.view.device.DeviceFragment
import com.aitronbiz.arron.view.room.RoomFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MainFragment : Fragment(), OnStartDragListener {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var viewModel: MainViewModel
    private lateinit var sectionAdapter: SectionAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var rooms = ArrayList<Room>()
    private var subjectDialog: BottomSheetDialog? = null
    private var deviceDialog: BottomSheetDialog? = null
    private var devices = ArrayList<Device>()
    private var room = Room()
    private var homeId = 1
    private var deviceId = 0

    private val menuItems = mutableListOf(
        MenuItem("활동도", true),
        MenuItem("시간별 활동량", true),
        MenuItem("연속 거주 시간", true)
    )

    private var sections = mutableListOf(
        SectionItem.TodayActivity,
        SectionItem.DailyActivity,
        SectionItem.ResidenceTime
    )

    private val today = LocalDate.now()
    private var selectedDate = today
    private val baseWeekStart = today.with(DayOfWeek.SUNDAY)
    private val basePageIndex = 1000
    private val currentWeekStart = today.with(DayOfWeek.SUNDAY)
    private val weekOffset = baseWeekStart.until(currentWeekStart).days / 7
    private val currentPage = basePageIndex + weekOffset
    private var isFirstObserve = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        initUI()
        loadInitialData()
        setupSubjectDialog()
        updateSectionList()

        return binding.root
    }

    private fun initUI() {
        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            selectedDate = date
            sectionAdapter.updateSelectedDate(date)

            val weekAdapter = binding.viewPager.adapter as? WeekAdapter
            weekAdapter?.updateSelectedDate(date)

            if (isFirstObserve) {
                isFirstObserve = false
                return@observe
            }

            val sunday = date.with(DayOfWeek.SUNDAY)
            val weekOffset = ChronoUnit.WEEKS.between(baseWeekStart, sunday).toInt()
            val targetPage = basePageIndex + weekOffset
            binding.viewPager.setCurrentItem(targetPage, true)
        }

        sectionAdapter = SectionAdapter(requireContext(), room.id, deviceId, selectedDate, sections)
        binding.sectionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.sectionRecyclerView.adapter = sectionAdapter

        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = false
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.sectionRecyclerView)

        binding.btnSelectSubject.setOnClickListener { subjectDialog?.show() }

        binding.btnSelectDevice.setOnClickListener {
            if (room.id != 0) {
                showDeviceDialog()
            } else {
                Toast.makeText(requireActivity(), "등록된 홈이 없습니다", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEditMenu.setOnClickListener { showMenuEditSheet() }
    }

    private fun loadInitialData() {
        viewModel.updateSelectedDate(LocalDate.now())
        isFirstObserve = true
        rooms = dataManager.getRooms(AppController.prefs.getUID(), homeId)

        if (rooms.isNotEmpty()) {
            room = rooms[0]
            binding.tvSubjectName.text = room.name
            blinkAnimation()
            loadDevicesForSubject()
        } else {
            room = Room()
            binding.tvSubjectName.text = "룸"
        }

        binding.viewPager.adapter = WeekAdapter(
            requireContext(),
            deviceId = deviceId,
            baseDate = baseWeekStart,
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                viewModel.updateSelectedDate(date)
                sectionAdapter.updateSelectedDate(date)
            }
        )

        binding.viewPager.post {
            binding.viewPager.setCurrentItem(currentPage, false)
        }

        binding.btnDrag.setOnClickListener {
            val dialog = CalendarPopupDialog.newInstance(deviceId)
            dialog.show(parentFragmentManager, "calendar_dialog")
        }
    }

    private fun loadDevicesForSubject() {
        devices = dataManager.getDevices(homeId, room.id)

        if (devices.isNotEmpty()) {
            deviceId = devices[0].id
            binding.tvDeviceName.text = devices[0].name
        } else {
            deviceId = 0
            binding.tvDeviceName.text = "기기"
        }

        sectionAdapter.updateRoomAndDeviceId(room.id, deviceId)
        updateSectionList()
    }

    private fun setupSubjectDialog() {
        subjectDialog = BottomSheetDialog(requireContext())
        val subjectDialogView = layoutInflater.inflate(R.layout.dialog_select_room, null)
        val recyclerView = subjectDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddSubject = subjectDialogView.findViewById<ConstraintLayout>(R.id.btnAdd)

        subjectDialog!!.setContentView(subjectDialogView)

        val selectRoomDialogAdapter = SelectRoomDialogAdapter(rooms) { selectedItem ->
            room = selectedItem
            binding.tvSubjectName.text = selectedItem.name
            blinkAnimation()
            loadDevicesForSubject()
            Handler(Looper.getMainLooper()).postDelayed({
                subjectDialog?.dismiss()
            }, 300)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = selectRoomDialogAdapter

        btnAddSubject.setOnClickListener {
            subjectDialog?.dismiss()
            replaceFragment1(requireActivity().supportFragmentManager, RoomFragment())
        }
    }

    private fun showDeviceDialog() {
        devices = dataManager.getDevices(homeId, room.id)
        val deviceDialogView = layoutInflater.inflate(R.layout.dialog_select_device, null)
        val recyclerView = deviceDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddDevice = deviceDialogView.findViewById<ConstraintLayout>(R.id.btnAddDevice)

        deviceDialog = BottomSheetDialog(requireContext())
        deviceDialog!!.setContentView(deviceDialogView)

        val selectedIndex = devices.indexOfFirst { it.id == deviceId }.coerceAtLeast(0)

        val adapter = DeviceDialogAdapter(devices, onItemClick = { selectedItem ->
            deviceId = selectedItem.id
            binding.tvDeviceName.text = selectedItem.name
            sectionAdapter.updateRoomAndDeviceId(room.id, deviceId)
            updateSectionList()
            Handler(Looper.getMainLooper()).postDelayed({
                deviceDialog?.dismiss()
            }, 300)
        }, selectedPosition = selectedIndex)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.scrollToPosition(selectedIndex)

        btnAddDevice.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("roomId", room.id)
            }
            replaceFragment2(requireActivity().supportFragmentManager, DeviceFragment(), bundle)
            deviceDialog?.dismiss()
        }

        deviceDialog?.show()
    }

    private fun blinkAnimation() {
        if (room.status == EnumData.NORMAL.name) {
            binding.signLabel.visibility = View.GONE
        } else {
            binding.signLabel.visibility = View.VISIBLE
            binding.signLabel.text = if (room.status == EnumData.CAUTION.name) "주의" else "경고"
            binding.signLabel.setBackgroundColor(
                if (room.status == EnumData.CAUTION.name) "#FFD700".toColorInt() else Color.RED
            )
            ObjectAnimator.ofFloat(binding.signLabel, "alpha", 0f, 1f).apply {
                duration = 1000
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                start()
            }
        }
    }

    private fun showMenuEditSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_menu_edit, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.menuRecyclerView)
        val btnConfirm = view.findViewById<CardView>(R.id.btnConfirm)

        val adapter = MenuAdapter(menuItems.map { it.copy() }.toMutableList(), this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
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

    private fun updateSectionList() {
        val newSections = mutableListOf<SectionItem>()

        for (menuItem in menuItems) {
            when (menuItem.title) {
                "활동도" -> newSections.add(SectionItem.TodayActivity)
                "시간별 활동량" -> newSections.add(SectionItem.DailyActivity)
                "연속 거주 시간" -> newSections.add(SectionItem.ResidenceTime)
            }
        }

        sectionAdapter.updateSections(newSections)
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
