package com.aitronbiz.arron.view.home

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
import com.aitronbiz.arron.api.dto.HomeDTO
import com.aitronbiz.arron.database.DBHelper.Companion.HOME
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentAddHomeBinding
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class AddHomeFragment : Fragment() {
    private var _binding: FragmentAddHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddHomeBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, HomeFragment())
        }

        binding.btnAdd.setOnClickListener {
            if(binding.etName.text.trim().isEmpty()) {
                Toast.makeText(requireActivity(), "홈 이름을 입력하세요", Toast.LENGTH_SHORT).show()
            }else {
                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        val dto = HomeDTO(
                            name = binding.etName.text.trim().toString(),
                            province = "서울특별시",
                            city = "중구",
                            street = "세종대로 110",
                            detailAddress = "서울특별시청",
                            postalCode = "04524",
                        )

                        val response = RetrofitClient.apiService.createHome("Bearer ${AppController.prefs.getToken()}", dto)
                        if(response.isSuccessful) {
                            Log.d(TAG, "createHome: ${response.body()}")
                            Toast.makeText(requireActivity(), "저장되었습니다", Toast.LENGTH_SHORT).show()
                            replaceFragment1(requireActivity().supportFragmentManager, HomeFragment())
                        } else {
                            Log.e(TAG, "createHome: $response")
                            Toast.makeText(requireActivity(), "저장 실패하였습니다", Toast.LENGTH_SHORT).show()
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