package com.aitronbiz.arron.view.home

import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.material.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
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
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.viewmodel.ActivityViewModel
import com.aitronbiz.arron.viewmodel.FallViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

class FallDetectionFragment : Fragment() {
    private val viewModel: FallViewModel by activityViewModels()
    private val token: String = AppController.prefs.getToken().toString()
    private var homeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeId = arguments?.getString("homeId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                FallChartScreen(
                    viewModel = viewModel,
                    token = token,
                    homeId = homeId!!,
                    onBackClick = {
                        replaceFragment1(parentFragmentManager, MainFragment())
                    }
                )
            }
        }
    }
}

@Composable
fun FallChartScreen(
    viewModel: FallViewModel,
    token: String,
    homeId: String,
    onBackClick: () -> Unit
) {
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val selectedRoomId by viewModel.selectedRoomId.collectAsState()
    val selectedDate by viewModel.selectedDate
    val scrollState = rememberScrollState()
    val statusBarHeight = respirationStatusBarHeight()
    val density = LocalDensity.current

    val mockData by remember(selectedDate, selectedRoomId) {
        mutableStateOf(generateMockData())
    }

    // 방 목록 불러오기
    LaunchedEffect(Unit) {
        viewModel.fetchRooms(token, homeId)
    }

    // 화면 진입 시 mockData의 마지막 데이터로 이동
    LaunchedEffect(mockData) {
        if (mockData.isNotEmpty()) {
            val lastIdx = mockData.maxOf {
                val (h, m) = it.timeLabel.split(":").map { t -> t.toInt() }
                h * 60 + m
            }
            viewModel.selectBar(lastIdx)

            val offsetPx = with(density) {
                (6.dp).roundToPx() * (lastIdx - 30).coerceAtLeast(0)
            }
            scrollState.scrollTo(offsetPx)
        }
    }

    // rooms가 변경되면 presence 전체 갱신
    LaunchedEffect(rooms) {
        if (rooms.isNotEmpty()) {
            viewModel.fetchAllPresence(token)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(top = statusBarHeight + 15.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 상단 타이틀바
        Box(
            modifier = Modifier.fillMaxWidth()
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
            Text(
                text = "낙상 감지",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 주간 달력
        FallWeekCalendar(
            selectedDate = selectedDate,
            onDateSelected = viewModel::updateSelectedDate
        )

        Spacer(modifier = Modifier.height(25.dp))

        if (mockData.isNotEmpty() && rooms.isNotEmpty()) {
            FallLineChart(
                rawData = mockData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                scrollState = scrollState,
                selectedIndex = selectedIndex,
                onPointSelected = { index ->
                    viewModel.selectBar(index)
                },
                maxY = 5,
                selectedDate = selectedDate
            )
            Spacer(modifier = Modifier.height(30.dp))
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

            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                rooms.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        row.forEach { room ->
                            val isSelected = room.id == selectedRoomId
                            val presence = viewModel.roomPresenceMap[room.id]
                            val isPresent = presence?.isPresent == true

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(90.dp)
                                    .background(
                                        color = Color(0xFF123456),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) Color.White else Color(0xFF1A4B7C),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.selectRoom(room.id) }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = room.name,
                                        color = if (isSelected) Color.White else Color(0xFF7C7C7C),
                                        fontSize = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    if (isPresent) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = Color(0x3290EE90),
                                                    shape = RoundedCornerShape(5.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "재실",
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = Color(0x25AFAFAF),
                                                    shape = RoundedCornerShape(5.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FallWeekCalendar(
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
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FallLineChart(
    rawData: List<ChartPoint>,
    modifier: Modifier = Modifier,
    maxY: Int = 5,
    scrollState: ScrollState = rememberScrollState(),
    selectedIndex: Int,
    onPointSelected: (Int) -> Unit,
    selectedDate: LocalDate
) {
    val chartHeight = 180.dp
    val pointSpacing = 1.dp
    val density = LocalDensity.current

    val filledData = remember(rawData) {
        val slots = MutableList(1440) { index ->
            val h = index / 60
            val m = index % 60
            ChartPoint("%02d:%02d".format(h, m), 0f)
        }
        rawData.forEach { point ->
            val parts = point.timeLabel.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val index = h * 60 + m
            if (index in slots.indices) {
                slots[index] = slots[index].copy(value = point.value)
            }
        }
        slots
    }

    val today = LocalDate.now()
    val now = LocalTime.now()
    val isToday = selectedDate == today
    val isPast = selectedDate.isBefore(today)
    val isFuture = selectedDate.isAfter(today)

    val nowIndex = now.hour * 60 + now.minute
    val endIndex = when {
        isFuture -> 0 // 미래면 표시 안 함
        isToday -> nowIndex
        else -> 1439 // 과거면 하루 끝까지
    }

    val visibleData = if (rawData.isEmpty()) emptyList() else filledData.take(endIndex + 1)
    val totalWidth = with(density) { (1440 * pointSpacing.toPx()).toDp() }

    // 초기 툴팁 위치
    LaunchedEffect(filledData, selectedDate) {
        if (visibleData.isNotEmpty()) {
            val initialIndex = when {
                isFuture -> 0
                isToday -> nowIndex
                else -> 1439
            }
            val offsetPx = with(density) { (initialIndex * pointSpacing.toPx()).toInt() }
            scrollState.scrollTo(offsetPx)
            onPointSelected(initialIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight + 120.dp)
            .horizontalScroll(scrollState)
            .padding(start = 45.dp, end = 50.dp)
    ) {
        if (visibleData.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .width(totalWidth)
                    .height(chartHeight + 120.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val clickedIndex = (offset.x / pointSpacing.toPx()).toInt()
                            if (clickedIndex in visibleData.indices) {
                                onPointSelected(clickedIndex)
                            }
                        }
                    }
            ) {
                val chartAreaHeight = chartHeight.toPx()
                val unitHeight = chartAreaHeight / maxY
                val widthPerPoint = pointSpacing.toPx()

                // Y축
                for (i in 0..maxY) {
                    val y = chartAreaHeight - i * unitHeight
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        i.toString(),
                        -30f,
                        y + 10f,
                        Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 28f
                            textAlign = Paint.Align.RIGHT
                            isAntiAlias = true
                        }
                    )
                }

                // X축
                for (slot in 0..1440 step 240) {
                    val h = slot / 60
                    val m = slot % 60
                    val label = String.format("%02d:%02d", h, m)
                    val x = slot * widthPerPoint
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x,
                        chartAreaHeight + 40f,
                        Paint().apply {
                            textAlign = Paint.Align.LEFT
                            textSize = 28f
                            color = android.graphics.Color.WHITE
                            isAntiAlias = true
                        }
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(x, chartAreaHeight),
                        end = Offset(x, chartAreaHeight + 10f),
                        strokeWidth = 1f
                    )
                }

                // 데이터 라인
                val points = visibleData.mapIndexed { index, point ->
                    val x = index * widthPerPoint
                    val y = chartAreaHeight - point.value * unitHeight
                    Offset(x, y)
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
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }

                // 툴팁
                if (selectedIndex in points.indices) {
                    val point = points[selectedIndex]
                    val chartPoint = visibleData[selectedIndex]

                    drawCircle(color = Color.Red, radius = 10f, center = point)

                    val tooltipWidth = 160f
                    val tooltipHeight = 70f
                    val tooltipX = point.x - tooltipWidth / 2
                    val tooltipY = point.y - tooltipHeight - 15f

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
                        textSize = 26f
                    }
                    canvas.drawText(chartPoint.timeLabel, point.x, tooltipY + 30f, paint)
                    canvas.drawText("${chartPoint.value.toInt()}", point.x, tooltipY + 60f, paint)
                }
            }
        } else {
            // 미래일 경우 표시할 데이터가 없을 때 메시지 출력
            Text(
                text = "데이터 없음",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

fun generateMockData(): List<ChartPoint> {
    val tempList = mutableListOf<ChartPoint>()
    val randomMinutes = (0 until 1440).shuffled().take(30)
    randomMinutes.forEach { minuteOfDay ->
        val hour = minuteOfDay / 60
        val minute = minuteOfDay % 60
        val timeLabel = String.format("%02d:%02d", hour, minute)
        val value = (0..5).random().toFloat()
        tempList.add(ChartPoint(timeLabel, value))
    }
    return tempList.sortedBy {
        val parts = it.timeLabel.split(":")
        parts[0].toInt() * 60 + parts[1].toInt()
    }
}

@Composable
fun fallStatusBarHeight(): Dp {
    val context = LocalContext.current
    val resourceId = remember {
        context.resources.getIdentifier("status_bar_height", "dimen", "android")
    }
    val heightPx = remember {
        if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }
    return with(LocalDensity.current) { heightPx.toDp() }
}