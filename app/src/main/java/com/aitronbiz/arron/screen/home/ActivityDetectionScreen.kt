package com.aitronbiz.arron.screen.home

import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.viewmodel.ActivityViewModel
import com.aitronbiz.arron.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
fun ActivityDetectionScreen(
    homeId: String,
    roomId: String,
    selectedDate: LocalDate,
    viewModel: ActivityViewModel = viewModel(),
    navController: NavController,
    mainViewModel: MainViewModel = viewModel()
) {
    val token = AppController.prefs.getToken().orEmpty()

    // 데이터/상태
    val data by viewModel.chartData.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val selectedDate by viewModel.selectedDate
    var showMonthlyCalendar by remember { mutableStateOf(false) }

    // 알림 배지
    var hasUnreadNotification by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mainViewModel.checkNotifications { hasUnreadNotification = it } }

    // 장소 목록/재실
    val rooms by viewModel.rooms.collectAsState()
    val presenceByRoomId by mainViewModel.presenceByRoomId.collectAsState()
    var selectedRoomId by remember { mutableStateOf(roomId) }

    LaunchedEffect(homeId) {
        if (token.isNotBlank() && rooms.isEmpty()) {
            viewModel.fetchRooms(token, homeId)
        }
    }

    LaunchedEffect(rooms) {
        if (selectedRoomId.isBlank() && rooms.isNotEmpty()) {
            selectedRoomId = rooms.first().id
        }
    }

    LaunchedEffect(selectedRoomId, selectedDate) {
        if (selectedRoomId.isNotBlank() && token.isNotBlank()) {
            viewModel.fetchActivityData(token, selectedRoomId, selectedDate)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 5.dp, end = 20.dp, top = 2.dp, bottom = 6.dp)
        ) {
            IconButton(onClick = {
                val popped = navController.popBackStack()
                if (!popped) navController.navigateUp()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "활동량 감지",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.clickable { navController.navigate("notification") }
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bell),
                        contentDescription = "알림",
                        modifier = Modifier.size(16.dp),
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

        ActivityDetectionWeeklyCalendarHeader(
            selectedDate = selectedDate,
            onClick = { showMonthlyCalendar = true }
        )

        ActivityDetectionWeeklyCalendarPager(
            selectedDate = selectedDate,
            onDateSelected = { d -> if (!d.isAfter(LocalDate.now())) viewModel.updateSelectedDate(d) }
        )

        if (showMonthlyCalendar) {
            ActivityDetectionMonthlyCalendarDialog(
                selectedDate = selectedDate,
                onDateSelected = { d -> if (!d.isAfter(LocalDate.now())) viewModel.updateSelectedDate(d) },
                onDismiss = { showMonthlyCalendar = false }
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item { Spacer(modifier = Modifier.height(20.dp)) }

            item {
                val scrollState = rememberScrollState()

                val today = LocalDate.now()
                val now = LocalTime.now()
                val isToday = selectedDate == today
                val isFuture = selectedDate.isAfter(today)

                val nowIndex = (now.hour * 60 + now.minute) / 10
                val endIndex = when {
                    isFuture -> 0
                    isToday -> nowIndex.coerceIn(0, 143)
                    else -> 143
                }

                // 첫 진입 시 마지막 데이터 지점으로 스크롤 & 선택
                var didInitSelection by remember(selectedDate, selectedRoomId) { mutableStateOf(false) }
                val density = LocalDensity.current

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val chartViewportWidth = maxWidth - 40.dp - 7.dp - 20.dp
                    val pointSpacing = 3.dp

                    LaunchedEffect(data, selectedDate, endIndex) {
                        if (!didInitSelection && data.isNotEmpty() && endIndex >= 0) {
                            val endX: Dp = (endIndex + 1) * pointSpacing
                            if (endX > chartViewportWidth) {
                                val scrollPx = with(density) {
                                    (endX - chartViewportWidth).toPx()
                                }.toInt().coerceAtLeast(0)
                                scrollState.scrollTo(scrollPx)
                            } else {
                                scrollState.scrollTo(0)
                            }
                            viewModel.selectBar(endIndex)
                            didInitSelection = true
                        }
                    }

                    if (data.isNotEmpty() && !isFuture) {
                        ActivityLineChart(
                            rawData = data,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            scrollState = scrollState,
                            selectedIndex = selectedIndex,
                            onPointSelected = { index -> viewModel.selectBar(index) },
                            selectedDate = selectedDate
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFuture) "미래 날짜는 표시할 수 없습니다" else "데이터가 없습니다",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                val totalActivity = remember(data, endIndex, isFuture) {
                    if (isFuture) 0 else totalActivityUpToEndIndex(data, endIndex)
                }

                Spacer(modifier = Modifier.height(40.dp))

                val roomTitle = rooms.firstOrNull { it.id == selectedRoomId }?.name.orEmpty()
                ActivitySummaryCard(
                    locations = roomTitle.ifBlank { "-" },
                    totalActiveSlots = totalActivity,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val lastSlotValue = remember(data, selectedDate, endIndex) {
                    if (isToday) latestSlotValueForToday(data) else 0f
                }
                if (isToday) {
                    if (lastSlotValue > 0f) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img2),
                                contentDescription = "이상 활동 감지",
                                modifier = Modifier.size(220.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .offset(y = (-17).dp)
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE53935))
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "이상 활동이 감지되었습니다",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(40.dp))
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 장소 목록
            item {
                Text(
                    text = "장소 목록",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                val cardBg = Color(0x5A185078)
                val cardBorder = Color(0xFF185078)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    val columns = 3
                    val spacing = 8.dp
                    val itemWidth = (maxWidth - spacing * (columns - 1)) / columns

                    Column(modifier = Modifier.fillMaxWidth()) {
                        rooms.chunked(columns).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                repeat(columns) { idx ->
                                    val r = rowItems.getOrNull(idx)
                                    Box(
                                        modifier = Modifier
                                            .width(itemWidth)
                                            .height(80.dp)
                                    ) {
                                        if (r != null) {
                                            val selected = r.id == selectedRoomId
                                            val present = presenceByRoomId[r.id] == true
                                            RoomItemCell(
                                                name = r.name.ifBlank { "방" },
                                                selected = selected,
                                                present = present,
                                                bg = cardBg,
                                                borderDefault = cardBorder,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                if (selectedRoomId != r.id) {
                                                    selectedRoomId = r.id
                                                    if (token.isNotBlank()) {
                                                        viewModel.fetchActivityData(token, r.id, selectedDate)
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.height(80.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(spacing))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun ActivityDetectionWeeklyCalendarHeader(
    selectedDate: LocalDate,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 22.dp, bottom = 10.dp),
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
fun ActivityDetectionWeeklyCalendarPager(
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
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
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

        Spacer(modifier = Modifier.height(4.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val startOfWeek = baseSunday.plusWeeks((page - 1000).toLong())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
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
fun ActivityDetectionMonthlyCalendarDialog(
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
        val monthStart = firstDay.withDayOfMonth(1)
        val daysInMonth = monthStart.lengthOfMonth()
        val firstDow = monthStart.dayOfWeek.value % 7
        return (firstDow + daysInMonth + 6) / 7
    }

    val cellSize: Dp = 40.dp

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
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
                            scope.launch { pagerState.scrollToPage(1000) }
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
                val firstOfMonth = today.plusMonths(monthOffset.toLong()).withDayOfMonth(1)
                val daysInMonth = firstOfMonth.lengthOfMonth()
                val firstDow = (firstOfMonth.dayOfWeek.value % 7)
                val totalCells = ((daysInMonth + firstDow + 6) / 7) * 7

                val dates = (0 until totalCells).map { index ->
                    val day = index - firstDow + 1
                    if (day in 1..daysInMonth) firstOfMonth.withDayOfMonth(day) else null
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
                                    val sizeToUse: Dp = if (isSelected || isToday) (40.dp - 8.dp) else 40.dp

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
                                    Box(modifier = Modifier.size(40.dp))
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
fun ActivityLineChart(
    rawData: List<ChartPoint>,
    modifier: Modifier = Modifier,
    maxY: Int = 100,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
    selectedIndex: Int,
    onPointSelected: (Int) -> Unit,
    selectedDate: LocalDate
) {
    val chartHeight = 140.dp
    val pointSpacing = 3.dp
    val density = LocalDensity.current

    val filledData = remember(rawData) {
        val slots = MutableList(144) { index ->
            val totalMinutes = index * 10
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            ChartPoint("%02d:%02d".format(h, m), 0f)
        }
        val map = rawData.associateBy { it.timeLabel }
        slots.map { map[it.timeLabel] ?: it }
    }

    val today = LocalDate.now()
    val now = LocalTime.now()
    val isToday = selectedDate == today
    val isFuture = selectedDate.isAfter(today)

    val nowIndex = (now.hour * 60 + now.minute) / 10
    val endIndex = when {
        isFuture -> 0
        isToday -> nowIndex.coerceIn(0, 143)
        else -> 143
    }

    val visibleData = if (rawData.isEmpty()) emptyList() else filledData.take(endIndex + 1)
    val totalWidth = with(density) { (144 * pointSpacing.toPx()).toDp() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight + 120.dp)
    ) {
        // Y축
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight()
        ) {
            val chartAreaHeight = chartHeight.toPx()
            val unitHeight = chartAreaHeight / maxY

            for (i in 0..5) {
                val value = i * (maxY / 5f)
                val y = chartAreaHeight - value * unitHeight
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(size.width - 5f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    value.toInt().toString(),
                    size.width - 10f,
                    y + 10f,
                    Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        textAlign = Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(7.dp))

        // 그래프
        Box(
            modifier = Modifier
                .horizontalScroll(scrollState)
        ) {
            if (visibleData.isNotEmpty()) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .width(totalWidth)
                        .height(chartHeight + 120.dp)
                        .pointerInput(endIndex) {
                            detectTapGestures { offset ->
                                val clickedIndex =
                                    (offset.x / with(density) { 3.dp.toPx() }).toInt()
                                if (clickedIndex in 0..endIndex) {
                                    onPointSelected(clickedIndex)
                                }
                            }
                        }
                ) {
                    val chartAreaHeight = chartHeight.toPx()
                    val unitHeight = chartAreaHeight / maxY
                    val widthPerPoint = with(density) { 3.dp.toPx() }

                    // X축 라벨
                    for (slot in 0..144 step 36) {
                        if (slot == 144) continue
                        val x = slot * widthPerPoint
                        val totalMinutes = slot * 10
                        val h = totalMinutes / 60
                        val m = totalMinutes % 60
                        val label = "%02d:%02d".format(h, m)
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x,
                            chartAreaHeight + 50f,
                            Paint().apply {
                                textAlign = android.graphics.Paint.Align.LEFT
                                textSize = 28f
                                color = android.graphics.Color.WHITE
                                isAntiAlias = true
                            }
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.20f),
                            start = Offset(0f, chartAreaHeight),
                            end = Offset(size.width, chartAreaHeight),
                            strokeWidth = 1f
                        )
                    }

                    val points = visibleData.mapIndexed { index, point ->
                        val x = index * widthPerPoint
                        val v = point.value.coerceIn(0f, maxY.toFloat())
                        val y = chartAreaHeight - v * unitHeight
                        Offset(x, y)
                    }

                    if (points.size > 1) {
                        val areaPath = Path().apply {
                            moveTo(points.first().x, chartAreaHeight)
                            for (i in points.indices) {
                                val p0 = if (i == 0) points[i] else points[i - 1]
                                val p1 = points[i]
                                val midX = if (i == 0) p1.x else (p0.x + p1.x) / 2f
                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            }
                            lineTo(points.last().x, chartAreaHeight)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF5CEAFF).copy(alpha = 0.70f),
                                    Color(0xFF5CEAFF).copy(alpha = 0.38f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = chartAreaHeight
                            )
                        )

                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val p0 = points[i - 1]
                                val p1 = points[i]
                                val midX = (p0.x + p1.x) / 2f
                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = Color(0xFF5CEAFF),
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )
                    }

                    // 선택 툴팁
                    if (selectedIndex in points.indices) {
                        val point = points[selectedIndex]
                        val chartPoint = filledData[selectedIndex]

                        drawCircle(color = Color.Red, radius = 10f, center = point)

                        val tooltipWidth = 150f
                        val tooltipHeight = 70f
                        val tooltipX = (point.x - tooltipWidth / 2).coerceAtLeast(0f)
                        val tooltipY = (point.y - tooltipHeight - 15f).coerceAtLeast(0f)

                        drawRoundRect(
                            color = Color(0xFF0D1B2A),
                            topLeft = Offset(tooltipX, tooltipY),
                            size = Size(tooltipWidth, tooltipHeight),
                            cornerRadius = CornerRadius(16f, 16f)
                        )

                        val canvas = drawContext.canvas.nativeCanvas
                        val paint = Paint().apply {
                            textAlign = android.graphics.Paint.Align.CENTER
                            color = android.graphics.Color.WHITE
                            isAntiAlias = true
                            textSize = 26f
                        }
                        canvas.drawText(chartPoint.timeLabel, point.x, tooltipY + 30f, paint)
                        canvas.drawText("${chartPoint.value.toInt()}", point.x, tooltipY + 60f, paint)
                    }
                }
            } else {
                Text(
                    text = "데이터 없음",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun ActivitySummaryCard(
    locations: String,
    totalActiveSlots: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x5A185078))
            .border(
                width = 1.4.dp,
                color = Color(0xFF185078),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("장소", color = Color.White.copy(alpha = 0.95f), fontSize = 14.sp)
                Text(
                    text = locations,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("총 활동량", color = Color.White.copy(alpha = 0.95f), fontSize = 14.sp)
                Text(
                    text = totalActiveSlots.toString(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}

private fun latestSlotValueForToday(rawData: List<ChartPoint>): Float {
    val minute10 = FloatArray(144)
    rawData.forEach { p ->
        runCatching {
            val (h, m) = p.timeLabel.split(":").map(String::toInt)
            val idx = (h * 60 + m) / 10
            if (idx in 0..143) minute10[idx] += p.value
        }
    }
    val now = LocalTime.now()
    val idx = (now.hour * 60 + now.minute) / 10
    return minute10.getOrNull(idx) ?: 0f
}

private fun totalActivityUpToEndIndex(
    rawData: List<ChartPoint>,
    endIndex: Int
): Int {
    if (rawData.isEmpty() || endIndex < 0) return 0
    val minute10 = FloatArray(144)
    rawData.forEach { p ->
        runCatching {
            val (h, m) = p.timeLabel.split(":").map(String::toInt)
            val idx = (h * 60 + m) / 10
            if (idx in 0..143) minute10[idx] += p.value
        }
    }
    var total = 0f
    for (i in 0..max(0, endIndex).coerceAtMost(143)) total += minute10[i]
    return total.toInt()
}

@Composable
private fun RoomItemCell(
    name: String,
    selected: Boolean,
    present: Boolean,
    bg: Color,
    borderDefault: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Color.White else borderDefault
    val textColor = if (selected) Color.White else Color(0xFFBFC6D1)
    val badgeBg = if (present) Color(0x3322D3EE) else Color(0x339A9EA8)
    val badgeFg = if (present) Color(0xFF00D0E6) else Color(0xFF9EA4AE)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(width = 1.4.dp, color = borderColor, shape = RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            Text(
                text = if (present) "재실중" else "부재중",
                color = badgeFg,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeBg)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}