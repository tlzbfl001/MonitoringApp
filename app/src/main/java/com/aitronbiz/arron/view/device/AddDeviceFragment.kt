package com.aitronbiz.arron.view.device

import com.aitronbiz.arron.R
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
import com.aitronbiz.arron.api.dto.SubjectDTO
import com.aitronbiz.arron.database.DBHelper.Companion.DEVICE
import com.aitronbiz.arron.database.DBHelper.Companion.SUBJECT
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentAddDeviceBinding
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import java.time.LocalDateTime
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.home.HomeFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddDeviceFragment : Fragment() {
    private var _binding: FragmentAddDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var homeData: Home? = null
    private var subjectData: Subject? = null
    private var roomData: Room? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDeviceBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireContext())

        arguments?.let {
            homeData = it.getParcelable("homeData")
            subjectData = it.getParcelable("subjectData")
            roomData = it.getParcelable("roomData")
        }

        val bundle = Bundle().apply {
            putParcelable("homeData", homeData)
            putParcelable("subjectData", subjectData)
            putParcelable("roomData", roomData)
        }

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, DeviceFragment(), bundle)
        }

        binding.btnAdd.setOnClickListener {
            when {
                binding.etName.text.trim().isEmpty() -> Toast.makeText(requireActivity(), "장소 이름을 입력하세요", Toast.LENGTH_SHORT).show()
                binding.etProduct.text.trim().isEmpty() -> Toast.makeText(requireActivity(), "제품 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                binding.etSerial.text.trim().isEmpty() -> Toast.makeText(requireActivity(), "시리얼 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                homeData == null -> Toast.makeText(requireActivity(), "등록된 홈이 없습니다. 홈 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                subjectData == null -> Toast.makeText(requireActivity(), "등록된 대상자가 없습니다. 홈 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                roomData == null -> Toast.makeText(requireActivity(), "등록된 룸이 없습니다. 홈 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                else -> {
                    val data = Device(
                        uid = AppController.prefs.getUID(),
                        homeId = homeData!!.id,
                        subjectId = subjectData!!.id,
                        roomId = roomData!!.id,
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
                                    roomId = roomData!!.serverId!!
                                )
                                val response = RetrofitClient.apiService.createDevice("Bearer ${AppController.prefs.getToken()}", dto)
                                if(response.isSuccessful) {
                                    Log.d(TAG, "createDevice: ${response.body()}")
                                    dataManager.updateData(DEVICE, "serverId", response.body()!!.device.id, insertedId)
                                } else {
                                    Log.e(TAG, "createDevice: $response")
                                }

                                Toast.makeText(requireActivity(), "저장되었습니다", Toast.LENGTH_SHORT).show()
                                replaceFragment2(requireActivity().supportFragmentManager, DeviceFragment(), bundle)
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