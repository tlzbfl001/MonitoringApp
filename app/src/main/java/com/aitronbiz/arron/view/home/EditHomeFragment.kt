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
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentEditHomeBinding
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class EditHomeFragment : Fragment() {
    private var _binding: FragmentEditHomeBinding? = null
    private val binding get() = _binding!!

    private var homeId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditHomeBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        homeId = arguments?.getString("homeId")

        lifecycleScope.launch(Dispatchers.IO) {
            val response = RetrofitClient.apiService.getHome("Bearer ${AppController.prefs.getToken()}", homeId!!)
            if(response.isSuccessful) {
                binding.etName.setText(response.body()!!.home.name)
            }else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                Log.e(TAG, "getHome: $errorResponse")
            }
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.btnEdit.setOnClickListener {
            when {
                binding.etName.text.trim().toString().isEmpty() -> Toast.makeText(requireActivity(), "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                homeId == null -> Toast.makeText(requireActivity(), "등록된 홈이 없습니다. 홈 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                else -> {
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

                            val response = RetrofitClient.apiService.updateHome("Bearer ${AppController.prefs.getToken()}", homeId!!, dto)
                            if(response.isSuccessful) {
                                Log.d(TAG, "updateHome: ${response.body()}")
                                Toast.makeText(requireActivity(), "수정되었습니다", Toast.LENGTH_SHORT).show()
                                replaceFragment1(requireActivity().supportFragmentManager, HomeFragment())
                            } else {
                                Log.e(TAG, "updateHome: $response")
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