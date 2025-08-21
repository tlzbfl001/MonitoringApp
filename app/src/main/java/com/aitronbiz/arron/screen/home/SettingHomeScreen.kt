package com.aitronbiz.arron.screen.home

import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Text
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
fun SettingHomeScreen(
    homeId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var homeName by remember { mutableStateOf("") }
    var deviceList by remember { mutableStateOf<List<Device>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showAddBottomSheet by remember { mutableStateOf(false) }

    // 삭제 확인 다이얼로그 상태
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    // 홈 정보 및 디바이스 정보 불러오기
    LaunchedEffect(homeId) {
        try {
            val token = AppController.prefs.getToken()

            val homeResponse = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getHome("Bearer $token", homeId)
            }
            if (homeResponse.isSuccessful) {
                homeName = homeResponse.body()?.home?.name ?: ""
            }

            val getAllDevice = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getAllDevice("Bearer $token", homeId)
            }
            if (getAllDevice.isSuccessful) {
                deviceList = getAllDevice.body()?.devices ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
        } finally {
            isLoading = false
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
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = homeName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu),
                        contentDescription = "메뉴",
                        modifier = Modifier.size(19.dp),
                        tint = Color.White
                    )
                }
                ShowHomePopupWindow(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onEditHome = {
                        showMenu = false
                        navController.navigate("editHome/$homeId")
                    },
                    onDeleteHome = {
                        // 바로 삭제하지 않고, 확인 다이얼로그 표시
                        showMenu = false
                        showDeleteDialog = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "등록된 디바이스",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            // 기기 목록 표시
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(deviceList) { device ->
                        Box(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                                .height(50.dp)
                                .clickable {
                                    navController.navigate("settingDevice/${device.id}")
                                }
                                .background(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            AndroidView(
                                factory = { context ->
                                    FrameLayout(context).apply {
                                        background = ContextCompat.getDrawable(context, R.drawable.rec_10_blue)
                                    }
                                },
                                modifier = Modifier.matchParentSize()
                            )

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = device.name,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 15.dp, bottom = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedButton(
                                onClick = { showAddBottomSheet = true },
                                modifier = Modifier.height(37.dp),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(0.7.dp, Color.White),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 0.dp)
                            ) {
                                androidx.compose.material3.Text("+ 추가하기", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 기기 추가
            if (showAddBottomSheet) {
                DeviceBottomSheet(
                    onDismiss = { showAddBottomSheet = false },
                    onInputClick = {
                        navController.navigate("addDevice/$homeId")
                        showAddBottomSheet = false
                    },
                    onQrClick = {
                        Toast.makeText(context, "QR 스캔 선택됨", Toast.LENGTH_SHORT).show()
                        showAddBottomSheet = false
                    }
                )
            }
        }
    }

    // ───────── 삭제 확인 다이얼로그 ─────────
    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!deleting) showDeleteDialog = false },
            title = { androidx.compose.material3.Text("홈 삭제") },
            text = { androidx.compose.material3.Text("정말 삭제하시겠습니까?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (deleting) return@TextButton
                        scope.launch {
                            try {
                                deleting = true
                                val token = AppController.prefs.getToken()
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService.deleteHome("Bearer $token", homeId)
                                }
                                showDeleteDialog = false
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                    val popped = navController.popBackStack()
                                    if (!popped) navController.navigateUp()
                                } else {
                                    Log.e(TAG, "deleteHome failed: ${response.errorBody()}")
                                    Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "deleteHome error: ${e.message}")
                                Toast.makeText(context, "삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                            } finally {
                                deleting = false
                            }
                        }
                    },
                    enabled = !deleting
                ) {
                    androidx.compose.material3.Text(if (deleting) "삭제 중..." else "확인")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { if (!deleting) showDeleteDialog = false },
                    enabled = !deleting
                ) {
                    androidx.compose.material3.Text("취소")
                }
            }
        )
    }
}

@Composable
fun ShowHomePopupWindow(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEditHome: () -> Unit,
    onDeleteHome: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onDismiss() },
        offset = DpOffset(x = (-15).dp, y = (-10).dp),
        modifier = Modifier.background(Color.White)
    ) {
        DropdownMenuItem(
            text = { Text("홈 수정", color = Color.Black) },
            onClick = {
                onDismiss()
                onEditHome()
            }
        )
        DropdownMenuItem(
            text = { Text("홈 삭제", color = Color.Black) },
            onClick = {
                onDismiss()
                // 여기서는 바로 삭제하지 않고, 상위에서 다이얼로그를 띄우도록 콜백만 호출
                onDeleteHome()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceBottomSheet(
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

            androidx.compose.material3.Text(
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
                        androidx.compose.material3.Text(
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
                        androidx.compose.material3.Text(
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