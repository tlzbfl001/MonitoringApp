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
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.databinding.FragmentAddDeviceBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.location
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.home.MainFragment
import com.aitronbiz.arron.view.room.SettingRoomFragment
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddDeviceFragment : Fragment() {
    private var _binding: FragmentAddDeviceBinding? = null
    private val binding get() = _binding!!

    private var homeId: String? = ""
    private var roomId: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDeviceBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        arguments?.let {
            homeId = it.getString("homeId")
            roomId = it.getString("roomId")
        }

        binding.btnBack.setOnClickListener {
            replaceFragment()
        }

        binding.btnAdd.setOnClickListener {
            when {
                binding.etName.text.trim().isEmpty() -> Toast.makeText(requireActivity(), "장소 이름을 입력하세요", Toast.LENGTH_SHORT).show()
                binding.etProduct.text.trim().isEmpty() -> Toast.makeText(requireActivity(), "제품 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                binding.etSerial.text.trim().isEmpty() -> Toast.makeText(requireActivity(), "시리얼 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                homeId == null -> Toast.makeText(requireActivity(), "등록된 홈이 없습니다. 홈 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                roomId == null -> Toast.makeText(requireActivity(), "등록된 룸이 없습니다. 룸 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                else -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            val dto = DeviceDTO(
                                name = binding.etName.text.trim().toString(),
                                roomId = roomId!!
                            )
                            val response = RetrofitClient.apiService.createDevice("Bearer ${AppController.prefs.getToken()}", dto)
                            if(response.isSuccessful) {
                                Log.d(TAG, "createDevice: ${response.body()}")
                                Toast.makeText(requireActivity(), "저장되었습니다.", Toast.LENGTH_SHORT).show()
                                replaceFragment()
                            } else {
                                Log.e(TAG, "createDevice 실패: ${response.code()}")
                                Toast.makeText(requireActivity(), "저장 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        return binding.root
    }

    private fun replaceFragment() {
        val bundle = Bundle().apply {
            putString("homeId", homeId)
            putString("roomId", roomId)
        }
        if(location == 1) {
            replaceFragment2(parentFragmentManager, SettingRoomFragment(), bundle)
        }else {
            replaceFragment2(requireActivity().supportFragmentManager, DeviceFragment(), bundle)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.hideBottomNav()
    }
}