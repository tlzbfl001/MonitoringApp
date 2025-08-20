package com.aitronbiz.arron.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.LifePatterns
import com.aitronbiz.arron.viewmodel.LifePatternsViewModel
import com.aitronbiz.arron.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
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
    selectedDate: LocalDate,
    viewModel: LifePatternsViewModel = viewModel(),
    navController: NavController,
    mainViewModel: MainViewModel = viewModel()
) {
    val token = AppController.prefs.getToken().orEmpty()

    val selectedDate by viewModel.selectedDate
    val lifePatterns by viewModel.lifePatterns.collectAsState()

    var showMonthlyCalendar by remember { mutableStateOf(false) }
    var hasUnreadNotification by remember { mutableStateOf(false) }

    // 최초 진입 시 설정/데이터
    LaunchedEffect(Unit) {
        viewModel.resetState(token, homeId)
        viewModel.fetchLifePatternsData(token, homeId, selectedDate)
        mainViewModel.checkNotifications { hasUnreadNotification = it }
    }

    // 날짜 변경 시 데이터
    LaunchedEffect(selectedDate) {
        viewModel.fetchLifePatternsData(token, homeId, selectedDate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 5.dp, end = 20.dp, top = 2.dp, bottom = 6.dp)
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
                text = "생활 패턴",
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // 날짜 헤더 (탭 → 월간 달력)
            LifePatternsWeeklyCalendarHeader(
                selectedDate = selectedDate,
                onClick = { showMonthlyCalendar = true }
            )

            // 주간 달력 (요일 + 일자) — Fall과 동일 레이아웃/간격
            LifePatternsWeeklyCalendarPager(
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    if (!date.isAfter(LocalDate.now())) viewModel.updateSelectedDate(date)
                }
            )

            // 월간 달력 바텀시트
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

            Spacer(modifier = Modifier.height(20.dp))

            // 요약 카드
            if (lifePatterns != null) {
                LifePatternsSummary(data = lifePatterns!!)
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
}

// ---------------------- Calendar (Fall과 동일 톤/크기) ----------------------

@Composable
fun LifePatternsWeeklyCalendarHeader(
    selectedDate: LocalDate,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 22.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${selectedDate.monthValue}.${selectedDate.dayOfMonth} " +
                    selectedDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
            color = Color.White,
            fontSize = 15.sp
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
            .padding(bottom = 10.dp)
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

        Spacer(modifier = Modifier.height(10.dp))

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
                            .size(23.dp) // Fall과 동일 크기
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
            // 상단 컨트롤 (이전/현재월/다음 + 오늘)
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
                    Icon(
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
                    Text(
                        text = "${currentMonth.year}.${currentMonth.monthValue}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Icon(
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

                Text(
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
                    Text(
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

            // 월간 달력 페이지
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
                                        Text(
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
fun LifePatternsSummary(
    data: LifePatterns
) {
    val strong = Color.White
    val soft = Color.White.copy(alpha = 0.75f)

    // ── 상단 버튼 배경: 더 투명 (alpha 0.25)
    val bgActive   = Color(0x26FF6B6B)   // 총 활동 시간
    val bgInactive = Color(0x264A90E2)   // 총 비활동 시간
    val bgAvg      = Color(0x26FFD166)   // 평균 점수
    val bgMax      = Color(0x2506D6A0)   // 최고 점수

    // 테두리색: 원래 컨셉 유지
    val brActive   = Color(0x8FFF6B6B)
    val brInactive = Color(0x8F4A90E2)
    val brAvg      = Color(0x8FFFD166)
    val brMax      = Color(0x8F06D6A0)

    // 값 컬러
    val vActive   = Color(0xCFFF6B6B)
    val vInactive = Color(0xCF4A90E2)
    val vAvg      = Color(0xCEFFD166)
    val vMax      = Color(0xCB06D6A0)

    val titleSize = 13.sp
    val labelBig  = 13.sp
    val valueBig  = 18.sp
    val pairValue = 16.sp

    val zoneId = ZoneId.of("Asia/Seoul")
    fun formatMinutes(mins: Int?): String {
        val m = mins ?: 0
        val h = m / 60
        val r = m % 60
        return when {
            h == 0 && r == 0 -> "0분"
            h > 0 && r == 0  -> "${h}시간"
            h > 0            -> "${h}시간 ${r}분"
            else             -> "${r}분"
        }
    }
    fun hhmm(utc: String?): String = try {
        if (utc.isNullOrBlank()) "정보 없음" else {
            val t = java.time.Instant.parse(utc).atZone(zoneId).toLocalTime()
            if (t.minute == 0) "${t.hour}시" else "${t.hour}시 ${t.minute}분"
        }
    } catch (_: Exception) { "정보 없음" }
    fun patternKo(s: String?): String = when (s?.lowercase()) {
        "regular" -> "규칙적"
        "irregular" -> "불규칙적"
        "night_owl" -> "야간형"
        "early_bird" -> "주간형"
        "inactive" -> "저활동적"
        else -> "알 수 없음"
    }

    Text(
        text = "생활 패턴 세부사항",
        color = strong,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
    )

    val tileH = 92.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BorderedStatButton(
            bg = bgActive,
            borderColor = brActive,
            title = "총 활동 시간",
            value = formatMinutes(data.totalActiveMinutes),
            titleColor = strong, valueColor = vActive,
            titleSize = labelBig, valueSize = valueBig,
            modifier = Modifier.weight(1f).height(tileH)
        )
        BorderedStatButton(
            bg = bgInactive,
            borderColor = brInactive,
            title = "총 비활동 시간",
            value = formatMinutes(data.totalInactiveMinutes),
            titleColor = strong, valueColor = vInactive,
            titleSize = labelBig, valueSize = valueBig,
            modifier = Modifier.weight(1f).height(tileH)
        )
    }

    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BorderedStatButton(
            bg = bgAvg,
            borderColor = brAvg,
            title = "평균 점수",
            value = "${data.averageActivityScore.toInt()}점",
            titleColor = strong, valueColor = vAvg,
            titleSize = labelBig, valueSize = valueBig,
            modifier = Modifier.weight(1f).height(tileH)
        )
        BorderedStatButton(
            bg = bgMax,
            borderColor = brMax,
            title = "최고 점수",
            value = "${data.maxActivityScore}점",
            titleColor = strong, valueColor = vMax,
            titleSize = labelBig, valueSize = valueBig,
            modifier = Modifier.weight(1f).height(tileH)
        )
    }

    Spacer(Modifier.height(20.dp))

    // 하단 세부
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x2D7E7E7E))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PairRowClean("첫 활동", hhmm(data.firstActivityTime),
            "마지막 활동", hhmm(data.lastActivityTime),
            labelBig, pairValue, soft, strong)
        PairRowClean("수면", formatMinutes(data.estimatedSleepMinutes),
            "수면 시간",
            run {
                val s = hhmm(data.estimatedSleepStart)
                val e = hhmm(data.estimatedSleepEnd)
                if (s == "정보 없음" || e == "정보 없음") "정보 없음" else "$s ~ $e"
            },
            labelBig, pairValue, soft, strong)
        PairRowClean("활동적 시간", "${data.mostActiveHour}시",
            "비활동적 시간", "${data.leastActiveHour}시",
            labelBig, pairValue, soft, strong)
        PairRowClean("패턴 유형", patternKo(data.activityPatternType),
            "규칙성 점수", "${data.activityRegularityScore.toInt()}점",
            labelBig, pairValue, soft, strong)
    }
}

@Composable
private fun BorderedStatButton(
    bg: Color,
    borderColor: Color,
    title: String,
    value: String,
    titleColor: Color,
    valueColor: Color,
    titleSize: TextUnit,
    valueSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = titleColor, fontSize = titleSize, fontWeight = FontWeight.Normal)
            Text(value, color = valueColor, fontSize = valueSize, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun PairRowClean(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
    labelSize: TextUnit,
    valueSize: TextUnit,
    labelColor: Color,
    valueColor: Color
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(leftLabel, color = labelColor, fontSize = labelSize)
            Spacer(Modifier.height(2.dp))
            Text(leftValue, color = valueColor, fontSize = valueSize, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(rightLabel, color = labelColor, fontSize = labelSize)
            Spacer(Modifier.height(2.dp))
            Text(rightValue, color = valueColor, fontSize = valueSize,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        }
    }
}