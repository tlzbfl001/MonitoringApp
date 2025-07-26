package com.aitronbiz.arron.view.room

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
import com.aitronbiz.arron.api.dto.RoomDTO
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.databinding.FragmentAddRoomBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.location
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.device.DeviceFragment
import com.aitronbiz.arron.view.home.SettingHomeFragment
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddRoomFragment : Fragment() {
    private var _binding: FragmentAddRoomBinding? = null
    private val binding get() = _binding!!

    private var homeId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddRoomBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        arguments?.let {
            homeId = it.getString("homeId")
        }

        binding.btnBack.setOnClickListener {
            replaceFragment()
        }

        binding.btnAdd.setOnClickListener {
            when {
                binding.etName.text.trim().toString().isEmpty() -> Toast.makeText(requireActivity(), "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                homeId == null -> Toast.makeText(requireActivity(), "등록된 홈이 없습니다. 홈 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                else -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            val dto = RoomDTO(
                                name = binding.etName.text.trim().toString(),
                                homeId = homeId!!
                            )
                            val response = RetrofitClient.apiService.createRoom("Bearer ${AppController.prefs.getToken()}", dto)
                            if(response.isSuccessful) {
                                Log.d(TAG, "createRoom: ${response.body()}")
                                Toast.makeText(requireActivity(), "저장되었습니다", Toast.LENGTH_SHORT).show()
                                replaceFragment()
                            } else {
                                val errorBody = response.errorBody()?.string()
                                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                Log.e(TAG, "createRoom: $errorResponse")
                                Toast.makeText(requireActivity(), "저장 실패하였습니다", Toast.LENGTH_SHORT).show()
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
        }
        if(location == 1) {
            replaceFragment2(parentFragmentManager, SettingHomeFragment(), bundle)
        }else {
            replaceFragment1(parentFragmentManager, DeviceFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.hideBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}