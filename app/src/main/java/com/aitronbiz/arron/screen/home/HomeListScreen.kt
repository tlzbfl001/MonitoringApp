package com.aitronbiz.arron.screen.home

import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeListScreen(
    navController: NavController
) {
    var homeList by remember { mutableStateOf<List<Home>>(emptyList()) }

    // 데이터 로드
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val token = AppController.prefs.getToken()
                val response = RetrofitClient.apiService.getAllHome("Bearer $token")
                if (response.isSuccessful) {
                    val homes = response.body()?.homes ?: emptyList()
                    withContext(Dispatchers.Main) {
                        homeList = homes
                    }
                } else {
                    Log.e(TAG, "getAllHome: $response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getAllHome: ${e.message}")
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
                .padding(horizontal = 9.dp, vertical = 8.dp)
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
                text = "홈 설정",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { navController.navigate("addHome") },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "추가하기",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
        }

        // 스크롤 가능한 영역
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 8.dp)
            ) {
                items(homeList) { home ->
                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                            .height(53.dp)
                            .clickable {
                                navController.navigate("settingHome/${home.id}")
                            }
                            .background(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
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
                                text = home.name,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}