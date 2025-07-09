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
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentNotificationBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.requestPushNotification
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.device.DeviceSettingFragment
import com.aitronbiz.arron.view.home.MainFragment

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
            val fcmToken = AppController.prefs.getFcmToken()
            if (fcmToken.isNullOrBlank()) {
                Log.e(TAG, "저장된 FCM 토큰 없음")
            }else {
                requestPushNotification(
                    fcmToken,
                    "title1",
                    "content1"
                )
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