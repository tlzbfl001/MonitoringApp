package com.aitronbiz.arron.screen.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.HomeDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddHomeScreen(
    navBack: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    var homeName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // 상단 바
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                navController.navigate("homeList") {
                    popUpTo("homeList") { inclusive = true }
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "뒤로가기",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "홈 추가",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "홈 이름",
            color = Color.White,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = homeName,
            onValueChange = { if (it.length <= 10) homeName = it },
            placeholder = { Text("홈 이름을 입력하세요", color = Color.LightGray) },
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF184378),
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.White,
                textColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (homeName.trim().isEmpty()) {
                    Toast.makeText(context, "홈 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val dto = HomeDTO(
                                name = homeName.trim(),
                                province = "서울특별시",
                                city = "중구",
                                street = "세종대로 110",
                                detailAddress = "서울특별시청",
                                postalCode = "04524",
                            )
                            val response = RetrofitClient.apiService.createHome(
                                "Bearer ${AppController.prefs.getToken()}",
                                dto
                            )
                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                                    navBack()
                                } else {
                                    Toast.makeText(context, "저장 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "에러 발생: ${e.message}", Toast.LENGTH_SHORT).show()
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
            Text("저장", color = Color.White, fontSize = 16.sp)
        }
    }
}
