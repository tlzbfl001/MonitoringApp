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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.DeviceItemAdapter
import com.aitronbiz.arron.adapter.SelectHomeDialogAdapter
import com.aitronbiz.arron.adapter.SelectRoomDialogAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Device
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.databinding.FragmentDeviceBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.location
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.home.HomeFragment
import com.aitronbiz.arron.view.room.AddRoomFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceFragment : Fragment() {
    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DeviceItemAdapter
    private var optionalDialog: BottomSheetDialog? = null
    private var homeDialog: BottomSheetDialog? = null
    private var roomDialog: BottomSheetDialog? = null
    private var homes = ArrayList<Home>()
    private var rooms = ArrayList<Room>()
    private var devices = ArrayList<Device>()
    private var homeId = ""
    private var roomId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        location = 2

        // arguments가 존재하고, "homeId"와 "roomId" 값이 모두 존재하는 경우에만 homeId와 roomId 변수에 값을 할당함.
        arguments?.let {
            if(it.getString("homeId") != null && it.getString("roomId") != null) {
                homeId = it.getString("homeId")!!
                roomId = it.getString("roomId")!!
            }
        }

        setupRecyclerView()
        setupOptionalDialog()
        setupHomeDialog()
        setupRoomDialog()
        fetchHomesAndRooms()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = DeviceItemAdapter(
            devices,
            onItemClick = { device ->
                when {
                    homeId.isBlank() -> Toast.makeText(context, "홈을 등록해주세요.", Toast.LENGTH_SHORT).show()
                    roomId.isBlank() -> Toast.makeText(context, "룸을 등록해주세요.", Toast.LENGTH_SHORT).show()
                    device.id.isBlank() -> Toast.makeText(context, "기기 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    else -> {
                        val bundle = Bundle().apply {
                            putString("homeId", homeId)
                            putString("roomId", roomId)
                            putString("deviceId", device.id)
                        }
                        replaceFragment2(requireActivity().supportFragmentManager, SettingDeviceFragment(), bundle)
                    }
                }
            },
            onAddClick = {
                when {
                    homeId.isBlank() -> Toast.makeText(context, "홈을 등록해주세요.", Toast.LENGTH_SHORT).show()
                    roomId.isBlank() -> Toast.makeText(context, "룸을 등록해주세요.", Toast.LENGTH_SHORT).show()
                    else -> {
                        val bundle = Bundle().apply {
                            putString("homeId", homeId)
                            putString("roomId", roomId)
                        }
                        replaceFragment2(requireActivity().supportFragmentManager, AddDeviceFragment(), bundle)
                    }
                }
            }
        )
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter
    }

    private fun setupHomeDialog() {
        homeDialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_home, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddHome = dialogView.findViewById<ConstraintLayout>(R.id.btnAdd)

        btnAddHome.setOnClickListener {
            homeDialog?.dismiss()
            replaceFragment1(requireActivity().supportFragmentManager, HomeFragment())
        }

        homeDialog!!.setContentView(dialogView)

        binding.btnHome.setOnClickListener {
            val selectedIndex = homes.indexOfFirst { it.id == homeId }.coerceAtLeast(0)
            val adapter = SelectHomeDialogAdapter(homes, { selectedHome ->
                updateHome(selectedHome)
                Handler(Looper.getMainLooper()).postDelayed({
                    homeDialog?.dismiss()
                }, 300)
            }, selectedIndex)

            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter

            homeDialog!!.show()
        }
    }

    private fun setupRoomDialog() {
        roomDialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_room, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddRoom = dialogView.findViewById<ConstraintLayout>(R.id.btnAdd)

        btnAddRoom.setOnClickListener {
            roomDialog?.dismiss()
            if(homeId != "") {
                val bundle = Bundle().apply {
                    putString("homeId", homeId)
                }
                replaceFragment2(requireActivity().supportFragmentManager, AddRoomFragment(), bundle)
            }else {
                Toast.makeText(context, "홈 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        roomDialog!!.setContentView(dialogView)

        binding.btnRoom.setOnClickListener {
            val selectedIndex = rooms.indexOfFirst { it.id == roomId }.coerceAtLeast(0)
            val adapter = SelectRoomDialogAdapter(rooms, { selectedRoom ->
                updateRoom(selectedRoom)
                Handler(Looper.getMainLooper()).postDelayed({
                    roomDialog?.dismiss()
                }, 300)
            }, selectedIndex)

            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter

            roomDialog!!.show()
        }
    }

    private fun setupOptionalDialog() {
        optionalDialog = BottomSheetDialog(requireContext())
        val optionalDialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
        val btnOption1 = optionalDialogView.findViewById<CardView>(R.id.buttonOption1)
        val btnOption2 = optionalDialogView.findViewById<CardView>(R.id.buttonOption2)
        optionalDialog!!.setContentView(optionalDialogView)

        btnOption1.setOnClickListener {
            when {
                homeId.isBlank() -> Toast.makeText(context, "홈을 등록해주세요.", Toast.LENGTH_SHORT).show()
                roomId.isBlank() -> Toast.makeText(context, "룸을 등록해주세요.", Toast.LENGTH_SHORT).show()
                else -> {
                    val bundle = Bundle().apply {
                        putString("homeId", homeId)
                        putString("roomId", roomId)
                    }
                    replaceFragment2(requireActivity().supportFragmentManager, AddDeviceFragment(), bundle)
                    optionalDialog!!.dismiss()
                }
            }
        }

        btnOption2.setOnClickListener {
            when {
                homeId.isBlank() -> Toast.makeText(context, "홈을 등록해주세요.", Toast.LENGTH_SHORT).show()
                roomId.isBlank() -> Toast.makeText(context, "룸을 등록해주세요.", Toast.LENGTH_SHORT).show()
                else -> {
                    val bundle = Bundle().apply {
                        putString("homeId", homeId)
                        putString("roomId", roomId)
                    }
                    replaceFragment2(requireActivity().supportFragmentManager, AddDeviceFragment(), bundle)
                    optionalDialog!!.dismiss()
                }
            }
        }
    }

    private fun fetchHomesAndRooms() {
        lifecycleScope.launch(Dispatchers.IO) {
            val homeResponse = RetrofitClient.apiService.getAllHome("Bearer ${AppController.prefs.getToken()}")

            if (homeResponse.isSuccessful) {
                homes = homeResponse.body()?.homes ?: arrayListOf()

                if (homes.isNotEmpty()) {
                    val selectedHome = homes.find { it.id == homeId } ?: homes[0]
                    homeId = selectedHome.id

                    withContext(Dispatchers.Main) {
                        binding.tvHome.text = "홈 : ${selectedHome.name}"
                    }

                    val roomResponse = RetrofitClient.apiService.getAllRoom("Bearer ${AppController.prefs.getToken()}", homeId)
                    if (roomResponse.isSuccessful) {
                        rooms = roomResponse.body()?.rooms ?: arrayListOf()

                        val selectedRoom = rooms.find { it.id == roomId } ?: rooms.firstOrNull()
                        roomId = selectedRoom?.id ?: ""

                        withContext(Dispatchers.Main) {
                            binding.tvRoom.text = "룸 : ${selectedRoom?.name ?: ""}"
                            fetchDevices()
                        }
                    }else {
                        Log.e(TAG, "roomResponse: ${roomResponse.code()}")
                    }
                }
            }else {
                Log.e(TAG, "roomResponse: ${homeResponse.code()}")
            }
        }
    }

    private fun updateHome(selectedHome: Home) {
        homeId = selectedHome.id
        binding.tvHome.text = "홈 : ${selectedHome.name}"

        devices.clear()
        adapter.notifyDataSetChanged()

        lifecycleScope.launch(Dispatchers.IO) {
            val response = RetrofitClient.apiService.getAllRoom("Bearer ${AppController.prefs.getToken()}", homeId)

            if (response.isSuccessful) {
                rooms = response.body()?.rooms ?: arrayListOf()

                withContext(Dispatchers.Main) {
                    if (rooms.isNotEmpty()) {
                        roomId = rooms[0].id
                        binding.tvRoom.text = "룸 : ${rooms[0].name}"
                    } else {
                        roomId = ""
                        binding.tvRoom.text = "룸 : "
                    }
                    fetchDevices()
                }
            }else {
                Log.e(TAG, "getAllRoom 실패: ${response.code()}")
            }
        }
    }

    private fun updateRoom(selectedRoom: Room) {
        roomId = selectedRoom.id
        binding.tvRoom.text = "룸 : ${selectedRoom.name}"
        fetchDevices()
    }

    private fun fetchDevices() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (roomId.isBlank()) {
                withContext(Dispatchers.Main) {
                    adapter.updateData(emptyList())
                }
                return@launch
            }

            val response = RetrofitClient.apiService.getAllDevice("Bearer ${AppController.prefs.getToken()}", roomId)
            if (response.isSuccessful) {
                devices = response.body()?.devices ?: arrayListOf()
                withContext(Dispatchers.Main) {
                    adapter.updateData(devices)
                }
            } else {
                Log.e(TAG, "getAllDevice 실패: ${response.code()}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.showBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}