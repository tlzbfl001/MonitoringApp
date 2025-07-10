package com.aitronbiz.arron.view.home

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.aitronbiz.arron.adapter.SelectHomeDialogAdapter
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
import com.aitronbiz.arron.view.notification.NotificationFragment
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
    private var homeDialog: BottomSheetDialog? = null
    private var roomDialog: BottomSheetDialog? = null
    private var deviceDialog: BottomSheetDialog? = null
    private var editMenuDialog: BottomSheetDialog? = null
    private lateinit var menuAdapter: MenuAdapter
    private var rooms = ArrayList<Room>()
    private var room = Room()
    private var devices = ArrayList<Device>()
    private var homeId = 0
    private var deviceId = 0

    private val menuItems = mutableListOf(
        MenuItem("활동도", true),
        MenuItem("시간별 활동량", true),
        MenuItem("호흡수", true)
    )

    private var sections = mutableListOf(
        SectionItem.TodayActivity,
        SectionItem.DailyActivity
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
        setupHomeDialog()
        setupRoomDialog()
        setupDeviceDialog()
        setupMenuEditSheet()

        return binding.root
    }

    private fun initUI() {
        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        viewModel.updateSelectedDate(LocalDate.now())
        isFirstObserve = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
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

        binding.viewPager.post {
            binding.viewPager.setCurrentItem(currentPage, false)
        }

        binding.btnNotification.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, NotificationFragment())
        }

        binding.btnDrag.setOnClickListener {
            val dialog = CalendarPopupDialog.newInstance(deviceId)
            dialog.show(parentFragmentManager, "calendar_dialog")
        }

        binding.btnSelectHome.setOnClickListener {
            homeDialog!!.show()
        }

        binding.btnSelectRoom.setOnClickListener {
            if (homeId > 0) {
                roomDialog?.show()
            } else {
                Toast.makeText(requireActivity(), "등록된 홈이 없습니다", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSelectDevice.setOnClickListener {
            if (room.id > 0) {
                deviceDialog?.show()
            } else {
                Toast.makeText(requireActivity(), "등록된 룸이 없습니다", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEditMenu.setOnClickListener {
            menuAdapter.setMenuItems(menuItems.map { it.copy() })
            menuAdapter.notifyDataSetChanged()
            editMenuDialog?.show()
        }
    }

    private fun setupHomeDialog() {
        homeDialog = BottomSheetDialog(requireContext())
        val homeDialogView = layoutInflater.inflate(R.layout.dialog_select_home, null)
        val homeRecyclerView = homeDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddHome = homeDialogView.findViewById<ConstraintLayout>(R.id.btnAdd)

        val homes = dataManager.getHomes(AppController.prefs.getUID())
        homeDialog!!.setContentView(homeDialogView)

        val selectHomeDialogAdapter = SelectHomeDialogAdapter(homes) { selectedItem ->
            homeId = selectedItem.id
            binding.tvHomeName.text = "${selectedItem.name}"
            Handler(Looper.getMainLooper()).postDelayed({
                rooms = dataManager.getRooms(AppController.prefs.getUID(), homeId)

                if(rooms.isNotEmpty()) {
                    room = rooms[0]
                    binding.tvRoomName.text = room.name
                }else {
                    room = Room()
                    binding.tvRoomName.text = "룸"
                }

                blinkAnimation()
                loadDevices()

                homeDialog?.dismiss()
            }, 300)
        }

        homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        homeRecyclerView.adapter = selectHomeDialogAdapter

        if(homes.isNotEmpty()) {
            homeId = homes[0].id
            binding.tvHomeName.text = "${homes[0].name}"
        }else {
            binding.tvHomeName.text = "사용자 홈"
        }

        blinkAnimation()

        btnAddHome.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, HomeFragment())
            homeDialog?.dismiss()
        }
    }

    private fun setupRoomDialog() {
        roomDialog = BottomSheetDialog(requireContext())
        val roomDialogView = layoutInflater.inflate(R.layout.dialog_select_room, null)
        val roomRecyclerView = roomDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddRoom = roomDialogView.findViewById<ConstraintLayout>(R.id.btnAdd)
        rooms = dataManager.getRooms(AppController.prefs.getUID(), homeId)
        roomDialog!!.setContentView(roomDialogView)

        val selectRoomDialogAdapter = SelectRoomDialogAdapter(rooms) { selectedItem ->
            room = selectedItem
            binding.tvRoomName.text = selectedItem.name
            Handler(Looper.getMainLooper()).postDelayed({
                blinkAnimation()
                loadDevices()
                roomDialog?.dismiss()
            }, 300)
        }

        roomRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        roomRecyclerView.adapter = selectRoomDialogAdapter

        if(rooms.isNotEmpty()) {
            room = rooms[0]
            binding.tvRoomName.text = room.name
            blinkAnimation()
        }else {
            room = Room()
            binding.tvRoomName.text = "룸"
        }

        btnAddRoom.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("homeId", homeId)
            }
            replaceFragment2(requireActivity().supportFragmentManager, RoomFragment(), bundle)
            roomDialog?.dismiss()
        }
    }

    private fun setupDeviceDialog() {
        deviceDialog = BottomSheetDialog(requireContext())
        val deviceDialogView = layoutInflater.inflate(R.layout.dialog_select_device, null)
        val recyclerView = deviceDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddDevice = deviceDialogView.findViewById<ConstraintLayout>(R.id.btnAddDevice)
        deviceDialog!!.setContentView(deviceDialogView)
        loadDevices()

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
    }

    private fun loadDevices() {
        devices = dataManager.getDevices(homeId, room.id)

        if(devices.isNotEmpty()) {
            deviceId = devices[0].id
            binding.tvDeviceName.text = devices[0].name
        }else {
            deviceId = 0
            binding.tvDeviceName.text = "기기"
        }

        sectionAdapter.updateRoomAndDeviceId(room.id, deviceId)
        updateSectionList()
    }

    private fun blinkAnimation() {
        if (room.status == EnumData.NORMAL.name || room.status == null || room.status == "") {
            binding.signLabel.visibility = View.GONE
        } else {
            binding.signLabel.visibility = View.VISIBLE
            binding.tvSign.text = if (room.status == EnumData.CAUTION.name) "주의" else "경고"
            binding.signLabel.backgroundTintList = ColorStateList.valueOf(
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

    private fun setupMenuEditSheet() {
        editMenuDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_menu_edit, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.menuRecyclerView)
        val btnConfirm = view.findViewById<CardView>(R.id.btnConfirm)

        menuAdapter = MenuAdapter(menuItems.map { it.copy() }.toMutableList(), this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = menuAdapter

        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                menuAdapter.moveItem(from.adapterPosition, to.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = false
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        btnConfirm.setOnClickListener {
            // 데이터 갱신
            menuItems.clear()
            menuItems.addAll(menuAdapter.getMenuItems())
            editMenuDialog?.dismiss()
            updateSectionList()
        }

        editMenuDialog!!.setContentView(view)
    }

    private fun updateSectionList() {
        val newSections = mutableListOf<SectionItem>()

        for (menuItem in menuItems) {
            when (menuItem.title) {
                "활동도" -> newSections.add(SectionItem.TodayActivity)
                "시간별 활동량" -> newSections.add(SectionItem.DailyActivity)
                "호흡수" -> newSections.add(SectionItem.DailyMission)
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
