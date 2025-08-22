package com.aitronbiz.arron.screen.device

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Device
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.navigation.NavController
import com.aitronbiz.arron.api.dto.UpdateDeviceDTO

@Composable
fun EditDeviceScreen(
    navController: NavController,
    deviceId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var device by remember { mutableStateOf(Device()) }
    var name by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }

    // 기기 정보 불러오기
    LaunchedEffect(deviceId) {
        scope.launch(Dispatchers.IO) {
            try {
                val token = AppController.prefs.getToken()
                val response = RetrofitClient.apiService.getDevice("Bearer $token", deviceId)
                if (response.isSuccessful) {
                    device = response.body()?.device ?: Device()
                    name = device.name ?: ""
                    serial = device.serialNumber ?: ""
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
            .windowInsetsPadding(WindowInsets.statusBars)
            .pointerInput(Unit) {
                detectTapGestures {
                    keyboardController?.hide()
                }
            }
    ) {
        // 상단 바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            IconButton(onClick = {
                val popped = navController.popBackStack()
                if (!popped) navController.navigateUp()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "디바이스 수정",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .weight(1f)
        ) {
            // 이름
            Text("이름", fontSize = 14.sp, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("예: 아르온A", color = Color.LightGray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(color = Color.Transparent, shape = RoundedCornerShape(10.dp)),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White,
                    cursorColor = Color.White,
                    textColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 시리얼번호
            Text("시리얼 번호", fontSize = 14.sp, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = serial,
                onValueChange = { serial = it },
                placeholder = { Text("예: FD6EF9CA47B3", color = Color.LightGray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(color = Color.Transparent, shape = RoundedCornerShape(10.dp)),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White,
                    cursorColor = Color.White,
                    textColor = Color.White
                )
            )
        }

        // 수정 버튼
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
                                val dto = UpdateDeviceDTO(
                                    name = name,
                                    serialNumber = serial
                                )
                                val response = RetrofitClient.apiService.updateDevice(
                                    "Bearer $token",
                                    deviceId,
                                    dto
                                )
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        Log.d(TAG, "updateDevice: ${response.body()}")
                                        Toast.makeText(context, "수정되었습니다.", Toast.LENGTH_SHORT).show()
                                        navController.navigate("settingDevice/${deviceId}")
                                    } else {
                                        Toast.makeText(context, "수정 실패", Toast.LENGTH_SHORT).show()
                                        Log.e(TAG, "updateDevice: $response")
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
                .padding(horizontal = 20.dp)
                .height(45.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF184378))
        ) {
            Text("수정", color = Color.White, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

