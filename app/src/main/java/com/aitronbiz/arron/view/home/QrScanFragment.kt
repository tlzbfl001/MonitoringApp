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
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentQrScanBinding
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QrScanFragment : Fragment() {
    private var _binding: FragmentQrScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScanBinding.inflate(inflater, container, false)

        dataManager = DataManager.getInstance(requireContext())

        // 카메라 권한 확인
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
        } else {
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

        // 데이터 처리 및 디바이스 추가
        handleQRCodeData(rawValue)
    }

    private fun handleQRCodeData(rawValue: String) {
        // 비동기 처리
        lifecycleScope.launch(Dispatchers.IO) {
            val getSubject = dataManager.getSubject(1)

            // 대상자가 존재하는 경우에만 처리
            if (getSubject.createdAt!!.isNotEmpty()) {
                try {
                    // QR 코드 데이터 파싱
                    val data = JSONObject(rawValue)
                    val productNumber = data.getString("0")
                    val serialNumber = data.getString("1")

                    // 새로운 Device 객체 생성
                    val device = Device(
                        uid = AppController.prefs.getUID(),
                        subjectId = getSubject.id,
                        name = "",  // 디바이스 이름은 추가 후 처리 가능
                        productNumber = productNumber,
                        serialNumber = serialNumber,
                        createdAt = LocalDateTime.now().toString()
                    )

                    val success = dataManager.insertDevice(device)

                    // DB 처리 후 UI 갱신
                    withContext(Dispatchers.Main) {
                        if (success) {
                            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
                        } else {
                            Log.e(TAG, "디바이스 추가 실패")
                        }
                    }
                } catch (error: JSONException) {
                    Log.e(TAG, "JSON Parsing Error: $error")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeScanner.resume() // 스캔 재개
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScanner.pause() // 스캔 일시 정지
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}