package com.aitronbiz.arron.view.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.viewmodel.EntryPatternsViewModel

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

        Spacer(modifier = Modifier.height(30.dp))

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

        Spacer(modifier = Modifier.height(60.dp))

        // 룸 선택 리스트
        if (rooms.isNotEmpty()) {
            Text(
                text = "룸 선택",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.noto_sans_kr_bold)),
                modifier = Modifier.padding(start = 22.dp)
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
                            .padding(top = 3.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        width = 2.dp,
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

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun EntryPatternsCharts(
    hourlyPatterns: List<HourlyPattern>,
    weeklyPatterns: List<WeeklyPattern>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "시간별 출입 패턴",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // 시간별 출입 패턴 차트
        HourlyEntryChart(hourlyPatterns)

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "요일별 출입 패턴",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // 요일별 출입 패턴 차트
        WeeklyEntryChart(weeklyPatterns)
    }
}

@Composable
fun HourlyEntryChart(patterns: List<HourlyPattern>) {
    val barWidth = 12.dp
    val chartHeight = 160.dp
    val maxCount = (patterns.maxOfOrNull { maxOf(it.entryCount ?: 0, it.exitCount ?: 0) } ?: 1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight + 40.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        patterns.forEach { pattern ->
            val entryHeight = (pattern.entryCount ?: 0).toFloat() / maxCount
            val exitHeight = (pattern.exitCount ?: 0).toFloat() / maxCount
            val hour = pattern.timeSlot ?: 0

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // 막대
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(chartHeight * entryHeight)
                            .background(Color(0xFF2D60FF)) // 파란색 entry
                    )
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(chartHeight * exitHeight)
                            .background(Color(0xFF84FFB1)) // 연두색 exit
                    )
                }

                // 2시간 단위 레이블만 표시
                if (hour % 2 == 0) {
                    Text(
                        text = "${hour}시",
                        color = Color.White,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .graphicsLayer {
                                rotationZ = -45f
                            }
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun WeeklyEntryChart(patterns: List<WeeklyPattern>) {
    val barWidth = 20.dp
    val chartHeight = 160.dp
    val maxCount = (patterns.maxOfOrNull { maxOf(it.entryCount ?: 0, it.exitCount ?: 0) } ?: 1)
    val dayMap = mapOf(
        "Sunday" to "일",
        "Monday" to "월",
        "Tuesday" to "화",
        "Wednesday" to "수",
        "Thursday" to "목",
        "Friday" to "금",
        "Saturday" to "토"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight + 40.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        patterns.forEach { pattern ->
            val entryHeight = (pattern.entryCount ?: 0).toFloat() / maxCount
            val exitHeight = (pattern.exitCount ?: 0).toFloat() / maxCount
            val label = dayMap[pattern.metadata?.dayName] ?: ""

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // 막대
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(chartHeight * entryHeight)
                            .background(Color(0xFFFFD05A))
                    )
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(chartHeight * exitHeight)
                            .background(Color(0xFFFF314B))
                    )
                }

                // 아래쪽 레이블
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
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