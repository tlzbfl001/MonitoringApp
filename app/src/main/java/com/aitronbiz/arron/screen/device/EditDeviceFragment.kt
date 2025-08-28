package com.aitronbiz.arron.screen.device

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.UpdateDeviceDTO
import com.aitronbiz.arron.api.response.Device
import com.aitronbiz.arron.databinding.FragmentEditDeviceBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditDeviceFragment : Fragment() {
    private var _binding: FragmentEditDeviceBinding? = null
    private val binding get() = _binding!!

    private var serialOverriddenByScan = false
    private val homeId: String  by lazy { arguments?.getString("homeId").orEmpty() }
    private val deviceId: String by lazy { arguments?.getString("deviceId").orEmpty() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val argName   = arguments?.getString("deviceName").orEmpty()
        val argSerial = arguments?.getString("deviceSerial").orEmpty()
        val fromScan  = arguments?.getBoolean("fromScan") == true

        binding.etName.setText(argName)
        binding.tvSerial.text = argSerial

        if (fromScan && argSerial.isNotBlank()) {
            serialOverriddenByScan = true
            Toast.makeText(requireContext(), "QR 스캔 완료", Toast.LENGTH_SHORT).show()
        }

        binding.btnBack.setOnClickListener { onBack() }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = onBack()
            }
        )

        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) hideKeyboard()
            false
        }

        // QR 스캐너 이동
        binding.btnScanQr.setOnClickListener {
            val curName   = binding.etName.text?.toString().orEmpty()
            val curSerial = binding.tvSerial.text?.toString().orEmpty()

            val bundle = Bundle().apply {
                putString("homeId", homeId)
                putString("deviceId", deviceId)
                putString("deviceName", curName)
                putString("deviceSerial", curSerial)
            }
            replaceFragment(
                fragmentManager = requireActivity().supportFragmentManager,
                fragment = QrScannerFragmentForEdit(),
                bundle = bundle
            )
        }

        loadDevice()

        binding.btnAdd.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim().orEmpty()
            val serial = binding.tvSerial.text?.toString()?.trim().orEmpty()

            when {
                name.isBlank()   -> Toast.makeText(requireContext(), "이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                serial.isBlank() -> Toast.makeText(requireContext(), "시리얼 번호를 입력하세요.", Toast.LENGTH_SHORT).show()
                else -> updateDevice(name, serial)
            }
        }
    }

    private fun loadDevice() {
        if (deviceId.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = AppController.prefs.getToken()
                val response = RetrofitClient.apiService.getDevice("Bearer $token", deviceId)
                if (response.isSuccessful) {
                    val device = response.body()?.device ?: Device()
                    withContext(Dispatchers.Main) {
                        if (binding.etName.text.isNullOrBlank()) {
                            binding.etName.setText(device.name)
                        }
                        if (!serialOverriddenByScan) {
                            binding.tvSerial.text = device.serialNumber
                        }
                    }
                } else {
                    Log.e(TAG, "getDevice: $response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getDevice error: $e")
            }
        }
    }

    private fun updateDevice(name: String, serial: String) {
        if (deviceId.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = AppController.prefs.getToken()
                val dto = UpdateDeviceDTO(name = name, serialNumber = serial)
                val response = RetrofitClient.apiService.updateDevice("Bearer $token", deviceId, dto)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "수정되었습니다.", Toast.LENGTH_SHORT).show()
                        onBack()
                    } else {
                        Toast.makeText(requireContext(), "수정 실패", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "updateDevice: $response")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateDevice error: ${e.message}")
            }
        }
    }

    private fun onBack() {
        val bundle = Bundle().apply {
            putString("deviceId", deviceId)
            putString("homeId", homeId)
        }
        replaceFragment(
            fragmentManager = requireActivity().supportFragmentManager,
            fragment = SettingDeviceFragment(),
            bundle = bundle
        )
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService<InputMethodManager>()
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
        binding.root.clearFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
