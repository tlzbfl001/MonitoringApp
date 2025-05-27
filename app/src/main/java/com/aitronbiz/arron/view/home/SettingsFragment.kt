package com.aitronbiz.arron.view.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.MenuAdapter
import com.aitronbiz.arron.adapter.OnStartDragListener
import com.aitronbiz.arron.databinding.FragmentSettingsBinding
import com.aitronbiz.arron.entity.MenuItem
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.google.android.material.bottomsheet.BottomSheetDialog

class SettingsFragment : Fragment(), OnStartDragListener {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var itemTouchHelper: ItemTouchHelper

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

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.btnDevice.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
        }

        binding.btnSettingMonitoringAlarm.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.btnSettingCycle.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.btnAppInfo.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.btnEditMenu.setOnClickListener {
            showMenuEditSheet()
        }

        binding.btnLogout.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
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
            menuItems[index].visible = isVisible
        }, this)

        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.adapter = adapter

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                val fromPos = from.adapterPosition
                val toPos = to.adapterPosition
                adapter.moveItem(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = false // 드래그 핸들로만!
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        btnConfirm.setOnClickListener {
            val reorderedItems = adapter.getMenuItems()
            // TODO: 저장 처리, menuProvider.updateOrder() 등
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }
}