package com.aitronbiz.arron.view.home

import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.viewmodel.ActivityViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import androidx.core.graphics.toColorInt
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import java.time.LocalTime

class ActivityDetectionFragment : Fragment() {
    private val viewModel: ActivityViewModel by activityViewModels()
    private val token: String = AppController.prefs.getToken().toString()
    private var homeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeId = arguments?.getString("homeId")
        viewModel.resetState() // 초기 상태 리셋(데이터 초기화 등)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ActivityChartScreen(
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

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.hideBottomNav()
    }
}

@Composable
fun ActivityChartScreen(
    viewModel: ActivityViewModel,
    token: String,
    homeId: String,
    onBackClick: () -> Unit
) {
    val data by viewModel.chartData.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val selectedRoomId by viewModel.selectedRoomId.collectAsState()
    val selectedDate by viewModel.selectedDate
    val scrollState = rememberScrollState()
    val statusBarHeight = rememberStatusBarHeight()
    val density = LocalDensity.current

    // 방 목록 불러오기
    LaunchedEffect(Unit) {
        viewModel.fetchRooms(token, homeId)
    }

    // 선택된 방/날짜 변경 시 데이터 새로 불러오기
    LaunchedEffect(selectedRoomId, selectedDate) {
        if (selectedRoomId.isNotBlank()) {
            viewModel.fetchActivityData(token, selectedRoomId, selectedDate)
        }
    }

    // rooms가 변경되면 presence 전체 갱신
    LaunchedEffect(rooms) {
        if (rooms.isNotEmpty()) {
            viewModel.fetchAllPresence(token)
        }
    }

    // 화면 진입 시 마지막 데이터 선택 + 스크롤
    LaunchedEffect(data) {
        if (data.isNotEmpty()) {
            val lastIdx = data.maxOf {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(top = statusBarHeight + 15.dp)
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
            Text(
                text = "활동량 감지",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 주간 달력
        WeekCalendar(
            selectedDate = selectedDate,
            onDateSelected = viewModel::updateSelectedDate
        )

        Spacer(modifier = Modifier.height(25.dp))

        // 활동량 차트
        if (data.isNotEmpty()) {
            TimeLineChart(
                rawData = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                scrollState = scrollState,
                selectedIndex = selectedIndex,
                onPointSelected = { index ->
                    viewModel.selectBar(index)
                },
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

            val infiniteTransition = rememberInfiniteTransition(label = "blink")
            val blinkAlpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                label = "alpha"
            )

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
                            val status = viewModel.roomMap[room.id]
                            val presence = viewModel.roomPresenceMap[room.id]
                            val isPresent = presence?.isPresent == true
                            val showWarning = selectedDate == LocalDate.now() && status != null &&
                                    (status <= 10 || status >= 80)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .height(90.dp)
                                    .background(
                                        color = Color(0xFF123456),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.5.dp,
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

                                    if(isPresent) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        ) {
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

                                            if(showWarning) {
                                                Spacer(modifier = Modifier.width(5.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = Color.Red.copy(alpha = blinkAlpha),
                                                            shape = RoundedCornerShape(5.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "경고",
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
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
fun WeekCalendar(
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
fun TimeLineChart(
    rawData: List<ChartPoint>,
    modifier: Modifier = Modifier,
    maxY: Int = 100,
    scrollState: ScrollState = rememberScrollState(),
    selectedIndex: Int,
    onPointSelected: (Int) -> Unit,
    selectedDate: LocalDate,
    smoothing: Float = 0.5f
) {
    val chartHeight = 180.dp
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
        isToday -> nowIndex
        else -> 143
    }

    val visibleData = if (rawData.isEmpty()) emptyList() else filledData.take(endIndex + 1)
    val totalWidth = with(density) { (144 * pointSpacing.toPx()).toDp() }

    LaunchedEffect(filledData, selectedDate) {
        if (visibleData.isNotEmpty()) {
            val initialIndex = when {
                isFuture -> 0
                isToday -> nowIndex
                else -> 143
            }
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
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val clickedIndex = (offset.x / pointSpacing.toPx()).toInt()
                                if (clickedIndex in 0..nowIndex) {
                                    onPointSelected(clickedIndex)
                                }
                            }
                        }
                ) {
                    val chartAreaHeight = chartHeight.toPx()
                    val unitHeight = chartAreaHeight / maxY
                    val widthPerPoint = pointSpacing.toPx()

                    // X축 눈금
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

                    // 데이터 곡선
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

                    // 툴팁
                    if (selectedIndex in points.indices) {
                        val point = points[selectedIndex]
                        val chartPoint = filledData[selectedIndex]

                        drawCircle(color = Color.Red, radius = 10f, center = point)

                        val tooltipWidth = 150f
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
                            textAlign = android.graphics.Paint.Align.CENTER
                            color = android.graphics.Color.WHITE
                            isAntiAlias = true
                            textSize = 26f
                        }
                        canvas.drawText(chartPoint.timeLabel, point.x, tooltipY + 30f, paint)
                        canvas.drawText("${chartPoint.value.toInt()} 회", point.x, tooltipY + 60f, paint)
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
fun rememberStatusBarHeight(): Dp {
    val context = LocalContext.current
    val resourceId = remember {
        context.resources.getIdentifier("status_bar_height", "dimen", "android")
    }
    val heightPx = remember {
        if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }
    return with(LocalDensity.current) { heightPx.toDp() }
}