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
import com.aitronbiz.arron.api.response.Device
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingDeviceScreen(
    deviceId: String,
    navController: NavController,
    navBack: () -> Unit
) {
    val context = LocalContext.current
    var device by remember { mutableStateOf(Device()) }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    // 데이터 로드
    LaunchedEffect(deviceId) {
        withContext(Dispatchers.IO) {
            try {
                val getDevice = RetrofitClient.apiService.getDevice("Bearer ${AppController.prefs.getToken()}", deviceId)
                withContext(Dispatchers.Main) {
                    if(getDevice.isSuccessful) {
                        device = getDevice.body()?.device ?: Device()
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
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 상단 바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            IconButton(onClick = { navBack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "디바이스 정보",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu),
                        contentDescription = "메뉴",
                        modifier = Modifier.size(21.dp),
                        tint = Color.White
                    )
                }
                ShowDevicePopupWindow(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onEditDevice = {
                        navController.navigate("editDevice/$deviceId")
                    },
                    onDeleteDevice = {
                        scope.launch {
                            val token = AppController.prefs.getToken()
                            val response = withContext(Dispatchers.IO) {
                                RetrofitClient.apiService.deleteDevice("Bearer $token", deviceId)
                            }
                            if (response.isSuccessful) {
                                Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                navBack()
                            } else {
                                Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "이름: ${device.name}",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Text(
            text = "버전: ${device.version}",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Text(
            text = "모델명: ${device.modelName}",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Text(
            text = "제품 번호: ${device.modelNumber}",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Text(
            text = "시리얼 번호: ${device.serialNumber}",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun ShowDevicePopupWindow(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEditDevice: () -> Unit,
    onDeleteDevice: () -> Unit
) {
    androidx.compose.material.DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onDismiss() },
        offset = DpOffset(x = (-15).dp, y = 0.dp),
        modifier = Modifier.background(Color.White)
    ) {
        DropdownMenuItem(
            text = { androidx.compose.material.Text("디바이스 수정", color = Color.Black) },
            onClick = {
                onDismiss()
                onEditDevice()
            }
        )
        DropdownMenuItem(
            text = { androidx.compose.material.Text("디바이스 삭제", color = Color.Black) },
            onClick = {
                onDismiss()
                onDeleteDevice()
            }
        )
    }
}