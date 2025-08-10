package com.aitronbiz.arron.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.aitronbiz.arron.api.response.LifePatterns
import com.aitronbiz.arron.viewmodel.LifePatternsViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@Composable
fun LifePatternScreen(
    homeId: String,
    viewModel: LifePatternsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    navController: NavController
) {
    val token = AppController.prefs.getToken().toString()

    val selectedDate by viewModel.selectedDate
    val lifePatterns by viewModel.lifePatterns.collectAsState()
    val statusBarHeight = lifePatternsBarHeight()

    // 최초 진입 시
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
            .padding(top = statusBarHeight + 15.dp)
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

        Spacer(modifier = Modifier.height(20.dp))

        // 주간 달력
        LifePatternsWeekCalendar(
            selectedDate = selectedDate,
            onDateSelected = viewModel::updateSelectedDate
        )

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
fun LifePatternsWeekCalendar(
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
                        androidx.compose.material.Text(
                            text = label,
                            color = Color.White,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        androidx.compose.material.Text(
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

@Composable
fun lifePatternsBarHeight(): Dp {
    val context = LocalContext.current
    val resourceId = remember {
        context.resources.getIdentifier("status_bar_height", "dimen", "android")
    }
    val heightPx = remember {
        if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }
    return with(LocalDensity.current) { heightPx.toDp() }
}