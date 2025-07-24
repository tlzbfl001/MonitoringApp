package com.aitronbiz.arron.view.home

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

class ActivityDetectionFragment : Fragment() {
    private val viewModel: ActivityViewModel by activityViewModels()

    private val token: String = AppController.prefs.getToken().toString()
    private val roomId: String = "fd87cdd2-9486-4aef-9bfb-fa4aea9edc11"

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
                    roomId = roomId,
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
    onBackClick: () -> Unit,
    token: String,
    roomId: String
) {
    val data by viewModel.chartData.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()

    val maxY = (data.maxOfOrNull { it.value } ?: 0f) + 10f
    val barWidth = 30.dp
    val barSpacing = 12.dp
    val chartHeight = 200.dp
    val scrollState = rememberScrollState()
    val statusBarHeight = rememberStatusBarHeight()
    val density = LocalDensity.current

    // 초기 데이터 fetch
    LaunchedEffect(Unit) {
        viewModel.fetchActivityData(token, roomId)
    }

    // 그래프 처음 진입 시 자동 스크롤 + 마지막 막대 선택
    LaunchedEffect(data) {
        if (data.isNotEmpty()) {
            viewModel.selectBar(data.lastIndex)
            val offsetPx = with(density) {
                (barWidth + barSpacing).roundToPx() * (data.size - 8)
            }
            scrollState.scrollTo(offsetPx)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(top = statusBarHeight + 15.dp, start = 20.dp, end = 20.dp)
    ) {
        Column {
            // 상단 바
            Box(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
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

            Spacer(modifier = Modifier.height(35.dp))

            // 그래프 영역
            Box(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .height(chartHeight + 80.dp)
                    .padding(start = 2.dp, end = 20.dp)
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

                    // 막대 및 라벨
                    data.forEachIndexed { i, point ->
                        val x = i * (barPx + spacePx) + 60f
                        val barHeight = point.value * unitHeight
                        val y = chartAreaHeight - barHeight
                        val isSelected = i == selectedIndex

                        // 막대 그리기
                        drawRoundRect(
                            brush = if (isSelected) SolidColor(Color(0xFFFF3B30))
                            else Brush.verticalGradient(
                                listOf(Color(0xFF64B5F6), Color(0xFFB3E5FC)),
                                startY = 0f,
                                endY = chartAreaHeight
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barPx, barHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        // 라벨
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

                        // 툴팁
                        if (isSelected) {
                            val tooltip = "%.2f".format(point.value)
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

                        // 클릭 이벤트를 캔버스 외부에서 감지할 수 없기 때문에 대신 Modifier로 투명한 Row를 겹쳐서 클릭 인식
                    }
                }

                // 클릭 가능한 오버레이
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
                                .clickable {
                                    viewModel.selectBar(i)
                                }
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