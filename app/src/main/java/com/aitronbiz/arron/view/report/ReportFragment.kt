package com.aitronbiz.arron.view.report

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentReportBinding
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.device.DeviceFragment

class ReportFragment : Fragment() {
    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        binding.btnHeartRate.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, HeartRateFragment())
        }

        return binding.root
    }
}