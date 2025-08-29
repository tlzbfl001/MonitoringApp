package com.aitronbiz.arron.screen.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.aitronbiz.arron.R
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
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
                    showScannedFeedback(value)
                    returnResult(value)
                } else {
                    Toast.makeText(requireContext(), "QR을 인식할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "gallery scan error", e)
                Toast.makeText(requireContext(), "스캔 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_qr_scanner, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val homeId = arguments?.getString("homeId").orEmpty()

        // 뷰 참조
        barcodeView = view.findViewById(R.id.barcodeView)
        requireNotNull(barcodeView) { "DecoratedBarcodeView(R.id.barcodeView)를 레이아웃에 추가해주세요." }

        // 상태 텍스트 초기화
        barcodeView?.setStatusText("")

        // 화면 레이아웃 조정
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

        // 디코더/카메라 설정은 1회만
        val formats = listOf(BarcodeFormat.QR_CODE)
        barcodeView!!.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
        barcodeView!!.barcodeView.cameraSettings.isAutoFocusEnabled = true

        // 연속 디코딩 콜백
        barcodeView!!.decodeContinuous { result ->
            val txt = result?.text ?: return@decodeContinuous
            if (txt.isNotBlank()) {
                requireActivity().runOnUiThread {
                    if (scannedOnce) return@runOnUiThread
                    scannedOnce = true
                    showScannedFeedback(txt)
                    barcodeView?.pause()
                    returnResult(txt)
                }
            }
        }

        ensureCameraPermissionThenStart()
    }

    override fun onResume() {
        super.onResume()
        if (!scannedOnce && hasCameraPermission()) {
            barcodeView?.resume()
        }
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

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun ensureCameraPermissionThenStart() {
        if (hasCameraPermission()) startScanning()
        else requestCamera.launch(Manifest.permission.CAMERA)
    }

    private fun startScanning() {
        scannedOnce = false
        barcodeView?.resume()
    }

    @SuppressLint("MissingPermission")
    private fun showScannedFeedback(text: String) {
        barcodeView?.setStatusText("스캔: $text")
        Toast.makeText(requireContext(), "스캔: $text", Toast.LENGTH_SHORT).show()
        try {
            val vib = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) {
                vib.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(80)
            }
        } catch (_: Exception) { /* no-op */ }
    }

    private fun returnResult(value: String) {
        val homeId = arguments?.getString("homeId").orEmpty()
        val bundle = Bundle().apply {
            putString("homeId", homeId)
            putString("serial", value)
        }
        replaceFragment2(
            fragmentManager = requireActivity().supportFragmentManager,
            fragment = AddDeviceFragment(),
            bundle = bundle
        )
    }

    private fun dismissSelf() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }
}
