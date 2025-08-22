package com.aitronbiz.arron.screen.device

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.AppController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingRoomScreen(
    roomId: String,
    navController: NavController
) {
    var room by remember { mutableStateOf(Room()) }
    var isLoading by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }

    // 삭제 확인 다이얼로그 상태
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 장소 정보 불러오기
    LaunchedEffect(roomId) {
        try {
            val res = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getRoom("Bearer ${AppController.prefs.getToken()}", roomId)
            }
            if (res.isSuccessful) {
                room = res.body()?.room ?: Room()
            } else {
                Log.e(TAG, "getRoom: $res")
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
                text = room.name,
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
                ShowRoomPopupWindow(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onEditHome = {
                        showMenu = false
                        navController.navigate("editRoom/$roomId")
                    },
                    onDeleteHome = {
                        showMenu = false
                        showDeleteDialog = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        androidx.compose.material3.Text(
            text = "이름: ${room.name}",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!deleting) showDeleteDialog = false },
            title = { androidx.compose.material3.Text("장소 삭제") },
            text = { androidx.compose.material3.Text("정말 삭제하시겠습니까?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (deleting) return@TextButton
                        val token = AppController.prefs.getToken()
                        scope.launch {
                            try {
                                deleting = true
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService.deleteRoom("Bearer $token", roomId)
                                }
                                showDeleteDialog = false
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                    val popped = navController.popBackStack()
                                    if (!popped) navController.navigateUp()
                                } else {
                                    Log.e(TAG, "deleteRoom failed: ${response.errorBody()}")
                                    Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "deleteRoom error: ${e.message}")
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
fun ShowRoomPopupWindow(
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
            text = { Text("장소 수정", color = Color.Black) },
            onClick = {
                onDismiss()
                onEditHome()
            }
        )
        DropdownMenuItem(
            text = { Text("장소 삭제", color = Color.Black) },
            onClick = {
                onDismiss()
                onDeleteHome()
            }
        )
    }
}