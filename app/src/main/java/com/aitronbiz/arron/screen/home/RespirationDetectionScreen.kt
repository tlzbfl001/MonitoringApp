package com.aitronbiz.arron.screen.home

import android.graphics.Paint
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.viewmodel.ActivityViewModel
import com.aitronbiz.arron.viewmodel.RespirationViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Composable
fun RespirationDetectionScreen(
    homeId: String,
    navController: NavController,
    viewModel: RespirationViewModel,
    onBackClick: () -> Unit
) {
    val data by viewModel.chartData.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val selectedRoomId by viewModel.selectedRoomId.collectAsState()
    val selectedDate by viewModel.selectedDate
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val nowLabel = rememberMinuteLabel()

    // 방 목록 불러오기
    LaunchedEffect(Unit) {
        viewModel.fetchRooms(homeId)
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    // 선택된 방/날짜 변경 시 데이터 새로 불러오기
    LaunchedEffect(selectedRoomId, selectedDate) {
        if (selectedRoomId.isNotBlank()) {
            viewModel.fetchRespirationData(selectedRoomId, selectedDate)
        }
    }

    // rooms가 변경되면 presence 전체 갱신
    LaunchedEffect(rooms) {
        if (rooms.isNotEmpty()) {
            viewModel.fetchAllPresence()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(top = 15.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                        .clickable { onBackClick() }
                )
                androidx.compose.material.Text(
                    text = "호흡 감지",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 주간 달력
            RespirationWeekCalendar(
                selectedDate = selectedDate,
                onDateSelected = viewModel::updateSelectedDate
            )

            Spacer(modifier = Modifier.height(25.dp))

            // 차트
            RespirationLineChart(
                rawData = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                scrollState = scrollState,
                selectedIndex = selectedIndex,
                onPointSelected = { index -> viewModel.selectBar(index) },
                selectedDate = selectedDate,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp)
                    .background(Color(0xFF007ACC), RoundedCornerShape(8.dp))
                    .clickable { navController.navigate("realTimeRespiration") }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "실시간 데이터",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // 룸 선택 리스트
            if (rooms.isNotEmpty()) {
                Text(
                    text = "룸 선택",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                    modifier = Modifier.padding(start = 22.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Column {
                    rooms.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 3.dp, start = 20.dp, end = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            row.forEach { room ->
                                val isSelected = room.id == selectedRoomId
                                val isPresent = viewModel.roomPresenceMap[room.id]?.isPresent == true

                                // 현재 시각에 해당하는 호흡수(찾은 항목이 있으면 그 값, 없으면 null)
                                val currentPoint: ChartPoint? = data.find { point ->
                                    point.timeLabel == nowLabel
                                }
                                val currentValue: Float? = currentPoint?.value

                                // 경고 여부: 데이터/재실/날짜/nowLabel 바뀔 때만 재계산
                                val showWarning = remember(data, selectedDate, nowLabel) {
                                    if (selectedDate != LocalDate.now()) return@remember false
                                    val isPresent = viewModel.roomPresenceMap[room.id]?.isPresent == true
                                    val currentValue = data.firstOrNull { it.timeLabel == nowLabel }?.value
                                    isPresent && currentValue != null && (currentValue <= 12f || currentValue >= 24f)
                                }

                                Log.d(TAG, "currentValue: $currentValue")

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(5.dp)
                                        .height(90.dp)
                                        .background(
                                            color = Color(0xFF123456),
                                            shape = RoundedCornerShape(13.dp)
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSelected) Color.White else Color(
                                                0xFF1A4B7C
                                            ),
                                            shape = RoundedCornerShape(13.dp)
                                        )
                                        .clickable { viewModel.selectRoom(room.id) }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        androidx.compose.material.Text(
                                            text = room.name,
                                            color = if (isSelected) Color.White else Color(
                                                0xFF7C7C7C
                                            ),
                                            fontSize = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (isPresent) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(color = Color(0x3290EE90), RoundedCornerShape(5.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    androidx.compose.material.Text(
                                                        text = "재실",
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(5.dp))
                                                WarningBlinkBadge(visible = showWarning)
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .background(color = Color(0x25AFAFAF), RoundedCornerShape(5.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                androidx.compose.material.Text(
                                                    text = "부재중",
                                                    color = Color.White,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RespirationWeekCalendar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val basePage = 1000
    val pagerState = rememberPagerState(initialPage = basePage) { basePage * 2 }
    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x00FFFFFF))
            .padding(start = 15.dp, end = 15.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(0.dp),
            pageSpacing = 0.dp,
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = true
        ) { page ->
            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .plusWeeks((page - basePage).toLong())
            val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDates.forEachIndexed { index, date ->
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val label = dayLabels[index]

                    Column(
                        modifier = Modifier
                            .width(44.dp)
                            .aspectRatio(0.9f)
                            .clip(RoundedCornerShape(5.dp))
                            .then(
                                if (isSelected) Modifier
                                    .border(0.7.dp, Color(0xFF5F66FF), RoundedCornerShape(5.dp))
                                    .background(Color(0x257D83FF), RoundedCornerShape(5.dp))
                                else Modifier
                            )
                            .clickable {
                                onDateSelected(date)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        Text(
                            text = date.dayOfMonth.toString(),
                            color = Color.White,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RespirationLineChart(
    rawData: List<ChartPoint>,
    modifier: Modifier = Modifier,
    maxY: Int = 40,
    scrollState: ScrollState = rememberScrollState(),
    selectedIndex: Int,
    onPointSelected: (Int) -> Unit,
    selectedDate: LocalDate,
    viewModel: RespirationViewModel
) {
    val chartHeight = 180.dp
    val pointSpacing = 0.5.dp
    val density = LocalDensity.current
    val yAxisWidth = 30.dp

    // 하루 1440분 데이터 슬롯 생성
    val filledData = remember(rawData) {
        val slots = MutableList(1440) { index ->
            val h = index / 60
            val m = index % 60
            ChartPoint("%02d:%02d".format(h, m), 0f)
        }
        val map = rawData.associateBy { it.timeLabel }
        slots.map { map[it.timeLabel] ?: it }
    }

    val today = LocalDate.now()
    val now = LocalTime.now()
    val isToday = selectedDate == today
    val isFuture = selectedDate.isAfter(today)
    val nowIndex = now.hour * 60 + now.minute
    val endIndex = when {
        isFuture -> 0
        isToday -> nowIndex
        else -> 1439
    }

    val visibleData = filledData.take(endIndex + 1)
    val totalWidth = with(density) { (1440 * pointSpacing.toPx()).toDp() }
    val pointSpacingPx = with(density) { pointSpacing.toPx() }

    LaunchedEffect(visibleData) {
        if (visibleData.isNotEmpty()) {
            onPointSelected(visibleData.lastIndex)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight + 120.dp)
    ) {
        // Y축
        Column(
            modifier = Modifier
                .width(yAxisWidth)
                .height(chartHeight),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            for (i in 0..4) {
                val value = (maxY / 4f) * (4 - i)
                androidx.compose.material.Text(
                    text = value.toInt().toString(),
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(7.dp))

        // 차트
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState)
                .padding(end = 20.dp)
        ) {
            if (visibleData.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .width(totalWidth)
                        .height(chartHeight + 120.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val clickedIndex =
                                    (offset.x / pointSpacingPx).toInt().coerceIn(0, nowIndex)
                                onPointSelected(clickedIndex)
                                viewModel.setAutoScrollEnabled(false)
                            }
                        }
                ) {
                    val chartAreaHeight = chartHeight.toPx()
                    val unitHeight = chartAreaHeight / maxY

                    for (i in 0..4) {
                        val y = (chartAreaHeight * i) / 4f
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f),
                            start = Offset(0f, chartAreaHeight - y),
                            end = Offset(size.width, chartAreaHeight - y),
                            strokeWidth = 1f
                        )
                    }

                    val points = visibleData.mapIndexed { index, point ->
                        val x = index * pointSpacingPx
                        val y = chartAreaHeight - (point.value * unitHeight)
                        Offset(x, y)
                    }

                    for (slot in 0..1440 step 360) {
                        if (slot == 1440) continue
                        val x = slot * pointSpacingPx
                        val h = slot / 60
                        val m = slot % 60
                        val label = "%02d:%02d".format(h, m)
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x,
                            chartAreaHeight + 50f,
                            Paint().apply {
                                textAlign = Paint.Align.LEFT
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

                    if (points.size > 1) {
                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val curr = points[i]
                                val midX = (prev.x + curr.x) / 2
                                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF5CEAFF),
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )
                    }

                    // 툴팁
                    if (selectedIndex in points.indices) {
                        val point = points[selectedIndex]
                        val chartPoint = filledData[selectedIndex]

                        drawCircle(
                            color = Color.Red,
                            radius = 10f,
                            center = point
                        )

                        val tooltipWidth = 170f
                        val tooltipHeight = 90f
                        val tooltipX = (point.x - tooltipWidth / 2)
                            .coerceIn(0f, size.width - tooltipWidth)
                        val tooltipY = (point.y - tooltipHeight - 15f)
                            .coerceAtLeast(0f)

                        drawRoundRect(
                            color = Color(0xFF0D1B2A),
                            topLeft = Offset(tooltipX, tooltipY),
                            size = Size(tooltipWidth, tooltipHeight),
                            cornerRadius = CornerRadius(16f, 16f)
                        )

                        val canvas = drawContext.canvas.nativeCanvas
                        val paint = Paint().apply {
                            textAlign = Paint.Align.CENTER
                            color = android.graphics.Color.WHITE
                            isAntiAlias = true
                            textSize = 30f
                        }
                        canvas.drawText(chartPoint.timeLabel, point.x, tooltipY + 30f, paint)
                        canvas.drawText("${chartPoint.value.toInt()} bpm", point.x, tooltipY + 60f, paint)
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberMinuteLabel(): String {
    val fmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val label = androidx.compose.runtime.produceState(
        initialValue = LocalTime.now().truncatedTo(ChronoUnit.MINUTES).format(fmt)
    ) {
        while (true) {
            val now = LocalTime.now()
            val ms = ((60 - now.second) * 1000L) - now.nano / 1_000_000L
            kotlinx.coroutines.delay(ms)
            value = LocalTime.now().truncatedTo(ChronoUnit.MINUTES).format(fmt)
        }
    }
    return label.value
}

@Composable
private fun WarningBlinkBadge(visible: Boolean) {
    if (!visible) return
    val infinite = rememberInfiniteTransition(label = "blink")
    val alpha by infinite.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .background(Color.Red.copy(alpha = alpha), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        androidx.compose.material.Text(
            text = "경고",
            color = Color.White,
            fontSize = 11.sp
        )
    }
}