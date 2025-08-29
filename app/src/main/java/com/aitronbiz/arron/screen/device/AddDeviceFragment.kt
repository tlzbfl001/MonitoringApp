package com.aitronbiz.arron.screen.device

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.DeviceDTO
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.component.OutlineOnlyInput
import com.aitronbiz.arron.component.WhiteBoxInput
import com.aitronbiz.arron.screen.home.SettingHomeFragment
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.layoutType
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddDeviceFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val homeId   = arguments?.getString("homeId").orEmpty()
        val preSerial = arguments?.getString("serial").orEmpty()

        val onBack: () -> Unit = {
            when(layoutType) {
                1 -> replaceFragment2(requireActivity().supportFragmentManager, DeviceFragment(), null)
                2 -> {
                    val bundle = Bundle().apply { putString("homeId", homeId) }
                    replaceFragment2(requireActivity().supportFragmentManager, SettingHomeFragment(), bundle)
                }
                else -> requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) { override fun handleOnBackPressed() = onBack() }
        )

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { AddDeviceScreen(homeId = homeId, preSerial = preSerial, onBack = onBack) }
        }
    }
}

@Composable
private fun AddDeviceScreen(
    homeId: String,
    preSerial: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var roomsList by remember { mutableStateOf<List<Room>>(emptyList()) }
    var roomId by remember { mutableStateOf("") }
    var isRoomDialogOpen by remember { mutableStateOf(false) }
    var name by rememberSaveable(homeId) { mutableStateOf("") }
    var roomName by remember { mutableStateOf("장소 선택") }
    var serial by remember { mutableStateOf(preSerial) }

    // 서버에서 룸 목록을 불러오기
    LaunchedEffect(homeId) {
        scope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "homeId: $homeId")
                val response = RetrofitClient.apiService.getAllRoom(
                    "Bearer ${AppController.prefs.getToken()}",
                    homeId
                )
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        roomsList = response.body()?.rooms ?: emptyList()
                        if (roomsList.isNotEmpty()) {
                            val firstRoom = roomsList.first()
                            roomName = firstRoom.name
                            roomId = firstRoom.id
                        } else {
                            roomName = "장소 선택"
                            roomId = ""
                        }
                    }
                } else {
                    Log.e(TAG, "getAllRoom: $response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getAllRoom: $e")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .windowInsetsPadding(WindowInsets.statusBars)
            .pointerInput(Unit) { detectTapGestures { keyboardController?.hide() } }
    ) {
        // 상단 바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "디바이스 추가",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .background(Color.Transparent)
                    .clickable { isRoomDialogOpen = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = roomName,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(5.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_down),
                    contentDescription = "Dropdown",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }

            // 리스트 다이얼로그
            if (isRoomDialogOpen) {
                RoomSelectorBottomSheet(
                    roomsList = roomsList,
                    isDialogOpen = isRoomDialogOpen,
                    onRoomSelected = { selectedRoom ->
                        roomName = selectedRoom.name
                        roomId = selectedRoom.id
                        isRoomDialogOpen = false
                    },
                    onAddRoomClick = {
                        val bundle = Bundle().apply { putString("homeId", homeId) }
                        replaceFragment2(
                            fragmentManager = activity.supportFragmentManager,
                            fragment = RoomListFragment(),
                            bundle = bundle
                        )
                        isRoomDialogOpen = false
                    },
                    onDismiss = { isRoomDialogOpen = false }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text("이름", fontSize = 14.sp, color = Color.White)
            Spacer(modifier = Modifier.height(5.dp))
            WhiteBoxInput(
                value = name,
                onValueChange = { if (it.length <= 20) name = it },
                placeholder = "예: 아르온A",
                modifier = Modifier.fillMaxWidth(),
                hintColor = Color(0xFFBFC7D5),
                textColor = Color.Black
            )

            Spacer(modifier = Modifier.height(25.dp))

            Text("시리얼 번호", fontSize = 14.sp, color = Color.White)
            Spacer(Modifier.height(10.dp))
            OutlineOnlyInput(
                value = serial,
                onValueChange = { input -> if (input.length <= 30) serial = input },
                readOnly = false,
                modifier = Modifier.fillMaxWidth(),
                textColor = if (serial.isBlank()) Color(0xFFBFC7D5) else Color.White,
                hintColor = Color(0xFFBFC7D5),
                placeholder = "시리얼 번호",
                height = 46.dp
            )

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    val b = Bundle().apply { putString("homeId", homeId) }
                    replaceFragment2(
                        fragmentManager = activity.supportFragmentManager,
                        fragment = QrScannerFragment(),
                        bundle = b
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text("QR 스캔", fontSize = 14.sp, color = Color.White)
            }
        }

        Button(
            onClick = {
                when {
                    name.isBlank() -> Toast.makeText(context, "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                    serial.isBlank() -> Toast.makeText(context, "시리얼 번호가 없습니다.", Toast.LENGTH_SHORT).show()
                    homeId.isBlank() -> Toast.makeText(context, "홈 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    roomId.isBlank() -> Toast.makeText(context, "장소 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    else -> {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val dto = DeviceDTO(
                                    name = name,
                                    serialNumber = serial,
                                    homeId = homeId,
                                    roomId = roomId
                                )
                                val res = RetrofitClient.apiService.createDevice(
                                    "Bearer ${AppController.prefs.getToken()}",
                                    dto
                                )
                                withContext(Dispatchers.Main) {
                                    if (res.isSuccessful) {
                                        Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                                        when(layoutType) {
                                            1 -> replaceFragment2(activity.supportFragmentManager, DeviceFragment(), null)
                                            2 -> {
                                                val bundle = Bundle().apply { putString("homeId", homeId) }
                                                replaceFragment2(activity.supportFragmentManager, SettingHomeFragment(), bundle)
                                            }
                                            else -> activity.onBackPressedDispatcher.onBackPressed()
                                        }
                                    } else {
                                        Log.e(TAG, "createDevice: $res")
                                        Toast.makeText(context, "저장 실패", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "$e")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "에러 발생", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(45.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF184378))
        ) {
            Text("저장", color = Color.White, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSelectorBottomSheet(
    roomsList: List<Room>,
    isDialogOpen: Boolean,
    onRoomSelected: (Room) -> Unit,
    onAddRoomClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    LaunchedEffect(isDialogOpen) {
        if (isDialogOpen) sheetState.show() else sheetState.hide()
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = "장소 선택",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(15.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(roomsList) { room ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                onRoomSelected(room)
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
                            Text(
                                text = room.name,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
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
                            onAddRoomClick()
                        }
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "장소 설정",
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
