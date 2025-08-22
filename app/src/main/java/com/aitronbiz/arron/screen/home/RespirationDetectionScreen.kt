package com.aitronbiz.arron.screen.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.viewmodel.MainViewModel
import com.aitronbiz.arron.viewmodel.RespStats
import com.aitronbiz.arron.viewmodel.RespirationViewModel
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.ceil
import kotlin.math.min

@Composable
fun RespirationDetectionScreen(
    homeId: String,
    roomId: String,
    selectedDate: LocalDate,
    viewModel: RespirationViewModel = viewModel(),
    navController: NavController,
    mainViewModel: MainViewModel = viewModel()
) {
    val token = AppController.prefs.getToken().orEmpty()

    val chartData by viewModel.chartData.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val currentBpm by viewModel.currentBpm.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val presenceByRoomId by viewModel.presenceByRoomId.collectAsState()

    var selectedRoomId by remember { mutableStateOf(roomId) }

    var hasUnreadNotification by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mainViewModel.checkNotifications { hasUnreadNotification = it } }

    LaunchedEffect(selectedDate) {
        viewModel.updateSelectedDate(selectedDate)
    }

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
        if (selectedRoomId.isNotBlank()) {
            viewModel.fetchRespirationData(selectedRoomId, selectedDate)
            if (token.isNotBlank() && selectedDate == LocalDate.now()) {
                viewModel.fetchPresence(token, selectedRoomId)
            }
        }
    }

    LaunchedEffect(selectedDate, chartData) {
        if (chartData.isNotEmpty()) {
            val endIndex = endDrawIndexFor(selectedDate, chartData.lastIndex)
            if (endIndex >= 0 && endIndex != selectedIndex) {
                viewModel.selectBar(endIndex)
            }
        }
    }

    val contentScroll = rememberScrollState()

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
                .padding(start = 5.dp, end = 17.dp, top = 2.dp)
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
                text = "호흡 감지",
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp, start = 22.dp, end = 20.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDate.toString(),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.weight(1f))

            RealTimeButton(
                text = "실시간",
                modifier = Modifier
                    .width(80.dp)
                    .height(29.dp),
                onClick = { navController.navigate("realTimeRespiration/$roomId") }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 10.dp)
                .verticalScroll(contentScroll)
        ) {
            // 차트
            if (chartData.isNotEmpty()) {
                RespirationLineChart(
                    raw = chartData,
                    selectedDate = selectedDate,
                    selectedIndex = selectedIndex,
                    fixedMaxY = ceil(
                        chartData
                            .take(endDrawIndexFor(selectedDate, chartData.lastIndex) + 1)
                            .maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
                    ).toInt(),
                    onPointSelected = { idx -> viewModel.selectBar(idx) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(end = 20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "데이터가 없습니다",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(25.dp))

            RespirationStatsCard(
                current = currentBpm.toInt(),
                stats = stats,
                showTime = selectedDate == LocalDate.now(),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(30.dp))

            // 방 목록
            if (rooms.isNotEmpty()) {
                Text(
                    text = "장소 목록",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
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
                                            RespirationRoomItemCell(
                                                name = r.name,
                                                selected = selected,
                                                present = present,
                                                bg = cardBg,
                                                borderDefault = cardBorder,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                if (selectedRoomId != r.id) {
                                                    selectedRoomId = r.id
                                                    if (token.isNotBlank() && selectedDate == LocalDate.now()) {
                                                        viewModel.fetchPresence(token, r.id)
                                                    }
                                                    viewModel.fetchRespirationData(r.id, selectedDate)
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
fun RespirationLineChart(
    raw: List<ChartPoint>,
    selectedDate: LocalDate,
    selectedIndex: Int,
    fixedMaxY: Int,
    onPointSelected: (Int) -> Unit,
    yAxisWidth: Dp = 34.dp,
    modifier: Modifier = Modifier
) {
    if (raw.isEmpty()) return

    val density = LocalDensity.current
    val labelSp = 10.sp
    val labelPx = with(density) { labelSp.toPx() }

    var viewportWidthPx by remember { mutableStateOf(0) }
    val extraRightDp = 100.dp
    val baseContentWidthDp = remember(viewportWidthPx) {
        if (viewportWidthPx <= 0) 0.dp else with(density) { (viewportWidthPx * 1.15f).toDp() }
    }
    val contentWidthDp = if (baseContentWidthDp > 0.dp) baseContentWidthDp + extraRightDp else 0.dp
    val hScroll = rememberScrollState()

    var scrollViewportPx by remember { mutableStateOf(0) }

    LaunchedEffect(scrollViewportPx, contentWidthDp, raw.size, selectedDate, selectedIndex) {
        if (scrollViewportPx <= 0 || contentWidthDp <= 0.dp) return@LaunchedEffect
        val contentWidthPx = with(density) { contentWidthDp.toPx() }
        val pxPerMin = contentWidthPx / 1440f
        val endI = endDrawIndexFor(selectedDate, raw.lastIndex).coerceAtLeast(0)
        val endX = endI * pxPerMin
        if (endX > scrollViewportPx) {
            val target = (endX - scrollViewportPx).toInt().coerceAtLeast(0)
            hScroll.scrollTo(target)
        } else {
            hScroll.scrollTo(0)
        }
    }

    Row(
        modifier = modifier
            .height(160.dp)
            .fillMaxWidth()
            .onGloballyPositioned { coords -> viewportWidthPx = coords.size.width }
    ) {
        // 고정 Y축
        Canvas(
            modifier = Modifier
                .width(yAxisWidth)
                .fillMaxHeight()
        ) {
            val chartH = size.height
            val baseY = chartH - 22f
            val topPad = 10f
            val usableH = baseY - topPad
            val maxY = fixedMaxY.coerceAtLeast(1)

            val yPaint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.RIGHT
                textSize = labelPx
                color = android.graphics.Color.WHITE
                isAntiAlias = true
            }
            val ticks = 4
            for (i in 0..ticks) {
                val ratio = i / ticks.toFloat()
                val y = baseY - ratio * usableH
                val v = (maxY * ratio).toInt()
                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = Offset(size.width - 5f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    v.toString(), size.width - 6f, y + labelPx * 0.35f, yPaint
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { coords -> scrollViewportPx = coords.size.width }
                .horizontalScroll(hScroll)
        ) {
            Canvas(
                modifier = Modifier
                    .width(if (contentWidthDp > 0.dp) contentWidthDp else 1.dp)
                    .fillMaxHeight()
                    .clickable(enabled = true, onClick = { /* consume */ })
            ) {
                val topPad = 10f
                val bottomPad = 22f
                val axisX = 0f
                val chartW = size.width
                val chartH = size.height
                val baseY = chartH - bottomPad
                val usableH = baseY - topPad
                val maxY = fixedMaxY.coerceAtLeast(1)

                val pxPerMin = chartW / 1440f
                fun xOf(minuteIndex: Int): Float = axisX + minuteIndex * pxPerMin
                fun yOf(v: Float): Float {
                    val m = maxY.toFloat()
                    val vv = v.coerceIn(0f, m)
                    return baseY - (vv / m) * usableH
                }

                // X축
                drawLine(
                    color = Color.White.copy(alpha = 0.30f),
                    start = Offset(axisX, baseY),
                    end = Offset(chartW, baseY),
                    strokeWidth = 2f
                )

                // 데이터 라인/영역
                val eIdx = endDrawIndexFor(selectedDate, raw.lastIndex)
                val points = (0..eIdx).map { i -> Offset(xOf(i), yOf(raw[i].value)) }
                if (points.size >= 2) {
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

                    val areaPath = Path().apply {
                        moveTo(points.first().x, baseY)
                        for (i in points.indices) {
                            val p0 = if (i == 0) points[i] else points[i - 1]
                            val p1 = points[i]
                            val midX = if (i == 0) p1.x else (p0.x + p1.x) / 2f
                            cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                        }
                        lineTo(points.last().x, baseY)
                        close()
                    }
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF5CEAFF).copy(alpha = 0.55f),
                                Color(0xFF5CEAFF).copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            startY = topPad,
                            endY = baseY
                        )
                    )
                }

                // X 라벨
                val xPaint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.LEFT
                    textSize = labelPx
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                }
                val hours = listOf(0, 6, 12, 18)
                val labels = listOf("12AM", "6AM", "12PM", "6PM")
                val gap = 4.dp.toPx()
                hours.forEachIndexed { idx, h ->
                    val mi = h * 60
                    val xl = xOf(mi) + 2.dp.toPx()
                    drawContext.canvas.nativeCanvas.drawText(
                        labels[idx], xl, baseY + labelPx + gap, xPaint
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.40f),
                        start = Offset(xOf(mi), baseY),
                        end = Offset(xOf(mi), baseY + 8f),
                        strokeWidth = 2f
                    )
                }

                // 툴팁 포인트
                val tipIdx = selectedIndex.coerceIn(0, endDrawIndexFor(selectedDate, raw.lastIndex))
                val tip = Offset(xOf(tipIdx), yOf(raw[tipIdx].value))
                drawCircle(Color(0xFFFF5C5C), radius = 5.5f, center = tip)

                // 툴팁 박스
                val tipW = 150f
                val tipH = 64f
                val safeMaxX = size.width - tipW
                val tipX = if (safeMaxX <= 0f) 0f else (tip.x - tipW / 2).coerceIn(0f, safeMaxX)
                val safeMinY = topPad + 2f
                val calculatedY = tip.y - tipH - 10f
                val tipY = if (calculatedY < safeMinY) safeMinY else calculatedY

                drawRoundRect(
                    color = Color(0xFF0D1B2A),
                    topLeft = Offset(tipX, tipY),
                    size = Size(tipW, tipH),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                val tp = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                    textSize = labelPx
                }
                val timeText = raw[tipIdx].timeLabel
                val valueText = "${raw[tipIdx].value.toInt()} bpm"
                drawContext.canvas.nativeCanvas.drawText(timeText, tipX + tipW / 2, tipY + 26f, tp)
                drawContext.canvas.nativeCanvas.drawText(valueText, tipX + tipW / 2, tipY + 48f, tp)
            }
        }
    }
}

@Composable
private fun RespirationStatsCard(
    current: Int,
    stats: RespStats,
    showTime: Boolean,
    modifier: Modifier = Modifier
) {
    val labelStyle = TextStyle(
        color = Color.White.copy(alpha = 0.95f),
        fontSize = 14.sp,
    )
    val valueStyle = TextStyle(
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )

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
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("현재 호흡수", style = labelStyle)

                if (showTime) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "(${formatNowHHmm()})",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp
                    )
                }

                Spacer(Modifier.weight(1f))
                Text(
                    "$current bpm",
                    style = valueStyle,
                    textAlign = TextAlign.Right
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("최저 호흡수", style = labelStyle)
                Text("${stats.min} bpm", style = valueStyle, textAlign = TextAlign.Right)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("최고 호흡수", style = labelStyle)
                Text("${stats.max} bpm", style = valueStyle, textAlign = TextAlign.Right)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("평균 호흡수", style = labelStyle)
                Text("${stats.avg} bpm", style = valueStyle, textAlign = TextAlign.Right)
            }
        }
    }
}

private fun endDrawIndexFor(selectedDate: LocalDate, lastIndex: Int): Int {
    val today = LocalDate.now()
    return if (selectedDate == today) {
        val now = LocalTime.now()
        min(lastIndex, now.hour * 60 + now.minute)
    } else lastIndex
}

private fun formatNowHHmm(): String = LocalTime.now().run { String.format("%02d:%02d", hour, minute) }

@Composable
fun RealTimeButton(
    text: String = "실시간",
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF39C0FF), Color(0xFF007ACC)))
    val corner = RoundedCornerShape(9.dp)

    val infinite = rememberInfiniteTransition()
    val shimmerX by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing))
    )

    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(120))

    Box(
        modifier = modifier
            .scale(scale)
            .clip(corner)
            .shadow(10.dp, corner, clip = true)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                brush = gradient,
                size = size,
                cornerRadius = CornerRadius(9.dp.toPx())
            )
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val start = Offset(x = w * (shimmerX - 0.4f), y = 0f)
            val end = Offset(x = w * (shimmerX + 0.4f), y = h)
            drawRect(
                brush = Brush.linearGradient(
                    0f to Color.Transparent,
                    0.5f to Color.White.copy(alpha = 0.10f),
                    1f to Color.Transparent,
                    start = start, end = end
                ),
                size = size
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        if (pressed) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(corner)
                    .background(Color.Black.copy(alpha = 0.08f))
            )
        }
    }
}

@Composable
fun RespirationRoomItemCell(
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
                text = name.ifBlank { "방" },
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