package com.aitronbiz.arron.view.device

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.aitronbiz.arron.databinding.FragmentDeviceSettingBinding
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar

class DeviceSettingFragment : Fragment() {
    private var _binding: FragmentDeviceSettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var device: Device
    private var currentLedBright: Float = 20f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding = FragmentDeviceSettingBinding.inflate(inflater, container, false)

        setupUI()

        return binding.root
    }

    private fun setupUI() {
        setStatusBar(requireActivity(), binding.mainLayout)

        arguments?.let {
            device = it.getParcelable("device")!!
        }

        binding.etDeviceName.setText(device.name)
        binding.ledBrightnessSlider.progress = currentLedBright.toInt()

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
        }

        binding.ledBrightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentLedBright = progress.toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnConfirm.setOnClickListener {

        }
    }
}