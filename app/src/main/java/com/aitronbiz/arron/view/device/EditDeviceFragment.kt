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
import com.aitronbiz.arron.databinding.FragmentEditDeviceBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditDeviceFragment : Fragment() {
    private var _binding: FragmentEditDeviceBinding? = null
    private val binding get() = _binding!!

    private var homeId: String? = ""
    private var roomId: String? = ""
    private var deviceId: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditDeviceBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        arguments?.let {
            homeId = it.getString("homeId")
            roomId = it.getString("roomId")
            deviceId = it.getString("deviceId")
        }

        val bundle = Bundle().apply {
            putString("homeId", homeId)
            putString("roomId", roomId)
            putString("deviceId", deviceId)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val getDevice = RetrofitClient.apiService.getDevice("Bearer ${AppController.prefs.getToken()}", deviceId!!)
            withContext(Dispatchers.Main) {
                if (getDevice.isSuccessful) {
                    binding.etName.setText(getDevice.body()!!.device.name)
                }else {
                    Log.e(TAG, "getDevice: $getDevice")
                }
            }
        }

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, SettingDeviceFragment(), bundle)
        }

        binding.btnEdit.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    val dto = DeviceDTO(
                        name = binding.etName.text.toString(),
                        roomId = roomId!!
                    )
                    val response = RetrofitClient.apiService.updateDevice("Bearer ${AppController.prefs.getToken()}", deviceId!!, dto)
                    if(response.isSuccessful) {
                        Log.d(TAG, "updateDevice: ${response.body()}")
                        Toast.makeText(context, "수정되었습니다", Toast.LENGTH_SHORT).show()
                        replaceFragment2(requireActivity().supportFragmentManager, SettingDeviceFragment(), bundle)
                    }else {
                        val errorBody = response.errorBody()?.string()
                        val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                        Log.e(TAG, "updateDevice: $errorResponse")
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