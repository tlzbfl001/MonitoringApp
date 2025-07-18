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
import com.aitronbiz.arron.util.BottomNavVisibilityController
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
    private var home: Home? = null
    private var room: Room? = null
    private var device: Device? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditDeviceBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())

        arguments?.let {
            home = it.getParcelable("home",)
            room = it.getParcelable("room")
            device = it.getParcelable("device")
        }

        val bundle = Bundle().apply {
            putParcelable("home", home)
            putParcelable("room", room)
        }

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, SettingRoomFragment(), bundle)
        }

        binding.btnEdit.setOnClickListener {
            val data = Device(
                id = device!!.id,
                uid = AppController.prefs.getUID(),
                name = binding.etName.text.trim().toString(),
                createdAt = LocalDateTime.now().toString(),
            )

            lifecycleScope.launch(Dispatchers.IO) {
                val updatedRows = dataManager.updateDevice(data)
                withContext(Dispatchers.Main) {
                    if(updatedRows > 0 && room?.serverId != null && room?.serverId != "") {
                        val dto = DeviceDTO(
                            name = binding.etName.text.trim().toString(),
                            roomId = room!!.serverId!!
                        )
                        val response = RetrofitClient.apiService.updateDevice("Bearer ${AppController.prefs.getToken()}", device!!.serverId!!, dto)
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

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.hideBottomNav()
    }
}