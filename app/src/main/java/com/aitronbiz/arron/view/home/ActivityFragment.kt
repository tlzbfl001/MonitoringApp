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
import com.aitronbiz.arron.R
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.viewmodel.ActivityViewModel

class ActivityFragment : Fragment() {
    private val viewModel: ActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ActivityBarChartScreen(
                    viewModel = viewModel,
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
    onBackClick: () -> Unit
) {
    val data by viewModel.chartData.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()

    val maxY = (data.maxOfOrNull { it.value } ?: 0f) + 10f
    val barWidth = 30.dp
    val barSpacing = 12.dp
    val chartHeight = 200.dp
    val scrollState = rememberScrollState()
    val statusBarHeight = rememberStatusBarHeight()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .padding(top = statusBarHeight + 15.dp, start = 20.dp, end = 20.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
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

            Box(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .height(chartHeight + 80.dp)
                    .padding(start = 2.dp, end = 20.dp)
            ) {
                Canvas(modifier = Modifier
                    .width((barWidth + barSpacing) * data.size)
                    .height(chartHeight + 80.dp)) {

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

                    // 막대 + 라벨
                    data.forEachIndexed { i, point ->
                        val x = i * (barPx + spacePx) + 60f
                        val barHeight = point.value * unitHeight
                        val y = chartAreaHeight - barHeight
                        val isSelected = i == selectedIndex
                        val barColor = if (isSelected) Color(0xFFFF3B30) else null

                        drawRoundRect(
                            brush = barColor?.let { SolidColor(it) } ?: Brush.verticalGradient(
                                colors = listOf(Color(0xFF64B5F6), Color(0xFFB3E5FC)),
                                startY = 0f,
                                endY = chartAreaHeight
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barPx, barHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

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
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.selectBar(10)
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
