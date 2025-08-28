package com.aitronbiz.arron.screen.home

import android.content.Context
import android.os.Bundle
import android.view.View
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.screen.device.QrScannerFragment
import com.aitronbiz.arron.screen.notification.NotificationFragment
import com.aitronbiz.arron.screen.theme.MyAppTheme
import com.aitronbiz.arron.util.CustomUtil.replaceFragment
import com.aitronbiz.arron.viewmodel.MainViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainFragment : Fragment(R.layout.fragment_main) {
    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val composeView = view.findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            MyAppTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenNotification = {
                        replaceFragment(parentFragmentManager, NotificationFragment(), null)
                    },
                    onOpenHomeList = {
                        replaceFragment(parentFragmentManager, HomeListFragment(), null)
                    },
                    onFall = { homeId, date ->
                        replaceFragment(
                            parentFragmentManager,
                            FallDetectionFragment.newInstance(homeId, date), null
                        )
                    },
                    onActivity = { homeId, date ->
                        replaceFragment(
                            parentFragmentManager,
                            ActivityFragment.newInstance(homeId, date), null
                        )
                    },
                    onRespiration = { homeId, date ->
                        replaceFragment(
                            parentFragmentManager,
                            RespirationFragment.newInstance(homeId, date), null
                        )
                    },
                    onLifePattern = { homeId, date ->
                        replaceFragment(
                            parentFragmentManager,
                            LifePatternFragment.newInstance(homeId, date), null
                        )
                    },
                    onEntryPattern = { homeId, date ->
                        replaceFragment(
                            parentFragmentManager,
                            EntryPatternFragment.newInstance(homeId, date), null
                        )
                    },
                    onNightActivity = {
                        Toast.makeText(context, "지금은 준비중이에요.", Toast.LENGTH_SHORT).show()
                    },
                    onEmergencyCall = {
                        Toast.makeText(context, "지금은 준비중이에요.", Toast.LENGTH_SHORT).show()
                    },
                    onAddDevice = { homeId ->
                        val f = QrScannerFragment().apply {
                            arguments = Bundle().apply { putString("homeId", homeId) }
                        }
                        replaceFragment(requireActivity().supportFragmentManager, f, null)
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    viewModel: MainViewModel,
    onOpenNotification: () -> Unit,
    onOpenHomeList: () -> Unit,
    onFall: (homeId: String, date: LocalDate) -> Unit,
    onActivity: (homeId: String, date: LocalDate) -> Unit,
    onRespiration: (homeId: String, date: LocalDate) -> Unit,
    onLifePattern: (homeId: String, date: LocalDate) -> Unit,
    onEntryPattern: (homeId: String, date: LocalDate) -> Unit,
    onNightActivity: () -> Unit,
    onEmergencyCall: () -> Unit,
    onAddDevice: (homeId: String) -> Unit
) {
    val context = LocalContext.current
    var topBarHeight by remember { mutableIntStateOf(0) }
    val token by viewModel.token.collectAsState()
    val homes by viewModel.homes.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val selectedHomeId by viewModel.selectedHomeId.collectAsState("")
    val selectedHomeName by viewModel.selectedHomeName.collectAsState()
    val vmRoomId by viewModel.selectedRoomId.collectAsState(null)

    val today = remember { LocalDate.now() }
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isToday = selectedDate == today

    // 홈 재실 상태 + 첫 재실 방 이름
    val isHomePresent by viewModel.isHomePresent.collectAsState()
    val presentRoomName by viewModel.presentRoomName.collectAsState()

    val hasDevices: Boolean? by viewModel.hasDevices.collectAsState(initial = null)

    val todayFallCountByRoomId by viewModel.todayFallCountByRoomId.collectAsState()
    val dangerFallTodayByRoomId by viewModel.dangerTodayByRoomId.collectAsState()

    val todayActivityByRoomId by viewModel.todayActivityCurrentByRoomId.collectAsState()
    val dangerActivityTodayByRoomId by viewModel.dangerActivityTodayByRoomId.collectAsState()

    val todayRespByRoomId by viewModel.todayRespCurrentByRoomId.collectAsState()
    val dangerRespTodayByRoomId by viewModel.dangerRespTodayByRoomId.collectAsState()

    val lifePatterns by viewModel.lifePatterns.collectAsState()
    val lifePatternValue: String = lifePatterns?.let {
        "총 활동시간: " + formatMinutes(it.totalActiveMinutes)
    } ?: "총 활동시간: 0분"

    val entryPatterns by viewModel.entryPatterns.collectAsState()
    val entryPatternValue: String = entryPatterns?.let {
        "입실 0회\n퇴실 0회"
    } ?: "입실 0회\n퇴실 0회"

    var pastFallCount by remember { mutableIntStateOf(0) }
    var pastActivityAvg by remember { mutableIntStateOf(0) }
    var pastRespAvg by remember { mutableIntStateOf(0) }
    var showHomeSelector by remember { mutableStateOf(false) }
    var showMonthlyCalendar by remember { mutableStateOf(false) }
    var hasUnreadNotification by remember { mutableStateOf(false) }

    // 토큰 들어오면 홈/알림 로드
    LaunchedEffect(token) {
        if (token.isNotBlank()) {
            viewModel.fetchHomes(token)
            viewModel.checkNotifications(token) { hasUnreadNotification = it }
        } else {
            hasUnreadNotification = false
        }
    }

    // 화면 떠날 때 워처 정지
    DisposableEffect(Unit) {
        onDispose { viewModel.stopWatcher() }
    }

    // 홈 목록이 채워지고 아직 선택된 홈이 없다면 첫 홈 선택
    LaunchedEffect(homes) {
        if (homes.isNotEmpty() && selectedHomeId.isBlank()) {
            val first = homes.first()
            viewModel.selectHome(first)
        }
    }

    // 선택된 홈 변경 시 디바이스 로드 + 홈 재실 상태 갱신
    LaunchedEffect(token, selectedHomeId) {
        if (token.isNotBlank() && selectedHomeId.isNotBlank()) {
            viewModel.getDevices(token, selectedHomeId)
            viewModel.refreshHomePresence(selectedHomeId, token)
        }
    }

    // 방 자동 선택
    LaunchedEffect(rooms) {
        val newId = when {
            rooms.isNotEmpty() && vmRoomId != null -> vmRoomId!!
            rooms.isNotEmpty() -> rooms.first().id
            else -> ""
        }
        if (newId.isNotBlank() && newId != vmRoomId) {
            viewModel.selectRoom(newId)
        }
    }

    // 워처 시작
    LaunchedEffect(token, rooms) {
        if (token.isNotBlank() && rooms.isNotEmpty()) {
            viewModel.startFallAlertWatcher(token)
            viewModel.startActivityWatcher(token)
            viewModel.startRespirationWatcher(token)
        }
    }

    // 날짜/선택 변경 시 데이터 로드
    LaunchedEffect(token, vmRoomId, selectedDate, selectedHomeId) {
        if (token.isBlank() || vmRoomId.isNullOrBlank() || selectedHomeId.isBlank()) return@LaunchedEffect
        if (!isToday) {
            pastFallCount = viewModel.getFallTotalForDateAcrossHome(token, selectedHomeId, selectedDate)
            pastActivityAvg = viewModel.getActivityAvgForDateAcrossHome(token, selectedHomeId, selectedDate)
            pastRespAvg = viewModel.getRespAvgForDateAcrossHome(token, selectedHomeId, selectedDate)
        } else {
            pastFallCount = 0
            pastActivityAvg = 0
            pastRespAvg = 0
        }

        viewModel.fetchLifePatternsData(token, selectedHomeId, selectedDate)
        viewModel.fetchEntryPatternsData(selectedHomeId, selectedDate)
    }

    val fallValue = if (isToday) (todayFallCountByRoomId[vmRoomId ?: ""] ?: 0) else pastFallCount
    val fallDanger = isToday && (dangerFallTodayByRoomId[vmRoomId ?: ""] == true)

    val activityValueNowOrAvg = if (isToday) (todayActivityByRoomId[vmRoomId ?: ""] ?: 0) else pastActivityAvg
    val activityDanger = isToday && (dangerActivityTodayByRoomId[vmRoomId ?: ""] == true)
    val activityValueText = if (isToday) "활동도 ${activityValueNowOrAvg}점" else "평균 활동도 ${activityValueNowOrAvg}점"

    val respValueNowOrAvg = if (isToday) (todayRespByRoomId[vmRoomId ?: ""] ?: 0) else pastRespAvg
    val respDanger = isToday && (dangerRespTodayByRoomId[vmRoomId ?: ""] == true)
    val respValueText = if (isToday) "$respValueNowOrAvg bpm" else "평균 분당 ${respValueNowOrAvg}회"

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
                    onDateSelected = { date -> viewModel.updateSelectedDate(date) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (hasDevices != false) {
                    DetectionCardList(
                        onFallClick = {
                            safeNavigateIfHomeExists(context, selectedHomeId) {
                                onFall(selectedHomeId, selectedDate)
                            }
                        },
                        onActivityClick = {
                            safeNavigateIfHomeExists(context, selectedHomeId) {
                                onActivity(selectedHomeId, selectedDate)
                            }
                        },
                        onRespirationClick = {
                            safeNavigateIfHomeExists(context, selectedHomeId) {
                                onRespiration(selectedHomeId, selectedDate)
                            }
                        },
                        onEntryPatternClick = {
                            safeNavigateIfHomeExists(context, selectedHomeId) {
                                onEntryPattern(selectedHomeId, selectedDate)
                            }
                        },
                        onLifePatternClick = {
                            safeNavigateIfHomeExists(context, selectedHomeId) {
                                onLifePattern(selectedHomeId, selectedDate)
                            }
                        },
                        onNightActivityClick = onNightActivity,
                        onEmergencyCallClick = onEmergencyCall,
                        fallValue = fallValue,
                        activityValue = activityValueNowOrAvg,
                        respirationValue = respValueNowOrAvg,
                        fallDanger = fallDanger,
                        activityDanger = activityDanger,
                        respirationDanger = respDanger,
                        entryPatternText = entryPatternValue,
                        lifePatternText = lifePatternValue,
                        activityLabelOverride = activityValueText,
                        respirationLabelOverride = respValueText
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    EmptyStateAddDevice(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        onAddDevice = {
                            if (selectedHomeId.isNotBlank()) onAddDevice(selectedHomeId)
                            else Toast.makeText(context, "홈을 먼저 선택하세요.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }

        // 상단 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF174176).copy(alpha = 0.8f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .onGloballyPositioned { coordinates -> topBarHeight = coordinates.size.height }
        ) {
            Column(
                modifier = Modifier.padding(start = 22.dp, end = 20.dp, top = 15.dp)
            ) {
                val presenceLabel = if (isHomePresent) {
                    presentRoomName?.let { "재실중($it)" } ?: "재실중"
                } else {
                    "부재중"
                }

                TopBar(
                    selectedHomeName = selectedHomeName,
                    hasUnreadNotification = hasUnreadNotification,
                    presenceLabel = presenceLabel,
                    onClickHomeSelector = { showHomeSelector = true },
                    onClickNotification = onOpenNotification
                )

                Spacer(modifier = Modifier.height(15.dp))

                WeeklyCalendarHeader(
                    selectedDate = selectedDate,
                    onClick = { showMonthlyCalendar = true }
                )

                if(showHomeSelector) {
                    HomeSelectorBottomSheet(
                        homes = homes,
                        selectedHomeId = selectedHomeId,
                        onDismiss = { showHomeSelector = false },
                        onHomeSelected = { selectedHome ->
                            viewModel.selectHome(selectedHome)
                            if (token.isNotBlank()) {
                                viewModel.getDevices(token, selectedHome.id)
                                viewModel.refreshHomePresence(selectedHome.id, token)
                            }
                        },
                        onNavigateToSettingHome = {
                            showHomeSelector = false
                            onOpenHomeList()
                        }
                    )
                }

                if (showMonthlyCalendar) {
                    MonthlyCalendarBottomSheet(
                        selectedDate = selectedDate,
                        onDateSelected = { date -> viewModel.updateSelectedDate(date) },
                        onDismiss = { showMonthlyCalendar = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateAddDevice(
    modifier: Modifier = Modifier,
    onAddDevice: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.2.dp, Color(0x331C86C8), RoundedCornerShape(16.dp))
            .background(Color(0x331C86C8), RoundedCornerShape(16.dp))
            .clickable { onAddDevice() }
            .padding(horizontal = 40.dp, vertical = 25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Color(0x441C86C8)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_plus),
                contentDescription = "디바이스 추가",
                tint = Color(0xFFBFD8F5),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "디바이스가 없어요.",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(15.dp))
        Text(
            text = "데이터를 확인하려면 디바이스를 추가하세요.",
            color = Color(0xFFBFD1E6),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TopBar(
    selectedHomeName: String,
    hasUnreadNotification: Boolean,
    presenceLabel: String,
    onClickHomeSelector: () -> Unit,
    onClickNotification: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onClickHomeSelector() }
            ) {
                Text(selectedHomeName, color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_down),
                    contentDescription = "홈 메뉴",
                    modifier = Modifier.size(15.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            val isPresent = presenceLabel.startsWith("재실중")
            val badgeBg = if (isPresent) Color(0x3322D3EE) else Color(0x339A9EA8)
            val badgeFg = if (isPresent) Color(0xFF00D0E6) else Color(0xFF9EA4AE)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = presenceLabel,
                    color = badgeFg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.clickable { onClickNotification() }
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bell),
                        contentDescription = "알림",
                        modifier = Modifier.size(17.dp),
                        tint = Color.White
                    )
                    if (hasUnreadNotification) {
                        Box(
                            modifier = Modifier
                                .size(5.5.dp)
                                .offset(x = (-1.5).dp)
                                .background(Color.Red, CircleShape)
                        )
                    }
                }
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
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${selectedDate.monthValue}.${selectedDate.dayOfMonth} " +
                    selectedDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
            color = Color.White,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.width(5.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_caret_down),
            contentDescription = "날짜 선택",
            modifier = Modifier.size(7.dp),
            tint = Color.White
        )
    }
}

@Composable
fun WeeklyCalendarPager(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 1000) { 1001 }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val baseSunday = remember { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) }
    val days = listOf("일", "월", "화", "수", "목", "금", "토")

    LaunchedEffect(selectedDate) {
        val targetSunday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val offset = ChronoUnit.WEEKS.between(baseSunday, targetSunday)
        val targetPage = (1000 + offset.toInt()).coerceAtMost(1000)
        if (pagerState.currentPage != targetPage) {
            scope.launch { pagerState.scrollToPage(targetPage) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF174176))
            .padding(top = 6.dp)
    ) {
        // 요일 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEachIndexed { index, day ->
                val isSelected = (selectedDate.dayOfWeek.value % 7) == index
                Box(
                    modifier = Modifier
                        .size(22.dp)
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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(47.dp)
        ) { page ->
            val startOfWeek = baseSunday.plusWeeks((page - 1000).toLong())
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..6).forEach { offset ->
                    val date = startOfWeek.plusDays(offset.toLong())
                    val disabled = date.isAfter(today)
                    Box(
                        modifier = Modifier
                            .size(23.dp)
                            .clip(CircleShape)
                            .alpha(if (disabled) 0.4f else 1f)
                            .clickable(enabled = !disabled) { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            color = Color.White,
                            fontSize = 13.sp
                        )
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
    val pagerState = rememberPagerState(initialPage = 1000) { 1001 }
    var currentMonth by remember { mutableStateOf(today.withDayOfMonth(1)) }

    LaunchedEffect(selectedDate) {
        val offset = ChronoUnit.MONTHS.between(
            today.withDayOfMonth(1),
            selectedDate.withDayOfMonth(1)
        )
        scope.launch { pagerState.scrollToPage((1000 + offset.toInt()).coerceAtMost(1000)) }
    }

    LaunchedEffect(pagerState.currentPage) {
        val monthOffset = pagerState.currentPage - 1000
        currentMonth = today.plusMonths(monthOffset.toLong()).withDayOfMonth(1)
    }

    fun rowsInMonth(firstDay: LocalDate): Int {
        val daysInMonth = firstDay.lengthOfMonth()
        the@ run {

        }
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
                                    val disabled = date.isAfter(today)
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
    onFallClick: () -> Unit,
    onActivityClick: () -> Unit,
    onRespirationClick: () -> Unit,
    onEntryPatternClick: () -> Unit,
    onLifePatternClick: () -> Unit,
    onNightActivityClick: () -> Unit,
    onEmergencyCallClick: () -> Unit,
    fallValue: Int = 0,
    activityValue: Int? = 0,
    respirationValue: Int? = 0,
    fallDanger: Boolean = false,
    activityDanger: Boolean,
    respirationDanger: Boolean = false,
    entryPatternText: String? = null,
    lifePatternText: String? = null,
    activityLabelOverride: String? = null,
    respirationLabelOverride: String? = null
) {
    Column {
        DetectionCard(
            title = "낙상위험 감지",
            value = "${fallValue}회",
            imageRes = R.drawable.img1,
            isDanger = fallDanger,
            onClick = onFallClick
        )

        DetectionCard(
            title = "활동량 감지",
            value = activityLabelOverride ?: "활동도 ${activityValue ?: 0}점",
            imageRes = R.drawable.img2,
            isDanger = activityDanger,
            onClick = onActivityClick
        )

        DetectionCard(
            title = "호흡 감지",
            value = respirationLabelOverride ?: "현재 ${respirationValue ?: 0} bpm",
            imageRes = R.drawable.img3,
            isDanger = respirationDanger,
            onClick = onRespirationClick
        )

        DetectionCard(
            title = "출입 패턴",
            value = entryPatternText ?: "데이터 없음",
            imageRes = R.drawable.img6,
            onClick = onEntryPatternClick,
            valueSpacing = 4.dp
        )

        DetectionCard(
            title = "생활 패턴",
            value = lifePatternText,
            imageRes = R.drawable.img5,
            onClick = onLifePatternClick
        )

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
    onClick: () -> Unit,
    valueSpacing: Dp = 8.dp
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

    val normalBg = Color(0x5A1C5985)
    val dangerBase = Color(0xFFEF5350)
    val dangerBg = dangerBase.copy(alpha = 0.48f * blinkAlpha)
    val backgroundColor = if (isDanger) dangerBg else normalBg
    val borderColor = if (isDanger) dangerBase.copy(alpha = blinkAlpha) else Color(0xFF1A5783)

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
                    Spacer(modifier = Modifier.height(valueSpacing))
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
    homes: List<Home>,
    selectedHomeId: String?,
    onDismiss: () -> Unit,
    onHomeSelected: (Home) -> Unit,
    onNavigateToSettingHome: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

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
                items(homes, key = { it.id }) { home ->
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
                            if (selectedHomeId == home.id) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = "선택됨",
                                    tint = Color(0x7A808080),
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

private fun safeNavigateIfHomeExists(
    context: Context,
    homeId: String,
    block: () -> Unit
) {
    if (homeId.isNotBlank()) {
        block()
    } else {
        Toast.makeText(context, "홈 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
    }
}

private fun formatMinutes(mins: Int?): String {
    val m = mins ?: 0
    val h = m / 60
    val r = m % 60
    return when {
        h == 0 && r == 0 -> "0분"
        h > 0 && r == 0  -> "${h}시간"
        h > 0            -> "${h}시간 ${r}분"
        else             -> "${r}분"
    }
}
