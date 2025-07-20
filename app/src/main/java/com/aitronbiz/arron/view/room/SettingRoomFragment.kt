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
import com.aitronbiz.arron.database.DBHelper.Companion.ROOM
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentSettingRoomBinding
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.device.AddDeviceFragment
import com.aitronbiz.arron.view.device.SettingDeviceFragment
import com.aitronbiz.arron.view.home.EditHomeFragment
import com.aitronbiz.arron.view.home.SettingHomeFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingRoomFragment : Fragment() {
    private var _binding: FragmentSettingRoomBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var deleteDialog : Dialog? = null
    private lateinit var adapter: DeviceItemAdapter
    private lateinit var deviceList: MutableList<Device>
    private var home: Home? = null
    private var room: Room? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingRoomBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())

        arguments?.let {
            home = it.getParcelable("home")
            room = it.getParcelable("room")
        }

        if(room != null) binding.tvTitle.text = room!!.name

        deleteDialog = Dialog(requireActivity())
        deleteDialog!!.setContentView(R.layout.dialog_delete)
        deleteDialog!!.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val btnCancel = deleteDialog!!.findViewById<CardView>(R.id.btnCancel)
        val btnDelete = deleteDialog!!.findViewById<CardView>(R.id.btnDelete)

        if(home != null) {
            deviceList = dataManager.getDevices(home!!.id, room!!.id).toMutableList()
        }else {
            deviceList = mutableListOf()
        }

        adapter = DeviceItemAdapter(
            deviceList,
            onItemClick = { device ->
                val bundle = Bundle().apply {
                    putParcelable("home", home)
                    putParcelable("room", room)
                    putParcelable("device", device)
                }
                replaceFragment2(parentFragmentManager, SettingDeviceFragment(), bundle)
            }
        )

        val args = Bundle().apply {
            putParcelable("home", home)
            putParcelable("room", room)
        }

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, SettingHomeFragment(), args)
        }

        binding.btnSetting.setOnClickListener {
            val dialog = BottomSheetDialog(requireActivity())
            val dialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_home_menu, null)

            val tvEdit = dialogView.findViewById<TextView>(R.id.tvEdit)
            val tvDelete = dialogView.findViewById<TextView>(R.id.tvDelete)

            tvEdit.setOnClickListener {
                replaceFragment2(parentFragmentManager, EditHomeFragment(), args)
                dialog.dismiss()
            }

            tvDelete.setOnClickListener {
                btnCancel.setOnClickListener {
                    deleteDialog!!.dismiss()
                }

                btnDelete.setOnClickListener {
                    if(home != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            if(room!!.serverId != null && room!!.serverId != "") {
                                val response = RetrofitClient.apiService.deleteRoom("Bearer ${AppController.prefs.getToken()}", room!!.serverId!!)
                                if(response.isSuccessful) {
                                    Log.d(TAG, "deleteRoom: ${response.body()}")
                                    dataManager.deleteData(ROOM, room!!.id)
                                    replaceFragment2(parentFragmentManager, SettingHomeFragment(), args)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "삭제되었습니다", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Log.e(TAG, "deleteRoom: $response")
                                }
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