package com.aitronbiz.arron.screen.device

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingDeviceScreen(
    homeId: String,
    deviceId: String,
    navController: NavController,
    navBack: () -> Unit
) {
    val context = LocalContext.current
    var deviceName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    // 데이터 로드
    LaunchedEffect(homeId) {
        withContext(Dispatchers.IO) {
            try {
                val getDevice = RetrofitClient.apiService.getDevice("Bearer ${AppController.prefs.getToken()}", deviceId)
                withContext(Dispatchers.Main) {
                    if(getDevice.isSuccessful) {
                        deviceName = getDevice.body()?.device?.name ?: ""
                    }else {
                        Log.e(TAG, "getDevice: ${getDevice.code()}")
                    }
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
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // 상단 바
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navBack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = deviceName.ifBlank { "디바이스" },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu),
                        contentDescription = "메뉴",
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    offset = DpOffset(x = (-20).dp, y = 0.dp),
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("디바이스 수정", color = Color.Black) },
                        onClick = {
                            showMenu = false
                            navController.navigate("editDevice/$deviceId")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("디바이스 삭제", color = Color.Red) },
                        onClick = {
                            showMenu = false
                            scope.launch {
                                val token = AppController.prefs.getToken()
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService.deleteDevice("Bearer $token", deviceId)
                                }
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                    navBack()
                                } else {
                                    Log.e(TAG, "deleteDevice: $response")
                                    Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
