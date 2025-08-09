package com.aitronbiz.arron.screen.home

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.response.RealTimeRespirationResponse
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.aitronbiz.arron.R

@Composable
fun RealTimeRespirationScreen(
    navController: NavController,
    navBack: () -> Unit
) {
    val url =
        "https://dev.arron.aitronbiz.com/api/breathing/rooms/fd87cdd2-9486-4aef-9bfb-fa4aea9edc11/stream"

    val gson = remember { Gson() }
    val token = remember { AppController.prefs.getToken().orEmpty() }

    // 최근 120개만 보관해서 부드럽게 스크롤되도록
    val maxPoints = 120
    val rates = remember { mutableStateListOf<Float>() }

    // SSE 클라이언트 (readTimeout 무제한)
    val client = remember {
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    // 화면 생명주기에 맞춰 연결/해제
    DisposableEffect(Unit) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        val handler = Handler(Looper.getMainLooper())
        val esFactory = EventSources.createFactory(client)
        val listener = object : EventSourceListener() {
            override fun onOpen(es: EventSource, response: Response) {
                Log.d(TAG, "SSE opened")
            }

            override fun onEvent(es: EventSource, id: String?, type: String?, data: String) {
                try {
                    val parsed =
                        gson.fromJson(data, RealTimeRespirationResponse::class.java)
                    val rate = parsed.data.breathingRate?.toFloat() ?: return
                    handler.post {
                        rates.add(rate)
                        if (rates.size > maxPoints) {
                            rates.removeAt(0)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "parse error: ${e.message} / raw=$data")
                }
            }

            override fun onFailure(es: EventSource, t: Throwable?, response: Response?) {
                Log.e(TAG, "SSE failure: ${t?.message} / code=${response?.code}")
            }

            override fun onClosed(es: EventSource) {
                Log.d(TAG, "SSE closed")
            }
        }

        val eventSource = esFactory.newEventSource(request, listener)

        onDispose {
            eventSource.cancel()
        }
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
                .padding(horizontal = 9.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = { navBack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "실시간 호흡수",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        // 실시간 라인 차트
        RealTimeRespirationChart(
            rates = rates,
            maxPoints = maxPoints,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFFF7F7F7))
                .padding(8.dp)
        )

        Spacer(Modifier.height(8.dp))
        val latest = rates.lastOrNull()
        Text(
            text = if (latest != null) "현재 호흡수: ${"%.1f".format(latest)} bpm" else "데이터 수신 대기중…",
            fontSize = 14.sp
        )
    }
}

@Composable
fun RealTimeRespirationChart(
    rates: List<Float>,
    maxPoints: Int,
    modifier: Modifier = Modifier,
    gridLines: Int = 4,
    yPaddingRatio: Float = 0.2f
) {
    // y 스케일링 계산
    val yMinRaw = rates.minOrNull() ?: 0f
    val yMaxRaw = rates.maxOrNull() ?: 1f
    val yRange = (yMaxRaw - yMinRaw).coerceAtLeast(1f)
    val pad = yRange * yPaddingRatio
    val yMin = (yMinRaw - pad).coerceAtMost(yMinRaw)
    val yMax = (yMaxRaw + pad).coerceAtLeast(yMaxRaw)
    val effectiveRange = (yMax - yMin).coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val leftPad = 42f
        val rightPad = 10f
        val topPad = 10f
        val bottomPad = 24f

        val chartW = w - leftPad - rightPad
        val chartH = h - topPad - bottomPad

        val stepY = chartH / gridLines
        for (i in 0..gridLines) {
            val y = topPad + i * stepY
            drawLine(
                color = Color(0xFFE0E0E0),
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartW, y),
                strokeWidth = 1f
            )
        }

        val vertLines = 6
        val stepX = chartW / vertLines
        for (i in 0..vertLines) {
            val x = leftPad + i * stepX
            drawLine(
                color = Color(0xFFEAEAEA),
                start = Offset(x, topPad),
                end = Offset(x, topPad + chartH),
                strokeWidth = 1f
            )
        }

        drawLine(
            color = Color(0xFFBDBDBD),
            start = Offset(leftPad, topPad + chartH),
            end = Offset(leftPad + chartW, topPad + chartH),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xFFBDBDBD),
            start = Offset(leftPad, topPad),
            end = Offset(leftPad, topPad + chartH),
            strokeWidth = 2f
        )

        // 라인 Path
        if (rates.isNotEmpty()) {
            val path = Path()
            val count = rates.size.coerceAtMost(maxPoints)
            val xStep = if (count <= 1) chartW else chartW / (count - 1).toFloat()

            fun toPoint(index: Int, value: Float): Offset {
                val x = leftPad + index * xStep
                val norm = (value - yMin) / effectiveRange
                val y = topPad + chartH * (1f - norm)
                return Offset(x, y)
            }

            // 시작점
            path.moveTo(
                toPoint(0, rates[rates.lastIndex - count + 1]).x,
                toPoint(0, rates[rates.lastIndex - count + 1]).y
            )

            // 최신 N개를 순서대로 그리기
            for (i in 0 until count) {
                val v = rates[rates.size - count + i]
                val p = toPoint(i, v)
                path.lineTo(p.x, p.y)
            }

            drawPath(
                path = path,
                color = Color(0xFF2962FF),
                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        val labelMin = String.format("%.0f", yMin)
        val labelMid = String.format("%.0f", (yMin + yMax) / 2f)
        val labelMax = String.format("%.0f", yMax)

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.DKGRAY
                textAlign = android.graphics.Paint.Align.RIGHT
                textSize = 24f
                isAntiAlias = true
            }
            // Max
            this.drawText(labelMax, leftPad - 8f, topPad + 12f, paint)
            // Mid
            this.drawText(labelMid, leftPad - 8f, topPad + chartH / 2f + 8f, paint)
            // Min
            this.drawText(labelMin, leftPad - 8f, topPad + chartH + 8f, paint)

            // x축 라벨
            val xPaint = android.graphics.Paint(paint).apply {
                textAlign = android.graphics.Paint.Align.CENTER
            }
            this.drawText("Old", leftPad, topPad + chartH + 20f, xPaint)
            this.drawText("Now", leftPad + chartW, topPad + chartH + 20f, xPaint)
        }
    }
}
