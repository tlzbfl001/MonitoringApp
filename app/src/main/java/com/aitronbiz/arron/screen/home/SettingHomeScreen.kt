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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
fun SettingHomeScreen(
    homeId: String,
    navController: NavController,
    navBack: () -> Unit
) {
    val context = LocalContext.current
    var homeName by remember { mutableStateOf("") }
    var roomList by remember { mutableStateOf<List<Room>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    // 서버에서 홈 정보 및 룸 리스트 불러오기
    LaunchedEffect(homeId) {
        try {
            val token = AppController.prefs.getToken()

            val homeResponse = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getHome("Bearer $token", homeId)
            }
            if (homeResponse.isSuccessful) {
                homeName = homeResponse.body()?.home?.name ?: ""
            }

            val roomsResponse = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
            }
            if (roomsResponse.isSuccessful) {
                roomList = roomsResponse.body()?.rooms ?: emptyList()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
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

            androidx.compose.material3.Text(
                text = "홈 설정",
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
                ShowHomePopupWindow(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onEditHome = {
                        navController.navigate("editHome/$homeId")
                    },
                    onDeleteHome = {
                        scope.launch {
                            val token = AppController.prefs.getToken()
                            val response = withContext(Dispatchers.IO) {
                                RetrofitClient.apiService.deleteHome("Bearer $token", homeId)
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

        Spacer(modifier = Modifier.height(20.dp))

        // 전체 룸 목록 컨테이너
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 25.dp)
        ) {
            Text(
                text = "등록된 룸",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 13.dp, bottom = 8.dp)
            ) {
                items(roomList) { room ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .padding(vertical = 7.dp)
                            .clickable {
                                navController.navigate("settingDevice/$homeId/${room.id}")
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
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
                                androidx.compose.material3.Text(
                                    text = room.name,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 룸 추가 버튼
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OutlinedButton(
                            onClick = { navController.navigate("addRoom/$homeId") },
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
        offset = DpOffset(x = (-15).dp, y = 0.dp),
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
                onDeleteHome()
            }
        )
    }
}