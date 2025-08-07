package com.aitronbiz.arron.screen.device

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
fun SettingRoomScreen(
    homeId: String,
    roomId: String,
    navController: NavController,
    navBack: () -> Unit
) {
    val context = LocalContext.current
    var roomName by remember { mutableStateOf("") }
    val deviceList = remember { mutableStateListOf<Device>() }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    // 데이터 로드
    LaunchedEffect(roomId) {
        withContext(Dispatchers.IO) {
            try {
                val token = AppController.prefs.getToken()
                val getRoom = RetrofitClient.apiService.getRoom("Bearer $token", roomId)
                val getAllDevice = RetrofitClient.apiService.getAllDevice("Bearer $token", roomId)

                withContext(Dispatchers.Main) {
                    if (getRoom.isSuccessful) {
                        roomName = getRoom.body()?.room?.name ?: ""
                    }
                    if (getAllDevice.isSuccessful) {
                        deviceList.clear()
                        deviceList.addAll(getAllDevice.body()?.devices ?: emptyList())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
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
                text = roomName.ifBlank { "룸" },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu),
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    offset = DpOffset(x = (-15).dp, y = 0.dp),
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("룸 수정", color = Color.Black) },
                        onClick = {
                            showMenu = false
                            navController.navigate("editRoom/$homeId/$roomId")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("룸 삭제", color = Color.Red) },
                        onClick = {
                            showMenu = false
                            scope.launch {
                                val token = AppController.prefs.getToken()
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService.deleteRoom("Bearer $token", roomId)
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("등록된 기기", color = Color.White, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(deviceList) { device ->
                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("settingDevice/$homeId/${device.id}")
                        },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(device.name, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            // 기기 추가 버튼
            item {
                OutlinedButton(
                    onClick = { navController.navigate("addDevice/$homeId") },
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Text("Add Device")
                }
            }
        }
    }
}
