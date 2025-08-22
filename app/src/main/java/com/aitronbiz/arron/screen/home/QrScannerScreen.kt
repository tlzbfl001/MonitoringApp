package com.aitronbiz.arron.screen.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val bg = Color(0xFF0B2741)

    // 카메라 권한
    var hasCameraPermission by remember { mutableStateOf(false) }
    val requestCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = granted
        if (!granted) requestCamera.launch(Manifest.permission.CAMERA)
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(context.contentResolver, uri)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val intArray = IntArray(bmp.width * bmp.height)
                bmp.getPixels(intArray, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                val source = RGBLuminanceSource(bmp.width, bmp.height, intArray)
                val bitmap = BinaryBitmap(HybridBinarizer(source))
                val reader = MultiFormatReader().apply {
                    setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
                }
                val result = reader.decode(bitmap)
                val value = result.text
                if (!value.isNullOrBlank()) onScanned(value)
                else Toast.makeText(context, "QR을 인식할 수 없습니다.", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "스캔 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var torchOn by remember { mutableStateOf(false) }
    var scannedOnce by remember { mutableStateOf(false) }
    var barcodeViewRef by remember { mutableStateOf<DecoratedBarcodeView?>(null) }

    Box(Modifier.fillMaxSize().background(bg)) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    DecoratedBarcodeView(ctx).apply {
                        // QR 전용 디코더
                        decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
                        // 연속 스캔 콜백
                        decodeContinuous { result ->
                            val txt = result?.text
                            if (!txt.isNullOrBlank() && !scannedOnce) {
                                scannedOnce = true
                                // 스캔 멈추기
                                this.pause()
                                onScanned(txt)
                            }
                        }
                        // 시작
                        resume()
                        barcodeViewRef = this
                    }
                },
                update = { view ->
                    barcodeViewRef = view
                    // 플래시 상태 반영
                    try {
                        if (torchOn) view.setTorchOn() else view.setTorchOff()
                    } catch (_: Exception) { /* 일부 기기 토치 미지원 */ }
                }
            )

            // 상단 바
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp)
                    .align(Alignment.TopStart)
            ) {
                IconButton(onClick = {
                    // 종료 전 정리
                    barcodeViewRef?.pause()
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text("QR 스캔", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            // 안내 텍스트 + 둥근 사각 프레임
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("안내선에 QR을 맞춰주세요.", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Box(Modifier.size(240.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = Color.White,
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }

            // 하단 버튼: 플래시 / 갤러리
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = {
                        torchOn = !torchOn
                        try {
                            if (torchOn) barcodeViewRef?.setTorchOn() else barcodeViewRef?.setTorchOff()
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0x1AFFFFFF))
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Flash", tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                FilledIconButton(
                    onClick = { pickImage.launch("image/*") },
                    shape = RoundedCornerShape(24.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0x1AFFFFFF))
                ) {
                    Icon(Icons.Default.AccountBox, contentDescription = "Gallery", tint = Color.White)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("카메라 권한이 필요합니다.", color = Color.White)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { requestCamera.launch(Manifest.permission.CAMERA) }) {
                    Text("권한 허용")
                }
            }
        }
    }
}
