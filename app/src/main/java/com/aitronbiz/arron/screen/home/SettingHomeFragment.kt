package com.aitronbiz.arron.screen.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Device
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.api.response.RoomsResponse
import com.aitronbiz.arron.screen.device.AddRoomFragment
import com.aitronbiz.arron.screen.device.QrScannerFragment
import com.aitronbiz.arron.screen.device.SettingDeviceFragment
import com.aitronbiz.arron.screen.device.SettingRoomFragment
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.layoutType
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import retrofit2.Response

class SettingHomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        layoutType = 2
        val homeId = arguments?.getString("homeId").orEmpty()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { SettingHomeScreenForFragment(homeId = homeId) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    replaceFragment2(
                        requireActivity().supportFragmentManager,
                        HomeListFragment(),
                        null
                    )
                }
            }
        )
    }
}

@Composable
fun SettingHomeScreenForFragment(homeId: String) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()

    // 상태
    var homeName by remember { mutableStateOf("") }
    var roomList by remember { mutableStateOf<List<Room>>(emptyList()) }
    var deviceList by remember { mutableStateOf<List<Device>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var canDeleteHome by remember { mutableStateOf(true) }

    // 데이터 로드
    LaunchedEffect(homeId) {
        try {
            val token = AppController.prefs.getToken()

            // 홈 이름
            val homeResponse = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getHome("Bearer $token", homeId)
            }
            if (homeResponse.isSuccessful) {
                homeName = homeResponse.body()?.home?.name.orEmpty()
            } else Log.e("SettingHome", "getHome: $homeResponse")

            // 장소 목록
            val roomsRes: Response<RoomsResponse> = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
            }
            roomList = if (roomsRes.isSuccessful) roomsRes.body()?.rooms ?: emptyList() else emptyList()

            Log.d(TAG, "homeId: $homeId")
            Log.d(TAG, "roomList: $roomList")

            // 기기 목록
            val devicesRes = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getAllDevice("Bearer $token", homeId)
            }
            deviceList = if (devicesRes.isSuccessful) devicesRes.body()?.devices ?: emptyList() else emptyList()

            // 삭제 가능 여부
            val allHomesRes = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getAllHome("Bearer $token")
            }
            canDeleteHome = if (allHomesRes.isSuccessful) {
                (allHomesRes.body()?.homes?.size ?: 0) > 1
            } else false
        } catch (t: Throwable) {
            Log.e("SettingHome", "load error: ${t.message}")
            canDeleteHome = false
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .statusBarsPadding()
    ) {
        // 상단바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            IconButton(onClick = {
                replaceFragment2(activity.supportFragmentManager, HomeListFragment(), null)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(Modifier.width(3.dp))
            Text(
                text = homeName,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "more",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                ShowHomePopupWindow(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onEditHome = {
                        showMenu = false
                        val bundle = Bundle().apply { putString("homeId", homeId) }
                        replaceFragment2(
                            fragmentManager = activity.supportFragmentManager,
                            fragment = EditHomeFragment(),
                            bundle = bundle
                        )
                    },
                    onDeleteHome = {
                        showMenu = false
                        if (!canDeleteHome) {
                            Toast.makeText(context, "홈은 1개 이상이어야합니다.", Toast.LENGTH_SHORT).show()
                        } else showDeleteDialog = true
                    }
                )
            }
        }

        Text(
            text = "등록된 장소 및 기기",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 20.dp, top = 10.dp, bottom = 6.dp)
        )

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(roomList) { room ->
                        ItemCard(
                            label = room.name.ifBlank { "장소" },
                            onClick = {
                                val bundle = Bundle().apply {
                                    putString("homeId", homeId)
                                    putString("roomId", room.id)
                                }
                                replaceFragment2(
                                    activity.supportFragmentManager,
                                    SettingRoomFragment(),
                                    bundle
                                )
                            }
                        )
                    }
                    item {
                        AddCard(
                            label = "+ 장소 추가",
                            onClick = {
                                val bundle = Bundle().apply { putString("homeId", homeId) }
                                replaceFragment2(
                                    activity.supportFragmentManager,
                                    AddRoomFragment(),
                                    bundle
                                )
                            }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(deviceList) { device ->
                        ItemCard(
                            label = device.name.ifBlank { "기기" },
                            onClick = {
                                val bundle = Bundle().apply {
                                    putString("deviceId", device.id)
                                    putString("homeId", homeId)
                                }
                                replaceFragment2(
                                    activity.supportFragmentManager,
                                    SettingDeviceFragment(),
                                    bundle
                                )
                            }
                        )
                    }
                    item {
                        AddCard(
                            label = "+ 기기 추가",
                            onClick = {
                                val bundle = Bundle().apply { putString("homeId", homeId) }
                                replaceFragment2(
                                    activity.supportFragmentManager,
                                    QrScannerFragment(),
                                    bundle
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!deleting) showDeleteDialog = false },
            title = { Text("홈 삭제") },
            text = { Text("정말 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
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
                                    replaceFragment2(
                                        activity.supportFragmentManager,
                                        HomeListFragment(),
                                        null
                                    )
                                } else {
                                    Log.e("SettingHome", "deleteHome failed: ${response.errorBody()}")
                                    Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("SettingHome", "deleteHome error: ${e.message}")
                                Toast.makeText(context, "삭제 중 오류", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                            } finally {
                                deleting = false
                            }
                        }
                    },
                    enabled = !deleting
                ) { Text(if (deleting) "삭제 중..." else "확인") }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!deleting) showDeleteDialog = false },
                    enabled = !deleting
                ) { Text("취소") }
            }
        )
    }
}

@Composable
private fun ItemCard(
    label: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(49.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x5A185078)),
        border = BorderStroke(1.4.dp, Color(0xFF185078))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AddCard(
    label: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(49.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x5A185078)),
        border = BorderStroke(1.4.dp, Color(0xFF185078))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ShowHomePopupWindow(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEditHome: () -> Unit,
    onDeleteHome: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        DropdownMenuItem(
            text = { Text("홈 수정", color = Color.Black) },
            onClick = { onDismiss(); onEditHome() }
        )
        DropdownMenuItem(
            text = { Text("홈 삭제", color = Color.Black) },
            onClick = { onDismiss(); onDeleteHome() }
        )
    }
}
