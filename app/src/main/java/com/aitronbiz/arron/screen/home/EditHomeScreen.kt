package com.aitronbiz.arron.screen.home

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
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
import com.aitronbiz.arron.api.dto.HomeDTO
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditHomeScreen(
    homeId: String,
    navBack: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    var homeName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // 서버에서 홈 불러오기
    LaunchedEffect(homeId) {
        scope.launch(Dispatchers.IO) {
            try {
                val token = AppController.prefs.getToken()
                val response = RetrofitClient.apiService.getHome("Bearer $token", homeId)
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        homeName = response.body()?.home?.name ?: ""
                    }
                } else {
                    Log.e(TAG, "getHome: $response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getHome: $e")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(16.dp)
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
                text = "홈 수정",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 홈 이름 입력
        OutlinedTextField(
            value = homeName,
            onValueChange = { homeName = it },
            label = { Text("홈 이름") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.Gray,
                cursorColor = Color.White,
                textColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 수정 버튼
        Button(
            onClick = {
                when {
                    homeName.trim().isEmpty() -> {
                        Toast.makeText(context, "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val token = AppController.prefs.getToken()
                                val dto = HomeDTO(
                                    name = homeName.trim(),
                                    province = "서울특별시",
                                    city = "중구",
                                    street = "세종대로 110",
                                    detailAddress = "서울특별시청",
                                    postalCode = "04524",
                                )
                                val response = RetrofitClient.apiService.updateHome("Bearer $token", homeId, dto)
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "수정되었습니다.", Toast.LENGTH_SHORT).show()
                                        navController.navigate("homeList") {
                                            popUpTo("homeList") { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(context, "수정 실패", Toast.LENGTH_SHORT).show()
                                        Log.e(TAG, "updateHome: $response")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "updateHome: $e")
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF184378),
                contentColor = Color.White
            )
        ) {
            Text("수정", fontSize = 16.sp, color = Color.White)
        }
    }
}
