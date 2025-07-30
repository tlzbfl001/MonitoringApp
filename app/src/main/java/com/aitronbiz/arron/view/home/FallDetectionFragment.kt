package com.aitronbiz.arron.view.home

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
    val rooms by viewModel.rooms.collectAsState()

    // 서버 데이터 대신 임의 데이터 사용
    val mockData = remember {
        val tempList = mutableListOf<ChartPoint>()
        val randomMinutes = (0 until 1440).shuffled().take(30)
        randomMinutes.forEach { minuteOfDay ->
            val hour = minuteOfDay / 60
            val minute = minuteOfDay % 60
            val timeLabel = String.format("%02d:%02d", hour, minute)
            val value = (1..5).random().toFloat()
            tempList.add(ChartPoint(timeLabel, value))
        }
        // 시간순 정렬
        tempList.sortedBy {
            val parts = it.timeLabel.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        }
    }

    val selectedDate by viewModel.selectedDate
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val scrollState = rememberScrollState()
    val statusBarHeight = fallStatusBarHeight()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(top = statusBarHeight + 15.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 상단 타이틀
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
                text = "낙상 감지",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 주간 캘린더
        FallWeekCalendar(
            selectedDate = selectedDate,
            onDateSelected = viewModel::updateSelectedDate
        )

        Spacer(modifier = Modifier.height(20.dp))

        FallLineChart(
            rawData = mockData,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            scrollState = scrollState,
            selectedIndex = selectedIndex,
            onPointSelected = { index ->
                viewModel.selectBar(index)
            }
        )

        Spacer(modifier = Modifier.height(30.dp))
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
    onPointSelected: (Int) -> Unit
) {
    val chartHeight = 180.dp
    val pointSpacing = 1.dp
    val density = LocalDensity.current

    // 하루 시간 초기화
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

    val now = LocalTime.now()
    val nowIndex = now.hour * 60 + now.minute
    val visibleData = filledData // 전체시간 표시
    val totalWidth = with(density) { (1440 * pointSpacing.toPx()).toDp() }

    // 화면 진입 시 현재 시간 데이터 자동 선택 & 스크롤
    LaunchedEffect(filledData) {
        if (filledData.isNotEmpty()) {
            val offsetPx = with(density) { (nowIndex * pointSpacing.toPx()).toInt() }
            scrollState.scrollTo(offsetPx)
            onPointSelected(nowIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight + 120.dp)
            .horizontalScroll(scrollState)
            .padding(start = 45.dp, end = 50.dp)
    ) {
        Canvas(
            modifier = Modifier
                .width(totalWidth)
                .height(chartHeight + 120.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val clickedIndex = (offset.x / pointSpacing.toPx()).toInt()
                        if (clickedIndex in visibleData.indices && clickedIndex <= nowIndex) {
                            onPointSelected(clickedIndex)
                        }
                    }
                }
        ) {
            val chartAreaHeight = chartHeight.toPx()
            val unitHeight = chartAreaHeight / maxY
            val widthPerPoint = pointSpacing.toPx()

            // Y축 가이드라인
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

            // X축 레이블(2시간 단위)
            for (slot in 0..1440 step 120) {
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

            // 현재 시각까지 라인만 그림
            val points = visibleData.take(nowIndex + 1).mapIndexed { index, point ->
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

            // 현재 시각 데이터에 툴팁 표시
            if (selectedIndex in points.indices) {
                val point = points[selectedIndex]
                val chartPoint = visibleData[selectedIndex]

                drawCircle(
                    color = Color.Red,
                    radius = 10f,
                    center = point
                )

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
                }
                paint.textSize = 26f
                canvas.drawText(chartPoint.timeLabel, point.x, tooltipY + 30f, paint)
                canvas.drawText("${chartPoint.value.toInt()}", point.x, tooltipY + 60f, paint)
            }
        }
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