package com.aitronbiz.arron.screen.home

import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.viewmodel.ActivityViewModel
import com.aitronbiz.arron.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun ActivityDetectionScreen(
    homeId: String,
    roomId: String,
    selectedDate: LocalDate,
    navController: NavController,
    viewModel: ActivityViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val token = AppController.prefs.getToken().orEmpty()

    val data by viewModel.chartData.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val presenceByRoomId by viewModel.presenceByRoomId.collectAsState()
    var selectedRoomId by remember { mutableStateOf(roomId) }

    var hasUnreadNotification by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        mainViewModel.checkNotifications { hasUnreadNotification = it }
    }

    LaunchedEffect(homeId) {
        if (token.isNotEmpty()) {
            viewModel.fetchRooms(token, homeId)
        }
    }
    LaunchedEffect(rooms, selectedDate) {
        if (selectedDate == LocalDate.now() && token.isNotEmpty()) {
            rooms.forEach { r -> viewModel.fetchPresence(token, r.id) }
        }
    }

    // 선택된 룸/날짜 변경 시 데이터 로드
    LaunchedEffect(selectedRoomId, selectedDate) {
        if (selectedRoomId.isNotBlank() && token.isNotBlank()) {
            viewModel.updateSelectedDate(selectedDate)
            viewModel.fetchActivityData(token, selectedRoomId, selectedDate)
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
                .padding(start = 5.dp, end = 20.dp, top = 2.dp)
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

            Box(modifier = Modifier.clickable { navController.navigate("notification") }) {
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

        // 선택 날짜
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp, start = 22.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDate.toString(),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 차트 영역
            val today = LocalDate.now()
            val now = LocalTime.now()
            val isToday = selectedDate == today

            val nowIndex = (now.hour * 60 + now.minute) / 10
            val endIndex = if (isToday) nowIndex.coerceIn(0, 143) else 143

            val scrollState = rememberScrollState()
            val density = LocalDensity.current
            var didInitSelection by remember(selectedDate, selectedRoomId) { mutableStateOf(false) }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val chartViewportWidth = maxWidth - 40.dp - 7.dp - 20.dp
                val pointSpacing = 3.dp

                LaunchedEffect(data, selectedDate, endIndex) {
                    if (!didInitSelection && data.isNotEmpty() && endIndex >= 0) {
                        val endX: Dp = pointSpacing * (endIndex + 1).toFloat()
                        val scrollPx = with(density) { (endX - chartViewportWidth).toPx() }
                            .toInt().coerceAtLeast(0)
                        if (endX > chartViewportWidth) scrollState.scrollTo(scrollPx) else scrollState.scrollTo(0)
                        viewModel.selectBar(endIndex)
                        didInitSelection = true
                    }
                }

                if (data.isNotEmpty()) {
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
                            text = "데이터가 없습니다",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 통계
            val stats = remember(data, endIndex) { computeActivityStats(data, endIndex) }
            ActivitySummaryCard(
                current = stats.current,
                average = stats.average,
                min = stats.min,
                max = stats.max,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 오늘 이상활동 배너
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

            Spacer(modifier = Modifier.height(8.dp))

            // 장소 목록
            if (rooms.isNotEmpty()) {
                Text(
                    text = "장소 목록",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

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
                                            RoomItemCellWithPresence(
                                                name = r.name.ifBlank { "방" },
                                                selected = selected,
                                                present = present,
                                                bg = cardBg,
                                                borderDefault = cardBorder,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                if (!selected) {
                                                    selectedRoomId = r.id
                                                    if (token.isNotEmpty()) {
                                                        viewModel.fetchActivityData(token, r.id, selectedDate)
                                                        // 오늘이면 선택 변경 시 해당 방 재실 갱신 한 번 더
                                                        if (selectedDate == LocalDate.now()) {
                                                            viewModel.fetchPresence(token, r.id)
                                                        }
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
                        val chartPoint = visibleData.getOrNull(selectedIndex)
                            ?: ChartPoint("00:00", 0f)

                        drawCircle(color = Color.Red, radius = 10f, center = point)

                        val tooltipWidth = 150f
                        val tooltipHeight = 70f
                        val tooltipX = (point.x - tooltipWidth / 2).coerceAtLeast(0f)
                        val tooltipY = (point.y - tooltipHeight - 15f).coerceAtLeast(0f)

                        drawRoundRect(
                            color = Color(0xFF0D1B2A),
                            topLeft = Offset(tooltipX, tooltipY),
                            size = Size(tooltipWidth, tooltipHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
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
    current: Int,
    average: Int,
    min: Int,
    max: Int,
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("현재 활동량", color = Color.White.copy(alpha = 0.95f), fontSize = 14.sp)
                Text(current.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("평균 활동량", color = Color.White.copy(alpha = 0.95f), fontSize = 14.sp)
                Text(average.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("최소 활동량", color = Color.White.copy(alpha = 0.95f), fontSize = 14.sp)
                Text(min.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("최고 활동량", color = Color.White.copy(alpha = 0.95f), fontSize = 14.sp)
                Text(max.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

private data class ActivityStats(
    val current: Int,
    val average: Int,
    val min: Int,
    val max: Int
)

private fun computeActivityStats(
    rawData: List<ChartPoint>,
    endIndex: Int
): ActivityStats {
    if (endIndex < 0) return ActivityStats(0, 0, 0, 0)

    val minute10 = FloatArray(144)
    rawData.forEach { p ->
        runCatching {
            val (h, m) = p.timeLabel.split(":").map(String::toInt)
            val idx = (h * 60 + m) / 10
            if (idx in 0..143) minute10[idx] += p.value
        }
    }

    val upto = endIndex.coerceIn(0, 143)
    val slice = minute10.sliceArray(0..upto)

    val current = slice.lastOrNull()?.toInt() ?: 0

    val nonZero = slice.filter { it > 0f }
    val average = if (nonZero.isNotEmpty()) nonZero.average().toInt() else 0
    val min = if (nonZero.isNotEmpty()) nonZero.minOrNull()!!.toInt() else 0
    val max = slice.maxOrNull()?.toInt() ?: 0

    return ActivityStats(current, average, min, max)
}

@Composable
private fun RoomItemCellWithPresence(
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
