package com.aitronbiz.arron.view.home

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.R
import com.aitronbiz.arron.databinding.FragmentMainBinding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.adapter.DeviceDialogAdapter
import com.aitronbiz.arron.adapter.MenuAdapter
import com.aitronbiz.arron.adapter.SectionAdapter
import com.aitronbiz.arron.adapter.SubjectDialogAdapter
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.entity.MenuItem
import com.aitronbiz.arron.entity.SectionItem
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.util.OnStartDragListener
import com.aitronbiz.arron.view.device.AddDeviceFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFragment : Fragment(), OnStartDragListener {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var sectionAdapter: SectionAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var subjects = ArrayList<Subject>()
    private var subjectDialog: BottomSheetDialog? = null
    private var deviceDialog: BottomSheetDialog? = null
    private var devices = ArrayList<Device>()
    private var subject = Subject()
    private var deviceId = 0

    // 메뉴 항목
    private val menuItems = mutableListOf(
        MenuItem("활동도", true),
        MenuItem("기간별 활동도", true),
        MenuItem("연속 거주 시간", true),
        MenuItem("스마트 절전", true)
    )

    // 섹션 아이템들
    private var sections = mutableListOf<SectionItem>(
        SectionItem.TodayActivity,
        SectionItem.WeeklyActivity,
        SectionItem.ResidenceTime,
        SectionItem.SmartEnergy
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        dataManager = DataManager.getInstance(requireActivity())

        initUI()
        loadInitialData()
        setupSubjectDialog()
        updateSectionList()

        return binding.root
    }

    private fun initUI() {
        setStatusBar(requireActivity(), binding.mainLayout)

        sectionAdapter = SectionAdapter(requireContext(), subject.id, deviceId, sections, this)
        binding.sectionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.sectionRecyclerView.adapter = sectionAdapter

        // ItemTouchHelper 설정
        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(
                rv: RecyclerView,
                from: RecyclerView.ViewHolder,
                to: RecyclerView.ViewHolder
            ): Boolean {
                // 아이템 이동을 막음
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = false  // 일반 터치로 드래그 안 되게 설정
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.sectionRecyclerView)

        binding.btnSelectSubject.setOnClickListener {
            subjectDialog?.show()
        }

        binding.btnSelectDevice.setOnClickListener {
            if (subject.id != 0) {
                showDeviceDialog()
            } else {
                Toast.makeText(requireActivity(), "대상자를 먼저 등록해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEditMenu.setOnClickListener {
            showMenuEditSheet()
        }
    }

    private fun loadInitialData() {
        subjects = dataManager.getSubjects(AppController.prefs.getUID())

        if (subjects.isNotEmpty()) {
            subject = subjects[0]
            binding.tvSubjectName.text = subject.name
            blinkAnimation()
            loadDevicesForSubject()
        } else {
            subject = Subject()
            binding.tvSubjectName.text = "대상자"
        }
    }

    private fun loadDevicesForSubject() {
        devices = dataManager.getDevices(subject.id)

        if (devices.isNotEmpty()) {
            deviceId = devices[0].id
            binding.tvDeviceName.text = devices[0].name
        } else {
            deviceId = 0
            binding.tvDeviceName.text = "기기"
        }

        // deviceId를 갱신하고 UI를 업데이트
        sectionAdapter.updateSubjectAndDeviceId(subject.id, deviceId)
        updateSectionList()
    }

    private fun setupSubjectDialog() {
        subjectDialog = BottomSheetDialog(requireContext())
        val subjectDialogView = layoutInflater.inflate(R.layout.dialog_select_subject, null)
        val recyclerView = subjectDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddSubject = subjectDialogView.findViewById<ConstraintLayout>(R.id.btnAddSubject)

        subjectDialog!!.setContentView(subjectDialogView)

        val selectSubjectDialogAdapter = SubjectDialogAdapter(subjects) { selectedItem ->
            subject = selectedItem
            binding.tvSubjectName.text = selectedItem.name
            blinkAnimation()

            loadDevicesForSubject()

            Handler(Looper.getMainLooper()).postDelayed({
                subjectDialog?.dismiss()
            }, 300)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = selectSubjectDialogAdapter

        btnAddSubject.setOnClickListener {
            subjectDialog?.dismiss()
            replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
        }
    }

    private fun showDeviceDialog() {
        devices = dataManager.getDevices(subject.id)

        val deviceDialogView = layoutInflater.inflate(R.layout.dialog_select_device, null)
        val recyclerView = deviceDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddDevice = deviceDialogView.findViewById<ConstraintLayout>(R.id.btnAddDevice)

        deviceDialog = BottomSheetDialog(requireContext())
        deviceDialog!!.setContentView(deviceDialogView)

        val selectedIndex = devices.indexOfFirst { it.id == deviceId }.coerceAtLeast(0)

        val adapter = DeviceDialogAdapter(devices, onItemClick = { selectedItem ->
            deviceId = selectedItem.id
            binding.tvDeviceName.text = selectedItem.name
            sectionAdapter.updateSubjectAndDeviceId(subject.id, deviceId)
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
                putInt("subjectId", subject.id)
            }
            replaceFragment2(requireActivity().supportFragmentManager, AddDeviceFragment(), bundle)
            deviceDialog?.dismiss()
        }

        deviceDialog?.show()
    }

    private fun blinkAnimation() {
        if (subject.status == EnumData.NORMAL.name) {
            binding.signLabel.visibility = View.GONE
        } else {
            binding.signLabel.visibility = View.VISIBLE
            binding.signLabel.text = if (subject.status == EnumData.CAUTION.name) "주의" else "경고"
            binding.signLabel.setBackgroundColor(
                if (subject.status == EnumData.CAUTION.name) "#FFA500".toColorInt() else Color.RED
            )
            ObjectAnimator.ofFloat(binding.signLabel, "alpha", 0f, 1f).apply {
                duration = 1000
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
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
            updateSectionList() // 메뉴 변경된 순서 반영
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun updateSectionList() {
        val newSections = mutableListOf<SectionItem>()

        // 메뉴 순서에 맞게 SectionItem을 동적으로 갱신
        for (menuItem in menuItems) {
            when (menuItem.title) {
                "활동도" -> newSections.add(SectionItem.TodayActivity)
                "기간별 활동도" -> newSections.add(SectionItem.WeeklyActivity)
                "연속 거주 시간" -> newSections.add(SectionItem.ResidenceTime)
                "스마트 절전" -> newSections.add(SectionItem.SmartEnergy)
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

