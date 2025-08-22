package com.aitronbiz.arron.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.screen.device.AddDeviceScreen
import com.aitronbiz.arron.service.FirebaseMessagingService
import com.aitronbiz.arron.screen.device.DeviceScreen
import com.aitronbiz.arron.screen.home.HomeScreen
import com.aitronbiz.arron.screen.init.LoginActivity
import com.aitronbiz.arron.screen.home.ActivityDetectionScreen
import com.aitronbiz.arron.screen.home.EntryPatternScreen
import com.aitronbiz.arron.screen.home.LifePatternScreen
import com.aitronbiz.arron.screen.home.NightActivityScreen
import com.aitronbiz.arron.screen.home.RespirationDetectionScreen
import com.aitronbiz.arron.screen.home.HomeListScreen
import com.aitronbiz.arron.screen.setting.SettingsScreen
import com.aitronbiz.arron.screen.device.AddRoomScreen
import com.aitronbiz.arron.screen.device.EditDeviceScreen
import com.aitronbiz.arron.screen.device.EditRoomScreen
import com.aitronbiz.arron.screen.device.RoomListScreen
import com.aitronbiz.arron.screen.home.SearchAddressScreen
import com.aitronbiz.arron.screen.device.SettingDeviceScreen
import com.aitronbiz.arron.screen.device.SettingRoomScreen
import com.aitronbiz.arron.screen.home.FallDetectionScreen
import com.aitronbiz.arron.screen.home.EditHomeScreen
import com.aitronbiz.arron.screen.home.AddHomeScreen
import com.aitronbiz.arron.screen.home.EmergencyCallScreen
import com.aitronbiz.arron.screen.home.RealTimeRespirationScreen
import com.aitronbiz.arron.screen.home.SettingHomeScreen
import com.aitronbiz.arron.screen.notification.DetailNotificationScreen
import com.aitronbiz.arron.screen.notification.NotificationScreen
import com.aitronbiz.arron.screen.setting.AppInfoScreen
import com.aitronbiz.arron.screen.setting.Terms1Screen
import com.aitronbiz.arron.screen.setting.Terms2Screen
import com.aitronbiz.arron.screen.setting.Terms3Screen
import com.aitronbiz.arron.screen.setting.TermsInfoScreen
import com.aitronbiz.arron.screen.setting.UserScreen
import com.aitronbiz.arron.screen.theme.MyAppTheme
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.viewmodel.MainViewModel
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)

        // Android 13 이상 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

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
                    Log.d(TAG, "아직 JWT 없음, 로그인 후에 서버에 FCM 토큰 보내야 함")
                }
            } else {
                Log.e(TAG, "FCM error", task.exception)
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
    val context = LocalContext.current
    val navController = rememberNavController()
    val systemUiController = rememberSystemUiController()
    val bottomNavVisible = remember { mutableStateOf(true) }

    var lastBackPressedAt by remember { mutableStateOf(0L) }

    fun isAtRoot(): Boolean {
        val route = navController.currentBackStackEntry?.destination?.route
        return navController.previousBackStackEntry == null || route == "home"
    }

    BackHandler(enabled = true) {
        if (isAtRoot()) {
            val now = System.currentTimeMillis()
            if (now - lastBackPressedAt <= 2000L) {
                (context as? Activity)?.finishAffinity()
            } else {
                lastBackPressedAt = now
                Toast.makeText(context, "한 번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            navController.navigateUp()
        }
    }

    // 시스템 바를 투명 처리 (다크모드 무시: 항상 같은 아이콘 밝기 사용)
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = true // 필요 시 false로 고정 변경 가능
        )
        systemUiController.setNavigationBarColor(Color.Transparent)
    }

    Scaffold(
        // Scaffold가 기본적으로 삽입하는 content insets를 비활성화
        contentWindowInsets = WindowInsets(0),

        bottomBar = {
            if (bottomNavVisible.value) {
                BottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .padding(innerPadding)
                // 상/좌/우 안전영역만 NavHost에 적용
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Start + WindowInsetsSides.End
                    )
                )
                // 키보드가 뜰 때 본문 겹침 방지
                .imePadding()
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }
            composable("homeList") {
                HomeListScreen(
                    navController = navController
                )
            }
            composable("settingHome/{homeId}") { backStackEntry ->
                val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                SettingHomeScreen(
                    homeId = homeId,
                    navController = navController
                )
            }
            composable("addHome") {
                AddHomeScreen(
                    navController = navController
                )
            }
            composable("editHome/{homeId}") { backStackEntry ->
                val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                EditHomeScreen(
                    homeId = homeId,
                    navController = navController
                )
            }
            composable("addDevice/{homeId}") { backStackEntry ->
                val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                AddDeviceScreen(
                    navController = navController,
                    homeId = homeId
                )
            }
            composable("settingDevice/{deviceId}") { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                SettingDeviceScreen(
                    deviceId = deviceId,
                    navController = navController
                )
            }
            composable("editDevice/{deviceId}") { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                EditDeviceScreen(
                    navController = navController,
                    deviceId = deviceId
                )
            }
            composable("roomList/{homeId}") { backStackEntry ->
                val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                RoomListScreen(
                    homeId = homeId,
                    navController = navController
                )
            }
            composable("settingRoom/{roomId}") { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                SettingRoomScreen(
                    roomId = roomId,
                    navController = navController
                )
            }
            composable("addRoom/{homeId}") { backStackEntry ->
                val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                AddRoomScreen(
                    homeId = homeId,
                    navController = navController
                )
            }
            composable("editRoom/{roomId}") { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                EditRoomScreen(
                    roomId = roomId,
                    navController = navController
                )
            }
            composable("searchAddress") {
                SearchAddressScreen(
                    navController = navController
                )
            }
            composable("device") { backStackEntry ->
                val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                DeviceScreen(
                    navController = navController,
                    homeId = homeId
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }
            // 메인 메뉴
            composable("fallDetection/{homeId}/{roomId}/{date}") { backStackEntry ->
                val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                val dateStr = backStackEntry.arguments?.getString("date").orEmpty()
                val selectedDate = runCatching { LocalDate.parse(dateStr) }.getOrElse { LocalDate.now() }
                FallDetectionScreen(
                    homeId = homeId,
                    roomId = roomId,
                    selectedDate = selectedDate,
                    navController = navController
                )
            }
            composable("activityDetection/{homeId}/{roomId}/{date}") { backStackEntry ->
                val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                val dateStr = backStackEntry.arguments?.getString("date").orEmpty()
                val selectedDate = runCatching { LocalDate.parse(dateStr) }.getOrElse { LocalDate.now() }
                ActivityDetectionScreen(
                    homeId = homeId,
                    roomId = roomId,
                    selectedDate = selectedDate,
                    navController = navController
                )
            }
            composable("respirationDetection/{homeId}/{roomId}/{date}") { backStackEntry ->
                val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                val roomId = backStackEntry.arguments?.getString("homeId") ?: ""
                val dateStr = backStackEntry.arguments?.getString("date").orEmpty()
                val selectedDate = runCatching { LocalDate.parse(dateStr) }.getOrElse { LocalDate.now() }
                RespirationDetectionScreen(
                    homeId = homeId,
                    roomId = roomId,
                    selectedDate = selectedDate,
                    navController = navController
                )
            }
            composable("realTimeRespiration/{roomId}") { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                RealTimeRespirationScreen(
                    navController = navController,
                    roomId = roomId
                )
            }
            composable("lifePattern/{homeId}/{roomId}/{date}") { backStackEntry ->
                val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                val dateStr = backStackEntry.arguments?.getString("date").orEmpty()
                val selectedDate = runCatching { LocalDate.parse(dateStr) }.getOrElse { LocalDate.now() }
                LifePatternScreen(
                    homeId = homeId,
                    roomId = roomId,
                    selectedDate = selectedDate,
                    navController = navController
                )
            }
            composable("entryPattern/{homeId}/{roomId}/{date}") { backStackEntry ->
                val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                val roomId = backStackEntry.arguments?.getString("homeId") ?: ""
                val dateStr = backStackEntry.arguments?.getString("date").orEmpty()
                val selectedDate = runCatching { LocalDate.parse(dateStr) }.getOrElse { LocalDate.now() }
                EntryPatternScreen(
                    homeId = homeId,
                    roomId = roomId,
                    selectedDate = selectedDate,
                    navController = navController
                )
            }
            composable("nightActivity/{homeId}/{roomId}/{date}") { backStackEntry ->
                val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                val roomId = backStackEntry.arguments?.getString("homeId") ?: ""
                val dateStr = backStackEntry.arguments?.getString("date").orEmpty()
                val selectedDate = runCatching { LocalDate.parse(dateStr) }.getOrElse { LocalDate.now() }
                NightActivityScreen(
                    homeId = homeId,
                    roomId = roomId,
                    selectedDate = selectedDate,
                    navController = navController
                )
            }
            composable("emergencyCall/{homeId}/{roomId}") { backStackEntry ->
                val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                EmergencyCallScreen(
                    homeId = homeId,
                    roomId = roomId,
                    navController = navController
                )
            }
            composable("notification") {
                NotificationScreen(
                    navController = navController,
                    viewModel = viewModel(),
                    token = AppController.prefs.getToken().toString()
                )
            }
            composable("notificationDetail/{notificationId}") { backStackEntry ->
                val notificationId = backStackEntry.arguments?.getString("notificationId") ?: ""
                DetailNotificationScreen(
                    notificationId = notificationId,
                    token = AppController.prefs.getToken().toString(),
                    navController = navController
                )
            }
            composable("user") {
                UserScreen(
                    navController = navController,
                    viewModel = viewModel()
                )
            }
            composable("terms") {
                TermsInfoScreen(
                    navController = navController
                )
            }
            composable("appInfo") {
                AppInfoScreen(
                    navController = navController
                )
            }
            composable("terms1") {
                Terms1Screen(
                    navController = navController
                )
            }
            composable("terms2") {
                Terms2Screen(
                    navController = navController
                )
            }
            composable("terms3") {
                Terms3Screen(
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("홈", "home", Icons.Default.Home),
        BottomNavItem("디바이스", "device", Icons.Default.Share),
        BottomNavItem("설정", "settings", Icons.Default.Settings)
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    androidx.compose.material3.Surface(
        color = Color(0xFF174176),
        contentColor = Color.White,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(103.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 5.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (!selected) {
                                if (item.route == "home") {
                                    // 백스택에 home 있으면 복귀
                                    val popped = navController.popBackStack("home", inclusive = false)
                                    if (!popped) {
                                        // 없으면 새로 이동
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(20.dp),
                        tint = if (selected) Color.White else Color.LightGray
                    )
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        color = if (selected) Color.White else Color.LightGray,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)
