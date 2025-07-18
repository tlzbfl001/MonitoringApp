package com.aitronbiz.arron.view.device

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.DeviceDTO
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentEditDeviceBinding
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.room.SettingRoomFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class EditDeviceFragment : Fragment() {
    private var _binding: FragmentEditDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var homeData: Home? = null
    private var subjectData: Subject? = null
    private var roomData: Room? = null
    private var deviceData: Device? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditDeviceBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())

        arguments?.let {
            homeData = it.getParcelable("homeData",)
            subjectData = it.getParcelable("subjectData")
            roomData = it.getParcelable("roomData")
            deviceData = it.getParcelable("deviceData")
        }

        val bundle = Bundle().apply {
            putParcelable("homeData", homeData)
            putParcelable("subjectData", subjectData)
            putParcelable("roomData", roomData)
        }

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, SettingRoomFragment(), bundle)
        }

        binding.btnEdit.setOnClickListener {
            val data = Device(
                id = deviceData!!.id,
                uid = AppController.prefs.getUID(),
                name = binding.etName.text.trim().toString(),
                createdAt = LocalDateTime.now().toString(),
            )

            lifecycleScope.launch(Dispatchers.IO) {
                val updatedRows = dataManager.updateDevice(data)
                withContext(Dispatchers.Main) {
                    if(updatedRows > 0 && roomData?.serverId != null && roomData?.serverId != "") {
                        val dto = DeviceDTO(
                            name = binding.etName.text.trim().toString(),
                            roomId = roomData!!.serverId!!
                        )
                        val response = RetrofitClient.apiService.updateDevice("Bearer ${AppController.prefs.getToken()}", deviceData!!.serverId!!, dto)
                        if(response.isSuccessful) {
                            Log.d(TAG, "updateDevice: ${response.body()}")
                        } else {
                            Log.e(TAG, "updateDevice: $response")
                        }

                        Toast.makeText(requireActivity(), "수정되었습니다", Toast.LENGTH_SHORT).show()
                        replaceFragment2(requireActivity().supportFragmentManager, SettingRoomFragment(), bundle)
                    }else {
                        Toast.makeText(requireActivity(), "수정 실패하였습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return binding.root
    }
}