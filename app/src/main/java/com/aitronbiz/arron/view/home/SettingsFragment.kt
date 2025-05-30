package com.aitronbiz.arron.view.home

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.MenuAdapter
import com.aitronbiz.arron.adapter.OnStartDragListener
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentSettingsBinding
import com.aitronbiz.arron.entity.MenuItem
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.google.android.material.bottomsheet.BottomSheetDialog

class SettingsFragment : Fragment(), OnStartDragListener {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var dataManager: DataManager
    private var dialog : Dialog? = null

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
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        dataManager = DataManager(requireActivity())
        dataManager.open()

        dialog = Dialog(requireActivity())
        dialog!!.setContentView(R.layout.dialog_warning)
        dialog!!.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val btnConfirm = dialog!!.findViewById<CardView>(R.id.btnConfirm)

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.btnDevice.setOnClickListener {
            val subjects = dataManager.getSubjects(AppController.prefs.getUserPrefs())

            if(subjects.isNotEmpty()) {
                replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
            }else {
                dialog!!.show()
                btnConfirm.setOnClickListener {
                    replaceFragment1(requireActivity().supportFragmentManager, AddSubjectFragment())
                    dialog!!.dismiss()
                }
            }
        }

        binding.btnSettingMonitoringAlarm.setOnClickListener {

        }

        binding.btnSettingCycle.setOnClickListener {

        }

        binding.btnAppInfo.setOnClickListener {

        }

        binding.btnEditMenu.setOnClickListener {
            showMenuEditSheet()
        }

        binding.btnLogout.setOnClickListener {

        }

        return binding.root
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    private fun showMenuEditSheet() {
        val dialog = BottomSheetDialog(requireActivity())

        val view = layoutInflater.inflate(R.layout.dialog_menu_edit, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.menuRecyclerView)
        val btnConfirm = view.findViewById<CardView>(R.id.btnConfirm)

        val adapter = MenuAdapter(menuItems, { index, isVisible ->
            // 메뉴 항목의 visible 상태 변경
            menuItems[index].visible = isVisible
        }, this)

        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.adapter = adapter

        // 드래그를 이용한 아이템 순서 변경을 위한 ItemTouchHelper 콜백 정의
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, // 상하 드래그만 허용
            0 // 스와이프 비활성화
        ) {
            override fun onMove(
                rv: RecyclerView,
                from: RecyclerView.ViewHolder,
                to: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = from.adapterPosition
                val toPos = to.adapterPosition
                adapter.moveItem(fromPos, toPos) // 어댑터 내부 아이템 순서 변경
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled() = false
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // 확인 버튼 클릭 시 동작 정의
        btnConfirm.setOnClickListener {
            val reorderedItems = adapter.getMenuItems() // 변경된 순서 반영
            dialog.dismiss()
        }

        // BottomSheet에 뷰 설정 후 표시
        dialog.setContentView(view)
        dialog.show()
    }
}