package com.aitronbiz.arron.screen.device

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Device
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.launch

@Composable
fun DeviceScreen(
    navController: NavController,
    homeId: String? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var homes by remember { mutableStateOf<List<Home>>(emptyList()) }
    var homeServerId by remember { mutableStateOf(homeId ?: "") }
    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var showBottomSheet by remember { mutableStateOf(false) }

    // 홈 정보 및 디바이스 불러오기
    LaunchedEffect(homeId) {
        scope.launch {
            val token = AppController.prefs.getToken()
            val getAllHome = RetrofitClient.apiService.getAllHome("Bearer $token")
            if (getAllHome.isSuccessful) {
                homes = getAllHome.body()?.homes ?: emptyList()
                val selectedHome = homes.find { it.id == homeId } ?: homes.firstOrNull()
                selectedHome?.let {
                    homeServerId = it.id
                }
            } else {
                Log.e(TAG, "getAllHome: $getAllHome")
            }

            if (homeServerId.isNotBlank()) {
                val getAllDevice = RetrofitClient.apiService.getAllDevice("Bearer $token", homeServerId)
                if (getAllDevice.isSuccessful) {
                    devices = getAllDevice.body()?.devices ?: emptyList()
                } else {
                    Log.e(TAG, "getAllDevice: $getAllDevice")
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(horizontal = 20.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 15.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "디바이스",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "나의 홈",
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))
            }

            items(devices) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x5A185078)),
                    border = BorderStroke(1.4.dp, Color(0xFF185078))
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.padding(start = 20.dp)
                    ) {
                        Text(text = device.name, color = Color.White)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = { showBottomSheet = true },
                        modifier = Modifier.height(37.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(0.7.dp, Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 0.dp)
                    ) {
                        Text("+ 추가하기", fontSize = 13.sp)
                    }
                }
            }
        }

        if (showBottomSheet) {
            AddDeviceBottomSheet(
                onDismiss = { showBottomSheet = false },
                onInputClick = {
                    navController.navigate("addDevice/$homeId")
                    showBottomSheet = false
                },
                onQrClick = {
                    Toast.makeText(context, "QR 스캔 선택됨", Toast.LENGTH_SHORT).show()
                    showBottomSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceBottomSheet(
    onDismiss: () -> Unit,
    onInputClick: () -> Unit,
    onQrClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Divider(
                color = Color.LightGray,
                thickness = 4.dp,
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
            )
            Text("기기 등록", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8F8F8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInputClick() }
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "제품 입력",
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8F8F8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQrClick() }
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "QR 스캔",
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}