package com.aitronbiz.arron.view.room

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

    private var deleteDialog : Dialog? = null
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

        deleteDialog = Dialog(requireActivity())
        deleteDialog!!.setContentView(R.layout.dialog_delete)
        deleteDialog!!.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val btnCancel = deleteDialog!!.findViewById<CardView>(R.id.btnCancel)
        val btnDelete = deleteDialog!!.findViewById<CardView>(R.id.btnDelete)

        arguments?.let {
            homeId = it.getString("homeId")
            roomId = it.getString("roomId")
        }

        val args = Bundle().apply {
            putString("homeId", homeId)
            putString("roomId", roomId)
        }

        adapter = DeviceItemAdapter(
            deviceList,
            onItemClick = { device ->
                val bundle = Bundle().apply {
                    putString("homeId", homeId)
                    putString("roomId", roomId)
                    putString("deviceId", device.id)
                }
                replaceFragment2(parentFragmentManager, SettingDeviceFragment(), bundle)
            }
        )

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch(Dispatchers.IO) {
            if (roomId != null) {
                val getRoom = RetrofitClient.apiService.getRoom("Bearer ${AppController.prefs.getToken()}", roomId!!)
                val getAllDevice = RetrofitClient.apiService.getAllDevice("Bearer ${AppController.prefs.getToken()}", roomId!!)

                withContext(Dispatchers.Main) {
                    // Room 이름 설정
                    if (getRoom.isSuccessful) {
                        binding.tvTitle.text = getRoom.body()!!.room.name
                    } else {
                        Log.e(TAG, "getRoom: $getRoom")
                    }

                    // 디바이스 목록 업데이트
                    if (getAllDevice.isSuccessful) {
                        val devices = getAllDevice.body()!!.devices
                        deviceList.clear()
                        deviceList.addAll(devices)
                        adapter.notifyDataSetChanged()
                    } else {
                        val errorBody = getAllDevice.errorBody()?.string()
                        val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                        Log.e(TAG, "getAllDevice: $errorResponse")
                    }
                }
            }
        }

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, SettingHomeFragment(), args)
        }

        binding.btnSetting.setOnClickListener {
            val dialog = BottomSheetDialog(requireActivity())
            val dialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_home_menu, null)

            val tvEdit = dialogView.findViewById<TextView>(R.id.tvEdit)
            val tvDelete = dialogView.findViewById<TextView>(R.id.tvDelete)

            tvEdit.setOnClickListener {
                replaceFragment2(parentFragmentManager, EditRoomFragment(), args)
                dialog.dismiss()
            }

            tvDelete.setOnClickListener {
                btnCancel.setOnClickListener {
                    deleteDialog!!.dismiss()
                }

                btnDelete.setOnClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        if(roomId != "") {
                            val response = RetrofitClient.apiService.deleteRoom("Bearer ${AppController.prefs.getToken()}", roomId!!)
                            if(response.isSuccessful) {
                                Log.d(TAG, "deleteRoom: ${response.body()}")
                                replaceFragment2(parentFragmentManager, SettingHomeFragment(), args)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "삭제되었습니다", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val errorBody = response.errorBody()?.string()
                                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                Log.e(TAG, "deleteRoom: $errorResponse")
                            }
                        }
                    }

                    deleteDialog!!.dismiss()
                }

                dialog.dismiss()
                deleteDialog!!.show()
            }

            dialog.setContentView(dialogView)
            dialog.show()
        }

        binding.btnAddDevice.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, AddDeviceFragment(), args)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.hideBottomNav()
    }
}