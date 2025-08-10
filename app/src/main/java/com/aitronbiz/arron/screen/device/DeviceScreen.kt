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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Device
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DeviceScreen(
    navController: NavController,
    homeId: String? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var homes by remember { mutableStateOf<List<Home>>(emptyList()) }
    var homeServerId by rememberSaveable { mutableStateOf(homeId ?: "") }
    var selectedHomeName by rememberSaveable { mutableStateOf("나의 홈") }
    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var showHomeSheet by remember { mutableStateOf(false) }

    LaunchedEffect(homeId) {
        scope.launch {
            val token = AppController.prefs.getToken()
            val getAllHome = RetrofitClient.apiService.getAllHome("Bearer $token")
            if (getAllHome.isSuccessful) {
                homes = getAllHome.body()?.homes ?: emptyList()

                if (homeServerId.isBlank()) {
                    val selectedHome = homes.find { it.id == homeId } ?: homes.firstOrNull()
                    selectedHome?.let {
                        homeServerId = it.id
                        selectedHomeName = it.name
                    }
                }
            } else {
                Log.e(TAG, "getAllHome: $getAllHome")
            }
        }
    }

    LaunchedEffect(homeServerId) {
        if (homeServerId.isBlank()) return@LaunchedEffect
        val token = AppController.prefs.getToken()
        val getAllDevice = RetrofitClient.apiService.getAllDevice("Bearer $token", homeServerId)
        if (getAllDevice.isSuccessful) {
            devices = getAllDevice.body()?.devices ?: emptyList()
        } else {
            Log.e(TAG, "getAllDevice: $getAllDevice")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 상단 타이틀 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 15.dp, end = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(30.dp))
            Text(
                text = "디바이스",
                color = Color.White,
                fontSize = 17.sp,
                fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { showAddBottomSheet = true },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "추가하기",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 현재 선택된 홈 표시
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clickable { showHomeSheet = true }
        ) {
            Text(
                text = selectedHomeName,
                color = Color.White,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = "홈 선택",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 디바이스 목록
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(devices) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable {
                            navController.navigate("settingDevice/${device.id}")
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x5A185078)),
                    border = BorderStroke(1.4.dp, Color(0xFF185078))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(text = device.name, color = Color.White)
                    }
                }
            }
        }

        // 기기 추가
        if (showAddBottomSheet) {
            AddDeviceBottomSheet(
                onDismiss = { showAddBottomSheet = false },
                onInputClick = {
                    navController.navigate("addDevice/$homeServerId")
                    showAddBottomSheet = false
                },
                onQrClick = {
                    Toast.makeText(context, "QR 스캔 선택됨", Toast.LENGTH_SHORT).show()
                    showAddBottomSheet = false
                }
            )
        }

        // 홈 선택
        if (showHomeSheet) {
            DeviceHomeSelectorBottomSheet(
                homes = homes,
                selectedHomeId = homeServerId,
                onDismiss = { showHomeSheet = false },
                onHomeSelected = { home ->
                    selectedHomeName = home.name
                    homeServerId = home.id
                },
                onNavigateToSettingHome = {
                    showHomeSheet = false
                    navController.navigate("homeList")
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceHomeSelectorBottomSheet(
    homes: List<Home>,
    selectedHomeId: String,
    onDismiss: () -> Unit,
    onHomeSelected: (Home) -> Unit,
    onNavigateToSettingHome: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = "홈 선택",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(15.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(homes, key = { it.id }) { home ->
                    val isSelected = selectedHomeId == home.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                onHomeSelected(home)
                                scope.launch {
                                    delay(300)
                                    sheetState.hide()
                                    onDismiss()
                                }
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = "선택됨",
                                    tint = Color(0xFF174176),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Spacer(modifier = Modifier.width(28.dp))
                            }
                            Text(
                                text = home.name,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "홈 설정 >",
                color = Color(0xFF24599D),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                            onNavigateToSettingHome()
                        }
                    }
            )
            Spacer(modifier = Modifier.height(24.dp))
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
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "기기 등록",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(17.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF3F3F3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInputClick() }
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "제품 입력",
                            fontSize = 16.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(vertical = 15.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF3F3F3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQrClick() }
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "QR 스캔",
                            fontSize = 16.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(vertical = 15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(15.dp))
        }
    }
}