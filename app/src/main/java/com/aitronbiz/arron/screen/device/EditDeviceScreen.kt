package com.aitronbiz.arron.screen.device

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.DeviceDTO
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditDeviceScreen(
    deviceId: String,
    navBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }

    // 초기 로드: 기기 정보 불러오기
    LaunchedEffect(deviceId) {
        scope.launch(Dispatchers.IO) {
            try {
                val token = AppController.prefs.getToken()
                val response = RetrofitClient.apiService.getDevice("Bearer $token", deviceId)
                if (response.isSuccessful) {
                    val device = response.body()!!.device
                    withContext(Dispatchers.Main) {
                        name = device.name
                        version = device.version
                        modelName = device.modelName
                        product = device.modelNumber
                        serial = device.serialNumber
                    }
                } else {
                    Log.e(TAG, "getDevice: $response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getDevice: $e")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // 상단 바
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            androidx.compose.material.Text(
                text = "기기 수정",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            androidx.compose.material.Text(
                text = "뒤로",
                color = Color.White,
                modifier = Modifier.clickable { navBack() }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 이름
        androidx.compose.material.Text("이름", fontSize = 14.sp, color = Color.White)
        androidx.compose.material.OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { androidx.compose.material.Text("예: 아르온A", color = Color.LightGray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.LightGray,
                cursorColor = Color.White,
                textColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 버전
        androidx.compose.material.Text("버전", fontSize = 14.sp, color = Color.White)
        androidx.compose.material.OutlinedTextField(
            value = version,
            onValueChange = { version = it },
            placeholder = { androidx.compose.material.Text("예: 1.0.0", color = Color.LightGray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.LightGray,
                cursorColor = Color.White,
                textColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 모델명
        androidx.compose.material.Text("모델명", fontSize = 14.sp, color = Color.White)
        androidx.compose.material.OutlinedTextField(
            value = modelName,
            onValueChange = { modelName = it },
            placeholder = { androidx.compose.material.Text("예: ARRON 1", color = Color.LightGray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.LightGray,
                cursorColor = Color.White,
                textColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 제품번호
        androidx.compose.material.Text("제품번호", fontSize = 14.sp, color = Color.White)
        androidx.compose.material.OutlinedTextField(
            value = product,
            onValueChange = { product = it },
            placeholder = {
                androidx.compose.material.Text(
                    "예: ARRON-001",
                    color = Color.LightGray
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.LightGray,
                cursorColor = Color.White,
                textColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 시리얼번호
        androidx.compose.material.Text("시리얼 번호", fontSize = 14.sp, color = Color.White)
        androidx.compose.material.OutlinedTextField(
            value = serial,
            onValueChange = { serial = it },
            placeholder = {
                androidx.compose.material.Text(
                    "예: FD6EF9CA47B3",
                    color = Color.LightGray
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.LightGray,
                cursorColor = Color.White,
                textColor = Color.White
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        // 저장 버튼
        Button(
            onClick = {
                when {
                    name.isBlank() -> Toast.makeText(context, "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                    version.isBlank() -> Toast.makeText(context, "버전을 입력하세요", Toast.LENGTH_SHORT).show()
                    !version.matches(Regex("""^\d+\.\d+\.\d+$""")) -> Toast.makeText(context, "버전 형식이 올바르지 않습니다 (예: 1.0.0)", Toast.LENGTH_SHORT).show()
                    modelName.isBlank() -> Toast.makeText(context, "모델명을 입력하세요", Toast.LENGTH_SHORT).show()
                    product.isBlank() -> Toast.makeText(context, "제품 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                    serial.isBlank() -> Toast.makeText(context, "시리얼 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                    else -> {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val token = AppController.prefs.getToken()
                                val dto = DeviceDTO(
                                    name = name,
                                    version = version,
                                    modelName = modelName,
                                    modelNumber = product,
                                    serialNumber = serial
                                )
                                val response = RetrofitClient.apiService.updateDevice(
                                    "Bearer $token",
                                    deviceId,
                                    dto
                                )
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "수정되었습니다.", Toast.LENGTH_SHORT).show()
                                        navBack()
                                    } else {
                                        Toast.makeText(context, "수정 실패", Toast.LENGTH_SHORT).show()
                                        Log.e(TAG, "updateDevice: ${response.code()}")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error: ${e.message}")
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF184378))
        ) {
            androidx.compose.material.Text("수정", color = Color.White, fontSize = 15.sp)
        }
    }
}
