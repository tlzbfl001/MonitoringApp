package com.aitronbiz.arron.view.device

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitronbiz.arron.AppController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.DeviceAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.database.DBHelper.Companion.DEVICE
import com.aitronbiz.arron.database.DBHelper.Companion.ROOM
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentDeviceBinding
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.room.DetailRoomFragment
import com.aitronbiz.arron.view.room.EditRoomFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceFragment : Fragment() {
    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var adapter: DeviceAdapter
    private var optionalDialog : BottomSheetDialog? = null
    private var homeData: Home? = null
    private var subjectData: Subject? = null
    private var roomData: Room? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceBinding.inflate(inflater, container, false)

        initUI()
        setupOptionalDialog()

        return binding.root
    }

    private fun initUI() {
        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireContext())

        arguments?.let {
            homeData = it.getParcelable("homeData",)
            subjectData = it.getParcelable("subjectData")
            roomData = it.getParcelable("roomData")
        }

        val args = Bundle().apply {
            putParcelable("homeData", homeData)
            putParcelable("subjectData", subjectData)
            putParcelable("roomData", roomData)
        }

        val getDevices = dataManager.getDevices(homeData!!.id, roomData!!.id)

        adapter = DeviceAdapter(
            getDevices,
            onItemClick = { device ->
                if (homeData == null || subjectData == null || roomData == null) {
                    Toast.makeText(requireContext(), "홈과 대상자, 룸을 먼저 선택해주세요.", Toast.LENGTH_SHORT).show()
                    return@DeviceAdapter
                }

                val bundle = Bundle().apply {
                    putParcelable("homeData", homeData)
                    putParcelable("subjectData", subjectData)
                    putParcelable("roomData", roomData)
                    putParcelable("deviceData", device)
                }
                replaceFragment2(requireActivity().supportFragmentManager, DeviceSettingFragment(), bundle)
            },
            onEditClick = { device ->
                val bundle = Bundle().apply {
                    putParcelable("homeData", homeData)
                    putParcelable("subjectData", subjectData)
                    putParcelable("roomData", roomData)
                    putParcelable("deviceData", device)
                }
                replaceFragment2(parentFragmentManager, EditDeviceFragment(), bundle)
            },
            onDeleteClick = { device ->
                lifecycleScope.launch(Dispatchers.IO) {
                    if(device.serverId != null && device.serverId != "") {
                        val response = RetrofitClient.apiService.deleteDevice("Bearer ${AppController.prefs.getToken()}", device.serverId!!)
                        if(response.isSuccessful) {
                            Log.d(TAG, "deleteDevice: ${response.body()}")
                        } else {
                            Log.e(TAG, "deleteDevice: $response")
                        }
                    }

                    dataManager.deleteData(DEVICE, device.id)
                    getDevices.removeIf { it.id == device.id }
                    withContext(Dispatchers.Main) {
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        )

        binding.recyclerDevices.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerDevices.adapter = adapter

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, DetailRoomFragment(), args)
        }

        binding.btnAdd.setOnClickListener {
            optionalDialog!!.show()
        }
    }

    private fun setupOptionalDialog() {
        optionalDialog = BottomSheetDialog(requireContext())
        val optionalDialogView = layoutInflater.inflate(R.layout.dialog_add_device, null)
        val btnOption1 = optionalDialogView.findViewById<CardView>(R.id.buttonOption1)
        val btnOption2 = optionalDialogView.findViewById<CardView>(R.id.buttonOption2)
        optionalDialog!!.setContentView(optionalDialogView)

        btnOption1.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable("homeData", homeData)
                putParcelable("subjectData", subjectData)
                putParcelable("roomData", roomData)
            }
            replaceFragment2(requireActivity().supportFragmentManager, AddDeviceFragment(), bundle)
            optionalDialog!!.dismiss()
        }

        btnOption2.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, QrScanFragment())
            optionalDialog!!.dismiss()
        }
    }
}