package com.aitronbiz.arron.view.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.graphics.Color
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
import com.aitronbiz.arron.api.response.LifePatterns
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.viewmodel.EntryPatternsViewModel
import com.aitronbiz.arron.viewmodel.LifePatternsViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class EntryPatternsFragment : Fragment() {
    private val viewModel: EntryPatternsViewModel by activityViewModels()
    private var token: String = AppController.prefs.getToken().toString()
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
            viewModel.resetState(token, homeId!!)
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
fun EntryPatternsScreen(
    viewModel: EntryPatternsViewModel,
    token: String,
    homeId: String,
    onBackClick: () -> Unit
) {
    val selectedDate by viewModel.selectedDate
    val entryPatterns by viewModel.entryPatterns.collectAsState()
    val statusBarHeight = entryPatternsBarHeight()

    LaunchedEffect(Unit) {
        viewModel.fetchEntryPatternsData(token, homeId, viewModel.selectedDate.value)
    }

    // 선택된 날짜 변경 시 데이터 새로 불러오기
    LaunchedEffect(selectedDate) {
        viewModel.fetchEntryPatternsData(token, homeId, selectedDate)
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

        Spacer(modifier = Modifier.height(20.dp))

        // 주간 달력
        EntryPatternsWeekCalendar(
            selectedDate = selectedDate,
            onDateSelected = viewModel::updateSelectedDate
        )

        Spacer(modifier = Modifier.height(30.dp))

        // 출입 패턴 요약 카드
        if (entryPatterns != null) {
            EntryPatternsSummaryCard(entryPatterns!!)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryPatternsWeekCalendar(
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
fun EntryPatternsSummaryCard(data: LifePatterns) {
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
        Text(
            text = "일일 활동 요약",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("총 활동 시간", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Text(formatMinutes(data.totalActiveMinutes), color = Color.White, fontSize = 16.sp)
            }
            Column {
                Text("총 비활동 시간", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Text(formatMinutes(data.totalInactiveMinutes), color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("평균 점수", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Text("${data.averageActivityScore.toInt()}점", color = Color.White, fontSize = 16.sp)
            }
            Column {
                Text("최고 점수", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Text("${data.maxActivityScore}점", color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("첫 활동", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Text(formatUtcTime(data.firstActivityTime), color = Color.White, fontSize = 16.sp)
            }
            Column {
                Text("마지막 활동", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Text(formatUtcTime(data.lastActivityTime), color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("수면", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Text(formatMinutes(data.estimatedSleepMinutes), color = Color.White, fontSize = 16.sp)
            }
            Column {
                Text("수면 시간", color = Color.White.copy(0.7f), fontSize = 12.sp)
                val sleepStart = formatUtcTime(data.estimatedSleepStart)
                val sleepEnd = formatUtcTime(data.estimatedSleepEnd)
                if (sleepStart == "정보 없음" || sleepEnd == "정보 없음" || sleepStart == "0분" && sleepEnd == "0분") {
                    Text("0분", color = Color.White, fontSize = 16.sp)
                } else {
                    Text("$sleepStart ~ $sleepEnd", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("가장 활동적인 시간", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Text("${data.mostActiveHour}시", color = Color.White, fontSize = 16.sp)
            }
            Column {
                Text("가장 비활동적인 시간", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Text("${data.leastActiveHour}시", color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("패턴 유형", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Text(patternType, color = Color.White, fontSize = 16.sp)
            }
            Column {
                Text("규칙성 점수", color = Color.White.copy(0.7f), fontSize = 12.sp)
                Text("${data.activityRegularityScore.toInt()}점", color = Color.White, fontSize = 16.sp)
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