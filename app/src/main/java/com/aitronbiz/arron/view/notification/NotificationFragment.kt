package com.aitronbiz.arron.view.notification

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.adapter.DeviceListAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.SendNotificationDTO
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentNotificationBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.getIdFromJwtToken
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.device.DeviceSettingFragment
import com.aitronbiz.arron.view.home.MainFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationFragment : Fragment() {
    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var adapter: DeviceListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)

        initUI()

        return binding.root
    }

    private fun initUI() {
        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireContext())

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.tvTest.setOnClickListener {
            val userId = getIdFromJwtToken(AppController.prefs.getToken()!!)
            val fcmToken = AppController.prefs.getFcmToken()

            if (fcmToken.isNullOrBlank()) {
                Log.e(TAG, "저장된 FCM 토큰 없음")
            }else {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val request = SendNotificationDTO(
                            title = "테스트 알림",
                            body = "알림1",
                            data = null,
                            type = "general",
                            userId = userId!!
                        )

                        val response = RetrofitClient.apiService.sendNotification(
                            token = "Bearer ${AppController.prefs.getToken()}",
                            request = request
                        )
                        if (response.isSuccessful) {
                            Log.d(TAG, "알림 전송 성공: ${response.body()}")
                        } else {
                            Log.e(TAG, "알림 전송 실패: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "알림 전송 에러: ${e.message}")
                    }
                }
            }
        }

        val getDevices = dataManager.getDevices(1, 8)

        adapter = DeviceListAdapter(
            getDevices,
            onItemClick = { device ->
                val bundle = Bundle().apply {
                    putParcelable("device", device)
                }
                replaceFragment2(requireActivity().supportFragmentManager, DeviceSettingFragment(), bundle)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.adapter = adapter
    }
}