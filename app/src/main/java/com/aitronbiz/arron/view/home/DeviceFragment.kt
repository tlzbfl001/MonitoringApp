package com.aitronbiz.arron.view.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentDeviceBinding
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2

class DeviceFragment : Fragment() {
    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var bundle = Bundle()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceBinding.inflate(inflater, container, false)

        dataManager = DataManager(requireActivity())
        dataManager.open()

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, SettingsFragment())
        }

        binding.btnAddDevice.setOnClickListener {
            val dialog = BottomSheetDialog(requireContext())
            val sheetView = layoutInflater.inflate(R.layout.dialog_add_device, null)
            dialog.setContentView(sheetView)

            val btnOption1 = sheetView.findViewById<Button>(R.id.buttonOption1)
            val btnOption2 = sheetView.findViewById<Button>(R.id.buttonOption2)

            btnOption1.setOnClickListener {
                replaceFragment1(requireActivity().supportFragmentManager, AddDeviceFragment())
                dialog.dismiss()
            }

            btnOption2.setOnClickListener {
                replaceFragment1(requireActivity().supportFragmentManager, QrScanFragment())
                dialog.dismiss()
            }

            dialog.show()
        }

        binding.deviceList.setOnClickListener {
            bundle.putParcelable("device", Device(uid = 1, subjectId = 1, name = "test", productNumber = "11", serialNumber = "11", createdAt = ""))
            replaceFragment2(requireActivity().supportFragmentManager, DeviceSettingFragment(), bundle)
        }

        return binding.root
    }
}