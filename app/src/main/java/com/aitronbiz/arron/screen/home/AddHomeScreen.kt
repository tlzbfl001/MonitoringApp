package com.aitronbiz.arron.screen.home

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.aitronbiz.arron.api.dto.HomeDTO2
import com.aitronbiz.arron.model.AddressResult
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddHomeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var homeName by remember { mutableStateOf("") }

    // 주소 상태
    var province by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var street by rememberSaveable { mutableStateOf("") }
    var fullAddress by rememberSaveable { mutableStateOf("") }
    var postalCode by rememberSaveable { mutableStateOf("") }

    // SearchAddressScreen에서 돌아온 값 수신
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val addressResult = savedStateHandle
        ?.getStateFlow<AddressResult?>("address_result", null)
        ?.collectAsState(initial = null)
        ?.value

    // 전달값
    LaunchedEffect(addressResult) {
        addressResult?.let { r ->
            province = r.province
            city = r.city
            street = r.street
            fullAddress = r.fullAddress
            postalCode = r.postalCode
            Log.d(TAG, "HOME <- province=$province, city=$city, street=$street, full=$fullAddress, postal=$postalCode")
            savedStateHandle?.remove<AddressResult>("address_result")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .pointerInput(Unit) { detectTapGestures { keyboardController?.hide() } }
    ) {
        // 상단 바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
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
            Spacer(Modifier.width(4.dp))
            Text("홈 추가", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(20.dp))

        Text("홈 이름", color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(start = 20.dp))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = homeName,
            onValueChange = { if (it.length <= 10) homeName = it },
            placeholder = { Text("홈 이름을 입력하세요", color = Color.LightGray) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 20.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White,
                cursorColor = Color.White,
                textColor = Color.White,
                placeholderColor = Color.LightGray
            )
        )

        Spacer(Modifier.height(20.dp))

        // 주소 섹션
        Text("주소", color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(start = 20.dp))
        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 우편번호
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    placeholder = { Text("우편번호", color = Color.LightGray) },
                    singleLine = true,
                    modifier = Modifier.width(140.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        disabledBorderColor = Color.White.copy(alpha = 0.4f),
                        disabledTextColor = Color.White,
                        disabledPlaceholderColor = Color.LightGray,
                        disabledLabelColor = Color.LightGray
                    )
                )
                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        navController.navigate("searchAddress/0")
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF184378),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("주소 검색") }
            }

            Spacer(Modifier.height(12.dp))

            // 상세주소
            OutlinedTextField(
                value = fullAddress,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                placeholder = { Text("상세주소", color = Color.LightGray) },
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    disabledBorderColor = Color.White.copy(alpha = 0.4f),
                    disabledTextColor = Color.White,
                    disabledPlaceholderColor = Color.LightGray,
                    disabledLabelColor = Color.LightGray
                )
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                if (homeName.trim().isEmpty()) {
                    Toast.makeText(context, "홈 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (fullAddress.isBlank()) {
                    Toast.makeText(context, "주소를 검색해 선택하세요.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                scope.launch(Dispatchers.IO) {
                    try {
                        if(postalCode != "") {
                        }else {
                            val dto = HomeDTO2(
                                name        = homeName.trim(),
                                province    = province,
                                city        = city,
                                street      = street,
                                detailAddress = fullAddress
                            )

                            val response = RetrofitClient.apiService.createHome2(
                                "Bearer ${AppController.prefs.getToken()}",
                                dto
                            )
                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    Log.d(TAG, "createHome: ${response.body()}")
                                    Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                                    val popped = navController.popBackStack()
                                    if (!popped) navController.navigateUp()
                                }else {
                                    Log.e(TAG, "createHome: $response")
                                    Toast.makeText(context, "저장 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "에러 발생: ${e.message}", Toast.LENGTH_SHORT).show()
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
                backgroundColor = Color(0xFF184378),
                contentColor = Color.White
            )
        ) { Text("저장", color = Color.White, fontSize = 16.sp) }

        Spacer(Modifier.height(20.dp))
    }
}
