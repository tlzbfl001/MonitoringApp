package com.aitronbiz.arron.view.device

import android.app.Dialog
import android.graphics.Color
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
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.DeviceListAdapter
import com.aitronbiz.arron.adapter.SelectHomeDialogAdapter
import com.aitronbiz.arron.adapter.SelectRoomDialogAdapter
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentDeviceBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.home.AddHomeFragment
import com.aitronbiz.arron.view.home.MainFragment
import com.aitronbiz.arron.view.room.AddRoomFragment
import com.aitronbiz.arron.view.room.RoomFragment

class DeviceFragment : Fragment() {
    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var adapter: DeviceListAdapter
    private var warningDialog : Dialog? = null
    private var optionalDialog : BottomSheetDialog? = null
    private var homeDialog : BottomSheetDialog? = null
    private var roomDialog : BottomSheetDialog? = null
    private var homeId = 0
    private var roomId = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceBinding.inflate(inflater, container, false)

        setupUI()
        setupDialog()

        return binding.root
    }

    private fun setupUI() {
        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireContext())

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.btnAdd.setOnClickListener {
            onAddDeviceClick()
        }
    }

    private fun setupDialog() {
        // 다이얼로그 초기 설정
        homeDialog = BottomSheetDialog(requireContext())
        val homeDialogView = layoutInflater.inflate(R.layout.dialog_select_room, null)
        val homeRecyclerView = homeDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val tvTitle = homeDialogView.findViewById<TextView>(R.id.tvTitle)
        val tvBtnName = homeDialogView.findViewById<TextView>(R.id.tvBtnName)
        val btnAddHome = homeDialogView.findViewById<ConstraintLayout>(R.id.btnAdd)
        tvTitle.text = "홈 선택"
        tvBtnName.text = "홈 추가"

        roomDialog = BottomSheetDialog(requireContext())
        val roomDialogView = layoutInflater.inflate(R.layout.dialog_select_room, null)
        val roomRecyclerView = roomDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddRoom = roomDialogView.findViewById<ConstraintLayout>(R.id.btnAdd)

        warningDialog = Dialog(requireActivity())
        warningDialog!!.setContentView(R.layout.dialog_warning)
        warningDialog!!.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val btnConfirm = warningDialog!!.findViewById<CardView>(R.id.btnConfirm)

        optionalDialog = BottomSheetDialog(requireContext())
        val optionalDialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
        optionalDialog!!.setContentView(optionalDialogView)

        // 다이얼로그 데이터 설정
        val homes = dataManager.getHomes(AppController.prefs.getUID())
        if(homes.isNotEmpty()) homeId = homes[0].id
        homeDialog!!.setContentView(homeDialogView)

        val rooms = dataManager.getRooms(AppController.prefs.getUID(), homeId)
        if(rooms.isNotEmpty()) roomId = rooms[0].id
        roomDialog!!.setContentView(roomDialogView)

        adapter = DeviceListAdapter(
            mutableListOf(),
            onItemClick = { device ->
                val bundle = Bundle().apply {
                    putParcelable("device", device)
                }
                replaceFragment2(requireActivity().supportFragmentManager, DeviceSettingFragment(), bundle)
            }
        )

        val selectHomeDialogAdapter = SelectHomeDialogAdapter(homes) { selectedItem ->
            homeId = selectedItem.id
            binding.tvHome.text = "홈 :  ${selectedItem.name}"
            Handler(Looper.getMainLooper()).postDelayed({
                val devices = dataManager.getDevices(homeId, roomId) // Device 객체 리스트 반환
                Log.d(TAG, "devices: $devices")
                adapter.updateData(devices)
                homeDialog?.dismiss()
            }, 300)
        }

        homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        homeRecyclerView.adapter = selectHomeDialogAdapter

        if(homeId > 0) {
            binding.tvHome.text = "홈 :  ${homes[0].name}"
        }else {
            binding.tvHome.text = "홈 :  "
        }

        btnAddHome.setOnClickListener {
            homeDialog?.dismiss()
            replaceFragment1(requireActivity().supportFragmentManager, AddHomeFragment())
        }

        val selectRoomDialogAdapter = SelectRoomDialogAdapter(rooms) { selectedItem ->
            roomId = selectedItem.id
            binding.tvRoom.text = "룸 :  ${selectedItem.name}"
            Log.d(TAG, "roomId1: $roomId")
            Handler(Looper.getMainLooper()).postDelayed({
                val devices = dataManager.getDevices(homeId, roomId) // Device 객체 리스트 반환
                adapter.updateData(devices)
                roomDialog?.dismiss()
            }, 300)
        }

        roomRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        roomRecyclerView.adapter = selectRoomDialogAdapter

        if(roomId > 0) {
            val devices = dataManager.getDevices(homeId, roomId)
            adapter.updateData(devices)
            binding.tvRoom.text = "룸 :  ${rooms[0].name}"
        }else {
            binding.tvRoom.text = "룸 :  "
        }

        btnAddRoom.setOnClickListener {
            roomDialog?.dismiss()
            replaceFragment1(requireActivity().supportFragmentManager, AddRoomFragment())
        }

        btnConfirm.setOnClickListener {
            warningDialog?.dismiss()
            replaceFragment1(requireActivity().supportFragmentManager, RoomFragment())
        }

        binding.recyclerDevices.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerDevices.adapter = adapter

        binding.btnHome.setOnClickListener {
            homeDialog!!.show()
        }

        binding.btnRoom.setOnClickListener {
            roomDialog!!.show()
        }
    }

    private fun onAddDeviceClick() {
        if (roomId > 0) {
            val optionalDialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
            val btnOption1 = optionalDialogView.findViewById<CardView>(R.id.buttonOption1)
            val btnOption2 = optionalDialogView.findViewById<CardView>(R.id.buttonOption2)

            optionalDialog!!.setContentView(optionalDialogView)

            btnOption1.setOnClickListener {
                val bundle = Bundle()
                bundle.putInt("roomId", roomId)
                replaceFragment2(requireActivity().supportFragmentManager, AddDeviceFragment(), bundle)
                optionalDialog!!.dismiss()
            }

            btnOption2.setOnClickListener {
                replaceFragment1(requireActivity().supportFragmentManager, QrScanFragment())
                optionalDialog!!.dismiss()
            }

            optionalDialog!!.show()
        } else {
            Toast.makeText(requireActivity(), "등록된 룸이 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
}