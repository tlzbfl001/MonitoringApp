package com.aitronbiz.arron.view.device

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.databinding.FragmentSettingDeviceBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.location
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
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
            if(deviceId != null) {
                val getDevice = RetrofitClient.apiService.getDevice("Bearer ${AppController.prefs.getToken()}", deviceId!!)
                if(getDevice.isSuccessful) {
                    val device = getDevice.body()!!.device
                    withContext(Dispatchers.Main) {
                        binding.tvTitle.text = device.name
                    }
                }else {
                    val errorBody = getDevice.errorBody()?.string()
                    val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                    Log.e(TAG, "getDevice: $errorResponse")
                }
            }
        }

        binding.btnBack.setOnClickListener {
            replaceFragment()
        }

        binding.btnSetting.setOnClickListener { view ->
            showPopupMenu(view)
        }

        return binding.root
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireActivity(), view)
        popupMenu.menuInflater.inflate(R.menu.setting_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.edit -> {
                    if(homeId != "" && roomId != "" && deviceId != "") {
                        val bundle = Bundle().apply {
                            putString("homeId", homeId)
                            putString("roomId", roomId)
                            putString("deviceId", deviceId)
                        }
                        replaceFragment2(requireActivity().supportFragmentManager, EditDeviceFragment(), bundle)
                    }
                    true
                }
                R.id.delete -> {
                    val dialog = AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setTitle("디바이스 삭제")
                        .setMessage("정말 삭제 하시겠습니까?")
                        .setPositiveButton("확인", null)
                        .setNegativeButton("취소", null)
                        .create()

                    dialog.setOnShowListener {
                        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        positiveButton.setOnClickListener {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val response = RetrofitClient.apiService.deleteDevice("Bearer ${AppController.prefs.getToken()}", deviceId!!)
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        Log.d(TAG, "deleteDevice: ${response.body()}")
                                        Toast.makeText(context, "삭제되었습니다", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss() // 명시적으로 닫기
                                        replaceFragment()
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                        Log.e(TAG, "deleteDevice: $errorResponse")
                                        Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }

                    dialog.show()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
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