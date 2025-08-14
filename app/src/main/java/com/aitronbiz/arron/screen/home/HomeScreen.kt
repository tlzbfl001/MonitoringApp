package com.aitronbiz.arron.screen.home

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.util.ActivityAlertStore
import com.aitronbiz.arron.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onNavigateDevice: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var topBarHeight by remember { mutableIntStateOf(0) }
    var showHomeSelector by remember { mutableStateOf(false) }
    var showMonthlyCalendar by remember { mutableStateOf(false) }
    var showPresenceSheet by remember { mutableStateOf(false) }
    var homeId by remember { mutableStateOf("") }
    var roomId by remember { mutableStateOf("") }
    var hasUnreadNotification by remember { mutableStateOf(false) }
    val activityAlerts by ActivityAlertStore.alertByRoom.collectAsState()
    val today = remember { LocalDate.now() }
    val isToday = selectedDate == today

    // 현재 선택된 방의 위험 여부 → 카드 깜박임에 사용
    val activityDanger = isToday && roomId.isNotBlank() && (activityAlerts[roomId] == true)

    // 전체 방 중 하나라도 위험이면 true → 상단 재실 배지 옆 느낌표에 사용
    val anyRoomDanger = isToday && activityAlerts.values.any { it }

    // 초기 로딩
    LaunchedEffect(Unit) {
        if (!AppController.prefs.getToken().isNullOrEmpty()) {
            viewModel.fetchHomes(AppController.prefs.getToken()!!)
        }
        viewModel.checkNotifications { hasUnreadNotification = it }
    }
    DisposableEffect(Unit) { onDispose { viewModel.stopActivityAlertWatcher() } }

    // homes 로딩 후 첫 홈 선택
    LaunchedEffect(viewModel.homes) {
        if (viewModel.homes.isNotEmpty() && homeId.isBlank()) {
            val first = viewModel.homes.first()
            homeId = first.id
            viewModel.selectHome(first)
        }
    }

    // roomId 초기화
    LaunchedEffect(viewModel.rooms, viewModel.presenceByRoomId) {
        roomId = if (viewModel.rooms.isNotEmpty()) {
            viewModel.selectedRoomId ?: viewModel.rooms.first().id
        } else ""
    }

    // rooms가 준비되면 그때 watcher 시작
    LaunchedEffect(viewModel.rooms) {
        if (!AppController.prefs.getToken().isNullOrEmpty() && viewModel.rooms.isNotEmpty()) {
            viewModel.startActivityAlertWatcher(AppController.prefs.getToken()!!)
        }
    }

    // 현재 선택된 roomId의 재실 여부
    val selectedRoomPresent = roomId.isNotBlank() && (viewModel.presenceByRoomId[roomId] == true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F2B4E))
        ) {
            item {
                Box(
                    modifier = Modifier
                        .height(with(LocalDensity.current) { topBarHeight.toDp() })
                        .fillMaxWidth()
                        .background(Color(0xFF174176))
                )

                WeeklyCalendarPager(
                    selectedDate = selectedDate,
                    onDateSelected = {
                        selectedDate = it
                        viewModel.updateSelectedDate(it)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetectionCardList(
                    selectedDate = selectedDate,
                    onFallClick = { navigateIfHomeExists(homeId, roomId, context, navController, "fallDetection") },
                    onActivityClick = { navigateIfHomeExists(homeId, roomId, context, navController, "activityDetection") },
                    onRespirationClick = { navigateIfHomeExists(homeId, roomId, context, navController, "respirationDetection") },
                    onLifePatternClick = { navigateIfHomeExists(homeId, roomId, context, navController, "lifePattern") },
                    onEntryPatternClick = { navigateIfHomeExists(homeId, roomId, context, navController, "entryPattern") },
                    onNightActivityClick = { navigateIfHomeExists(homeId, roomId, context, navController, "nightActivity") },
                    onEmergencyCallClick = { navigateIfHomeExists(homeId, roomId, context, navController, "nightActivity") },
                    activityDanger = activityDanger
                )

                Spacer(modifier = Modifier.height(50.dp))
            }
        }

        // 상단 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF174176).copy(alpha = 0.8f))
                .onGloballyPositioned { coordinates -> topBarHeight = coordinates.size.height }
        ) {
            Column(
                modifier = Modifier.padding(start = 22.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
            ) {
                TopBar(
                    viewModel = viewModel,
                    navController = navController,
                    hasUnreadNotification = hasUnreadNotification,
                    onClickHomeSelector = { showHomeSelector = true },
                    onClickPresence = { showPresenceSheet = true },
                    presentTextIsPresent = selectedRoomPresent,
                    onNavigateDevice = onNavigateDevice,
                    onNavigateSettings = onNavigateSettings,
                    showGlobalDanger = anyRoomDanger
                )

                Spacer(modifier = Modifier.height(10.dp))

                WeeklyCalendarHeader(
                    selectedDate = selectedDate,
                    onClick = { showMonthlyCalendar = true }
                )

                if (showHomeSelector) {
                    HomeSelectorBottomSheet(
                        viewModel = viewModel,
                        onDismiss = { showHomeSelector = false },
                        onHomeSelected = { selectedHome ->
                            viewModel.selectHome(selectedHome)
                            homeId = selectedHome.id
                        },
                        onNavigateToSettingHome = {
                            showHomeSelector = false
                            navController.navigate("homeList")
                        }
                    )
                }

                if (showMonthlyCalendar) {
                    MonthlyCalendarBottomSheet(
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        onDismiss = { showMonthlyCalendar = false }
                    )
                }

                if (showPresenceSheet) {
                    PresenceBottomSheet(
                        viewModel = viewModel,
                        selectedRoomId = roomId,
                        onSelectRoom = { roomId = it },
                        onDismiss = { showPresenceSheet = false }
                    )
                }
            }
        }
    }
}

@Composable
fun TopBar(
    viewModel: MainViewModel,
    navController: NavController,
    hasUnreadNotification: Boolean,
    onClickHomeSelector: () -> Unit,
    onClickPresence: () -> Unit,
    presentTextIsPresent: Boolean,
    onNavigateDevice: () -> Unit,
    onNavigateSettings: () -> Unit,
    showGlobalDanger: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onClickHomeSelector() }
            ) {
                Text(viewModel.selectedHomeName, color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_down),
                    contentDescription = "홈 메뉴",
                    modifier = Modifier.size(15.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(7.dp))
            PresenceStatus(
                present = presentTextIsPresent,
                showExclaim = showGlobalDanger,
                onClick = onClickPresence
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.clickable { navController.navigate("notification") }
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bell),
                        contentDescription = "알림",
                        modifier = Modifier.size(15.dp),
                        tint = Color.White
                    )
                    if (hasUnreadNotification) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .offset(x = (-2).dp)
                                .background(Color.Red, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresenceStatus(
    present: Boolean,
    showExclaim: Boolean,
    onClick: () -> Unit
) {
    val bg = if (present) Color(0x3322D3EE) else Color(0x339A9EA8)
    val fg = if (present) Color.Cyan else Color.LightGray

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (present) "재실중" else "부재중",
            color = fg,
            fontSize = 11.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(bg)
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )

        // 전체 방 중 하나라도 위험이면 표시
        if (showExclaim) {
            Spacer(modifier = Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text("!", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WeeklyCalendarHeader(
    selectedDate: LocalDate,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${selectedDate.monthValue}.${selectedDate.dayOfMonth} " +
                    selectedDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
            color = Color.White,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.width(7.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_caret_down),
            contentDescription = "날짜 선택",
            modifier = Modifier.size(8.dp),
            tint = Color.White
        )
    }
}

@Composable
fun WeeklyCalendarPager(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    // 오늘 주(=page 1000)가 마지막 페이지가 되도록 pageCount = 1001
    val pagerState = rememberPagerState(initialPage = 1000) { 1001 }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val baseSunday = remember { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) }
    val days = listOf("일", "월", "화", "수", "목", "금", "토")

    LaunchedEffect(selectedDate) {
        val targetSunday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val offset = ChronoUnit.WEEKS.between(baseSunday, targetSunday)
        // 오늘 주(1000)보다 미래로 못 가도록 상한 고정
        val targetPage = (1000 + offset.toInt()).coerceAtMost(1000)
        if (pagerState.currentPage != targetPage) {
            scope.launch { pagerState.scrollToPage(targetPage) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF174176))
            .padding(bottom = 7.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEachIndexed { index, day ->
                val isSelected = (selectedDate.dayOfWeek.value % 7) == index
                Box(
                    modifier = Modifier
                        .size(25.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF174176) else Color.LightGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val startOfWeek = baseSunday.plusWeeks((page - 1000).toLong())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (0..6).forEach { offset ->
                    val date = startOfWeek.plusDays(offset.toLong())
                    val disabled = date.isAfter(today) // 미래 날짜 비활성화
                    Box(
                        modifier = Modifier
                            .size(23.dp)
                            .clip(CircleShape)
                            .alpha(if (disabled) 0.4f else 1f)
                            .clickable(enabled = !disabled) { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = date.dayOfMonth.toString(), color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyCalendarBottomSheet(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    val today = LocalDate.now()
    // 오늘이 포함된 달(=page 1000)이 마지막 페이지가 되도록 pageCount = 1001
    val pagerState = rememberPagerState(initialPage = 1000) { 1001 }
    var currentMonth by remember { mutableStateOf(today.withDayOfMonth(1)) }

    LaunchedEffect(selectedDate) {
        val offset = ChronoUnit.MONTHS.between(
            today.withDayOfMonth(1),
            selectedDate.withDayOfMonth(1)
        )
        // 미래 달로 점프 방지
        scope.launch { pagerState.scrollToPage((1000 + offset.toInt()).coerceAtMost(1000)) }
    }

    LaunchedEffect(pagerState.currentPage) {
        val monthOffset = pagerState.currentPage - 1000
        currentMonth = today.plusMonths(monthOffset.toLong()).withDayOfMonth(1)
    }

    fun rowsInMonth(firstDay: LocalDate): Int {
        val daysInMonth = firstDay.lengthOfMonth()
        val firstDayOfWeek = (firstDay.dayOfWeek.value % 7)
        val totalCells = ((daysInMonth + firstDayOfWeek + 6) / 7) * 7
        return totalCells / 7
    }

    val cellSize: Dp = 40.dp
    val reducedCellSize: Dp = cellSize - 8.dp

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 25.dp, bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(2f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_left),
                        contentDescription = "이전달",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        (pagerState.currentPage - 1).coerceAtLeast(0)
                                    )
                                }
                            }
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "${currentMonth.year}.${currentMonth.monthValue}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_right),
                        contentDescription = "다음달",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(22.dp)
                            // 오늘 달(page 1000)에서는 비활성
                            .clickable(enabled = pagerState.currentPage < 1000) {
                                if (pagerState.currentPage < 1000) {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                }
                            }
                    )
                }

                Text(
                    text = "오늘",
                    color = Color.Black,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.End)
                        .background(Color(0xFFECECEC), shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable {
                            onDateSelected(today)
                            scope.launch {
                                delay(500)
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                    Text(
                        text = day,
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val basePage = pagerState.currentPage
            val offsetFraction = pagerState.currentPageOffsetFraction
            val baseMonthFirst = today.plusMonths((basePage - 1000).toLong()).withDayOfMonth(1)
            val neighborPage = if (offsetFraction >= 0f) basePage + 1 else basePage - 1
            val neighborMonthFirst = today.plusMonths((neighborPage - 1000).toLong()).withDayOfMonth(1)

            val baseRows = rowsInMonth(baseMonthFirst)
            val neighborRows = rowsInMonth(neighborMonthFirst)
            val baseHeight = cellSize * baseRows
            val neighborHeight = cellSize * neighborRows
            val dynamicHeight = lerp(baseHeight, neighborHeight, abs(offsetFraction))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dynamicHeight)
            ) { page ->
                val monthOffset = page - 1000
                val currentMonth1 = today.plusMonths(monthOffset.toLong()).withDayOfMonth(1)
                val daysInMonth = currentMonth1.lengthOfMonth()
                val firstDayOfWeek = (currentMonth1.dayOfWeek.value % 7)
                val totalCells = ((daysInMonth + firstDayOfWeek + 6) / 7) * 7

                val dates = (0 until totalCells).map { index ->
                    val day = index - firstDayOfWeek + 1
                    if (day in 1..daysInMonth) currentMonth1.withDayOfMonth(day) else null
                }

                Column {
                    dates.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            week.forEach { date ->
                                if (date != null) {
                                    val isSelected = date == selectedDate
                                    val isToday = date == today
                                    val disabled = date.isAfter(today) // 미래 날짜 비활성화
                                    val sizeToUse = if (isSelected || isToday) reducedCellSize else cellSize

                                    Box(
                                        modifier = Modifier
                                            .size(sizeToUse)
                                            .clip(CircleShape)
                                            .alpha(if (disabled) 0.4f else 1f)
                                            .background(
                                                when {
                                                    isSelected -> Color.Black
                                                    isToday -> Color(0xFFE0E0E0)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable(enabled = !disabled) {
                                                scope.launch {
                                                    onDateSelected(date)
                                                    delay(500)
                                                    sheetState.hide()
                                                    onDismiss()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            color = when {
                                                isSelected -> Color.White
                                                isToday -> Color.Black
                                                else -> Color.Gray
                                            }
                                        )
                                    }
                                } else {
                                    Box(modifier = Modifier.size(cellSize))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetectionCardList(
    selectedDate: LocalDate,
    onFallClick: () -> Unit,
    onActivityClick: () -> Unit,
    onRespirationClick: () -> Unit,
    onLifePatternClick: () -> Unit,
    onEntryPatternClick: () -> Unit,
    onNightActivityClick: () -> Unit,
    onEmergencyCallClick: () -> Unit,
    activityDanger: Boolean
) {
    Column {
        DetectionCard("낙상감지", "1회", R.drawable.img1, onClick = onFallClick)
        DetectionCard(
            title = "활동량감지",
            value = "9시간 활동",
            imageRes = R.drawable.img2,
            isDanger = activityDanger,
            onClick = onActivityClick
        )
        DetectionCard("호흡 감지", "분당 15회", R.drawable.img3, onClick = onRespirationClick)
        DetectionCard("생활 패턴", "평균 취침 23:00\n평균 기상 07:30", R.drawable.img5, onClick = onLifePatternClick)
        DetectionCard("출입 패턴", "일일 출입 2회", R.drawable.img6, onClick = onEntryPatternClick)
        DetectionCard("야간활동 이상감지", "야간 출입 1회", R.drawable.img7, onClick = onNightActivityClick)
        DetectionCard("구조요청 자동연결", "", R.drawable.img8, onClick = onEmergencyCallClick)
    }
}

@Composable
fun DetectionCard(
    title: String,
    value: String?,
    imageRes: Int,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val blinkAlpha: Float = if (isDanger) {
        val infinite = rememberInfiniteTransition(label = "dangerBlink")
        val a by infinite.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "alpha"
        )
        a
    } else 1f

    val normalBg = Color(0x5A185078)
    val dangerBase = Color(0xFFEF5350)
    val dangerBg = dangerBase.copy(alpha = 0.48f * blinkAlpha)
    val backgroundColor = if (isDanger) dangerBg else normalBg
    val borderColor = if (isDanger) dangerBase.copy(alpha = blinkAlpha) else Color(0xFF185078)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.6.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier.size(65.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = if (value.isNullOrBlank()) Arrangement.Center else Arrangement.Top
            ) {
                Text(title, color = Color.White, fontSize = 16.sp)
                if (!value.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(value, color = Color.LightGray, fontSize = 13.sp)
                }
            }
        }

        if (isDanger) {
            DangerBadge(
                blinkAlpha = blinkAlpha,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
            )
        }
    }
}

@Composable
private fun DangerBadge(
    blinkAlpha: Float,
    modifier: Modifier = Modifier
) {
    val border = Color(0xFFF28B82)
    val textColor = Color(0xFFF87171)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.2.dp,
                color = border.copy(alpha = blinkAlpha),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            "위험",
            color = textColor.copy(alpha = 0.85f * blinkAlpha + 0.15f),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSelectorBottomSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onHomeSelected: (Home) -> Unit,
    onNavigateToSettingHome: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) {
        val token = AppController.prefs.getToken()
        if (!token.isNullOrEmpty()) {
            viewModel.fetchHomes(token)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(viewModel.homes, key = { it.id }) { home ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                onHomeSelected(home)
                                scope.launch {
                                    delay(300)
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
                            if (viewModel.selectedHomeName == home.name) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = "선택됨",
                                    tint = Color(0xFF174176),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Spacer(modifier = Modifier.width(28.dp))
                            }
                            Text(
                                text = home.name,
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
                androidx.compose.material3.Icon(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresenceBottomSheet(
    viewModel: MainViewModel,
    selectedRoomId: String,
    onSelectRoom: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
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
                text = "룸 목록",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(15.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(viewModel.rooms, key = { it.id }) { room ->
                    val checked = selectedRoomId == room.id
                    val present = viewModel.presenceByRoomId[room.id] == true

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                onSelectRoom(room.id)
                                viewModel.selectRoom(room.id)
                                scope.launch {
                                    delay(300)
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
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (checked) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = "선택됨",
                                    tint = Color(0x7C5D5F65),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Spacer(modifier = Modifier.width(28.dp))
                            }

                            Text(
                                text = room.name,
                                fontSize = 16.sp,
                                color = Color.Black,
                                modifier = Modifier.weight(1f)
                            )

                            val badgeBg: Color
                            val badgeFg: Color
                            if (present) {
                                badgeBg = Color(0x3322D3EE)
                                badgeFg = Color(0x8D006E7E)
                            } else {
                                badgeBg = Color(0x339A9EA8)
                                badgeFg = Color(0x7C5D5F65)
                            }
                            Text(
                                text = if (present) "재실중" else "부재중",
                                color = badgeFg,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun navigateIfHomeExists(
    homeId: String,
    roomId: String,
    context: Context,
    navController: NavController,
    route: String
) {
    if (homeId.isNotBlank()) {
        navController.navigate("$route/$homeId/$roomId")
    } else {
        Toast.makeText(context, "홈 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
    }
}