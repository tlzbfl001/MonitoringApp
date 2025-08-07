package com.aitronbiz.arron.screen.device

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.UpdateRoomDTO
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditRoomScreen(
    homeId: String,
    roomId: String,
    navBack: () -> Unit
) {
    val context = LocalContext.current
    var roomName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // 룸 정보 불러오기
    LaunchedEffect(roomId) {
        scope.launch(Dispatchers.IO) {
            try {
                val token = AppController.prefs.getToken()
                val response = RetrofitClient.apiService.getRoom("Bearer $token", roomId)
                if (response.isSuccessful) {
                    val name = response.body()?.room?.name ?: ""
                    withContext(Dispatchers.Main) {
                        roomName = name
                    }
                } else {
                    Log.e(TAG, "getRoom: $response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getRoom: $e")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단바
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { navBack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "뒤로가기",
                    tint = Color.White
                )
            }
            Text(
                text = "룸 수정",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(24.dp))
        }

        Spacer(modifier = Modifier.height(30.dp))

        // 입력 필드
        Text(
            text = "룸 이름",
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = roomName,
            onValueChange = { roomName = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("룸 이름을 입력하세요") },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.LightGray,
                textColor = Color.Black,
                cursorColor = Color.White,
                placeholderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        // 수정 버튼
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp),
            shape = CardDefaults.shape,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF184378)),
            elevation = CardDefaults.cardElevation(0.dp),
            onClick = {
                if (roomName.isBlank()) {
                    Toast.makeText(context, "이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val token = AppController.prefs.getToken()
                            val dto = UpdateRoomDTO(name = roomName)
                            val response = RetrofitClient.apiService.updateRoom("Bearer $token", roomId, dto)
                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "수정되었습니다.", Toast.LENGTH_SHORT).show()
                                    navBack()
                                } else {
                                    Log.e(TAG, "updateRoom: $response")
                                    Toast.makeText(context, "수정 실패하였습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "updateRoom: $e")
                        }
                    }
                }
            }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("수정", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}