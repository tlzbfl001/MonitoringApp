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
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentEditHomeBinding
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class EditHomeFragment : Fragment() {
    private var _binding: FragmentEditHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var home: Home? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditHomeBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())

        home = arguments?.getParcelable("home")

        if(home!!.name != null) binding.etName.setText(home!!.name)

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.btnEdit.setOnClickListener {
            val homeData = Home(
                id = home!!.id,
                uid = AppController.prefs.getUID(),
                name = binding.etName.text.trim().toString(),
                province = "서울특별시",
                city = "중구",
                street = "세종대로 110",
                detailAddress = "서울특별시청",
                postalCode = "04524",
                createdAt = LocalDateTime.now().toString(),
            )

            lifecycleScope.launch(Dispatchers.IO) {
                val updatedRows = dataManager.updateHome(homeData)
                withContext(Dispatchers.Main) {
                    if(updatedRows > 0 && home?.serverId != null && home?.serverId != "") {
                        val homeDTO = HomeDTO(
                            name = binding.etName.text.trim().toString(),
                            province = "서울특별시",
                            city = "중구",
                            street = "세종대로 110",
                            detailAddress = "서울특별시청",
                            postalCode = "04524",
                        )

                        val response = RetrofitClient.apiService.updateHome("Bearer ${AppController.prefs.getToken()}", home!!.serverId!!, homeDTO)
                        if(response.isSuccessful) {
                            Log.d(TAG, "updateHome: ${response.body()}")
                        } else {
                            Log.e(TAG, "updateHome: $response")
                        }

                        Toast.makeText(requireActivity(), "수정되었습니다", Toast.LENGTH_SHORT).show()
                        replaceFragment1(requireActivity().supportFragmentManager, HomeFragment())
                    }else {
                        Toast.makeText(requireActivity(), "수정 실패하였습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return binding.root
    }
}