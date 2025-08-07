package com.aitronbiz.arron.screen.device

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.RoomDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddRoomScreen(
    homeId: String,
    navBack: () -> Unit
) {
    val context = LocalContext.current
    var roomName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // 상단 바
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navBack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "뒤로가기",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "룸 추가",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "룸 이름",
            fontSize = 15.sp,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = roomName,
            onValueChange = { roomName = it },
            placeholder = { Text("이름을 입력하세요", color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.Black,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.LightGray,
                placeholderColor = Color.Gray
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))

        // 저장 버튼
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .padding(bottom = 30.dp),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF184378)),
            elevation = CardDefaults.cardElevation(0.dp),
            onClick = {
                when {
                    roomName.trim().isEmpty() -> {
                        Toast.makeText(context, "룸 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                    }
                    homeId.isBlank() -> {
                        Toast.makeText(context, "홈 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        scope.launch {
                            val token = AppController.prefs.getToken()
                            val dto = RoomDTO(name = roomName.trim(), homeId = homeId)
                            val response = withContext(Dispatchers.IO) {
                                RetrofitClient.apiService.createRoom("Bearer $token", dto)
                            }
                            if (response.isSuccessful) {
                                Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                                navBack()
                            } else {
                                Toast.makeText(context, "저장 실패하였습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("저장", fontSize = 15.sp, color = Color.White)
            }
        }
    }
}