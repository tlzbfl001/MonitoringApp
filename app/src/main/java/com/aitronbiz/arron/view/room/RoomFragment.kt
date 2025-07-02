package com.aitronbiz.arron.view.room

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.DeviceListAdapter
import com.aitronbiz.arron.adapter.RoomListAdapter
import com.aitronbiz.arron.adapter.SelectHomeDialogAdapter
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentRoomBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.home.AddHomeFragment
import com.aitronbiz.arron.view.home.MainFragment
import com.google.android.material.bottomsheet.BottomSheetDialog

class RoomFragment : Fragment() {
    private var _binding: FragmentRoomBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var adapter: RoomListAdapter
    private var homeDialog : BottomSheetDialog? = null
    private var homeId = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        dataManager = DataManager.getInstance(requireActivity())

        adapter = RoomListAdapter(mutableListOf())

        // 다이얼로그 초기 설정
        homeDialog = BottomSheetDialog(requireContext())
        val homeDialogView = layoutInflater.inflate(R.layout.dialog_select_room, null)
        val homeRecyclerView = homeDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val tvTitle = homeDialogView.findViewById<TextView>(R.id.tvTitle)
        val tvBtnName = homeDialogView.findViewById<TextView>(R.id.tvBtnName)
        val btnAddHome = homeDialogView.findViewById<ConstraintLayout>(R.id.btnAdd)
        tvTitle.text = "홈 선택"
        tvBtnName.text = "홈 추가"

        // 다이얼로그 데이터 설정
        val homes = dataManager.getHomes(AppController.prefs.getUID())
        if(homes.isNotEmpty()) homeId = homes[0].id
        homeDialog!!.setContentView(homeDialogView)

        val selectHomeDialogAdapter = SelectHomeDialogAdapter(homes) { selectedItem ->
            homeId = selectedItem.id
            binding.tvHome.text = "홈 : ${selectedItem.name}"
            Handler(Looper.getMainLooper()).postDelayed({
                val devices = dataManager.getRooms(AppController.prefs.getUID(), homeId) // Room 객체 리스트 반환
                adapter.updateData(devices)
                homeDialog?.dismiss()
            }, 300)
        }

        homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        homeRecyclerView.adapter = selectHomeDialogAdapter

        if(homeId > 0) {
            val devices = dataManager.getRooms(AppController.prefs.getUID(), homeId)
            adapter.updateData(devices)
            binding.tvHome.text = "홈 : ${homes[0].name}"
        }else {
            binding.tvHome.text = "홈 :   "
        }

        btnAddHome.setOnClickListener {
            homeDialog?.dismiss()
            replaceFragment1(requireActivity().supportFragmentManager, AddHomeFragment())
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.adapter = adapter

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.btnAdd.setOnClickListener {
            if(homeId > 0) {
                val args = Bundle().apply {
                    putInt("homeId", homeId)
                }
                replaceFragment2(requireActivity().supportFragmentManager, AddRoomFragment(), args)
            }else {
                Toast.makeText(requireActivity(), "등록된 홈이 없습니다", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnHome.setOnClickListener {
            homeDialog!!.show()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}