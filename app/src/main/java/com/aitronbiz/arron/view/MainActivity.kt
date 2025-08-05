package com.aitronbiz.arron.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.service.FirebaseMessagingService
import com.aitronbiz.arron.view.init.LoginActivity
import com.aitronbiz.arron.viewmodel.MainViewModel
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.R
import com.aitronbiz.arron.view.theme.MyAppTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyAppTheme {
                MainScreen(viewModel)
            }
        }

        // JWT 토큰 갱신시 호출
        viewModel.startTokenRefresh {
            lifecycleScope.launch(Dispatchers.Main) {
                AppController.prefs.removeToken()
                Toast.makeText(
                    this@MainActivity,
                    "로그인 세션이 만료되었습니다. 다시 로그인해 주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }

        // FCM 토큰 처리
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val fcmToken = task.result
                AppController.prefs.saveFcmToken(fcmToken)

                val jwtToken = AppController.prefs.getToken()
                if (!jwtToken.isNullOrEmpty()) {
                    FirebaseMessagingService.sendTokenToServer(fcmToken)
                } else {
                    Log.d("MainActivity", "아직 JWT 없음, 로그인 후에 서버에 FCM 토큰 보내야 함")
                }
            } else {
                Log.e("MainActivity", "FCM error", task.exception)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopTokenAutoRefresh()
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val systemUiController = rememberSystemUiController()
    val bottomNavVisible = remember { mutableStateOf(true) }

    // 상태바 색상 변경
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = false
        )
        systemUiController.setNavigationBarColor(Color.Black)
    }

    Scaffold(
        bottomBar = {
            if (bottomNavVisible.value) {
                BottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("main") {
                MainScreenContent(
                    onHideBottomNav = { bottomNavVisible.value = false },
                    onShowBottomNav = { bottomNavVisible.value = true }
                )
            }
            composable("device") { DeviceScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("홈", "main", Icons.Default.Home),
        BottomNavItem("기기", "device", Icons.Default.Share),
        BottomNavItem("설정", "settings", Icons.Default.Settings)
    )

    NavigationBar(containerColor = Color(0xFF174176)) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo("main") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun MainScreenContent(
    onHideBottomNav: () -> Unit,
    onShowBottomNav: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 상단바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF174176))
                .padding(16.dp)
        ) {
            TopBar()
        }

        WeeklyCalendarPager(
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F2B4E))
                .padding(16.dp)
        ) {
            DetectionCardList()
        }
    }
}

@Composable
fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("나의 홈", color = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = "홈 메뉴",
                modifier = Modifier.size(17.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("재실중", color = Color.Cyan)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_bell),
                contentDescription = "알림",
                modifier = Modifier.size(17.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                painter = painterResource(id = R.drawable.menu_dot),
                contentDescription = "메뉴",
                modifier = Modifier.size(17.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun WeeklyCalendarPager(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 1000) { Int.MAX_VALUE }
    val today = LocalDate.now()
    val days = listOf("일", "월", "화", "수", "목", "금", "토")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF174176))
            .padding(vertical = 8.dp)
    ) {
        // 날짜 표시줄
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                "${selectedDate.monthValue}.${selectedDate.dayOfMonth} " +
                        selectedDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                color = Color.White
            )
        }

        // 요일 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEachIndexed { index, day ->
                val isSelected = (selectedDate.dayOfWeek.value % 7) == index
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        color = if (isSelected) Color(0xFF174176) else Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 주간 날짜 
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val startOfWeek = today
                .minusDays(today.dayOfWeek.value.toLong())
                .plusWeeks((page - 1000).toLong())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (0..6).forEach { offset ->
                    val date = startOfWeek.plusDays(offset.toLong())
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            color = if (date == today && date != selectedDate) Color.Cyan
                            else Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetectionCardList() {
    Column {
        DetectionCard(title = "낙상감지", value = "1회")
        DetectionCard(title = "활동량감지", value = "9시간 활동")
        DetectionCard(title = "호흡 감지", value = "분당 15회")
        DetectionCard(title = "생활 패턴", value = "평균 취침 23:00\n평균 기상 07:30", isDanger = true)
    }
}

@Composable
fun DetectionCard(title: String, value: String, isDanger: Boolean = false) {
    androidx.compose.material.Card(
        backgroundColor = if (isDanger) Color(0xFF8B0000) else Color(0xFF0A2540),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.LightGray)
        }
    }
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun DeviceScreen() {
    Text("디바이스 화면")
}

@Composable
fun SettingsScreen() {
    Text("설정 화면")
}
