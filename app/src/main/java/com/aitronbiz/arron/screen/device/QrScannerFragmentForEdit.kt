package com.aitronbiz.arron.screen.device

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.aitronbiz.arron.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

class QrScannerFragmentForEdit : Fragment() {
    private var barcodeView: DecoratedBarcodeView? = null
    private var scannedOnce = false
    private var torchOn = false
    private var dismissed = false
    private var bottomNav: View? = null
    private var mainFrame: View? = null
    private var savedBottomToTop: Int = ConstraintLayout.LayoutParams.UNSET
    private var savedBottomToBottom: Int = ConstraintLayout.LayoutParams.UNSET

    companion object {
        private const val ARG_HOME_ID = "homeId"
        private const val ARG_DEVICE_ID = "deviceId"
        private const val ARG_DEVICE_NAME = "deviceName"
        private const val ARG_DEVICE_SERIAL = "deviceSerial"

        const val REQUEST_KEY = "qr_scan_request"
        const val BUNDLE_KEY_SERIAL = "serial"
        const val BUNDLE_KEY_CANCELED = "canceled"
        const val BUNDLE_KEY_HOME_ID = "homeId"
        const val BUNDLE_KEY_DEVICE_ID = "deviceId"
        const val BUNDLE_KEY_DEVICE_NAME = "deviceName"
        const val BUNDLE_KEY_DEVICE_SERIAL = "deviceSerial"
    }

    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startScanning()
        else {
            Toast.makeText(requireContext(), "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            sendCanceledAndPop()
        }
    }

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
                val bitmap = com.google.zxing.BinaryBitmap(HybridBinarizer(source))
                val reader = MultiFormatReader().apply {
                    setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
                }
                val result = reader.decode(bitmap)
                val value = result.text
                if (!value.isNullOrBlank()) returnResult(value)
                else Toast.makeText(requireContext(), "QR을 인식할 수 없습니다.", Toast.LENGTH_SHORT).show()
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
            sendCanceledAndPop()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    sendCanceledAndPop()
                }
            }
        )

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

    private fun baseArgs(): Bundle = Bundle().apply {
        putString(BUNDLE_KEY_HOME_ID, requireArguments().getString(ARG_HOME_ID).orEmpty())
        putString(BUNDLE_KEY_DEVICE_ID, requireArguments().getString(ARG_DEVICE_ID).orEmpty())
        putString(BUNDLE_KEY_DEVICE_NAME, requireArguments().getString(ARG_DEVICE_NAME).orEmpty())
        putString(BUNDLE_KEY_DEVICE_SERIAL, requireArguments().getString(ARG_DEVICE_SERIAL).orEmpty())
    }

    private fun sendCanceledAndPop() {
        if (dismissed) return
        dismissed = true
        barcodeView?.pause()
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            baseArgs().apply { putBoolean(BUNDLE_KEY_CANCELED, true) }
        )
        dismissSelf()
    }

    private fun returnResult(value: String) {
        if (dismissed) return
        dismissed = true
        barcodeView?.pause()
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            baseArgs().apply {
                putBoolean(BUNDLE_KEY_CANCELED, false)
                putString(BUNDLE_KEY_SERIAL, value)
            }
        )
        dismissSelf()
    }

    private fun dismissSelf() {
        parentFragmentManager.popBackStack()
    }
}