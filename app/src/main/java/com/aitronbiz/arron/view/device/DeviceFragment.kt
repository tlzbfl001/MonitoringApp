package com.aitronbiz.arron.view.device

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

class DeviceFragment : Fragment() {
    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var adapter: DeviceListAdapter
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

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireContext())

        val homes = dataManager.getHomes(AppController.prefs.getUID())
        if(homes.isNotEmpty()) {
            homeId = homes[0].id
            binding.tvHome.text = "홈 : ${homes[0].name}"
        }else {
            binding.tvHome.text = "홈 :  "
        }

        val rooms = dataManager.getRooms(AppController.prefs.getUID(), homeId)
        if(rooms.isNotEmpty()) {
            roomId = rooms[0].id
            binding.tvRoom.text = "룸 : ${rooms[0].name}"
        }else {
            binding.tvRoom.text = "룸 :  "
        }

        initUI()
        setupHomeDialog()
        setupRoomDialog()
        setupOptionalDialog()

        return binding.root
    }

    private fun initUI() {
        val getDevices = dataManager.getDevices(homeId, roomId)

        adapter = DeviceListAdapter(
            getDevices,
            onItemClick = { device ->
                val bundle = Bundle().apply {
                    putParcelable("device", device)
                }
                replaceFragment2(requireActivity().supportFragmentManager, DeviceSettingFragment(), bundle)
            }
        )

        binding.recyclerDevices.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerDevices.adapter = adapter

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.btnAdd.setOnClickListener {
            optionalDialog!!.show()
        }

        binding.btnHome.setOnClickListener {
            homeDialog!!.show()
        }

        binding.btnRoom.setOnClickListener {
            if(roomId > 0) {
                roomDialog!!.show()
            }else {
                Toast.makeText(requireActivity(), "등록된 룸이 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupHomeDialog() {
        homeDialog = BottomSheetDialog(requireContext())
        val homeDialogView = layoutInflater.inflate(R.layout.dialog_select_home, null)
        val homeRecyclerView = homeDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddHome = homeDialogView.findViewById<ConstraintLayout>(R.id.btnAdd)

        val homes = dataManager.getHomes(AppController.prefs.getUID())
        if(homes.isNotEmpty()) homeId = homes[0].id
        homeDialog!!.setContentView(homeDialogView)

        val selectHomeDialogAdapter = SelectHomeDialogAdapter(homes) { selectedItem ->
            homeId = selectedItem.id
            binding.tvHome.text = "홈 :  ${selectedItem.name}"
            Handler(Looper.getMainLooper()).postDelayed({
                val devices = dataManager.getDevices(homeId, roomId) // Device 객체 리스트 반환
                adapter.updateData(devices)
                homeDialog?.dismiss()
            }, 300)
        }

        homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        homeRecyclerView.adapter = selectHomeDialogAdapter

        btnAddHome.setOnClickListener {
            homeDialog?.dismiss()
            replaceFragment1(requireActivity().supportFragmentManager, AddHomeFragment())
        }
    }

    private fun setupRoomDialog() {
        roomDialog = BottomSheetDialog(requireContext())
        val roomDialogView = layoutInflater.inflate(R.layout.dialog_select_room, null)
        val roomRecyclerView = roomDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddRoom = roomDialogView.findViewById<ConstraintLayout>(R.id.btnAdd)

        val rooms = dataManager.getRooms(AppController.prefs.getUID(), homeId)
        if(rooms.isNotEmpty()) roomId = rooms[0].id
        roomDialog!!.setContentView(roomDialogView)

        val selectRoomDialogAdapter = SelectRoomDialogAdapter(rooms) { selectedItem ->
            roomId = selectedItem.id
            binding.tvRoom.text = "룸 : ${selectedItem.name}"
            Handler(Looper.getMainLooper()).postDelayed({
                val devices = dataManager.getDevices(homeId, roomId) // Device 객체 리스트 반환
                adapter.updateData(devices)
                roomDialog?.dismiss()
            }, 300)
        }

        roomRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        roomRecyclerView.adapter = selectRoomDialogAdapter

        btnAddRoom.setOnClickListener {
            roomDialog?.dismiss()
            replaceFragment1(requireActivity().supportFragmentManager, AddRoomFragment())
        }
    }

    private fun setupOptionalDialog() {
        optionalDialog = BottomSheetDialog(requireContext())
        val optionalDialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
        val btnOption1 = optionalDialogView.findViewById<CardView>(R.id.buttonOption1)
        val btnOption2 = optionalDialogView.findViewById<CardView>(R.id.buttonOption2)
        optionalDialog!!.setContentView(optionalDialogView)

        btnOption1.setOnClickListener {
            val bundle = Bundle()
            bundle.putInt("homeId", homeId)
            bundle.putInt("roomId", roomId)
            replaceFragment2(requireActivity().supportFragmentManager, AddDeviceFragment(), bundle)
            optionalDialog!!.dismiss()
        }

        btnOption2.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, QrScanFragment())
            optionalDialog!!.dismiss()
        }
    }
}