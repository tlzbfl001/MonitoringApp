package com.aitronbiz.arron.screen.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.RealTimeRespirationResponse
import com.aitronbiz.arron.databinding.FragmentRealTimeRespirationBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class RealTimeRespirationFragment : Fragment() {
    private var _binding: FragmentRealTimeRespirationBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_HOME_ID = "argHomeId"
        private const val ARG_ROOM_ID = "argRoomId"
        private const val ARG_DATE    = "argSelectedDate"

        fun newInstance(homeId: String, roomId: String, date: LocalDate) =
            RealTimeRespirationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_HOME_ID, homeId)
                    putString(ARG_ROOM_ID, roomId)
                    putLong(ARG_DATE, date.toEpochDay())
                }
            }
    }

    private lateinit var homeId: String
    private lateinit var roomId: String
    private lateinit var date: LocalDate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        homeId = args.getString(ARG_HOME_ID).orEmpty()
        roomId = args.getString(ARG_ROOM_ID).orEmpty()
        date   = LocalDate.ofEpochDay(args.getLong(ARG_DATE))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRealTimeRespirationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val roomId = requireArguments().getString(ARG_ROOM_ID)
            ?: throw IllegalArgumentException("roomId is required")

        binding.compose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )

        binding.compose.setContent {
            RealTimeRespirationScreen(
                roomId = roomId,
                onBack = { goBack() }
            )
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = goBack()
            }
        )
    }

    private fun goBack() {
        val popped = parentFragmentManager.popBackStackImmediate()
        if (!popped) {
            replaceFragment2(
                parentFragmentManager,
                RespirationFragment.newInstance(homeId, date), null
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@Composable
fun RealTimeRespirationScreen(
    roomId: String,
    onBack: () -> Unit
) {
    val url = "https://dev.arron.aitronbiz.com/api/breathing/rooms/$roomId/stream"
    val gson = remember { Gson() }
    val token = remember { AppController.prefs.getToken().orEmpty() }

    // 차트에 표시할 최대 포인트 수, 오래된 데이터는 삭제
    val maxPoints = 120
    val samples = remember { mutableStateListOf<RespSample>() }

    // SSE에서 받은 최신 호흡수 값
    var pendingRate by remember { mutableStateOf<Float?>(null) }

    // OkHttp SSE 클라이언트
    val client = remember {
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    // SSE 연결 설정
    DisposableEffect(Unit) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        val handler = Handler(Looper.getMainLooper())
        val esFactory = EventSources.createFactory(client)
        val listener = object : EventSourceListener() {
            override fun onEvent(es: EventSource, id: String?, type: String?, data: String) {
                try {
                    val parsed = gson.fromJson(data, RealTimeRespirationResponse::class.java)
                    val rate = parsed.data.breathingRate?.toFloat() ?: return
                    handler.post { pendingRate = rate }
                } catch (e: Exception) {
                    Log.e(TAG, "parse error: ${e.message} / raw=$data")
                }
            }
            override fun onFailure(es: EventSource, t: Throwable?, response: Response?) {
                Log.e(TAG, "SSE failure: ${t?.message} / code=${response?.code}")
            }
        }

        // SSE 시작
        val eventSource = esFactory.newEventSource(request, listener)

        onDispose { eventSource.cancel() }
    }

    // 1초마다 차트 데이터 업데이트
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            // 새 데이터가 없으면 0을 사용
            val v = pendingRate ?: 0f
            pendingRate = null
            // 현재 시간 + 값 추가
            val now = System.currentTimeMillis()
            samples.add(RespSample(now, v))
            // 최대 포인트 초과 시 오래된 데이터 삭제
            if (samples.size > maxPoints) samples.removeAt(0)
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
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
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

        Spacer(Modifier.height(10.dp))

        // 실시간 차트
        RealTimeRespirationChart(
            samples = samples,
            maxPoints = maxPoints,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 17.dp)
        )

        Spacer(Modifier.height(8.dp))

        // 최신 호흡수 표시
        val latest = samples.lastOrNull()?.value
        Text(
            text = if (latest != null) "현재 호흡수: ${"%.1f".format(latest)} bpm" else "데이터 수신 대기중…",
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.padding(start = 20.dp)
        )
    }
}

@Composable
fun RealTimeRespirationChart(
    samples: List<RespSample>,
    maxPoints: Int,
    modifier: Modifier = Modifier,
    yPaddingRatio: Float = 0.2f
) {
    // Y축 범위 계산
    val yMinRaw = samples.minOfOrNull { it.value } ?: 0f
    val yMaxRaw = samples.maxOfOrNull { it.value } ?: 1f
    val yRange = (yMaxRaw - yMinRaw).coerceAtLeast(1f)
    val pad = yRange * yPaddingRatio
    val yMin = yMinRaw - pad
    val yMax = yMaxRaw + pad
    val effectiveRange = (yMax - yMin).coerceAtLeast(1f)

    // 시간 범위
    val tMin = samples.firstOrNull()?.tMillis
    val tMax = samples.lastOrNull()?.tMillis

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leftPad = 20.dp.toPx()
        val rightPad = 15.dp.toPx()
        val topPad = 10.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val chartW = w - leftPad - rightPad
        val chartH = h - topPad - bottomPad

        // X축, Y축 라인
        drawLine(
            color = Color.White,
            start = Offset(leftPad, topPad + chartH),
            end = Offset(leftPad + chartW, topPad + chartH),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.White,
            start = Offset(leftPad, topPad),
            end = Offset(leftPad, topPad + chartH),
            strokeWidth = 2f
        )

        // 데이터 라인
        if (samples.isNotEmpty()) {
            val path = Path()
            val count = samples.size.coerceAtMost(maxPoints)
            val sub = samples.takeLast(count)

            val xStep = if (count <= 1) 0f else chartW / (count - 1).toFloat()
            fun point(i: Int, v: Float): Offset {
                val x = leftPad + i * xStep
                val norm = (v - yMin) / effectiveRange
                val y = topPad + chartH * (1f - norm)
                return Offset(x, y)
            }

            path.moveTo(point(0, sub.first().value).x, point(0, sub.first().value).y)
            for (i in 1 until count) {
                val p = point(i, sub[i].value)
                path.lineTo(p.x, p.y)
            }

            drawPath(
                path = path,
                color = Color(0xFF2962FF),
                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        val labelTextSizePx = 11.dp.toPx()
        val paintY = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.RIGHT
            textSize = labelTextSizePx
            isAntiAlias = true
        }
        val paintX = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 9.dp.toPx()
            isAntiAlias = true
        }

        // Y축 라벨
        val labelMin = String.format("%.0f", yMin)
        val labelMid = String.format("%.0f", (yMin + yMax) / 2f)
        val labelMax = String.format("%.0f", yMax)
        drawContext.canvas.nativeCanvas.apply {
            drawText(labelMax, leftPad - 8f, topPad + labelTextSizePx, paintY)
            drawText(labelMid, leftPad - 8f, topPad + chartH / 2f + (labelTextSizePx * 0.35f), paintY)
            drawText(labelMin, leftPad - 8f, topPad + chartH + (labelTextSizePx * 0.35f), paintY)
        }

        // X축 라벨
        if (tMin != null && tMax != null && tMax >= tMin) {
            val thirtySecMs = 30_000L
            fun floorTo30Sec(ms: Long) = (ms / thirtySecMs) * thirtySecMs
            var tick = floorTo30Sec(tMin)

            while (tick <= tMax) {
                if (tick >= tMin) {
                    val ratio = if (tMax == tMin) 1f else (tick - tMin).toFloat() / (tMax - tMin).toFloat()
                    val x = leftPad + chartW * ratio.coerceIn(0f, 1f)

                    val zdt = java.time.Instant.ofEpochMilli(tick)
                        .atZone(java.time.ZoneId.systemDefault())
                    val text = "%02d:%02d:%02d".format(zdt.hour, zdt.minute, zdt.second)

                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        x,
                        topPad + chartH + labelTextSizePx + 2f,
                        paintX
                    )
                }
                tick += thirtySecMs
            }
        }
    }
}

data class RespSample(val tMillis: Long, val value: Float)