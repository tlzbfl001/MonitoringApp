package com.aitronbiz.arron.screen.device

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import com.aitronbiz.arron.api.dto.RoomDTO

@Composable
fun AddDeviceScreen(
    homeId: String,
    navBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("") }
    var roomName by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }

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
            Text(
                text = "기기 추가",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "뒤로",
                color = Color.White,
                modifier = Modifier.clickable { navBack() }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 이름
        Text("이름", fontSize = 14.sp, color = Color.White)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("예: 아르온A", color = Color.LightGray) },
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
        Text("버전", fontSize = 14.sp, color = Color.White)
        OutlinedTextField(
            value = version,
            onValueChange = { version = it },
            placeholder = { Text("예: 1.0.0", color = Color.LightGray) },
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

        // 장소
        Text("장소", fontSize = 14.sp, color = Color.White)
        OutlinedTextField(
            value = roomName,
            onValueChange = { roomName = it },
            placeholder = { Text("예: 거실, 안방", color = Color.LightGray) },
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
        Text("모델명", fontSize = 14.sp, color = Color.White)
        OutlinedTextField(
            value = modelName,
            onValueChange = { modelName = it },
            placeholder = { Text("예: ARRON 1", color = Color.LightGray) },
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
        Text("제품번호", fontSize = 14.sp, color = Color.White)
        OutlinedTextField(
            value = product,
            onValueChange = { product = it },
            placeholder = { Text("예: ARRON-001", color = Color.LightGray) },
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
        Text("시리얼 번호", fontSize = 14.sp, color = Color.White)
        OutlinedTextField(
            value = serial,
            onValueChange = { serial = it },
            placeholder = { Text("예: FD6EF9CA47B3", color = Color.LightGray) },
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
                    roomName.isBlank() -> Toast.makeText(context, "장소를 입력하세요", Toast.LENGTH_SHORT).show()
                    modelName.isBlank() -> Toast.makeText(context, "모델명을 입력하세요", Toast.LENGTH_SHORT).show()
                    product.isBlank() -> Toast.makeText(context, "제품 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                    serial.isBlank() -> Toast.makeText(context, "시리얼 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                    homeId.isBlank() -> Toast.makeText(context, "홈 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    else -> {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val roomDTO = RoomDTO(
                                    name = roomName,
                                    homeId = homeId
                                )
                                val createRoom = RetrofitClient.apiService.createRoom(
                                    "Bearer ${AppController.prefs.getToken()}",
                                    roomDTO
                                )

                                withContext(Dispatchers.Main) {
                                    if (createRoom.isSuccessful) {
                                        val dto = DeviceDTO(
                                            name = name,
                                            version = version,
                                            modelName = modelName,
                                            modelNumber = product,
                                            serialNumber = serial
                                        )
                                        val response = RetrofitClient.apiService.createDevice(
                                            "Bearer ${AppController.prefs.getToken()}",
                                            dto
                                        )
                                        withContext(Dispatchers.Main) {
                                            if (response.isSuccessful) {
                                                Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                                                navBack()
                                            } else {
                                                Log.e(TAG, "createDevice: $response")
                                                Toast.makeText(context, "저장 실패", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "createRoom: $createRoom")
                                        Toast.makeText(context, "저장 실패", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }catch(e: Exception) {
                                Log.e(TAG, "${e.message}")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "에러 발생", Toast.LENGTH_SHORT).show()
                                }
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
            Text("저장", color = Color.White, fontSize = 15.sp)
        }
    }
}
