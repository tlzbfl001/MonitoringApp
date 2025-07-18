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
import com.aitronbiz.arron.database.DBHelper.Companion.DEVICE
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentAddDeviceBinding
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import java.time.LocalDateTime
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.room.SettingRoomFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddDeviceFragment : Fragment() {
    private var _binding: FragmentAddDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var home: Home? = null
    private var room: Room? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDeviceBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireContext())

        arguments?.let {
            home = it.getParcelable("home")
            room = it.getParcelable("room")
        }

        val bundle = Bundle().apply {
            putParcelable("home", home)
            putParcelable("room", room)
        }

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, SettingRoomFragment(), bundle)
        }

        binding.btnAdd.setOnClickListener {
            when {
                binding.etName.text.trim().isEmpty() -> Toast.makeText(requireActivity(), "장소 이름을 입력하세요", Toast.LENGTH_SHORT).show()
                binding.etProduct.text.trim().isEmpty() -> Toast.makeText(requireActivity(), "제품 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                binding.etSerial.text.trim().isEmpty() -> Toast.makeText(requireActivity(), "시리얼 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                home == null -> Toast.makeText(requireActivity(), "등록된 홈이 없습니다. 홈 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                room == null -> Toast.makeText(requireActivity(), "등록된 룸이 없습니다. 룸 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                else -> {
                    val data = Device(
                        uid = AppController.prefs.getUID(),
                        homeId = home!!.id,
                        roomId = room!!.id,
                        name = binding.etName.text.trim().toString(),
                        productNumber = binding.etProduct.text.trim().toString(),
                        serialNumber = binding.etSerial.text.trim().toString(),
                        createdAt = LocalDateTime.now().toString(),
                    )

                    lifecycleScope.launch(Dispatchers.IO) {
                        val insertedId = dataManager.insertDevice(data)
                        withContext(Dispatchers.Main) {
                            if(insertedId != -1) {
                                val dto = DeviceDTO(
                                    name = data.name!!,
                                    roomId = room!!.serverId!!
                                )
                                val response = RetrofitClient.apiService.createDevice("Bearer ${AppController.prefs.getToken()}", dto)
                                if(response.isSuccessful) {
                                    Log.d(TAG, "createDevice: ${response.body()}")
                                    dataManager.updateData(DEVICE, "serverId", response.body()!!.device.id, insertedId)
                                } else {
                                    Log.e(TAG, "createDevice: $response")
                                }

                                Toast.makeText(requireActivity(), "저장되었습니다", Toast.LENGTH_SHORT).show()
                                replaceFragment2(requireActivity().supportFragmentManager, SettingRoomFragment(), bundle)
                            }else {
                                Toast.makeText(requireActivity(), "저장 실패하였습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        return binding.root
    }
}