package com.aitronbiz.arron.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.viewmodel.FallViewModel
import com.aitronbiz.arron.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun FallDetectionScreen(
    homeId: String,
    roomId: String,
    selectedDate: LocalDate,
    viewModel: FallViewModel = viewModel(),
    navController: NavController,
    mainViewModel: MainViewModel = viewModel()
) {
    val chartData by viewModel.chartPoints.collectAsState()
    val totalFalls by viewModel.totalCount.collectAsState()
    val roomName by viewModel.roomName
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val presenceByRoomId by viewModel.presenceByRoomId.collectAsState()
    var selectedRoomId by remember { mutableStateOf("") }
    var hasUnreadNotification by remember { mutableStateOf(false) }

    // 알림 체크
    LaunchedEffect(Unit) {
        mainViewModel.checkNotifications { hasUnreadNotification = it }
    }

    LaunchedEffect(selectedDate) {
        viewModel.updateSelectedDate(selectedDate)
    }

    // 홈의 방 목록
    LaunchedEffect(homeId) {
        AppController.prefs.getToken()?.let { token ->
            if (token.isNotEmpty()) viewModel.fetchRooms(token, homeId)
        }
    }

    // 초기 데이터 로드
    LaunchedEffect(rooms) {
        if (rooms.isNotEmpty() && selectedRoomId.isBlank()) {
            selectedRoomId = if (roomId.isNotBlank()) roomId else rooms.first().id
            AppController.prefs.getToken()?.let { token ->
                if (token.isNotEmpty()) {
                    viewModel.fetchRoomName(token, selectedRoomId)
                    viewModel.fetchFallsData(token, selectedRoomId, selectedDate)
                    if (selectedDate == LocalDate.now()) viewModel.fetchPresence(token, selectedRoomId)
                }
            }
        }
    }

    // 룸/날짜 변경 시 데이터 로드
    LaunchedEffect(selectedRoomId, selectedDate) {
        if (selectedRoomId.isNotBlank()) {
            AppController.prefs.getToken()?.let { token ->
                if (token.isNotEmpty()) {
                    viewModel.fetchRoomName(token, selectedRoomId)
                    viewModel.fetchFallsData(token, selectedRoomId, selectedDate)
                    if (selectedDate == LocalDate.now()) viewModel.fetchPresence(token, selectedRoomId)
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
                text = "낙상감지",
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

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item { Spacer(modifier = Modifier.height(3.dp)) }

            item {
                val binMinutes = 30
                val binCount = 1440 / binMinutes
                val today = LocalDate.now()
                val isToday = selectedDate == today
                val isFuture = selectedDate.isAfter(today)

                val endBinInclusive =
                    if (isToday) rememberNowBin(binMinutes, selectedDate) else binCount - 1

                val lastBinValue by remember(chartData, selectedDate, endBinInclusive) {
                    mutableFloatStateOf(
                        if (isToday) latestBinValueForToday(chartData, binMinutes) else 0f
                    )
                }

                var didInitSelection by remember(selectedDate, selectedRoomId) {
                    mutableStateOf(false)
                }
                LaunchedEffect(selectedDate, selectedRoomId, endBinInclusive, isFuture) {
                    if (didInitSelection) return@LaunchedEffect
                    if (!isFuture) {
                        val targetBin = endBinInclusive.coerceAtLeast(0)
                        viewModel.selectBar(targetBin * binMinutes)
                    }
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
                        endBinInclusive = endBinInclusive,
                        showData = true,
                        showYAxis = true,
                        showTooltip = true
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "데이터가 없습니다.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // 요약 카드
                FallSummaryCard(
                    locations = roomName.ifBlank { "-" },
                    count = totalFalls,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedDate == today) {
                    if (lastBinValue > 0f) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_fall),
                                contentDescription = "낙상 감지",
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
                                text = "낙상 사고가 감지되었습니다",
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

            item { Spacer(modifier = Modifier.height(5.dp)) }

            // 장소 목록
            if (rooms.isNotEmpty()) {
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
                                                    name = r.name.ifBlank { "장소" },
                                                    selected = selected,
                                                    present = present,
                                                    bg = cardBg,
                                                    borderDefault = cardBorder,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    if (selectedRoomId != r.id) {
                                                        selectedRoomId = r.id
                                                        AppController.prefs.getToken()?.let { token ->
                                                            if (token.isNotEmpty()) {
                                                                viewModel.fetchRoomName(token, r.id)
                                                                viewModel.fetchFallsData(token, r.id, selectedDate)
                                                                if (selectedDate == LocalDate.now()) {
                                                                    viewModel.fetchPresence(token, r.id)
                                                                }
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
    endBinInclusive: Int,
    showData: Boolean = true,
    showYAxis: Boolean = true,
    showTooltip: Boolean = true
) {
    // 분 단위로 쌓기
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

    // 30분 bin
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

    // Y축
    val yMaxInt = maxY
    val yMax = yMaxInt.toFloat()
    val tickCount = 5
    val yStepValue = yMaxInt / tickCount.toFloat()

    data class Pads(
        val leftPad: Float, val rightPad: Float, val topPad: Float,
        val bottomPad: Float, val baseY: Float, val chartW: Float, val chartH: Float, val dx: Float
    )

    val xAxisExtraGap = 10.dp
    val yAxisGap = 10.dp

    val pointerMod = if (showTooltip && showData) {
        Modifier.pointerInput(binnedValues, clampedEnd) {
            detectTapGestures { offset ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()

                fun computePads(): Pads {
                    val outerLeft = 25.dp.toPx()
                    val outerRight = 20.dp.toPx()
                    val topPad = 12.dp.toPx()
                    val bottomPad = 24f + xAxisExtraGap.toPx()

                    val yPaint = android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.RIGHT
                        textSize = 28f
                        isAntiAlias = true
                    }
                    val labelWidthPx = if (showYAxis) yPaint.measureText(yMaxInt.toString()) else 0f
                    val gapPx = yAxisGap.toPx()

                    val leftPad = outerLeft + labelWidthPx + gapPx
                    val rightPad = outerRight
                    val chartW = (w - leftPad - rightPad).coerceAtLeast(1f)
                    val chartH = (h - topPad - bottomPad).coerceAtLeast(1f)
                    val baseY = topPad + chartH
                    val dx = if (binCount > 1) chartW / (binCount - 1) else chartW
                    return Pads(leftPad, rightPad, topPad, bottomPad, baseY, chartW, chartH, dx)
                }

                val pads = computePads()
                val xRel = (offset.x - pads.leftPad).coerceIn(0f, pads.chartW)
                val bin = (xRel / pads.dx).toInt().coerceIn(0, clampedEnd)
                onPointSelected(bin * binMinutes)
            }
        }
    } else Modifier

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .then(pointerMod)
    ) {
        val w = size.width
        val h = size.height

        fun computePads(): Pads {
            val outerLeft = 25.dp.toPx()
            val outerRight = 20.dp.toPx()
            val topPad = 12.dp.toPx()
            val bottomPad = 24f + xAxisExtraGap.toPx()

            val yPaint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.RIGHT
                textSize = 28f
                isAntiAlias = true
            }
            val labelWidthPx = if (showYAxis) yPaint.measureText(yMaxInt.toString()) else 0f
            val gapPx = yAxisGap.toPx()

            val leftPad = outerLeft + labelWidthPx + gapPx
            val rightPad = outerRight
            val chartW = (w - leftPad - rightPad).coerceAtLeast(1f)
            val chartH = (h - topPad - bottomPad).coerceAtLeast(1f)
            val baseY = topPad + chartH
            val dx = if (binCount > 1) chartW / (binCount - 1) else chartW
            return Pads(leftPad, rightPad, topPad, bottomPad, baseY, chartW, chartH, dx)
        }

        val pads = computePads()

        fun xOf(bin: Int) = pads.leftPad + bin * pads.dx
        fun yOf(v: Float): Float {
            val vv = v.coerceIn(0f, yMax)
            val ratio = vv / yMax
            return pads.baseY - ratio * pads.chartH
        }

        // X축 기준선
        drawLine(
            color = Color.White.copy(alpha = 0.30f),
            start = Offset(pads.leftPad, pads.baseY),
            end = Offset(w - pads.rightPad, pads.baseY),
            strokeWidth = 2.5f
        )

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

        // Y축 눈금/라벨
        if (showYAxis) {
            val yPaint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.RIGHT
                textSize = 28f
                color = android.graphics.Color.WHITE
                isAntiAlias = true
            }
            val gapPx = 10.dp.toPx()
            val labelX = pads.leftPad - gapPx

            for (i in 0..tickCount) {
                val ratio = i / tickCount.toFloat()
                val y = pads.baseY - ratio * pads.chartH
                val labelValue = (i * yStepValue).toInt()

                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = Offset(pads.leftPad - 5f, y),
                    end = Offset(pads.leftPad, y),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    labelValue.toString(),
                    labelX,
                    y + 10f,
                    yPaint
                )
            }
        }

        // X축 라벨
        val xPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.LEFT
            textSize = 28f
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
        val xLabelGap = 6.dp.toPx()
        val xInset = 2.dp.toPx()
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

        // 선택 툴팁
        val tipEnd = maxOf(0, clampedEnd)
        if (showTooltip && tipEnd >= 0) {
            val tipPoints = (0..tipEnd).map { i -> Offset(xOf(i), yOf(binnedValues[i])) }
            val selectedOk = selectedBin in 0..tipEnd && tipPoints.isNotEmpty()
            if (selectedOk) {
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
                val valueText = "${kotlin.math.min(binnedValues[selectedBin], yMax).toInt()} 회"

                val p = android.graphics.Paint().apply {
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
                Text("낙상 위치", color = Color.White.copy(alpha = 0.95f), fontSize = 14.sp)
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
                Text("낙상 횟수", color = Color.White.copy(alpha = 0.95f), fontSize = 14.sp)
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
