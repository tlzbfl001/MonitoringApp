package com.aitronbiz.arron.screen.setting

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.aitronbiz.arron.screen.init.LoginActivity
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MainViewModel // ❗ NavGraph에서 같은 backStackEntry로 주입하세요
) {
    val context = LocalContext.current

    // UI 상태
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showCalendarSheet by remember { mutableStateOf(false) }
    var muteUntilDate by remember { mutableStateOf<LocalDate?>(null) }

    // 날짜 포맷
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)
    }
    val muteSubText = muteUntilDate?.format(dateFormatter) ?: "-"

    // ViewModel의 사용자 데이터(State로 노출된 전제)
    val userData by viewModel.userData

    // ✅ 최초 1회: 같은 ViewModel 인스턴스 기준으로 fetch 호출
    LaunchedEffect(viewModel) {
        Log.d(TAG, "LaunchedEffect(viewModel) - fetchUserSession() 호출")
        viewModel.fetchUserSession()
    }

    // ✅ 실제 값 로그
    Log.d(TAG, "userData 값 - name=${userData.name}, email=${userData.email}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "설정",
                color = Color.White,
                fontSize = 17.sp,
                fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                // 사용자 카드
                SettingCard1(
                    title = userData.name.ifBlank { "이름 없음" },
                    subText = userData.email.ifBlank { "이메일 없음" },
                    iconRes = R.drawable.ic_user
                ) {
                    navController.navigate("user")
                }

                Spacer(modifier = Modifier.height(11.dp))

                // 기기 연동
                SettingCard2(title = "기기 연동") {
                    Toast.makeText(context, "기기 연동 클릭됨", Toast.LENGTH_SHORT).show()
                }
                Spacer(modifier = Modifier.height(11.dp))

                // 서비스 정책
                SettingCard2(title = "서비스 정책") {
                    navController.navigate("terms")
                }
                Spacer(modifier = Modifier.height(11.dp))

                // 모니터링 알림 금지
                SettingCard2(title = "모니터링 알림 금지", subText = muteSubText) {
                    showCalendarSheet = true
                }
                Spacer(modifier = Modifier.height(11.dp))

                // 어플정보
                SettingCard2(title = "어플정보") {
                    navController.navigate("appInfo")
                }
                Spacer(modifier = Modifier.height(11.dp))

                // 로그아웃
                SettingCard2(title = "로그아웃") {
                    showLogoutDialog = true
                }
            }
        }
    }

    // 로그아웃 다이얼로그
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.stopTokenAutoRefresh()
                        AppController.prefs.removeToken()
                        AppController.prefs.removeUID()

                        Toast.makeText(context, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

                        val intent = Intent(context, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)

                        showLogoutDialog = false
                    }
                ) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("취소") }
            },
            title = { Text("로그아웃") },
            text = { Text("정말 로그아웃 하시겠습니까?") }
        )
    }

    // 월간 달력
    if (showCalendarSheet) {
        MonthlyCalendarSheet(
            selectedDate = muteUntilDate ?: LocalDate.now(),
            onDateSelected = { picked ->
                muteUntilDate = picked
                showCalendarSheet = false
            },
            onDismiss = { showCalendarSheet = false }
        )
    }
}

@Composable
fun SettingCard1(
    title: String,
    subText: String? = null,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF174176))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 15.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                iconRes?.let {
                    Icon(
                        painter = painterResource(it), // ✅ 전달된 아이콘 사용
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(end = 20.dp)
                    )
                }
                Column {
                    Text(text = title, color = Color.White, fontSize = 16.sp)
                    subText?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = it, color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_right),
                contentDescription = "$title 이동",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
fun SettingCard2(
    title: String,
    subText: String? = null,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF174176))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 15.dp, top = 15.dp, bottom = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                iconRes?.let {
                    Icon(
                        painter = painterResource(id = it),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 10.dp)
                    )
                }
                Column {
                    Text(text = title, color = Color.White, fontSize = 16.sp)
                    subText?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = it, color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_right),
                contentDescription = "$title 이동",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyCalendarSheet(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    val today = LocalDate.now()
    val pagerState = rememberPagerState(initialPage = 1000) { Int.MAX_VALUE }
    var currentMonth by remember { mutableStateOf(today.withDayOfMonth(1)) }

    LaunchedEffect(selectedDate) {
        val offset = ChronoUnit.MONTHS.between(
            today.withDayOfMonth(1),
            selectedDate.withDayOfMonth(1)
        )
        scope.launch { pagerState.scrollToPage(1000 + offset.toInt()) }
    }

    LaunchedEffect(pagerState.currentPage) {
        val monthOffset = pagerState.currentPage - 1000
        currentMonth = today.plusMonths(monthOffset.toLong()).withDayOfMonth(1)
    }

    fun rowsInMonth(firstDay: LocalDate): Int {
        val daysInMonth = firstDay.lengthOfMonth()
        val firstDayOfWeek = (firstDay.dayOfWeek.value % 7)
        val totalCells = ((daysInMonth + firstDayOfWeek + 6) / 7) * 7
        return totalCells / 7
    }

    val cellSize: Dp = 40.dp
    val reducedCellSize: Dp = cellSize - 8.dp

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 25.dp, bottom = 40.dp)
        ) {
            // 상단 월 이동 헤더 (오늘 버튼 제거)
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
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    androidx.compose.material.Text(
                        text = "${currentMonth.year}-${currentMonth.monthValue}",
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
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                    )
                }

                // 오른쪽 여백으로 균형
                Spacer(modifier = Modifier.weight(1f))
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
            val dynamicHeight = lerp(baseHeight, neighborHeight, kotlin.math.abs(offsetFraction))

            // 월간 달력
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dynamicHeight)
            ) { page ->
                val monthOffset = page - 1000
                val currentMonth1 = today.plusMonths(monthOffset.toLong()).withDayOfMonth(1)
                val daysInMonth = currentMonth1.lengthOfMonth()
                val firstDayOfWeek = (currentMonth1.dayOfWeek.value % 7)
                val totalCells = ((daysInMonth + firstDayOfWeek + 6) / 7) * 7

                val dates = (0 until totalCells).map { index ->
                    val day = index - firstDayOfWeek + 1
                    if (day in 1..daysInMonth) currentMonth1.withDayOfMonth(day) else null
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
                                    val sizeToUse = if (isSelected || isToday) reducedCellSize else cellSize

                                    Box(
                                        modifier = Modifier
                                            .size(sizeToUse)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> Color.Black
                                                    isToday -> Color(0xFFE0E0E0)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { onDateSelected(date) },
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
                                    Box(modifier = Modifier.size(cellSize))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getUserInfo() {

}