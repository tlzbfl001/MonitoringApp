package com.aitronbiz.arron.screen.device

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Device
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.screen.home.HomeListFragment
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment
import com.aitronbiz.arron.util.CustomUtil.deviceType
import kotlinx.coroutines.launch

class DeviceFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { DeviceScreen() }
        }
    }
}

@Composable
fun DeviceScreen() {
    deviceType = 1
    val context = LocalContext.current
    val activity = context as FragmentActivity

    var homes by remember { mutableStateOf<List<Home>>(emptyList()) }
    var homeServerId by rememberSaveable { mutableStateOf("") }
    var selectedHomeName by rememberSaveable { mutableStateOf("나의 홈") }
    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var showHomeSheet by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    // 홈 목록 로드
    LaunchedEffect(Unit) {
        val token = AppController.prefs.getToken()
        val res = RetrofitClient.apiService.getAllHome("Bearer $token")
        if (res.isSuccessful) {
            homes = res.body()?.homes ?: emptyList()
            homes.firstOrNull()?.let {
                homeServerId = it.id
                selectedHomeName = it.name
            }
        } else {
            Log.e(TAG, "getAllHome: $res")
        }
    }

    // 선택된 홈의 디바이스 로드
    LaunchedEffect(homeServerId) {
        if (homeServerId.isBlank()) return@LaunchedEffect
        val res = RetrofitClient.apiService.getAllDevice("Bearer ${AppController.prefs.getToken()}", homeServerId)
        if (res.isSuccessful) {
            devices = res.body()?.devices ?: emptyList()
        } else {
            Log.e(TAG, "getAllDevice: $res")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 상단 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, top = 15.dp, end = 15.dp, bottom = 10.dp)
        ) {
            Text(
                text = "디바이스",
                color = Color.White,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                IconButton(
                    onClick = {
                        if (homeServerId.isNotBlank()) {
                            val b = Bundle().apply { putString("homeId", homeServerId) }
                            replaceFragment(
                                fragmentManager = activity.supportFragmentManager,
                                fragment = QrScannerFragment(),
                                bundle = b
                            )
                        } else {
                            Toast.makeText(context, "홈을 먼저 선택해 주세요.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_plus),
                        contentDescription = "추가하기",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box {
                    IconButton(
                        onClick = { moreMenuExpanded = true },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "더보기",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false },
                        modifier = Modifier
                            .width(140.dp)
                            .background(Color.White, RoundedCornerShape(10.dp))
                    ) {
                        DropdownMenuItem(
                            modifier = Modifier.height(30.dp),
                            text = { Text(text = "장소 설정", fontSize = 14.sp, color = Color.Black) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "장소 설정",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                moreMenuExpanded = false
                                if (homeServerId.isNotBlank()) {
                                    val b = Bundle().apply { putString("homeId", homeServerId) }
                                    replaceFragment(
                                        fragmentManager = activity.supportFragmentManager,
                                        fragment = RoomListFragment(),
                                        bundle = b
                                    )
                                } else {
                                    Toast.makeText(context, "홈을 먼저 선택해 주세요.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(start = 14.dp, end = 10.dp),
                            colors = MenuDefaults.itemColors(
                                textColor = Color.Black,
                                trailingIconColor = Color.Black
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clickable { showHomeSheet = true }
        ) {
            Text(text = selectedHomeName, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = "홈 선택",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

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
                        .height(49.dp)
                        .clickable {
                            val b = Bundle().apply {
                                putString("deviceId", device.id)
                                putString("homeId", homeServerId)
                            }
                            replaceFragment(
                                fragmentManager = activity.supportFragmentManager,
                                fragment = SettingDeviceFragment(),
                                bundle = b
                            )
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
                        Text(text = device.name, fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        }

        // 홈 선택 바텀시트
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
                    replaceFragment(
                        fragmentManager = activity.supportFragmentManager,
                        fragment = HomeListFragment(),
                        bundle = null
                    )
                }
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DeviceHomeSelectorBottomSheet(
    homes: List<Home>,
    selectedHomeId: String,
    onDismiss: () -> Unit,
    onHomeSelected: (Home) -> Unit,
    onNavigateToSettingHome: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { sheetState.show() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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

            if (homes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("등록된 홈이 없습니다.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
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
                                        tint = Color(0xA87C7C7C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Spacer(modifier = Modifier.width(28.dp))
                                }
                                Text(text = home.name, fontSize = 16.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                            onNavigateToSettingHome()
                        }
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "홈 설정",
                    color = Color(0xFF24599D),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_right),
                    contentDescription = "장소 등록 아이콘",
                    modifier = Modifier.size(15.dp),
                    tint = Color(0xFF24599D)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
