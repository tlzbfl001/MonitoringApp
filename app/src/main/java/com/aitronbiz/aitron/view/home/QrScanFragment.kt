package com.aitronbiz.aitron.view.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.aitronbiz.aitron.database.DataManager
import com.aitronbiz.aitron.databinding.FragmentQrScanBinding
import com.aitronbiz.aitron.entity.Device
import com.aitronbiz.aitron.entity.EnumData
import com.aitronbiz.aitron.util.CustomUtil.TAG
import com.aitronbiz.aitron.util.CustomUtil.replaceFragment1
import com.aitronbiz.aitron.util.CustomUtil.replaceFragment2
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDateTime

class QrScanFragment : Fragment() {
    private var _binding: FragmentQrScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var bundle = Bundle()
    private var resultType = EnumData.UNKNOWN

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
                    resultType = EnumData.DONE
                }

                when(resultType) {
                    EnumData.DONE -> bundle.putString("resultType", EnumData.DONE.name)
                    EnumData.NO_SUBJECT -> bundle.putString("resultType", EnumData.NO_SUBJECT.name)
                    EnumData.INVALID_NUMBER -> bundle.putString("resultType", EnumData.INVALID_NUMBER.name)
                    EnumData.UNKNOWN -> bundle.putString("resultType", EnumData.UNKNOWN.name)
                    else -> bundle.putString("resultType", EnumData.UNKNOWN.name)
                }

                replaceFragment2(requireActivity().supportFragmentManager, DeviceEnrollResultFragment(), bundle)
            }catch (error: JSONException) {
                Log.e(TAG, "JSON Parsing Error: $error")
                resultType = EnumData.UNKNOWN
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