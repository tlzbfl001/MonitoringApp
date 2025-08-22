package com.aitronbiz.arron.screen.init

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.DeviceDTO
import com.aitronbiz.arron.api.dto.DeviceDTO2
import com.aitronbiz.arron.api.dto.HomeDTO
import com.aitronbiz.arron.api.dto.HomeDTO1
import com.aitronbiz.arron.api.dto.HomeDTO2
import com.aitronbiz.arron.api.dto.RoomDTO
import com.aitronbiz.arron.api.response.Address
import com.aitronbiz.arron.component.OutlineOnlyInput
import com.aitronbiz.arron.component.WhiteBoxInput
import com.aitronbiz.arron.model.AddressResult
import com.aitronbiz.arron.model.HomeForm
import com.aitronbiz.arron.model.User
import com.aitronbiz.arron.screen.MainActivity
import com.aitronbiz.arron.screen.home.QrScannerScreen
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SetupStep { HOME, ROOM, DEVICE }

class InitAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SetupFlowScreen(
                onFinishGoMain = {}
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SetupFlowScreen(
    onFinishGoMain: () -> Unit
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableStateOf(SetupStep.HOME) }
    var homeForm by rememberSaveable { mutableStateOf(HomeForm()) }
    var roomName by rememberSaveable { mutableStateOf("") }
    var lastBackPressedAt by remember { mutableStateOf(0L) }

    // 뒤로가기 핸들링
    BackHandler {
        when (step) {
            SetupStep.HOME -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedAt <= 2000L) {
                    (context as? ComponentActivity)?.finishAffinity()
                } else {
                    lastBackPressedAt = now
                    Toast.makeText(context, "한 번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
                }
            }
            SetupStep.ROOM -> step = SetupStep.HOME
            SetupStep.DEVICE -> step = SetupStep.ROOM
        }
    }

    val enterTopDown = slideInVertically(
        initialOffsetY = { -it },
        animationSpec = tween(350, easing = FastOutSlowInEasing)
    ) + fadeIn(tween(200))
    val exitDown = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(350, easing = FastOutSlowInEasing)
    ) + fadeOut(tween(200))

    Surface(Modifier.fillMaxSize(), color = Color(0xFF0F2B4E)) {
        AnimatedContent(
            targetState = step,
            transitionSpec = { (enterTopDown with exitDown).using(SizeTransform(clip = false)) },
            label = "step-transition"
        ) { s ->
            when (s) {
                SetupStep.HOME -> AddHomeScreen(
                    onNext = { collected ->
                        homeForm = collected
                        step = SetupStep.ROOM
                    }
                )
                SetupStep.ROOM -> AddRoomScreen(
                    initial = roomName,
                    onNext = { rn ->
                        roomName = rn
                        step = SetupStep.DEVICE
                    }
                )
                SetupStep.DEVICE -> AddDeviceScreen(
                    home = homeForm,
                    roomName = roomName
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHomeScreen(
    onNext: (HomeForm) -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val hint = Color(0xFFBFC7D5)

    var homeName by remember { mutableStateOf("") }
    var province by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var street by rememberSaveable { mutableStateOf("") }
    var fullAddress by rememberSaveable { mutableStateOf("") }
    var postalCode by rememberSaveable { mutableStateOf("") }

    var showSearch by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { keyboard?.hide() } }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F2B4E))
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "아르온 앱을 사용하기 전에\n홈, 장소, 기기를 등록해주세요",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 25.dp, start = 20.dp, end = 20.dp),
                lineHeight = 30.sp
            )

            Spacer(Modifier.height(30.dp))

            // 홈 이름
            Text("홈 이름", color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(start = 20.dp))
            Spacer(Modifier.height(10.dp))
            WhiteBoxInput(
                value = homeName,
                onValueChange = { if (it.length <= 10) homeName = it },
                placeholder = "홈 이름을 입력하세요",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                hintColor = hint,
                height = 46.dp
            )

            Spacer(Modifier.height(30.dp))

            // 주소
            Text("주소", color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(start = 20.dp))
            Spacer(Modifier.height(10.dp))

            Column(Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val hasAddress = fullAddress.isNotBlank()
                    val (postalDisplay, postalTextColor) = when {
                        !hasAddress -> "" to hint
                        postalCode.isBlank() -> "우편번호 없음" to hint
                        else -> postalCode to Color.White
                    }

                    OutlineOnlyInput(
                        value = postalDisplay,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        textColor = postalTextColor,
                        hintColor = hint,
                        placeholder = "우편번호",
                        height = 46.dp
                    )

                    Spacer(Modifier.width(10.dp))

                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                        Button(
                            onClick = { showSearch = true },
                            modifier = Modifier
                                .height(46.dp)
                                .defaultMinSize(minHeight = 1.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF184378),
                                contentColor = Color.White
                            )
                        ) { Text("주소 검색", fontSize = 14.sp) }
                    }
                }

                if (showSearch) {
                    AddressSearchDialog(
                        onDismiss = { showSearch = false },
                        onSelected = { r ->
                            province = r.province
                            city = r.city
                            street = r.street
                            fullAddress = r.fullAddress
                            postalCode = r.postalCode
                            showSearch = false
                        }
                    )
                }

                Spacer(Modifier.height(10.dp))

                OutlineOnlyInput(
                    value = fullAddress,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    textColor = if (fullAddress.isBlank()) hint else Color.White,
                    hintColor = hint,
                    placeholder = "상세주소",
                    singleLine = false,
                    height = 46.dp
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (homeName.trim().isEmpty()) {
                        Toast.makeText(context, "홈 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (fullAddress.isBlank()) {
                        Toast.makeText(context, "주소를 검색해 선택하세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onNext(
                        HomeForm(
                            homeName = homeName.trim(),
                            province = province,
                            city = city,
                            street = street,
                            fullAddress = fullAddress,
                            postalCode = postalCode
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF184378),
                    contentColor = Color.White
                )
            ) { Text("다음", color = Color.White, fontSize = 16.sp) }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun AddRoomScreen(
    initial: String = "",
    onNext: (String) -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val hint = Color(0xFFBFC7D5)
    var roomName by remember { mutableStateOf(initial) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .pointerInput(Unit) { detectTapGestures { keyboard?.hide() } }
    ) {
        Spacer(Modifier.height(2.dp))
        Text(
            "장소 등록",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        )

        Spacer(Modifier.height(4.dp))

        Text("장소 이름", fontSize = 15.sp, color = Color.White, modifier = Modifier.padding(start = 20.dp))
        Spacer(Modifier.height(4.dp))

        WhiteBoxInput(
            value = roomName,
            onValueChange = { if (it.length <= 10) roomName = it },
            placeholder = "예: 거실, 주방",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            hintColor = hint,
            height = 46.dp
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                if (roomName.trim().isEmpty()) {
                    Toast.makeText(context, "장소 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                } else {
                    onNext(roomName.trim())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF184378), contentColor = Color.White)
        ) { Text("다음", fontSize = 16.sp) }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun AddDeviceScreen(
    home: HomeForm,
    roomName: String
) {
    val context = LocalContext.current
    var showScanner by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var deviceName by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }
    val hint = Color(0xFFBFC7D5)

    Box(Modifier
        .fillMaxSize()
        .background(Color(0xFF0F2B4E))) {
        Column(Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                androidx.compose.material.Text(
                    text = "디바이스 추가",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(25.dp))

            Text("장소명", fontSize = 15.sp, color = Color.White, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                roomName.ifBlank { "미입력" },
                color = Color.White,
                fontSize = 17.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(25.dp))

            Text("이름", fontSize = 15.sp, color = Color.White, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
            WhiteBoxInput(
                value = deviceName,
                onValueChange = { deviceName = it },
                placeholder = "예: 아르온A",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                hintColor = hint,
                height = 46.dp
            )

            Spacer(Modifier.height(25.dp))

            Text("시리얼 번호", fontSize = 15.sp, color = Color.White, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
            OutlineOnlyInput(
                value = if (serial.isBlank()) "QR 스캔으로 입력됩니다" else serial,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                textColor = if (serial.isBlank()) hint else Color.White,
                hintColor = hint,
                placeholder = "시리얼 번호",
                height = 46.dp
            )

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showScanner = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White)
            ) { Text("QR 스캔", fontSize = 15.sp) }

            if (showScanner) {
                Dialog(
                    onDismissRequest = { showScanner = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(Modifier
                        .fillMaxSize()
                        .background(Color(0xFF000000))) {
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

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    when {
                        home.homeName.isBlank() || home.fullAddress.isBlank() ->
                            Toast.makeText(context, "홈 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                        roomName.isBlank() ->
                            Toast.makeText(context, "장소 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                        deviceName.isBlank() ->
                            Toast.makeText(context, "디바이스 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                        serial.isBlank() ->
                            Toast.makeText(context, "QR 스캔이 필요합니다.", Toast.LENGTH_SHORT).show()
                        else -> {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val token = "Bearer ${AppController.prefs.getToken()}"
                                    // 홈
                                    val homeId: String = run {
                                        val dto = HomeDTO1(
                                            name = "나의 홈"
                                        )
                                        val res = RetrofitClient.apiService.createHome2(token, dto)
                                        if (!res.isSuccessful) throw RuntimeException("홈 저장 실패")
                                        res.body()!!.home.id
                                    }
                                    // 장소
                                    val roomId: String = run {
                                        val rDto = RoomDTO(name = "나의 장소", homeId = homeId)
                                        val rRes = RetrofitClient.apiService.createRoom(token, rDto)
                                        if (!rRes.isSuccessful) throw RuntimeException("장소 저장 실패")
                                        val listRes = RetrofitClient.apiService.getAllRoom(token, homeId)
                                        if (!listRes.isSuccessful) throw RuntimeException("장소 목록 조회 실패")
                                        listRes.body()?.rooms?.firstOrNull { it.name == roomName }?.id
                                            ?: throw RuntimeException("생성된 장소를 찾을 수 없습니다.")
                                    }
                                    // 디바이스
                                    val dDto = DeviceDTO2(
                                        name = "나의 기기",
                                        homeId = homeId,
                                        roomId = roomId
                                    )
                                    val dRes = RetrofitClient.apiService.createDevice2(token, dDto)
                                    if (!dRes.isSuccessful) throw RuntimeException("디바이스 저장 실패")

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "가입을 환영합니다!", Toast.LENGTH_SHORT).show()
                                        context.startActivity(
                                            Intent(context, MainActivity::class.java).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                                putExtra("showWelcome", true)
                                            }
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error: $e")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, e.message ?: "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .height(45.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF184378))
            ) { Text("완료", color = Color.White, fontSize = 15.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSearchDialog(
    onDismiss: () -> Unit,
    onSelected: (AddressResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hint = Color(0xFFBFC7D5)
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<Address>()) }
    var selected by remember { mutableStateOf<Address?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF102A4D))
        ) {
            Column(Modifier
                .fillMaxWidth()
                .padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "주소 검색",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) { Text("닫기", fontSize = 15.sp) }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlineOnlyInput(
                        value = query,
                        onValueChange = { query = it },
                        readOnly = false,
                        modifier = Modifier.weight(1f),
                        textColor = Color.White,
                        hintColor = hint,
                        placeholder = "예) 판교역로 235, 성암로 189",
                        height = 46.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentEnforcement provides false
                    ) {
                        FilledIconButton(
                            onClick = {
                                if (query.isBlank()) {
                                    Toast.makeText(context, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show()
                                    results = emptyList()
                                    selected = null
                                    return@FilledIconButton
                                }
                                scope.launch {
                                    val token = "Bearer ${AppController.prefs.getToken()}"
                                    Log.d(TAG, "getToken: ${AppController.prefs.getToken()}")
                                    val resp = withContext(Dispatchers.IO) {
                                        RetrofitClient.apiService.searchAddress(token, query.trim())
                                    }
                                    if (resp.isSuccessful) {
                                        results = resp.body()?.addresses ?: emptyList()
                                        selected = null
                                        if (results.isEmpty()) {
                                            Toast.makeText(context, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "검색 실패", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .defaultMinSize(minHeight = 1.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF184378)
                            )
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "검색",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (results.isEmpty() && query.isNotBlank()) {
                    Text(
                        "검색 결과가 없습니다.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 420.dp)
                    ) {
                        itemsIndexed(results, key = { index, item ->
                            "${item.postalCode}_${item.province}_${item.city}_${item.street}_$index"
                        }) { _, item ->
                            val isSelected = selected == item
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isSelected)
                                            Modifier.border(
                                                1.dp,
                                                Color.White,
                                                RoundedCornerShape(10.dp)
                                            )
                                        else Modifier
                                    )
                                    .clickable { selected = item }
                                    .padding(vertical = 10.dp, horizontal = 10.dp)
                            ) {
                                val postalStr = item.postalCode?.toString().orEmpty()
                                val full = listOf(
                                    item.province.orEmpty(),
                                    item.city.orEmpty(),
                                    item.street.orEmpty()
                                ).filter { it.isNotBlank() }.joinToString(" ")

                                Text(
                                    if (postalStr.isNotBlank()) postalStr else "우편번호 없음",
                                    color = Color(0xFFFF5A5A),
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "도로명  $full",
                                    color = Color(0xFF99C7FF),
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "지번    ${item.province.orEmpty()} ${item.city.orEmpty()}",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        val sel = selected ?: run {
                            Toast.makeText(context, "주소를 선택하세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val full = listOf(sel.province.orEmpty(), sel.city.orEmpty(), sel.street.orEmpty())
                            .filter { it.isNotBlank() }.joinToString(" ")
                        val postal = extractPostal(sel.postalCode, sel.city, sel.street, full)
                        onSelected(
                            AddressResult(
                                postalCode = postal,
                                fullAddress = full,
                                province = sel.province.orEmpty(),
                                city = sel.city.orEmpty(),
                                street = sel.street.orEmpty()
                            )
                        )
                    },
                    enabled = selected != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF184378),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF184378).copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(bottom = 7.dp)
                ) {
                    Text("확인", fontSize = 16.sp)
                }
            }
        }
    }
}

private fun extractPostal(vararg candidates: String?): String {
    candidates.forEach { s ->
        val m = s?.let { Regex("\\b\\d{5}\\b").find(it) }?.value
        if (!m.isNullOrBlank()) return m
    }
    return ""
}