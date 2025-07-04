package com.aitronbiz.arron.view.device

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.*
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aitronbiz.arron.databinding.FragmentQrScanBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class QrScanFragment : Fragment() {
    private var _binding: FragmentQrScanBinding? = null
    private val binding get() = _binding!!

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var scanned = false
    private lateinit var overlayView: ScannerOverlayView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else Toast.makeText(requireContext(), "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScanBinding.inflate(inflater, container, false)
        overlayView = binding.scannerOverlay

        if(ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        return binding.root
    }

    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())
//
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//
//            val preview = Preview.Builder().build().also {
//                it.setSurfaceProvider(binding.previewView.surfaceProvider)
//            }
//
//            val imageAnalyzer = ImageAnalysis.Builder()
//                .setTargetResolution(android.util.Size(1280, 720))
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//                .also {
//                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer())
//                }
//
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            cameraProvider.unbindAll()
//            cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalyzer)
//        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    private inner class QRCodeAnalyzer : ImageAnalysis.Analyzer {
        private val scanner = BarcodeScanning.getClient()

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return
            }

            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(inputImage).addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val code = barcode.rawValue
                    val box = barcode.boundingBox
                    val overlayRect: Rect = overlayView.getScanRect()

                    Log.d(TAG, "QR 인식됨: $code")

                    if (!scanned && code != null && box != null &&
                        overlayRect.contains(box.centerX(), box.centerY())
                    ) {
                        scanned = true
                        fetchProductInfo(code)

                        Handler(Looper.getMainLooper()).postDelayed({
                            scanned = false
                        }, 3000)

                        break
                    }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "QR 인식 실패: ${it.localizedMessage}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
        }
    }

    private fun fetchProductInfo(productId: String) {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
