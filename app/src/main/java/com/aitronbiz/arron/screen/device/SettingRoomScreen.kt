package com.aitronbiz.arron.screen.device

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingRoomScreen(
    roomId: String,
    navController: NavController
) {
    val context = LocalContext.current
    var room by remember { mutableStateOf(Room()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    // 장소 정보 불러오기
    LaunchedEffect(roomId) {
        try {
            val res = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getRoom("Bearer ${AppController.prefs.getToken()}", roomId)
            }
            if (res.isSuccessful) {
                room = res.body()?.room ?: Room()
            }else {
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
                        navController.navigate("editRoom/$roomId")
                    },
                    onDeleteHome = {
                        scope.launch {
                            val response = withContext(Dispatchers.IO) {
                                RetrofitClient.apiService.deleteRoom("Bearer ${AppController.prefs.getToken()}", roomId)
                            }
                            if (response.isSuccessful) {
                                Log.e(TAG, "deleteRoom: ${response.body()}")
                                Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                val popped = navController.popBackStack()
                                if (!popped) navController.navigateUp()
                            } else {
                                Log.e(TAG, "deleteRoom: $response")
                                Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
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