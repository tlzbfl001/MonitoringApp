package com.aitronbiz.arron.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.LifePatterns
import com.aitronbiz.arron.viewmodel.LifePatternsViewModel
import com.aitronbiz.arron.viewmodel.MainViewModel
import java.time.ZoneId

@Composable
fun LifePatternScreen(
    homeId: String,
    roomId: String,
    selectedDate: java.time.LocalDate,
    viewModel: LifePatternsViewModel = viewModel(),
    navController: NavController,
    mainViewModel: MainViewModel = viewModel()
) {
    val token = AppController.prefs.getToken().orEmpty()

    val lifePatterns by viewModel.lifePatterns.collectAsState()
    var hasUnreadNotification by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.resetState(token, homeId)
        viewModel.fetchLifePatternsData(token, homeId, selectedDate)
        mainViewModel.checkNotifications { hasUnreadNotification = it }
    }
    LaunchedEffect(selectedDate) {
        if (token.isNotBlank()) viewModel.fetchLifePatternsData(token, homeId, selectedDate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 상단 바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 5.dp, end = 20.dp, top = 2.dp)
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

            Text("생활 패턴", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Spacer(Modifier.weight(1f))

            Box(modifier = Modifier.clickable { navController.navigate("notification") }) {
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp, start = 20.dp, end = 20.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDate.toString(),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(10.dp))

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

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun LifePatternsSummary(
    data: LifePatterns
) {
    val strong = Color.White
    val soft = Color.White.copy(alpha = 0.75f)

    val tileBg   = Color(0x5A185078)
    val tileBr   = Color(0xFF185078)
    val accent   = Color(0xE6FF6B6B)

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
        text = "생활 패턴 세부 정보",
        color = strong,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )

    val tileH = 92.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BorderedStatButton(
            bg = tileBg,
            borderColor = tileBr,
            title = "총 활동 시간",
            value = formatMinutes(data.totalActiveMinutes),
            titleColor = strong, valueColor = accent,
            titleSize = labelBig, valueSize = valueBig,
            modifier = Modifier.weight(1f).height(tileH)
        )
        BorderedStatButton(
            bg = tileBg,
            borderColor = tileBr,
            title = "총 비활동 시간",
            value = formatMinutes(data.totalInactiveMinutes),
            titleColor = strong, valueColor = strong,
            titleSize = labelBig, valueSize = valueBig,
            modifier = Modifier.weight(1f).height(tileH)
        )
    }

    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BorderedStatButton(
            bg = tileBg,
            borderColor = tileBr,
            title = "평균 점수",
            value = "${data.averageActivityScore.toInt()}점",
            titleColor = strong, valueColor = accent,
            titleSize = labelBig, valueSize = valueBig,
            modifier = Modifier.weight(1f).height(tileH)
        )
        BorderedStatButton(
            bg = tileBg,
            borderColor = tileBr,
            title = "최고 점수",
            value = "${data.maxActivityScore}점",
            titleColor = strong, valueColor = strong,
            titleSize = labelBig, valueSize = valueBig,
            modifier = Modifier.weight(1f).height(tileH)
        )
    }

    Spacer(Modifier.height(20.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tileBg)
            .border(1.dp, tileBr, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PairRowClean("첫 활동", hhmm(data.firstActivityTime),
            "마지막 활동", hhmm(data.lastActivityTime),
            13.sp, pairValue, soft, strong)
        PairRowClean("수면", formatMinutes(data.estimatedSleepMinutes),
            "수면 시간",
            run {
                val s = hhmm(data.estimatedSleepStart)
                val e = hhmm(data.estimatedSleepEnd)
                if (s == "정보 없음" || e == "정보 없음") "정보 없음" else "$s ~ $e"
            },
            13.sp, pairValue, soft, strong)
        PairRowClean("활동적 시간", "${data.mostActiveHour}시",
            "비활동적 시간", "${data.leastActiveHour}시",
            13.sp, pairValue, soft, strong)
        PairRowClean("패턴 유형", patternKo(data.activityPatternType),
            "규칙성 점수", "${data.activityRegularityScore.toInt()}점",
            13.sp, pairValue, soft, strong)
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
fun PairRowClean(
    label1: String,
    value1: String,
    label2: String,
    value2: String,
    labelSize: TextUnit,
    valueSize: TextUnit,
    labelColor: Color,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = label1,
                color = labelColor,
                fontSize = labelSize,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = value1,
                color = valueColor,
                fontSize = valueSize,
                fontWeight = FontWeight.Bold
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = label2,
                color = labelColor,
                fontSize = labelSize,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = value2,
                color = valueColor,
                fontSize = valueSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
