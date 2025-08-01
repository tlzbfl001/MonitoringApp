package com.aitronbiz.arron.view.device

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.databinding.FragmentSettingDeviceBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.location
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.room.SettingRoomFragment
import com.aitronbiz.arron.view.setting.SettingsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingDeviceFragment : Fragment() {
    private var _binding: FragmentSettingDeviceBinding? = null
    private val binding get() = _binding!!

    private var homeId: String? = ""
    private var roomId: String? = ""
    private var deviceId: String? = ""

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

        lifecycleScope.launch(Dispatchers.IO) {
            if(deviceId != "") {
                val getDevice = RetrofitClient.apiService.getDevice("Bearer ${AppController.prefs.getToken()}", deviceId!!)
                if(getDevice.isSuccessful) {
                    val device = getDevice.body()!!.device
                    withContext(Dispatchers.Main) {
                        binding.tvTitle.text = device.name
                    }
                }else {
                    Log.e(TAG, "getDevice: ${getDevice.code()}")
                }
            }else {
                withContext(Dispatchers.Main) {
                    binding.tvTitle.text = "기기 불러오기 실패"
                }
            }
        }

        binding.btnBack.setOnClickListener {
            replaceFragment()
        }

        binding.btnSetting.setOnClickListener { view ->
            showCustomPopupWindow(view)
        }

        return binding.root
    }

    private fun showCustomPopupWindow(anchor: View) {
        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.popup_delete_layout, null)

        val popupWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 180f, anchor.resources.displayMetrics
        ).toInt()

        val screenWidth = resources.displayMetrics.widthPixels
        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val anchorX = anchorLocation[0]

        // anchor 기준 팝업이 화면을 넘지 않도록 왼쪽으로 offset 계산
        val offsetX = if (anchorX + popupWidth > screenWidth) {
            screenWidth - (anchorX + popupWidth) - 20 // -20은 추가 margin
        } else {
            -20 // 기본 왼쪽 offset
        }

        val popupWindow = PopupWindow(popupView, popupWidth, WindowManager.LayoutParams.WRAP_CONTENT, true)
        popupWindow.elevation = 10f
        popupWindow.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        popupWindow.isOutsideTouchable = true

        popupWindow.showAsDropDown(anchor, offsetX, 0)

        popupView.findViewById<TextView>(R.id.menuDevice).setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
            popupWindow.dismiss()
        }

        popupView.findViewById<TextView>(R.id.menuSetting).setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, SettingsFragment())
            popupWindow.dismiss()
        }
    }

    private fun replaceFragment() {
        val bundle = Bundle().apply {
            putString("homeId", homeId)
            putString("roomId", roomId)
        }
        if(location == 1) {
            replaceFragment2(requireActivity().supportFragmentManager, SettingRoomFragment(), bundle)
        }else {
            replaceFragment2(requireActivity().supportFragmentManager, DeviceFragment(), bundle)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.hideBottomNav()
    }
}