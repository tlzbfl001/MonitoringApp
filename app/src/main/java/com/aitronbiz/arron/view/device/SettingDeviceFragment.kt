package com.aitronbiz.arron.view.device

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.databinding.FragmentSettingDeviceBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.room.SettingRoomFragment
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingDeviceFragment : Fragment() {
    private var _binding: FragmentSettingDeviceBinding? = null
    private val binding get() = _binding!!

    private var homeId: String? = ""
    private var roomId: String? = ""
    private var deviceId: String? = ""
    private var currentLedBright: Float = 20f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding = FragmentSettingDeviceBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        arguments?.let {
            homeId = it.getString("homeId")
            roomId = it.getString("roomId")
            deviceId = it.getString("deviceId")
        }

        val bundle = Bundle().apply {
            putString("homeId", homeId)
            putString("roomId", roomId)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            if(deviceId != null) {
                val getDevice = RetrofitClient.apiService.getDevice("Bearer ${AppController.prefs.getToken()}", deviceId!!)
                if(getDevice.isSuccessful) {
                    val device = getDevice.body()!!.device
                    withContext(Dispatchers.Main) {
                        binding.tvTitle.text = device.name
                        binding.etDeviceName.setText(device.name)
                    }
                }else {
                    val errorBody = getDevice.errorBody()?.string()
                    val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                    Log.e(TAG, "getDevice: $errorResponse")
                }
            }
        }

        binding.ledBrightnessSlider.progress = currentLedBright.toInt()

        binding.btnBack.setOnClickListener {
            replaceFragment2(requireActivity().supportFragmentManager, SettingRoomFragment(), bundle)
        }

        binding.ledBrightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentLedBright = progress.toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.hideBottomNav()
    }
}