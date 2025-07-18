package com.aitronbiz.arron.view.device

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.aitronbiz.arron.databinding.FragmentSettingDeviceBinding
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.room.SettingRoomFragment

class SettingDeviceFragment : Fragment() {
    private var _binding: FragmentSettingDeviceBinding? = null
    private val binding get() = _binding!!

    private var home: Home? = null
    private var room: Room? = null
    private var device: Device? = null
    private var currentLedBright: Float = 20f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding = FragmentSettingDeviceBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        arguments?.let {
            home = it.getParcelable("home")
            room = it.getParcelable("room")
            device = it.getParcelable("device")
        }

        val bundle = Bundle().apply {
            putParcelable("home", home)
            putParcelable("room", room)
        }

        if(device!!.name != null && device!!.name != "") binding.etDeviceName.setText(device!!.name)
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