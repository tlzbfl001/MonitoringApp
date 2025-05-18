package com.aitronbiz.aitron.view.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import com.aitronbiz.aitron.databinding.FragmentDeviceSettingBinding
import com.aitronbiz.aitron.entity.Device
import com.aitronbiz.aitron.util.CustomUtil.replaceFragment1

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

        arguments?.let {
            device = it.getParcelable("device")!!
        }

        setupViews()

        return binding.root
    }
    private fun setupViews() {
        binding.apply {
            etDeviceName.setText(device.name)
            ledBrightnessSlider.progress = currentLedBright.toInt()

            btnBack.setOnClickListener {
                replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
            }

            ledBrightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    currentLedBright = progress.toFloat()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            autoOnButton.setOnClickListener {
                // TODO: 시간 선택 다이얼로그
            }

            autoOffButton.setOnClickListener {
                // TODO: 시간 선택 다이얼로그
            }

            sensitivityButton.setOnClickListener {
                // TODO: 민감도 선택 팝업
            }

            btnConfirm.setOnClickListener {
                updateDevice()
            }

            btnDelete.setOnClickListener {
                showDeleteConfirmation()
            }
        }
    }

    private fun updateDevice() {
        // 기기 업데이트 로직
        device.name = binding.etDeviceName.text.toString()
        // 여기에 DB 업데이트 코드 추가
        Toast.makeText(context, "기기 업데이트 완료", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmation() {
        // 기기 삭제 확인 다이얼로그
        // 여기에 삭제 확인 코드 추가
        Toast.makeText(context, "기기 삭제 확인", Toast.LENGTH_SHORT).show()
    }
}