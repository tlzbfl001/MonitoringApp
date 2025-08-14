package com.aitronbiz.arron.screen.home

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.util.ActivityAlertStore
import com.aitronbiz.arron.viewmodel.ActivityViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

@Composable
fun ActivityDetectionScreen(
    homeId: String,
    roomId: String,
    viewModel: ActivityViewModel = viewModel(),
    navController: NavController
) {
    val token = AppController.prefs.getToken().orEmpty()

    val data by viewModel.chartData.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val selectedDate by viewModel.selectedDate
    val scrollState = rememberScrollState()
    var showMonthlyCalendar by remember { mutableStateOf(false) }
    val activityAlerts by ActivityAlertStore.alertByRoom.collectAsState()
    val activityDanger = selectedDate == LocalDate.now() &&
            roomId.isNotBlank() &&
            (activityAlerts[roomId] == true)

    // roomId, 날짜 변경 시 데이터 로드
    LaunchedEffect(roomId, selectedDate) {
        if (roomId.isNotBlank() && token.isNotBlank()) {
            viewModel.fetchActivityData(token, roomId, selectedDate)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(top = 15.dp)
    ) {
        // 상단 타이틀바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.arrow_back),
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.CenterStart)
                    .clickable {
                        val popped = navController.popBackStack()
                        if (!popped) navController.navigateUp()
                    }
            )
            Text(
                text = "활동량 감지",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 날짜 헤더
        ActivityDetectionWeeklyCalendarHeader(
            selectedDate = selectedDate,
            onClick = { showMonthlyCalendar = true }
        )

        // 주간 달력
        ActivityDetectionWeeklyCalendarPager(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                if (!date.isAfter(LocalDate.now())) {
                    viewModel.updateSelectedDate(date)
                }
            }
        )

        // 월간 달력
        if (showMonthlyCalendar) {
            ActivityDetectionMonthlyCalendarDialog(
                selectedDate = selectedDate,
                onDateSelected = {
                    if (!it.isAfter(LocalDate.now())) {
                        viewModel.updateSelectedDate(it)
                    }
                },
                onDismiss = { showMonthlyCalendar = false }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 활동량 차트
        if (data.isNotEmpty()) {
            ActivityLineChart(
                rawData = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                scrollState = scrollState,
                selectedIndex = selectedIndex,
                onPointSelected = { index -> viewModel.selectBar(index) },
                selectedDate = selectedDate
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "데이터가 없습니다",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
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
            .padding(start = 25.dp, bottom = 8.dp),
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetectionMonthlyCalendarDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
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
        val firstDow = (firstDay.dayOfWeek.value % 7)
        val totalCells = ((daysInMonth + firstDow + 6) / 7) * 7
        return totalCells / 7
    }

    val cellSize: Dp = 40.dp

    androidx.compose.material3.ModalBottomSheet(
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
                    androidx.compose.material.Icon(
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
                    androidx.compose.material.Text(
                        text = "${currentMonth.year}.${currentMonth.monthValue}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    androidx.compose.material.Icon(
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

                androidx.compose.material.Text(
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
                    androidx.compose.material.Text(
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
                                                    sheetState.hide()
                                                    onDismiss()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.material.Text(
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
    selectedDate: LocalDate,
    smoothing: Float = 0.5f
) {
    val chartHeight = 180.dp
    val pointSpacing = 3.dp
    val density = androidx.compose.ui.platform.LocalDensity.current

    // 10분 간격 144 슬롯으로 채우기
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

    // 초기 스크롤 및 선택
    LaunchedEffect(filledData, selectedDate) {
        if (visibleData.isNotEmpty()) {
            val initialIndex = endIndex
            val offsetPx = with(density) { (initialIndex * pointSpacing.toPx()).toInt() }
            scrollState.scrollTo(offsetPx)
            onPointSelected(initialIndex)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight + 120.dp)
    ) {
        // Y축 레이블 영역
        Canvas(
            modifier = Modifier
                .width(50.dp)
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
                        textSize = 32f
                        textAlign = Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(7.dp))

        // 그래프 영역
        Box(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(end = 20.dp)
        ) {
            if (visibleData.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .width(totalWidth)
                        .height(chartHeight + 120.dp)
                        .pointerInput(endIndex) {
                            detectTapGestures { offset ->
                                val clickedIndex =
                                    (offset.x / with(density) { 3.dp.toPx() }).toInt()
                                val limit = endIndex
                                if (clickedIndex in 0..limit) {
                                    onPointSelected(clickedIndex)
                                }
                            }
                        }
                ) {
                    val chartAreaHeight = chartHeight.toPx()
                    val unitHeight = chartAreaHeight / maxY
                    val widthPerPoint = with(density) { 3.dp.toPx() }

                    // X축 눈금 (0, 6h, 12h, 18h)
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
                                textSize = 30f
                                color = android.graphics.Color.WHITE
                                isAntiAlias = true
                            }
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = Offset(x, chartAreaHeight),
                            end = Offset(x, chartAreaHeight + 10f),
                            strokeWidth = 2f
                        )
                    }

                    val points = visibleData.mapIndexed { index, point ->
                        val x = index * widthPerPoint
                        val y = chartAreaHeight - point.value * unitHeight
                        Offset(x, y)
                    }

                    if (points.size > 1) {
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val curr = points[i]
                                val controlX = prev.x + (curr.x - prev.x) * smoothing
                                val controlY = prev.y + (curr.y - prev.y) * smoothing
                                quadraticBezierTo(prev.x, prev.y, controlX, controlY)
                            }
                        }
                        drawPath(
                            path = path,
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
                androidx.compose.material.Text(
                    text = "데이터 없음",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}