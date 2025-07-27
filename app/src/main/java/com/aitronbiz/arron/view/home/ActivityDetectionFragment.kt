package com.aitronbiz.arron.view.home

import android.graphics.Paint
import android.os.Bundle
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.nativeCanvas
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
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.viewmodel.ActivityViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class ActivityDetectionFragment : Fragment() {
    private val viewModel: ActivityViewModel by activityViewModels()
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
                ActivityBarChartScreen(
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
fun ActivityBarChartScreen(
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

    val barWidth = 25.dp
    val barSpacing = 12.dp
    val chartHeight = 200.dp
    val maxY = data.maxOfOrNull { it.value } ?: 0f

    // 초기에 실행되어 홈에 연결된 방 목록을 가져옴
    LaunchedEffect(Unit) {
        viewModel.fetchRooms(token, homeId)
    }

    // 선택된 방 또는 날짜가 바뀔 때마다 해당 방의 활동 데이터를 새로 불러옴
    LaunchedEffect(selectedRoomId, selectedDate) {
        if (selectedRoomId.isNotBlank()) {
            viewModel.fetchActivityData(token, selectedRoomId, selectedDate)
        }
    }

    // 활동 데이터가 존재할 때, 가장 마지막 데이터를 선택하고 차트 스크롤 위치를 마지막 지점으로 이동
    LaunchedEffect(data) {
        if (data.isNotEmpty()) {
            viewModel.selectBar(data.lastIndex)
            val offsetPx = with(density) {
                (barWidth + barSpacing).roundToPx() * (data.size - 8)
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

        WeekCalendar(
            selectedDate = selectedDate,
            onDateSelected = viewModel::updateSelectedDate
        )

        Spacer(modifier = Modifier.height(25.dp))

        // 활동량 차트
        if (data.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .height(chartHeight + 80.dp)
                    .padding(start = 20.dp, end = 30.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .width((barWidth + barSpacing) * data.size)
                        .height(chartHeight + 80.dp)
                ) {
                    val barPx = barWidth.toPx()
                    val spacePx = barSpacing.toPx()
                    val chartAreaHeight = chartHeight.toPx()
                    val unitHeight = chartAreaHeight / maxY

                    // Y축 눈금
                    for (i in 0..5) {
                        val y = chartAreaHeight - (i * (maxY / 5)) * unitHeight
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            ((i * (maxY / 5)).toInt()).toString(),
                            0f,
                            y,
                            Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 26f
                                textAlign = Paint.Align.LEFT
                                isAntiAlias = true
                            }
                        )
                    }

                    // 막대
                    data.forEachIndexed { i, point ->
                        val x = i * (barPx + spacePx) + 60f
                        val barHeight = point.value * unitHeight
                        val y = chartAreaHeight - barHeight
                        val isSelected = i == selectedIndex

                        drawRoundRect(
                            brush = if (isSelected) SolidColor(Color(0xFFFF3B30)) else Brush.verticalGradient(
                                listOf(Color(0xFF64B5F6), Color(0xFFB3E5FC)),
                                startY = 0f,
                                endY = chartAreaHeight
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barPx, barHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            point.timeLabel,
                            x + barPx / 2,
                            chartAreaHeight + 30f,
                            Paint().apply {
                                textAlign = Paint.Align.CENTER
                                textSize = 24f
                                color = android.graphics.Color.WHITE
                                isAntiAlias = true
                            }
                        )

                        if (isSelected) {
                            val tooltip = point.value.toInt().toString()
                            val tooltipWidth = 100f
                            val tooltipHeight = 50f
                            val tooltipX = x + barPx / 2 - tooltipWidth / 2
                            val tooltipY = y - tooltipHeight - 10f

                            drawRoundRect(
                                color = Color(0xFF0D1B2A),
                                topLeft = Offset(tooltipX, tooltipY),
                                size = Size(tooltipWidth, tooltipHeight),
                                cornerRadius = CornerRadius(20f, 20f)
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                tooltip,
                                x + barPx / 2,
                                tooltipY + tooltipHeight / 1.7f,
                                Paint().apply {
                                    textAlign = Paint.Align.CENTER
                                    textSize = 28f
                                    color = android.graphics.Color.WHITE
                                    isAntiAlias = true
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .width((barWidth + barSpacing) * data.size)
                        .height(chartHeight + 80.dp)
                ) {
                    data.forEachIndexed { i, _ ->
                        Box(
                            modifier = Modifier
                                .width(barWidth + barSpacing)
                                .fillMaxHeight()
                                .clickable { viewModel.selectBar(i) }
                        )
                    }
                }
            }
        }else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "데이터가 없습니다",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }
        }

        // 룸 선택
        if(rooms.isNotEmpty()) {
            Text(
                text = "룸 선택",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                modifier = Modifier.padding(start = 20.dp)
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
                            .padding(top = 5.dp, start = 15.dp, end = 15.dp)
                    ) {
                        row.forEach { room ->
                            val isSelected = room.id == selectedRoomId
                            val roomActivity = viewModel.roomActivityMap[room.id]
                            val presence = viewModel.roomPresenceMap[room.id]
                            val isPresent = presence?.isPresent == true
                            val showWarning = isPresent && selectedDate == LocalDate.now() && roomActivity != null &&
                                    (roomActivity <= 10 || roomActivity >= 80)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(6.dp)
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