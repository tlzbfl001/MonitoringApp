package com.aitronbiz.arron.view.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.aitronbiz.arron.AppController
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentQrScanBinding
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDateTime

class QrScanFragment : Fragment() {
    private var _binding: FragmentQrScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var bundle = Bundle()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScanBinding.inflate(inflater, container, false)

        dataManager = DataManager(requireActivity())
        dataManager.open()

        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
        }else{
            binding.barcodeScanner.resume()
            binding.barcodeScanner.decodeSingle(callback)
        }

        binding.btnClose.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
        }

        return binding.root
    }

    private val callback = BarcodeCallback { result: BarcodeResult? ->
        if (result?.text == null) return@BarcodeCallback

        val rawValue = result.text
        binding.barcodeScanner.pause()

        val getSubject = dataManager.getSubject(1)
        if(getSubject.createdAt != "") {
            try {
                val data = JSONObject(rawValue)
                val productNumber = data.getString("0")
                val serialNumber = data.getString("1")

                val device = Device(
                    uid = 1,
                    subjectId = getSubject.id,
                    name = "",
                    productNumber = productNumber,
                    serialNumber = serialNumber,
                    createdAt = LocalDateTime.now().toString()
                )

                val success = dataManager.insertDevice(device)
                if (success) {
                    AppController.prefs.setStartActivityPrefs(LocalDateTime.now().toString()) // 활동 시작 시간 설정
                }

                replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
            }catch (error: JSONException) {
                Log.e(TAG, "JSON Parsing Error: $error")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScanner.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}