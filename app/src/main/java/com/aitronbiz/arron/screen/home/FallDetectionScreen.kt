package com.aitronbiz.arron.screen.home

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.viewmodel.FallViewModel
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
import kotlin.math.pow

@Composable
fun FallDetectionScreen(
    homeId: String,
    roomId: String,
    viewModel: FallViewModel = viewModel(),
    navController: NavController
) {
    val chartData by viewModel.chartPoints.collectAsState()
    val roomName by viewModel.roomName
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showMonthlyCalendar by remember { mutableStateOf(false) }

    // roomId, 날짜 변경 시 필요한 데이터 로드
    LaunchedEffect(roomId, selectedDate) {
        if (roomId.isNotBlank()) {
            AppController.prefs.getToken()?.let { token ->
                if (token.isNotEmpty()) {
                    viewModel.fetchRoomName(token, roomId)
                    if (selectedDate == LocalDate.now()) {
                        viewModel.fetchPresence(token, roomId)
                    }
                    viewModel.fetchFallChart(token, roomId, selectedDate)
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
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = {
                val popped = navController.popBackStack()
                if (!popped) navController.navigateUp()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "낙상감지",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 날짜 선택 UI
        FallDetectionWeeklyCalendarHeader(
            selectedDate = selectedDate,
            onClick = { showMonthlyCalendar = true }
        )

        FallDetectionWeeklyCalendarPager(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                if (!date.isAfter(LocalDate.now())) {
                    selectedDate = date
                    viewModel.updateSelectedDate(date)
                }
            }
        )

        if (showMonthlyCalendar) {
            FallDetectionMonthlyCalendarDialog(
                selectedDate = selectedDate,
                onDateSelected = {
                    if (!it.isAfter(LocalDate.now())) {
                        selectedDate = it
                        viewModel.updateSelectedDate(it)
                    }
                },
                onDismiss = { showMonthlyCalendar = false }
            )
        }

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            val binMinutes = 30
            val binCount = 1440 / binMinutes
            val today = LocalDate.now()
            val isToday = selectedDate == today
            val isFuture = selectedDate.isAfter(today)

            // 오늘이면 현재 bin까지만, 과거는 끝까지
            val endBinInclusive = if (isToday) rememberNowBin(binMinutes, selectedDate) else binCount - 1

            // 마지막 bin 값
            val lastBinValue by remember(chartData, selectedDate, endBinInclusive) {
                mutableFloatStateOf(if (isToday) latestBinValueForToday(chartData, binMinutes) else 0f)
            }

            // 화면 처음 진입 시 툴팁/선택
            var didInitSelection by remember(selectedDate, roomId) { mutableStateOf(false) }
            LaunchedEffect(selectedDate, roomId, endBinInclusive, isFuture) {
                if (didInitSelection) return@LaunchedEffect
                if (isFuture) {
                    didInitSelection = true
                    return@LaunchedEffect
                }
                val targetBin = endBinInclusive.coerceAtLeast(0)
                viewModel.selectBar(targetBin * binMinutes)
                didInitSelection = true
            }

            // 차트
            if (chartData.isNotEmpty() && !isFuture) {
                FallLineChart(
                    rawData = chartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    selectedIndex = selectedIndex,
                    onPointSelected = viewModel::selectBar,
                    selectedDate = selectedDate,
                    endBinInclusive = endBinInclusive,
                    showData = true,
                    showYAxis = true,
                    showTooltip = true
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
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            val totalFalls = remember(chartData, endBinInclusive, isFuture) {
                if (isFuture) 0 else totalFallsUpToEndBin(chartData, endBinInclusive, binMinutes)
            }

            // 요약 카드
            FallSummaryCard(
                locations = if (roomName.isNotBlank()) roomName else "-",
                count = totalFalls,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // 재실/알림
            if (selectedDate == today) {
                if (lastBinValue > 0f) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Image(
                        painter = painterResource(id = R.drawable.img_fall),
                        contentDescription = "낙상 감지",
                        modifier = Modifier
                            .size(220.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .offset(y = (-17).dp)
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE53935))
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "낙상 사고가 감지되었습니다",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                } else {
                    viewModel.isPresent.value?.let { present: Boolean ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!present) {
                                Text(
                                    text = "부재중입니다.",
                                    color = Color(0xFFDCDCDC),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun FallDetectionWeeklyCalendarHeader(
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
fun FallDetectionWeeklyCalendarPager(
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
fun FallDetectionMonthlyCalendarDialog(
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
        val firstDow = (firstDay.dayOfWeek.value % 7)
        val totalCells = ((daysInMonth + firstDow + 6) / 7) * 7
        return totalCells / 7
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
                                                    delay(500)
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
private fun rememberNowBin(binMinutes: Int, selectedDate: LocalDate): Int {
    val today = LocalDate.now()
    var now by remember(selectedDate) { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(selectedDate) {
        if (selectedDate == today) {
            while (true) {
                now = LocalTime.now()
                delay(60_000)
            }
        }
    }

    val totalMin = now.hour * 60 + now.minute
    val bin = (totalMin / binMinutes).coerceAtLeast(0)
    val maxBin = (1440 / binMinutes) - 1
    return bin.coerceIn(0, maxBin)
}

@Composable
fun FallLineChart(
    rawData: List<ChartPoint>,
    modifier: Modifier = Modifier,
    maxY: Int = 5,
    selectedIndex: Int,
    onPointSelected: (Int) -> Unit,
    selectedDate: LocalDate,
    endBinInclusive: Int,
    showData: Boolean = true,
    showYAxis: Boolean = true,
    showTooltip: Boolean = true
) {
    val minuteBuckets = remember(rawData) {
        FloatArray(1440) { 0f }.also { arr ->
            rawData.forEach { p ->
                runCatching {
                    val (h, m) = p.timeLabel.split(":").map(String::toInt)
                    val idx = h * 60 + m
                    if (idx in 0..1439) arr[idx] += p.value
                }
            }
        }
    }

    // 30분 bin으로 합산
    val binMinutes = 30
    val binCount = 1440 / binMinutes
    val binnedValues = remember(minuteBuckets) {
        List(binCount) { bin ->
            val from = bin * binMinutes
            val to = from + binMinutes
            var s = 0f
            for (i in from until to) s += minuteBuckets[i]
            s
        }
    }

    val clampedEnd = endBinInclusive.coerceIn(0, binCount - 1)
    val selectedBin = (selectedIndex / binMinutes)
        .coerceIn(0, if (clampedEnd >= 0) clampedEnd else 0)

    val yMaxInt = 5
    val stepInt = 1
    val yLabels: List<Int> = if (showYAxis) (0..yMaxInt step stepInt).toList() else emptyList()
    val yMax = yMaxInt.toFloat()

    // 패딩/좌표 계산
    data class Pads(
        val leftPad: Float, val rightPad: Float, val topPad: Float,
        val bottomPad: Float, val baseY: Float, val chartW: Float, val chartH: Float, val dx: Float
    )

    val density = androidx.compose.ui.platform.LocalDensity.current
    val xAxisExtraGapDp = 10.dp
    val yAxisGapDp = 10.dp

    fun Density.computePads(w: Float, h: Float, maxYLabelText: String): Pads {
        val outerLeft = 25.dp.toPx()
        val outerRight = 20.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 24f + xAxisExtraGapDp.toPx()

        val yPaint = Paint().apply {
            textAlign = android.graphics.Paint.Align.RIGHT
            textSize = 26f
            isAntiAlias = true
        }
        val labelWidthPx = if (showYAxis) yPaint.measureText(maxYLabelText) else 0f
        val gapPx = yAxisGapDp.toPx()

        val leftPad = outerLeft + labelWidthPx + gapPx
        val rightPad = outerRight

        val chartW = (w - leftPad - rightPad).coerceAtLeast(1f)
        val chartH = (h - topPad - bottomPad).coerceAtLeast(1f)
        val baseY = topPad + chartH
        val dx = if (binCount > 1) chartW / (binCount - 1) else chartW
        return Pads(leftPad, rightPad, topPad, bottomPad, baseY, chartW, chartH, dx)
    }

    val maxYLabelText = (yLabels.maxOrNull() ?: yMaxInt).toString()

    // 터치로 포인트 선택
    val pointerMod = if (showTooltip && showData) {
        Modifier.pointerInput(binnedValues, clampedEnd) {
            detectTapGestures { offset ->
                val w = size.width
                val h = size.height
                val pads = with(density) { computePads(w.toFloat(), h.toFloat(), maxYLabelText) }
                val xRel = (offset.x - pads.leftPad).coerceIn(0f, pads.chartW)
                val bin = (xRel / pads.dx).toInt().coerceIn(0, clampedEnd)
                onPointSelected(bin * binMinutes)
            }
        }
    } else Modifier

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .then(pointerMod)
    ) {
        val w = size.width
        val h = size.height
        val pads = with(density) { computePads(w, h, maxYLabelText) }

        fun xOf(bin: Int) = pads.leftPad + bin * pads.dx
        fun yOf(v: Float) = pads.baseY - (kotlin.math.min(v, yMax) / yMax) * pads.chartH

        // X축
        drawLine(
            color = Color.White.copy(alpha = 0.30f),
            start = Offset(pads.leftPad, pads.baseY),
            end = Offset(w - pads.rightPad, pads.baseY),
            strokeWidth = 2.5f
        )

        // 면/선
        if (showData && clampedEnd >= 0) {
            val points = (0..clampedEnd).map { i -> Offset(xOf(i), yOf(binnedValues[i])) }
            if (points.size >= 2) {
                val areaPath = Path().apply {
                    moveTo(points.first().x, pads.baseY)
                    for (i in points.indices) {
                        val p0 = if (i == 0) points[i] else points[i - 1]
                        val p1 = points[i]
                        val midX = if (i == 0) p1.x else (p0.x + p1.x) / 2f
                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }
                    lineTo(points.last().x, pads.baseY); close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF5CEAFF).copy(alpha = 0.70f),
                            Color(0xFF5CEAFF).copy(alpha = 0.38f),
                            Color.Transparent
                        ),
                        startY = pads.topPad,
                        endY = pads.baseY
                    )
                )
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val p0 = points[i - 1]; val p1 = points[i]
                        val midX = (p0.x + p1.x) / 2f
                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = Color(0xFF5CEAFF),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
        }

        if (showYAxis) {
            val yPaint = Paint().apply {
                textAlign = android.graphics.Paint.Align.RIGHT
                textSize = 26f
                color = android.graphics.Color.WHITE
                isAntiAlias = true
            }
            val gapPx = with(density) { 10.dp.toPx() }
            val labelX = pads.leftPad - gapPx

            val fm = yPaint.fontMetrics
            val topAllowance = -fm.top
            val bottomAllowance = fm.bottom

            yLabels.forEach { value ->
                var base = yOf(value.toFloat())
                val minBase = pads.topPad + topAllowance + 2f
                val maxBase = pads.baseY - bottomAllowance - 2f
                base = base.coerceIn(minBase, maxBase)
                drawContext.canvas.nativeCanvas.drawText(value.toString(), labelX, base, yPaint)
            }
        }

        // X축 라벨
        val xPaint = Paint().apply {
            textAlign = android.graphics.Paint.Align.LEFT
            textSize = 28f
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
        val xLabelGap = with(density) { 6.dp.toPx() }
        val xInset = with(density) { 2.dp.toPx() }
        listOf(0, 6, 12, 18).forEachIndexed { idx, hour ->
            val bin = hour * (60 / binMinutes)
            val xLeft = xOf(bin) + xInset
            drawContext.canvas.nativeCanvas.drawText(
                listOf("12AM", "6AM", "12PM", "6PM")[idx],
                xLeft,
                pads.baseY + 34f + xLabelGap,
                xPaint
            )
        }

        // 선택 지점 & 툴팁
        val tipEnd = maxOf(0, clampedEnd)
        if (showTooltip && tipEnd >= 0) {
            val tipPoints = (0..tipEnd).map { i -> Offset(xOf(i), yOf(binnedValues[i])) }
            if (selectedBin in 0..tipEnd && tipPoints.isNotEmpty()) {
                val sel = tipPoints[selectedBin]
                drawCircle(Color(0xFFFF5C5C), radius = 6f, center = sel)

                val tipW = 150f
                val tipH = 70f
                val tipX = (sel.x - tipW / 2).coerceIn(pads.leftPad, w - pads.rightPad - tipW)
                val tipY = (sel.y - tipH - 12f).coerceAtLeast(pads.topPad + 2f)

                drawRoundRect(
                    color = Color(0xFF0D1B2A),
                    topLeft = Offset(tipX, tipY),
                    size = Size(tipW, tipH),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                val totalMinutes = selectedBin * binMinutes
                val hLabel = totalMinutes / 60
                val mLabel = totalMinutes % 60
                val timeText = "%02d:%02d".format(hLabel, mLabel)
                val valueText = "${kotlin.math.min(binnedValues[selectedBin], yMax).toInt()} 회" // 표시는 클램프된 값 기준

                val p = Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                }
                p.textSize = 28f
                drawContext.canvas.nativeCanvas.drawText(timeText, tipX + tipW / 2, tipY + 30f, p)
                p.textSize = 26f
                drawContext.canvas.nativeCanvas.drawText(valueText, tipX + tipW / 2, tipY + 56f, p)
            }
        }
    }
}

private fun latestBinValueForToday(rawData: List<ChartPoint>, binMinutes: Int = 30): Float {
    if (binMinutes <= 0) return 0f
    val minute = FloatArray(1440)
    rawData.forEach { p ->
        runCatching {
            val (h, m) = p.timeLabel.split(":").map(String::toInt)
            val idx = h * 60 + m
            if (idx in 0..1439) minute[idx] += p.value
        }
    }
    val now = LocalTime.now()
    val nowMin = now.hour * 60 + now.minute
    val bin = nowMin / binMinutes
    val from = (bin * binMinutes).coerceAtMost(1439)
    val to = minOf(from + binMinutes, 1440)
    var sum = 0f
    for (i in from until to) sum += minute[i]
    return sum
}

@Composable
fun FallSummaryCard(
    locations: String,
    count: Int,
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
                Text("위치", color = Color.White.copy(alpha = 0.95f), fontSize = 14.sp)
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
                Text("총 낙상 횟수", color = Color.White.copy(alpha = 0.95f), fontSize = 14.sp)
                Text(
                    text = count.toString(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}

private fun totalFallsUpToEndBin(
    rawData: List<ChartPoint>,
    endBinInclusive: Int,
    binMinutes: Int = 30
): Int {
    if (rawData.isEmpty() || endBinInclusive < 0) return 0
    val minute = FloatArray(1440)
    rawData.forEach { p ->
        runCatching {
            val (h, m) = p.timeLabel.split(":").map(String::toInt)
            val idx = h * 60 + m
            if (idx in 0..1439) minute[idx] += p.value
        }
    }
    val binCount = 1440 / binMinutes
    val end = endBinInclusive.coerceIn(0, binCount - 1)
    var total = 0f
    for (bin in 0..end) {
        val from = bin * binMinutes
        val to = (from + binMinutes).coerceAtMost(1440)
        var s = 0f
        for (i in from until to) s += minute[i]
        total += s
    }
    return total.toInt()
}