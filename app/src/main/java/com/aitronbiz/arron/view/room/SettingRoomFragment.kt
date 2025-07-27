package com.aitronbiz.arron.view.room

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.DeviceItemAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Device
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.databinding.FragmentSettingRoomBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.device.AddDeviceFragment
import com.aitronbiz.arron.view.device.SettingDeviceFragment
import com.aitronbiz.arron.view.home.SettingHomeFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingRoomFragment : Fragment() {
    private var _binding: FragmentSettingRoomBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DeviceItemAdapter
    private var deviceList: MutableList<Device> = mutableListOf()
    private var homeId: String? = ""
    private var roomId: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingRoomBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        arguments?.let {
            homeId = it.getString("homeId")
            roomId = it.getString("roomId")
        }

        val bundle = Bundle().apply {
            putString("homeId", homeId)
            putString("roomId", roomId)
        }

        adapter = DeviceItemAdapter(
            deviceList,
            onItemClick = { device ->
                if(homeId != "" && roomId != "" && device.id != "") {
                    val arg = Bundle().apply {
                        putString("homeId", homeId)
                        putString("roomId", roomId)
                        putString("deviceId", device.id)
                    }
                    replaceFragment2(parentFragmentManager, SettingDeviceFragment(), arg)
                }else {
                    Toast.makeText(context, "기기 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            },
            onAddClick = {
                if(homeId != "" && roomId != "") {
                    replaceFragment2(requireActivity().supportFragmentManager, AddDeviceFragment(), bundle)
                }else {
                    Toast.makeText(context, "홈 또는 방 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch(Dispatchers.IO) {
            if (roomId != null) {
                val getRoom = RetrofitClient.apiService.getRoom("Bearer ${AppController.prefs.getToken()}", roomId!!)
                val getAllDevice = RetrofitClient.apiService.getAllDevice("Bearer ${AppController.prefs.getToken()}", roomId!!)

                withContext(Dispatchers.Main) {
                    if (getRoom.isSuccessful) {
                        binding.tvTitle.text = getRoom.body()!!.room.name
                    } else {
                        Log.e(TAG, "getRoom 실패: ${getRoom.code()}")
                    }

                    // 디바이스 목록 업데이트
                    if (getAllDevice.isSuccessful) {
                        val devices = getAllDevice.body()!!.devices
                        deviceList.clear()
                        deviceList.addAll(devices)
                        adapter.notifyDataSetChanged()
                    } else {
                        Log.e(TAG, "getAllDevice 실패: ${getAllDevice.code()}")
                    }
                }
            }
        }

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, SettingHomeFragment(), bundle)
        }

        binding.btnSetting.setOnClickListener {
            val dialog = BottomSheetDialog(requireActivity())
            val dialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_home_menu, null)

            val tvEdit = dialogView.findViewById<TextView>(R.id.tvEdit)
            val tvDelete = dialogView.findViewById<TextView>(R.id.tvDelete)

            tvEdit.setOnClickListener {
                replaceFragment2(parentFragmentManager, EditRoomFragment(), bundle)
                dialog.dismiss()
            }

            tvDelete.setOnClickListener {
                if(roomId != "") {
                    val deleteDialog = AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setTitle("룸 삭제")
                        .setMessage("정말 삭제 하시겠습니까?")
                        .setPositiveButton("확인") { _, _ ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val response = RetrofitClient.apiService.deleteRoom("Bearer ${AppController.prefs.getToken()}", roomId!!)
                                if(response.isSuccessful) {
                                    Log.d(TAG, "deleteRoom: ${response.body()}")
                                    replaceFragment2(parentFragmentManager, SettingHomeFragment(), bundle)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                    dialog.dismiss()
                                } else {
                                    Log.e(TAG, "deleteRoom 실패: ${response.code()}")
                                }
                            }
                        }
                        .setNegativeButton("취소", null)
                        .create()
                    deleteDialog.show()
                }else {
                    Toast.makeText(context, "룸 정보가 없어 삭제할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.setContentView(dialogView)
            dialog.show()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.hideBottomNav()
    }
}