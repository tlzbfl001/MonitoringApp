package kr.aitron.aitron.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import kr.aitron.aitron.MainViewModel
import kr.aitron.aitron.databinding.FragmentQrScanBinding
import kr.aitron.aitron.database.entity.EnumData
import kr.aitron.aitron.util.CustomUtil.replaceFragment2

class QrScanFragment : Fragment() {
    private var _binding: FragmentQrScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private var bundle = Bundle()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScanBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
        }else{
            binding.barcodeScanner.resume()
            binding.barcodeScanner.decodeSingle(callback)
        }

        viewModel.handleScannedQRResult.observe(viewLifecycleOwner, Observer { resultType ->
            handleEnrollResult(resultType)
            viewModel.resetInsertState()
        })

        return binding.root
    }

    private val callback = BarcodeCallback { result: BarcodeResult? ->
        if (result?.text == null) return@BarcodeCallback

        val rawValue = result.text
        binding.barcodeScanner.pause()

        viewModel.getSubjectByUid(1)

        viewModel.subjectLiveData.observe(viewLifecycleOwner, Observer { data ->
            if(data != null) {
                viewModel.handleScannedQRCode(rawValue, data.id, 1)
                viewModel.resetInsertState()
            }
        })
    }

    private fun handleEnrollResult(resultType: EnumData) {
        when(resultType) {
            EnumData.DONE -> bundle.putString("resultType", EnumData.DONE.name)
            EnumData.NO_SUBJECT -> bundle.putString("resultType", EnumData.NO_SUBJECT.name)
            EnumData.INVALID_NUMBER -> bundle.putString("resultType", EnumData.INVALID_NUMBER.name)
            EnumData.UNKNOWN -> bundle.putString("resultType", EnumData.UNKNOWN.name)
        }

        replaceFragment2(requireActivity().supportFragmentManager, DeviceEnrollResultFragment(), bundle)
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