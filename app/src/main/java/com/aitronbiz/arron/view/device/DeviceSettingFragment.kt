package com.aitronbiz.arron.view.device

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.aitronbiz.arron.databinding.FragmentDeviceSettingBinding
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.room.SettingRoomFragment

class DeviceSettingFragment : Fragment() {
    private var _binding: FragmentDeviceSettingBinding? = null
    private val binding get() = _binding!!

    private var deviceData: Device? = null
    private var homeData: Home? = null
    private var subjectData: Subject? = null
    private var roomData: Room? = null
    private var currentLedBright: Float = 20f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding = FragmentDeviceSettingBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        arguments?.let {
            homeData = it.getParcelable("homeData")
            subjectData = it.getParcelable("subjectData")
            roomData = it.getParcelable("roomData")
            deviceData = it.getParcelable("deviceData")!!
        }

        val bundle = Bundle().apply {
            putParcelable("homeData", homeData)
            putParcelable("subjectData", subjectData)
            putParcelable("roomData", roomData)
        }

        if(deviceData!!.name != null && deviceData!!.name != "") binding.etDeviceName.setText(deviceData!!.name)
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
}