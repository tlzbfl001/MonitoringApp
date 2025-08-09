package com.aitronbiz.arron.screen.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import java.time.temporal.TemporalAdjusters
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import com.aitronbiz.arron.api.response.Home
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.foundation.lazy.items
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
    var homeId by remember { mutableStateOf("") }
    var hasUnreadNotification by remember { mutableStateOf(false) }

    // 홈 목록
    LaunchedEffect(Unit) {
        val token = AppController.prefs.getToken()
        if (!token.isNullOrEmpty()) {
            viewModel.fetchHomes(token)
        }
    }

    // homes가 갱신되면 첫 번째 home으로 선택
    LaunchedEffect(viewModel.homes) {
        if (viewModel.homes.isNotEmpty() && homeId.isBlank()) {
            val firstHome = viewModel.homes.first()
            homeId = firstHome.id
            viewModel.setSelectedHomeId(firstHome.id)
            viewModel.selectedHomeName = firstHome.name
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
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
                    onDateSelected = { selectedDate = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetectionCardList(
                    selectedDate = selectedDate,
                    onFallClick = { navigateIfHomeExists(homeId, context, navController, "fallDetection") },
                    onActivityClick = { navigateIfHomeExists(homeId, context, navController, "activityDetection") },
                    onRespirationClick = { navigateIfHomeExists(homeId, context, navController, "respirationDetection") },
                    onLifePatternClick = { navigateIfHomeExists(homeId, context, navController, "lifePattern") },
                    onEntryPatternClick = { navigateIfHomeExists(homeId, context, navController, "entryPattern") },
                    onNightActivityClick = { navigateIfHomeExists(homeId, context, navController, "nightActivity") },
                    onEmergencyCallClick = { navigateIfHomeExists(homeId, context, navController, "nightActivity") }
                )

                Spacer(modifier = Modifier.height(70.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF174176).copy(alpha = 0.8f))
                .onGloballyPositioned { coordinates -> topBarHeight = coordinates.size.height }
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
            ) {
                TopBar(
                    viewModel = viewModel,
                    navController = navController,
                    hasUnreadNotification = hasUnreadNotification,
                    onClickHomeSelector = { showHomeSelector = true },
                    onNavigateDevice = onNavigateDevice,
                    onNavigateSettings = onNavigateSettings
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
                            viewModel.setSelectedHomeId(selectedHome.id)
                            viewModel.selectedHomeName = selectedHome.name
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
    onNavigateDevice: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 홈 선택 영역
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
            Spacer(modifier = Modifier.width(8.dp))
            Text("재실중", color = Color.Cyan)
        }

        // 알림
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clickable {
                        navController.navigate("notification")
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bell),
                    contentDescription = "알림",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                if (hasUnreadNotification) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Red, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Box {
                Icon(
                    painter = painterResource(id = R.drawable.menu_dot),
                    contentDescription = "메뉴",
                    modifier = Modifier
                        .size(17.dp)
                        .clickable { showMenu = true },
                    tint = Color.White
                )

                ShowCustomPopupWindow(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onNavigateDevice = onNavigateDevice,
                    onNavigateSettings = onNavigateSettings
                )
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
            fontSize = 16.sp
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
    val pagerState = rememberPagerState(initialPage = 1000) { Int.MAX_VALUE }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val baseSunday = remember {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    }
    val days = listOf("일", "월", "화", "수", "목", "금", "토")

    LaunchedEffect(selectedDate) {
        val targetSunday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val offset = ChronoUnit.WEEKS.between(baseSunday, targetSunday)
        val targetPage = 1000 + offset.toInt()
        if (selectedDate != today && pagerState.currentPage != targetPage) {
            scope.launch { pagerState.scrollToPage(targetPage) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF174176))
            .padding(bottom = 7.dp)
    ) {
        // 요일 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 3.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEachIndexed { index, day ->
                val isSelected = (selectedDate.dayOfWeek.value % 7) == index
                val circleSize = 25.dp

                Box(
                    modifier = Modifier
                        .size(circleSize)
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

        // 주간 날짜
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            val startOfWeek = baseSunday.plusWeeks((page - 1000).toLong())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (0..6).forEach { offset ->
                    val date = startOfWeek.plusDays(offset.toLong())
                    Box(
                        modifier = Modifier
                            .size(23.dp)
                            .clip(CircleShape)
                            .clickable { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            color = Color.White
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
    val pagerState = rememberPagerState(initialPage = 1000) { Int.MAX_VALUE }
    var currentMonth by remember { mutableStateOf(today.withDayOfMonth(1)) }

    LaunchedEffect(selectedDate) {
        val offset = ChronoUnit.MONTHS.between(
            today.withDayOfMonth(1),
            selectedDate.withDayOfMonth(1)
        )
        scope.launch { pagerState.scrollToPage(1000 + offset.toInt()) }
    }

    LaunchedEffect(pagerState.currentPage) {
        val monthOffset = pagerState.currentPage - 1000
        currentMonth = today.plusMonths(monthOffset.toLong()).withDayOfMonth(1)
    }

    // 동적 높이 보간을 위한 헬퍼
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
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
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
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
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
                            scope.launch { pagerState.scrollToPage(1000) }
                        }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 요일 헤더
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

            // 월간 달력
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
                                    val sizeToUse = if (isSelected || isToday) reducedCellSize else cellSize

                                    Box(
                                        modifier = Modifier
                                            .size(sizeToUse)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> Color.Black
                                                    isToday -> Color(0xFFE0E0E0)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                onDateSelected(date)
                                                onDismiss()
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
    onEmergencyCallClick: () -> Unit
) {
    Column {
        DetectionCard(
            title = "낙상감지",
            value = "1회",
            imageRes = R.drawable.img1,
            onClick = onFallClick
        )
        DetectionCard(
            title = "활동량감지",
            value = "9시간 활동",
            imageRes = R.drawable.img2,
            onClick = onActivityClick
        )
        DetectionCard(
            title = "호흡 감지",
            value = "분당 15회",
            imageRes = R.drawable.img3,
            onClick = onRespirationClick
        )
        DetectionCard(
            title = "생활 패턴",
            value = "평균 취침 23:00\n평균 기상 07:30",
            imageRes = R.drawable.img5,
            onClick = onLifePatternClick
        )
        DetectionCard(
            title = "출입 패턴",
            value = "일일 출입 2회",
            imageRes = R.drawable.img6,
            onClick = onEntryPatternClick
        )
        DetectionCard(
            title = "야간활동 이상감지",
            value = "야간 출입 1회",
            imageRes = R.drawable.img7,
            onClick = onNightActivityClick
        )
        DetectionCard(
            title = "구조요청 자동연결",
            value = "",
            imageRes = R.drawable.img8,
            onClick = onEmergencyCallClick
        )
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(width = 1.4.dp, color = Color(0xFF185078), shape = RoundedCornerShape(10.dp))
            .background(color = Color(0x5A185078))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
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
                verticalArrangement = if (value.isNullOrBlank()) Arrangement.Center else Arrangement.Top
            ) {
                Text(title, color = Color.White, fontSize = 16.sp)
                if (!value.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(value, color = Color.LightGray, fontSize = 13.sp)
                }
            }
        }
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

    // homes 목록 로드
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

            // 스크롤 가능한 리스트
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
                                viewModel.selectHome(home)
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

            Text(
                text = "홈 설정 >",
                color = Color(0xFF24599D),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                            onNavigateToSettingHome()
                        }
                    }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ShowCustomPopupWindow(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNavigateDevice: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onDismiss() },
        offset = DpOffset(x = (-15).dp, y = 0.dp),
        modifier = Modifier.background(Color.White)
    ) {
        DropdownMenuItem(
            text = { Text("기기 관리", color = Color.Black) },
            onClick = {
                onDismiss()
                onNavigateDevice()
            }
        )
        DropdownMenuItem(
            text = { Text("설정", color = Color.Black) },
            onClick = {
                onDismiss()
                onNavigateSettings()
            }
        )
    }
}

fun navigateIfHomeExists(
    homeId: String,
    context: Context,
    navController: NavController,
    route: String
) {
    val destination = "$route/$homeId"
    navController.navigate(destination)
    if (homeId.isNotBlank()) {
        val destination2 = "$route/$homeId"
        navController.navigate(destination2)
    } else {
        Toast.makeText(context, "홈 정보가 없어 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
    }
}