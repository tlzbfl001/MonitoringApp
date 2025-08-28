package com.aitronbiz.arron.screen.device

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.aitronbiz.arron.R
import com.aitronbiz.arron.util.CustomUtil.replaceFragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

class QrScannerFragment : Fragment() {
    private var barcodeView: DecoratedBarcodeView? = null
    private var scannedOnce = false
    private var torchOn = false
    private var bottomNav: View? = null
    private var mainFrame: View? = null
    private var savedBottomToTop: Int = ConstraintLayout.LayoutParams.UNSET
    private var savedBottomToBottom: Int = ConstraintLayout.LayoutParams.UNSET

    // 카메라 권한
    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startScanning()
        } else {
            Toast.makeText(requireContext(), "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            dismissSelf()
        }
    }

    // 갤러리에서 QR 이미지 선택
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp: Bitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(requireContext().contentResolver, uri)
                )
                val intArray = IntArray(bmp.width * bmp.height)
                bmp.getPixels(intArray, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                val source = RGBLuminanceSource(bmp.width, bmp.height, intArray)
                val bitmap = BinaryBitmap(HybridBinarizer(source))
                val reader = MultiFormatReader().apply {
                    setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
                }
                val result = reader.decode(bitmap)
                val value = result.text
                if (!value.isNullOrBlank()) {
                    returnResult(value)
                } else {
                    Toast.makeText(requireContext(), "QR을 인식할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "스캔 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_qr_scanner, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        barcodeView = view.findViewById(R.id.barcodeView)
        barcodeView?.setStatusText("")

        val statusId = resources.getIdentifier("zxing_status_view", "id", "com.journeyapps.barcodescanner")
        if (statusId != 0) view.findViewById<TextView>(statusId)?.visibility = View.GONE

        bottomNav = requireActivity().findViewById(R.id.navigation)
        mainFrame = requireActivity().findViewById(R.id.mainFrame)

        (mainFrame?.layoutParams as? ConstraintLayout.LayoutParams)?.let { lp ->
            savedBottomToTop = lp.bottomToTop
            savedBottomToBottom = lp.bottomToBottom
        }
        bottomNav?.visibility = View.GONE
        mainFrame?.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomToTop = ConstraintLayout.LayoutParams.UNSET
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            barcodeView?.pause()
            dismissSelf()
        }

        view.findViewById<ConstraintLayout>(R.id.btnFlash).setOnClickListener {
            torchOn = !torchOn
            try {
                if (torchOn) barcodeView?.setTorchOn() else barcodeView?.setTorchOff()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "플래시를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<ConstraintLayout>(R.id.btnGallery).setOnClickListener {
            pickImage.launch("image/*")
        }

        ensureCameraPermissionThenStart()
    }

    override fun onResume() {
        super.onResume()
        if (!scannedOnce) barcodeView?.resume()
    }

    override fun onPause() {
        barcodeView?.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainFrame?.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomToTop = savedBottomToTop
            bottomToBottom = savedBottomToBottom
        }
        bottomNav?.visibility = View.VISIBLE
        bottomNav = null
        mainFrame = null
        barcodeView = null
    }

    private fun ensureCameraPermissionThenStart() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) startScanning() else requestCamera.launch(Manifest.permission.CAMERA)
    }

    private fun startScanning() {
        scannedOnce = false
        barcodeView?.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        barcodeView?.decodeContinuous { result ->
            val txt = result?.text
            if (!txt.isNullOrBlank() && !scannedOnce) {
                scannedOnce = true
                barcodeView?.pause()
                returnResult(txt)
            }
        }
        barcodeView?.resume()
    }

    private fun returnResult(value: String) {
        val homeId = arguments?.getString("homeId").orEmpty()
        val bundle = Bundle().apply {
            putString("homeId", homeId)
            putString("serial", value)
        }
        replaceFragment(
            fragmentManager = requireActivity().supportFragmentManager,
            fragment = AddDeviceFragment(),
            bundle = bundle
        )
    }

    private fun dismissSelf() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }
}
