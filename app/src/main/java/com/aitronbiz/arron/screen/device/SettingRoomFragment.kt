package com.aitronbiz.arron.screen.device

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.aitronbiz.arron.R
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.screen.home.SettingHomeFragment
import com.aitronbiz.arron.util.CustomUtil.deviceType
import com.aitronbiz.arron.util.CustomUtil.replaceFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingRoomFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeId = arguments?.getString("homeId") ?: ""
        val roomId = arguments?.getString("roomId") ?: ""

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { SettingRoomScreenForFragment(homeId = homeId, roomId = roomId) }
        }
    }
}

@Composable
private fun SettingRoomScreenForFragment(
    homeId: String,
    roomId: String
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var room by remember { mutableStateOf(Room()) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var canDeleteRoom by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(roomId) {
        try {
            val token = "Bearer ${AppController.prefs.getToken()}"
            val res = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getRoom(token, roomId)
            }
            if (res.isSuccessful) {
                val loaded = res.body()?.room ?: Room()
                room = loaded

                val homeId = loaded.homeId ?: ""
                if (homeId.isNotBlank()) {
                    val roomsRes = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.getAllRoom(token, homeId)
                    }
                    if (roomsRes.isSuccessful) {
                        val count = roomsRes.body()?.rooms?.size ?: 0
                        canDeleteRoom = count > 1
                    } else {
                        Log.e(TAG, "getAllRoom: $roomsRes")
                        canDeleteRoom = false
                        Toast.makeText(context, "장소 목록을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    canDeleteRoom = false
                }
            } else {
                Log.e(TAG, "getRoom: $res")
                canDeleteRoom = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            canDeleteRoom = false
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
            IconButton(onClick = { activity.onBackPressedDispatcher.onBackPressed() }) {
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "더보기",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                ShowRoomPopupWindow(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onEditHome = {
                        showMenu = false
                        val bundle = Bundle().apply {
                            putString("homeId", homeId)
                            putString("roomId", roomId)
                        }
                        replaceFragment(
                            fragmentManager = activity.supportFragmentManager,
                            fragment = EditRoomFragment(),
                            bundle = bundle
                        )
                    },
                    onDeleteHome = {
                        showMenu = false
                        if (!canDeleteRoom) {
                            Toast.makeText(context, "장소는 1개 이상이어야합니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            showDeleteDialog = true
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

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!deleting) showDeleteDialog = false },
            title = { androidx.compose.material3.Text("장소 삭제") },
            text = { androidx.compose.material3.Text("정말 삭제하시겠습니까?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
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

                                    val bundle = Bundle().apply { putString("homeId", homeId) }
                                    when(deviceType) {
                                        1 -> {
                                            replaceFragment(activity.supportFragmentManager, RoomListFragment(), bundle)
                                        }
                                        else -> {
                                            replaceFragment(activity.supportFragmentManager, SettingHomeFragment(), bundle)
                                        }
                                    }

                                    activity.onBackPressedDispatcher.onBackPressed()
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
private fun ShowRoomPopupWindow(
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