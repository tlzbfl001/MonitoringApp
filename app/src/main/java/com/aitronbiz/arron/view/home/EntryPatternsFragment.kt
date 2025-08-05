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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.aitronbiz.arron.api.response.HourlyPattern
import com.aitronbiz.arron.api.response.WeeklyPattern
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.viewmodel.EntryPatternsViewModel
import kotlin.math.max
import kotlin.math.min

class EntryPatternsFragment : Fragment() {
    private val viewModel: EntryPatternsViewModel by activityViewModels()
    private var token: String? = null
    private var homeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val safeToken = AppController.prefs.getToken()
        val safeHomeId = arguments?.getString("homeId")

        if (safeToken.isNullOrBlank() || safeHomeId.isNullOrBlank()) {
            replaceFragment1(parentFragmentManager, MainFragment())
        } else {
            homeId = safeHomeId
            token = safeToken
            viewModel.resetState(token!!, homeId!!)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                EntryPatternsScreen(
                    viewModel = viewModel,
                    token = token!!,
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
fun EntryPatternsScreen(
    viewModel: EntryPatternsViewModel,
    token: String,
    homeId: String,
    onBackClick: () -> Unit
) {
    val entryPatterns by viewModel.entryPatterns.collectAsState()
    val statusBarHeight = entryPatternsBarHeight()
    val rooms by viewModel.rooms.collectAsState()
    val selectedRoomId by viewModel.selectedRoomId.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchEntryPatternsData(token, homeId)
    }

    LaunchedEffect(selectedRoomId) {
        if (selectedRoomId.isNotBlank()) {
            viewModel.fetchEntryPatternsData(token, selectedRoomId)
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
                text = "출입 패턴",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 출입 패턴 차트
        if (entryPatterns != null) {
            EntryPatternsCharts(
                hourlyPatterns = entryPatterns!!.hourlyPatterns,
                weeklyPatterns = entryPatterns!!.weeklyPatterns
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "데이터가 없습니다",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // 룸 선택 리스트
        if (rooms.isNotEmpty()) {
            Text(
                text = "룸 선택",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                modifier = Modifier.padding(start = 21.dp)
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
                            val presence = viewModel.roomPresenceMap[room.id]
                            val isPresent = presence?.isPresent == true

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
                                    .clickable { viewModel.selectRoom(room.id, token) }
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
                                            Text("재실", color = Color.White, fontSize = 11.sp)
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
                                            Text("부재중", color = Color.White, fontSize = 11.sp)
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

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun EntryPatternsCharts(
    hourlyPatterns: List<HourlyPattern>,
    weeklyPatterns: List<WeeklyPattern>
) {
    val totalEntry = hourlyPatterns.sumOf { it.entryCount ?: 0 }
    val totalExit = hourlyPatterns.sumOf { it.exitCount ?: 0 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "시간별 출입 패턴",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, bottom = 2.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 시간별 출입 패턴 차트
        HourlyEntryChart(hourlyPatterns)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "요일별 출입 패턴",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, bottom = 2.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 요일별 출입 패턴 차트
        WeeklyEntryChart(weeklyPatterns)

        Spacer(modifier = Modifier.height(12.dp))

        WeeklySummaryCard(totalEntry, totalExit)
    }
}

@Composable
fun HourlyEntryChart(
    patterns: List<HourlyPattern>,
    modifier: Modifier = Modifier,
    steps: Int = 4
) {
    val chartHeight = 160.dp
    val labelHeight = 28.dp
    val barGroupWidth = 20.dp
    val density = LocalDensity.current
    val yAxisWidth = 30.dp

    val maxCount = (patterns.maxOfOrNull { maxOf(it.entryCount ?: 0, it.exitCount ?: 0) } ?: 1)

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight + labelHeight + 5.dp)
            .padding(end = 20.dp)
    ) {
        // Y축 값
        Column(
            modifier = Modifier
                .width(yAxisWidth)
                .height(chartHeight),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            for (i in steps downTo 0) {
                val value = (maxCount * i / steps).toInt()
                Text(
                    text = "$value",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 차트
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            if (patterns.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .width(
                            with(density) {
                                val setSpacing = 7.dp.toPx()
                                val barWidth = barGroupWidth.toPx()
                                val totalWidthPx =
                                    patterns.size * barWidth + (patterns.size - 1) * setSpacing
                                totalWidthPx.toDp()
                            }
                        )
                        .height(chartHeight + labelHeight + 60.dp)
                        .pointerInput(patterns) {
                            detectTapGestures { offset ->
                                val barWidthPx = with(density) { barGroupWidth.toPx() }
                                val setSpacingPx = with(density) { 7.dp.toPx() }
                                val groupWidth = barWidthPx + setSpacingPx
                                val clickedIndex = (offset.x / groupWidth).toInt()
                                if (clickedIndex in patterns.indices) {
                                    selectedIndex =
                                        if (selectedIndex == clickedIndex) null else clickedIndex
                                }
                            }
                        }
                ) {
                    val chartAreaHeight = chartHeight.toPx()
                    val unitHeight = chartAreaHeight / steps
                    val barWidth = barGroupWidth.toPx()
                    val entryExitSpacing = 2.dp.toPx()
                    val setSpacing = 7.dp.toPx()
                    val singleBarWidth = (barWidth - entryExitSpacing) / 2

                    // Y축 라인
                    for (i in 0..steps) {
                        val value = (maxCount * i / steps).toInt()
                        val y = chartAreaHeight - i * unitHeight
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            value.toString(),
                            -30f,
                            y + 10f,
                            Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 30f
                                textAlign = Paint.Align.RIGHT
                                isAntiAlias = true
                            }
                        )
                    }

                    // X축 라인
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(0f, chartAreaHeight),
                        end = Offset(size.width, chartAreaHeight),
                        strokeWidth = 2f
                    )

                    val barPositions = mutableListOf<Pair<Int, Offset>>()
                    patterns.forEachIndexed { index, pattern ->
                        val entryHeight = (pattern.entryCount ?: 0).toFloat() / maxCount
                        val exitHeight = (pattern.exitCount ?: 0).toFloat() / maxCount

                        val entryTop = chartAreaHeight - (chartAreaHeight * entryHeight)
                        val exitTop = chartAreaHeight - (chartAreaHeight * exitHeight)

                        val barX = index * (barWidth + setSpacing)

                        drawRect(
                            color = Color(0xFF2D60FF),
                            topLeft = Offset(barX, entryTop),
                            size = Size(singleBarWidth, chartAreaHeight * entryHeight)
                        )
                        drawRect(
                            color = Color(0xFF84FFB1),
                            topLeft = Offset(barX + singleBarWidth + entryExitSpacing, exitTop),
                            size = Size(singleBarWidth, chartAreaHeight * exitHeight)
                        )

                        // X축 라벨
                        if ((pattern.timeSlot ?: 0) % 2 == 0) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "${pattern.timeSlot}시",
                                barX + barWidth / 2,
                                chartAreaHeight + 40f,
                                Paint().apply {
                                    textAlign = Paint.Align.CENTER
                                    textSize = 26f
                                    color = android.graphics.Color.WHITE
                                    isAntiAlias = true
                                }
                            )
                        }

                        barPositions.add(index to Offset(barX + barWidth / 2, min(entryTop, exitTop)))
                    }

                    // 툴팁
                    selectedIndex?.let { idx ->
                        val (centerX, topY) = barPositions.first { it.first == idx }.second
                        val pattern = patterns[idx]

                        val tooltipWidth = 150f
                        val tooltipHeight = 70f
                        val halfWidth = tooltipWidth / 2f
                        val tooltipX = (centerX - halfWidth).coerceIn(0f, size.width - tooltipWidth)
                        val tooltipY = max(0f, topY - tooltipHeight - 15f)

                        // 툴팁 배경
                        drawRoundRect(
                            color = Color(0xFF0D1B2A),
                            topLeft = Offset(tooltipX, tooltipY),
                            size = Size(tooltipWidth, tooltipHeight),
                            cornerRadius = CornerRadius(16f, 16f)
                        )

                        // 텍스트
                        val canvas = drawContext.canvas.nativeCanvas
                        val paint = Paint().apply {
                            textAlign = Paint.Align.CENTER
                            color = android.graphics.Color.WHITE
                            isAntiAlias = true
                            textSize = 26f
                        }

                        val lineSpacing = 32f
                        val startY = tooltipY + (tooltipHeight - lineSpacing) / 2f

                        canvas.drawText(
                            "입실 ${pattern.entryCount ?: 0}",
                            tooltipX + tooltipWidth / 2,
                            startY,
                            paint
                        )

                        canvas.drawText(
                            "퇴실 ${pattern.exitCount ?: 0}",
                            tooltipX + tooltipWidth / 2,
                            startY + lineSpacing,
                            paint
                        )
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
fun WeeklyEntryChart(patterns: List<WeeklyPattern>) {
    val chartHeight = 160.dp
    val labelHeight = 28.dp
    val yAxisWidth = 35.dp
    val maxCount = (patterns.maxOfOrNull { maxOf(it.entryCount ?: 0, it.exitCount ?: 0) } ?: 1)

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val dayMap = mapOf(
        "Sunday" to "일", "Monday" to "월", "Tuesday" to "화",
        "Wednesday" to "수", "Thursday" to "목", "Friday" to "금", "Saturday" to "토"
    )

    val ySteps = 4
    val yLabels = (0..ySteps).map { (maxCount * it / ySteps.toFloat()).toInt() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight + labelHeight + 5.dp)
            .padding(end = 20.dp),
    ) {
        // Y축 값
        Column(
            modifier = Modifier
                .width(yAxisWidth)
                .height(chartHeight),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yLabels.reversed().forEach { value ->
                Text(
                    text = "$value",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }

        // Y축과 차트 사이 간격
        Spacer(modifier = Modifier.width(8.dp))

        // 차트 영역
        Box(
            modifier = Modifier.weight(1f)
        ) {
            if (patterns.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(patterns) {
                            detectTapGestures { offset ->
                                val clickedIndex =
                                    (offset.x / (size.width / patterns.size)).toInt()
                                if (clickedIndex in patterns.indices) {
                                    selectedIndex =
                                        if (selectedIndex == clickedIndex) null else clickedIndex
                                }
                            }
                        }
                ) {
                    val chartAreaHeight = chartHeight.toPx()
                    val unitHeight = chartAreaHeight / ySteps

                    val entryExitSpacing = 3.dp.toPx()
                    val setSpacing = 7.dp.toPx()

                    val usableWidth = (size.width - (patterns.size - 1) * setSpacing)
                    val barGroupWidth = usableWidth / patterns.size
                    val singleBarWidth = (barGroupWidth - entryExitSpacing) / 2

                    // Y축 라인
                    yLabels.forEachIndexed { i, _ ->
                        val y = chartAreaHeight - i * unitHeight
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }

                    // X축 라인
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(0f, chartAreaHeight),
                        end = Offset(size.width, chartAreaHeight),
                        strokeWidth = 2f
                    )

                    val barPositions = mutableListOf<Pair<Int, Offset>>()

                    patterns.forEachIndexed { index, pattern ->
                        val entryHeight = (pattern.entryCount ?: 0).toFloat() / maxCount
                        val exitHeight = (pattern.exitCount ?: 0).toFloat() / maxCount

                        val entryTop = chartAreaHeight - (chartAreaHeight * entryHeight)
                        val exitTop = chartAreaHeight - (chartAreaHeight * exitHeight)

                        val barX = index * (barGroupWidth + setSpacing)

                        drawRect(
                            color = Color(0xFFFFEB91),
                            topLeft = Offset(barX, entryTop),
                            size = Size(singleBarWidth, chartAreaHeight * entryHeight)
                        )
                        drawRect(
                            color = Color(0xFFFF3333),
                            topLeft = Offset(barX + singleBarWidth + entryExitSpacing, exitTop),
                            size = Size(singleBarWidth, chartAreaHeight * exitHeight)
                        )

                        // X축 라벨
                        val label = dayMap[pattern.metadata?.dayName] ?: ""
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            barX + barGroupWidth / 2,
                            chartAreaHeight + 40f,
                            Paint().apply {
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 26f
                                color = android.graphics.Color.WHITE
                                isAntiAlias = true
                            }
                        )

                        barPositions.add(index to Offset(barX + barGroupWidth / 2, min(entryTop, exitTop)))
                    }

                    // 툴팁
                    selectedIndex?.let { idx ->
                        val (centerX, topY) = barPositions.first { it.first == idx }.second
                        val pattern = patterns[idx]

                        val tooltipWidth = 150f
                        val tooltipHeight = 70f
                        val tooltipX = centerX - tooltipWidth / 2
                        val tooltipY = max(0f, topY - tooltipHeight - 15f)

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
                            textSize = 24f
                        }

                        val lineSpacing = 30f
                        val textBlockHeight = lineSpacing
                        val startY = tooltipY + (tooltipHeight - textBlockHeight) / 2f

                        canvas.drawText(
                            "입실 ${pattern.entryCount ?: 0}",
                            centerX,
                            startY,
                            paint
                        )
                        canvas.drawText(
                            "퇴실 ${pattern.exitCount ?: 0}",
                            centerX,
                            startY + lineSpacing,
                            paint
                        )
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
fun WeeklySummaryCard(totalEntry: Int, totalExit: Int, isSelected: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                    width = 2.dp,
                    color = if (isSelected) Color.White else Color(0xFF1A4B7C),
                    shape = RoundedCornerShape(16.dp)
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("총 입실 횟수", color = Color.White, fontSize = 15.sp)
                    Text("${totalEntry}회", color = Color.White, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("총 퇴실 횟수", color = Color.White, fontSize = 15.sp)
                    Text("${totalExit}회", color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun entryPatternsBarHeight(): Dp {
    val context = LocalContext.current
    val resourceId = remember {
        context.resources.getIdentifier("status_bar_height", "dimen", "android")
    }
    val heightPx = remember {
        if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }
    return with(LocalDensity.current) { heightPx.toDp() }
}