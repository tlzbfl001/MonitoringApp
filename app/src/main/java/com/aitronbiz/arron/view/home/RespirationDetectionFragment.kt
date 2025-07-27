package com.aitronbiz.arron.view.home

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.aitronbiz.arron.viewmodel.RespirationViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class RespirationDetectionFragment : Fragment() {
    private val viewModel: RespirationViewModel by activityViewModels()
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
                RespirationBarChartScreen(
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
fun RespirationBarChartScreen(
    viewModel: RespirationViewModel,
    token: String,
    homeId: String,
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
    val statusBarHeight = respirationStatusBarHeight()
    val density = LocalDensity.current

    // 차트 관련 UI 구성
    val barWidth = 9.dp
    val barSpacing = 12.dp
    val chartHeight = 200.dp
    val maxY = data.maxOfOrNull { it.value } ?: 0f

    // 화면 진입 시 방 목록 불러오기
    LaunchedEffect(Unit) {
        viewModel.fetchRooms(token, homeId)
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    // 방/날짜 선택 시 데이터 불러오기
    LaunchedEffect(selectedRoomId, selectedDate) {
        if (selectedRoomId.isNotBlank()) {
            viewModel.fetchRespirationData(token, selectedRoomId, selectedDate)
        }
    }

    // rooms가 변경되면 presence 전체 갱신
    LaunchedEffect(rooms) {
        if (rooms.isNotEmpty()) {
            viewModel.fetchAllPresence(token)
        }
    }

    // 데이터 변경 시 마지막 막대 선택 + 스크롤 이동
    LaunchedEffect(data) {
        if (data.isNotEmpty()) {
            viewModel.selectBar(data.lastIndex)
            val offsetPx = with(density) {
                (barWidth + barSpacing).roundToPx() * (data.size - 8)
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

        // 호흡 감지 차트
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

                        if(isSelected) {
                            val tooltipWidth = 120f
                            val tooltipHeight = 65f
                            val tooltipX = x + barPx / 2 - tooltipWidth / 2
                            val tooltipY = y - tooltipHeight - 10f

                            drawRoundRect(
                                color = Color(0xFF0D1B2A),
                                topLeft = Offset(tooltipX, tooltipY),
                                size = Size(tooltipWidth, tooltipHeight),
                                cornerRadius = CornerRadius(20f, 20f)
                            )

                            val canvas = drawContext.canvas.nativeCanvas
                            val paint = Paint().apply {
                                textAlign = Paint.Align.CENTER
                                color = android.graphics.Color.WHITE
                                isAntiAlias = true
                            }

                            paint.textSize = 26f
                            val timeY = tooltipY + tooltipHeight / 2.2f - 6f
                            canvas.drawText(
                                point.timeLabel,
                                x + barPx / 2,
                                timeY,
                                paint
                            )

                            paint.textSize = 28f
                            val valueY = timeY + 32f
                            canvas.drawText(
                                point.value.toInt().toString() + "회",
                                x + barPx / 2,
                                valueY,
                                paint
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

        // 룸 선택 리스트
        if (rooms.isNotEmpty()) {
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
                            val status = viewModel.roomRespirationMap[room.id]
                            val presence = viewModel.roomPresenceMap[room.id]
                            val isPresent = presence?.isPresent == true
                            val showWarning = selectedDate == LocalDate.now() && status != null &&
                                    (status <= 12 || status >= 24)

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
fun respirationStatusBarHeight(): Dp {
    val context = LocalContext.current
    val resourceId = remember {
        context.resources.getIdentifier("status_bar_height", "dimen", "android")
    }
    val heightPx = remember {
        if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }
    return with(LocalDensity.current) { heightPx.toDp() }
}