package com.aitronbiz.arron.screen.device

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
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
import com.aitronbiz.arron.screen.home.SettingHomeFragment
import com.aitronbiz.arron.util.CustomUtil
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingDeviceFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val deviceId = arguments?.getString("deviceId").orEmpty()
        val homeId   = arguments?.getString("homeId").orEmpty()

        val onBack: () -> Unit = {
            when (CustomUtil.deviceType) {
                1 -> {
                    replaceFragment(requireActivity().supportFragmentManager, DeviceFragment(),null)
                }
                2  -> {
                    val bundle = Bundle().apply { putString("homeId", homeId) }
                    replaceFragment(requireActivity().supportFragmentManager, SettingHomeFragment(), bundle)
                }
                else -> requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = onBack()
            }
        )

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SettingDeviceScreen(
                    deviceId = deviceId,
                    homeId = homeId,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
fun SettingDeviceScreen(
    deviceId: String,
    homeId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var device by remember { mutableStateOf(Device()) }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    // 삭제 관련 상태
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    // 데이터 로드
    LaunchedEffect(deviceId, homeId) {
        withContext(Dispatchers.IO) {
            try {
                val token = "Bearer ${AppController.prefs.getToken()}"
                val getDevice = RetrofitClient.apiService.getDevice(token, deviceId)
                withContext(Dispatchers.Main) {
                    if (getDevice.isSuccessful) {
                        device = getDevice.body()?.device ?: Device()
                    } else {
                        Log.e(TAG, "getDevice: $getDevice")
                        Toast.makeText(context, "디바이스 정보를 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "load device: $e")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "디바이스 정보를 불러오는 중 오류가 발생했어요.", Toast.LENGTH_SHORT).show()
                }
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
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }

            Text(
                text = "디바이스 정보",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
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
                ShowDevicePopupWindow(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onEditDevice = {
                        showMenu = false
                        val bundle = Bundle().apply {
                            putString("deviceId", deviceId)
                            putString("deviceName", device.name ?: "")
                            putString("deviceSerial", device.serialNumber ?: "")
                            putString("homeId", homeId)
                        }
                        replaceFragment(
                            activity.supportFragmentManager,
                            EditDeviceFragment(),
                            bundle
                        )
                    },
                    onDeleteDevice = {
                        showMenu = false
                        showDeleteDialog = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "이름: ${device.name}",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Text(
            text = "시리얼 번호: ${device.serialNumber}",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }

    // 삭제 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!deleting) showDeleteDialog = false },
            title = { Text("디바이스 삭제") },
            text = { Text("정말 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deleting) return@TextButton
                        scope.launch {
                            try {
                                deleting = true
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService.deleteDevice(
                                        "Bearer ${AppController.prefs.getToken()}",
                                        deviceId
                                    )
                                }
                                showDeleteDialog = false
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                    onBack()
                                } else {
                                    Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "deleteDevice: $e")
                                Toast.makeText(context, "삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
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
                TextButton(onClick = { if (!deleting) showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun ShowDevicePopupWindow(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEditDevice: () -> Unit,
    onDeleteDevice: () -> Unit
) {
    androidx.compose.material.DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onDismiss() },
        modifier = Modifier.background(Color.White)
    ) {
        androidx.compose.material.DropdownMenuItem(
            onClick = { onDismiss(); onEditDevice() }
        ) {
            Text(text = "디바이스 수정", color = Color.Black, fontSize = 14.sp)
        }

        androidx.compose.material.DropdownMenuItem(
            onClick = { onDismiss(); onDeleteDevice() }
        ) {
            Text(text = "디바이스 삭제", color = Color.Black, fontSize = 14.sp)
        }
    }
}
