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
import com.aitronbiz.arron.api.dto.UpdateRoomDTO
import com.aitronbiz.arron.databinding.FragmentEditRoomBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.home.SettingHomeFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditRoomFragment : Fragment() {
    private var _binding: FragmentEditRoomBinding? = null
    private val binding get() = _binding!!

    private var homeId: String? = ""
    private var roomId: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditRoomBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        arguments?.let {
            homeId = it.getString("homeId")
            roomId = it.getString("roomId")
        }

        val bundle = Bundle().apply {
            putString("homeId", homeId)
            putString("roomId", roomId)
        }

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, SettingRoomFragment(), bundle)
        }

        binding.btnEdit.setOnClickListener {
            when {
                binding.etName.text.trim().toString().isEmpty() -> Toast.makeText(requireActivity(), "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                homeId == null -> Toast.makeText(requireActivity(), "등록된 홈이 없습니다. 홈 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                else -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            val dto = UpdateRoomDTO(
                                name = binding.etName.text.trim().toString()
                            )

                            val response = RetrofitClient.apiService.updateRoom("Bearer ${AppController.prefs.getToken()}", roomId!!, dto)
                            if(response.isSuccessful) {
                                Log.d(TAG, "updateRoom: ${response.body()}")
                                Toast.makeText(requireActivity(), "수정되었습니다", Toast.LENGTH_SHORT).show()
                                replaceFragment2(requireActivity().supportFragmentManager, SettingRoomFragment(), bundle)
                            } else {
                                Log.e(TAG, "updateRoom: $response")
                                Toast.makeText(requireActivity(), "수정 실패하였습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
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