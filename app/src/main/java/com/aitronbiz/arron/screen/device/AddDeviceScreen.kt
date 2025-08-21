package com.aitronbiz.arron.screen.device

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.DeviceDTO
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import com.aitronbiz.arron.api.response.Room
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.Text
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.navigation.NavController
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aitronbiz.arron.R
import com.aitronbiz.arron.screen.home.QrScannerScreen

@Composable
fun AddDeviceScreen(
    navController: NavController,
    homeId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var roomsList by remember { mutableStateOf<List<Room>>(emptyList()) }
    var roomId by remember { mutableStateOf("") }
    var isRoomDialogOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var roomName by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }

    // 서버에서 룸 목록을 불러오기
    LaunchedEffect(homeId) {
        scope.launch(Dispatchers.IO) {
            try {
                val token = AppController.prefs.getToken()
                val response = RetrofitClient.apiService.getAllRoom(
                    "Bearer $token",
                    homeId
                )
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        roomsList = response.body()?.rooms ?: emptyList()
                        if (roomsList.isNotEmpty()) {
                            val firstRoom = roomsList[0]
                            roomName = firstRoom.name
                            roomId = firstRoom.id
                        }
                    }
                }else {
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
            .pointerInput(Unit) {
                detectTapGestures {
                    keyboardController?.hide()
                }
            }
    ) {
        // 상단 바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            androidx.compose.material.IconButton(onClick = {
                val popped = navController.popBackStack()
                if (!popped) navController.navigateUp()
            }) {
                androidx.compose.material.Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "디바이스 추가",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .weight(1f)
        ) {
            // 장소 선택
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
                    color = if (roomName == "장소 선택") Color.LightGray else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(5.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_down),
                    contentDescription = "Dropdown",
                    modifier = Modifier
                        .size(16.dp)
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
                        navController.navigate("roomList/$homeId")
                        isRoomDialogOpen = false
                    },
                    onDismiss = {
                        isRoomDialogOpen = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(17.dp))

            // 이름
            Text("이름", fontSize = 14.sp, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))
            WhiteBoxInput(
                value = name,
                onValueChange = { name = it },
                placeholder = "예: 아르온A",
                modifier = Modifier.fillMaxWidth(),
                hintColor = Color(0xFFBFC7D5),
                textColor = Color.Black
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text("시리얼 번호", fontSize = 14.sp, color = Color.White)
            Spacer(Modifier.height(6.dp))
            OutlineOnlyInput(
                value = if (serial.isBlank()) "QR 스캔으로 입력됩니다" else serial,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                textColor = if (serial.isBlank()) Color(0xFFBFC7D5) else Color.White,
                hintColor = Color(0xFFBFC7D5),
                placeholder = "시리얼 번호",
                height = 46.dp
            )

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showScanner = true },
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

            // 스캐너 오버레이
            if (showScanner) {
                Dialog(
                    onDismissRequest = { showScanner = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(Modifier.fillMaxSize().background(Color(0xFF000000))) {
                        QrScannerScreen(
                            onBack = { showScanner = false },
                            onScanned = { value ->
                                serial = value
                                showScanner = false
                            }
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                when {
                    name.isBlank() -> Toast.makeText(context, "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                    serial.isBlank() -> Toast.makeText(context, "시리얼 번호를 입력하세요", Toast.LENGTH_SHORT).show()
                    homeId.isBlank() -> Toast.makeText(context, "홈 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    roomId.isBlank() -> Toast.makeText(context, "장소 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    else -> {
                        scope.launch(Dispatchers.IO) {
                            try {
                                withContext(Dispatchers.Main) {
                                    val dto = DeviceDTO(
                                        name = name,
                                        serialNumber = serial,
                                        homeId = homeId,
                                        roomId = roomId
                                    )
                                    val createDevice = RetrofitClient.apiService.createDevice(
                                        "Bearer ${AppController.prefs.getToken()}",
                                        dto
                                    )
                                    withContext(Dispatchers.Main) {
                                        if (createDevice.isSuccessful) {
                                            Log.d(TAG, "createRoom: ${createDevice.body()}")
                                            Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                                            val popped = navController.popBackStack()
                                            if (!popped) navController.navigateUp()
                                        } else {
                                            Log.e(TAG, "createDevice: $createDevice")
                                            Toast.makeText(context, "저장 실패", Toast.LENGTH_SHORT).show()
                                        }
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
        if (isDialogOpen) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
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

@Composable
private fun WhiteBoxInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    hintColor: Color = Color(0xFFBFC7D5),
    textColor: Color = Color.Black,
    height: Dp = 46.dp,
    singleLine: Boolean = true,
) {
    Box(
        modifier = modifier
            .height(height)
            .defaultMinSize(minHeight = 1.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
    ) {
        if (value.isEmpty()) {
            androidx.compose.material3.Text(
                text = placeholder,
                color = hintColor,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 12.dp)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = 14.sp, color = textColor),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun OutlineOnlyInput(
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    hintColor: Color = Color(0xFFBFC7D5),
    placeholder: String = "",
    singleLine: Boolean = true,
    height: Dp = 46.dp
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(height)
            .defaultMinSize(minHeight = 1.dp)
            .border(1.dp, Color.White.copy(alpha = 0.85f), shape)
            .padding(horizontal = 12.dp)
    ) {
        if (value.isEmpty()) {
            androidx.compose.material3.Text(
                text = placeholder,
                color = hintColor,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = { if (!readOnly) onValueChange(it) },
            readOnly = readOnly,
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = 14.sp, color = textColor),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
        )
    }
}