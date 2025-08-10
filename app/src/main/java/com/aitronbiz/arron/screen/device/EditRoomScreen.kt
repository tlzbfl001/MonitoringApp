package com.aitronbiz.arron.screen.device

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.HomeDTO
import com.aitronbiz.arron.api.dto.UpdateRoomDTO
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditRoomScreen(
    roomId: String,
    navController: NavController
) {
    val context = LocalContext.current
    var roomName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

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
            .windowInsetsPadding(WindowInsets.statusBars)
            .pointerInput(Unit) {
                detectTapGestures {
                    keyboardController?.hide()
                }
            }
    ) {
        // 상단바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            androidx.compose.material3.IconButton(onClick = {
                val popped = navController.popBackStack()
                if (!popped) navController.navigateUp()
            }) {
                androidx.compose.material3.Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "장소 수정",
                fontSize = 18.sp,
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
            Text(
                text = "장소 이름",
                color = Color.White,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = roomName,
                onValueChange = { roomName = it },
                singleLine = true,
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
        androidx.compose.material3.Button(
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
                                    Log.d(TAG, "updateRoom: ${response.body()}")
                                    Toast.makeText(context, "수정되었습니다.", Toast.LENGTH_SHORT).show()
                                    val popped = navController.popBackStack()
                                    if (!popped) navController.navigateUp()
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
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF184378),
                contentColor = Color.White
            )
        ) {
            Text("수정", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}