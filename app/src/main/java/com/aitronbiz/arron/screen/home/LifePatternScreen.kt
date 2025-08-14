package com.aitronbiz.arron.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.LifePatterns
import com.aitronbiz.arron.viewmodel.LifePatternsViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

@Composable
fun LifePatternScreen(
    homeId: String,
    roomId: String,
    viewModel: LifePatternsViewModel = viewModel(),
    navController: NavController
) {
    val token = AppController.prefs.getToken().orEmpty()

    val selectedDate by viewModel.selectedDate
    val lifePatterns by viewModel.lifePatterns.collectAsState()

    var showMonthlyCalendar by remember { mutableStateOf(false) }

    // 최초 진입
    LaunchedEffect(Unit) {
        viewModel.resetState(token, homeId)
        viewModel.fetchLifePatternsData(token, homeId, selectedDate)
    }

    // 날짜 변경 시
    LaunchedEffect(selectedDate) {
        viewModel.fetchLifePatternsData(token, homeId, selectedDate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(top = 15.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 상단 타이틀바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.arrow_back),
                contentDescription = "뒤로가기",
                tint = Color.White,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.CenterStart)
                    .clickable {
                        val popped = navController.popBackStack()
                        if (!popped) navController.navigateUp()
                    }
            )
            Text(
                text = "생활 패턴",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 날짜 헤더 (탭하면 월간 달력 오픈)
        LifePatternsWeeklyCalendarHeader(
            selectedDate = selectedDate,
            onClick = { showMonthlyCalendar = true }
        )

        // 주간 달력 (요일 + 일자)
        LifePatternsWeeklyCalendarPager(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                // 미래 날짜 선택 방지
                if (!date.isAfter(LocalDate.now())) {
                    viewModel.updateSelectedDate(date)
                }
            }
        )

        // 월간 달력 (바텀시트)
        if (showMonthlyCalendar) {
            LifePatternsMonthlyCalendarDialog(
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    if (!date.isAfter(LocalDate.now())) {
                        viewModel.updateSelectedDate(date)
                    }
                },
                onDismiss = { showMonthlyCalendar = false }
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // 생활 패턴 요약 카드
        if (lifePatterns != null) {
            LifePatternsSummaryCard(lifePatterns!!)
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

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun LifePatternsWeeklyCalendarHeader(
    selectedDate: LocalDate,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 25.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${selectedDate.monthValue}.${selectedDate.dayOfMonth} " +
                    selectedDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
            color = Color.White,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.width(7.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_caret_down),
            contentDescription = "날짜 선택",
            modifier = Modifier.size(8.dp),
            tint = Color.White
        )
    }
}

@Composable
fun LifePatternsWeeklyCalendarPager(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 1000) { 1001 }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val baseSunday = remember { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) }
    val days = listOf("일", "월", "화", "수", "목", "금", "토")

    // 선택 날짜가 바뀌면 해당 주 페이지로 스크롤
    LaunchedEffect(selectedDate) {
        val targetSunday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val offset = ChronoUnit.WEEKS.between(baseSunday, targetSunday)
        val targetPage = (1000 + offset.toInt()).coerceAtMost(1000)
        if (pagerState.currentPage != targetPage) {
            scope.launch { pagerState.scrollToPage(targetPage) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        // 요일 라벨
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEachIndexed { index, day ->
                val isSelected = (selectedDate.dayOfWeek.value % 7) == index
                val circleSize = 25.dp
                Box(
                    modifier = Modifier
                        .size(circleSize)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF174176) else Color.LightGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 주간 날짜 페이저
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val startOfWeek = baseSunday.plusWeeks((page - 1000).toLong())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (0..6).forEach { offset ->
                    val date = startOfWeek.plusDays(offset.toLong())
                    val disabled = date.isAfter(today)
                    Box(
                        modifier = Modifier
                            .size(23.dp)
                            .clip(CircleShape)
                            .alpha(if (disabled) 0.4f else 1f)
                            .clickable(enabled = !disabled) { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifePatternsMonthlyCalendarDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    val pagerState = rememberPagerState(initialPage = 1000) { 1001 }
    var currentMonth by remember { mutableStateOf(today.withDayOfMonth(1)) }

    LaunchedEffect(selectedDate) {
        val offset = ChronoUnit.MONTHS.between(
            today.withDayOfMonth(1),
            selectedDate.withDayOfMonth(1)
        )
        scope.launch { pagerState.scrollToPage((1000 + offset.toInt()).coerceAtMost(1000)) }
    }

    LaunchedEffect(pagerState.currentPage) {
        val monthOffset = pagerState.currentPage - 1000
        currentMonth = today.plusMonths(monthOffset.toLong()).withDayOfMonth(1)
    }

    fun rowsInMonth(firstDay: LocalDate): Int {
        val daysInMonth = firstDay.lengthOfMonth()
        val firstDow = (firstDay.dayOfWeek.value % 7)
        val totalCells = ((daysInMonth + firstDow + 6) / 7) * 7
        return totalCells / 7
    }

    val cellSize: Dp = 40.dp

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(top = 25.dp, bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(2f)
                ) {
                    androidx.compose.material.Icon(
                        painter = painterResource(id = R.drawable.ic_left),
                        contentDescription = "이전달",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        (pagerState.currentPage - 1).coerceAtLeast(0)
                                    )
                                }
                            }
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    androidx.compose.material.Text(
                        text = "${currentMonth.year}.${currentMonth.monthValue}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    androidx.compose.material.Icon(
                        painter = painterResource(id = R.drawable.ic_right),
                        contentDescription = "다음달",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(enabled = pagerState.currentPage < 1000) {
                                if (pagerState.currentPage < 1000) {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                }
                            }
                    )
                }

                androidx.compose.material.Text(
                    text = "오늘",
                    color = Color.Black,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.End)
                        .background(Color(0xFFECECEC), shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable {
                            onDateSelected(today)
                            scope.launch { pagerState.scrollToPage(1000) }
                            onDismiss()
                        }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 요일 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                    androidx.compose.material.Text(
                        text = day,
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val basePage = pagerState.currentPage
            val offsetFraction = pagerState.currentPageOffsetFraction
            val baseMonthFirst = today.plusMonths((basePage - 1000).toLong()).withDayOfMonth(1)
            val neighborPage = if (offsetFraction >= 0f) basePage + 1 else basePage - 1
            val neighborMonthFirst = today.plusMonths((neighborPage - 1000).toLong()).withDayOfMonth(1)

            val baseRows = rowsInMonth(baseMonthFirst)
            val neighborRows = rowsInMonth(neighborMonthFirst)
            val baseHeight = cellSize * baseRows
            val neighborHeight = cellSize * neighborRows
            val dynamicHeight = lerp(baseHeight, neighborHeight, abs(offsetFraction))

            // 월간 달력
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dynamicHeight)
            ) { page ->
                val monthOffset = page - 1000
                val firstOfMonth = today.plusMonths(monthOffset.toLong()).withDayOfMonth(1)
                val daysInMonth = firstOfMonth.lengthOfMonth()
                val firstDow = (firstOfMonth.dayOfWeek.value % 7)
                val totalCells = ((daysInMonth + firstDow + 6) / 7) * 7

                val dates = (0 until totalCells).map { index ->
                    val day = index - firstDow + 1
                    if (day in 1..daysInMonth) firstOfMonth.withDayOfMonth(day) else null
                }

                Column {
                    dates.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            week.forEach { date ->
                                if (date != null) {
                                    val isSelected = date == selectedDate
                                    val isToday = date == today
                                    val disabled = date.isAfter(today)
                                    val sizeToUse: Dp = if (isSelected || isToday) (40.dp - 8.dp) else 40.dp

                                    Box(
                                        modifier = Modifier
                                            .size(sizeToUse)
                                            .clip(CircleShape)
                                            .alpha(if (disabled) 0.4f else 1f)
                                            .background(
                                                when {
                                                    isSelected -> Color.Black
                                                    isToday -> Color(0xFFE0E0E0)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable(enabled = !disabled) {
                                                onDateSelected(date)
                                                onDismiss()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.material.Text(
                                            text = date.dayOfMonth.toString(),
                                            color = when {
                                                isSelected -> Color.White
                                                isToday -> Color.Black
                                                else -> Color.Gray
                                            }
                                        )
                                    }
                                } else {
                                    Box(modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LifePatternsSummaryCard(data: LifePatterns) {
    val zoneId = ZoneId.of("Asia/Seoul")

    // 시간/분 변환 함수
    fun formatMinutes(totalMinutes: Int?): String {
        val minutes = totalMinutes ?: 0
        val hours = minutes / 60
        val remainMinutes = minutes % 60
        return when {
            hours == 0 && remainMinutes == 0 -> "0분"
            hours > 0 && remainMinutes == 0 -> "${hours}시간"
            hours > 0 -> "${hours}시간 ${remainMinutes}분"
            else -> "${remainMinutes}분"
        }
    }

    // UTC → 한국시간 변환 함수
    fun formatUtcTime(utcString: String?): String {
        return try {
            if (utcString.isNullOrBlank()) "정보 없음"
            else {
                val instant = Instant.parse(utcString)
                val localDateTime = instant.atZone(zoneId).toLocalDateTime()
                val h = localDateTime.hour
                val m = localDateTime.minute
                when {
                    h == 0 && m == 0 -> "0분"
                    m == 0 -> "${h}시간"
                    else -> "${h}시간 ${m}분"
                }
            }
        } catch (e: Exception) {
            "정보 없음"
        }
    }

    // 패턴 유형 매핑
    val patternType = when (data.activityPatternType.lowercase()) {
        "regular" -> "규칙적"
        "irregular" -> "불규칙적"
        "night_owl" -> "야간형"
        "early_bird" -> "주간형"
        "inactive" -> "저활동적"
        else -> "알 수 없음"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C3C66))
            .padding(16.dp)
    ) {
        androidx.compose.material.Text(
            text = "일일 활동 요약",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 총 활동 시간 / 총 비활동 시간
        Row(modifier = Modifier.fillMaxWidth()) {
            Column {
                androidx.compose.material.Text("총 활동 시간", color = Color.White.copy(0.7f), fontSize = 12.sp)
                androidx.compose.material.Text(formatMinutes(data.totalActiveMinutes), color = Color.White, fontSize = 16.sp)
            }
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.material.Text("총 비활동 시간", color = Color.White.copy(0.7f), fontSize = 12.sp)
                androidx.compose.material.Text(formatMinutes(data.totalInactiveMinutes), color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 평균 점수 / 최고 점수
        Row(modifier = Modifier.fillMaxWidth()) {
            Column {
                androidx.compose.material.Text("평균 점수", color = Color.White.copy(0.7f), fontSize = 12.sp)
                androidx.compose.material.Text("${data.averageActivityScore.toInt()}점", color = Color.White, fontSize = 16.sp)
            }
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.material.Text("최고 점수", color = Color.White.copy(0.7f), fontSize = 12.sp)
                androidx.compose.material.Text("${data.maxActivityScore}점", color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 첫 활동 / 마지막 활동
        Row(modifier = Modifier.fillMaxWidth()) {
            Column {
                androidx.compose.material.Text("첫 활동", color = Color.White.copy(0.7f), fontSize = 12.sp)
                androidx.compose.material.Text(formatUtcTime(data.firstActivityTime), color = Color.White, fontSize = 16.sp)
            }
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.material.Text("마지막 활동", color = Color.White.copy(0.7f), fontSize = 12.sp)
                androidx.compose.material.Text(formatUtcTime(data.lastActivityTime), color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 수면 / 수면 시간
        Row(modifier = Modifier.fillMaxWidth()) {
            Column {
                androidx.compose.material.Text("수면", color = Color.White.copy(0.7f), fontSize = 12.sp)
                androidx.compose.material.Text(formatMinutes(data.estimatedSleepMinutes), color = Color.White, fontSize = 16.sp)
            }
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.material.Text("수면 시간", color = Color.White.copy(0.7f), fontSize = 12.sp)
                val sleepStart = formatUtcTime(data.estimatedSleepStart)
                val sleepEnd = formatUtcTime(data.estimatedSleepEnd)
                if (sleepStart == "정보 없음" || sleepEnd == "정보 없음" || (sleepStart == "0분" && sleepEnd == "0분")) {
                    androidx.compose.material.Text("0분", color = Color.White, fontSize = 16.sp)
                } else {
                    androidx.compose.material.Text("$sleepStart ~ $sleepEnd", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 가장 활동적인 시간 / 가장 비활동적인 시간
        Row(modifier = Modifier.fillMaxWidth()) {
            Column {
                androidx.compose.material.Text("가장 활동적인 시간", color = Color.White.copy(0.7f), fontSize = 12.sp)
                androidx.compose.material.Text("${data.mostActiveHour}시", color = Color.White, fontSize = 16.sp)
            }
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.material.Text("가장 비활동적인 시간", color = Color.White.copy(0.7f), fontSize = 12.sp)
                androidx.compose.material.Text("${data.leastActiveHour}시", color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 패턴 유형 / 규칙성 점수
        Row(modifier = Modifier.fillMaxWidth()) {
            Column {
                androidx.compose.material.Text("패턴 유형", color = Color.White.copy(0.7f), fontSize = 12.sp)
                androidx.compose.material.Text(patternType, color = Color.White, fontSize = 16.sp)
            }
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.material.Text("규칙성 점수", color = Color.White.copy(0.7f), fontSize = 12.sp)
                androidx.compose.material.Text("${data.activityRegularityScore.toInt()}점", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}