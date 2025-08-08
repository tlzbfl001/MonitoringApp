package com.aitronbiz.arron.screen.home

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.response.RealTimeRespirationResponse
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

@Composable
fun RealTimeRespirationScreen() {
    val url = "https://dev.arron.aitronbiz.com/api/breathing/rooms/fd87cdd2-9486-4aef-9bfb-fa4aea9edc11/stream"
    val context = LocalContext.current

    val client = remember { OkHttpClient() }
    val gson = remember { Gson() }

    // 실시간 데이터를 저장할 상태
    var realTimeData by remember { mutableStateOf<RealTimeRespirationResponse?>(null) }

    // SSE 연결을 위한 요청 생성
    val request = remember {
        Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${AppController.prefs.getToken().toString()}")
            .build()
    }

    // SSE 연결을 위한 실시간 데이터 수신
    LaunchedEffect(true) {
        // OkHttp를 사용해 SSE 요청
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to connect: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val reader = response.body?.charStream()  // Stream을 사용하여 각 라인 처리
                    reader?.forEachLine { line ->
                        try {
                            // JSON 문자열을 RealTimeRespirationResponse로 변환
//                            val parsedResponse = gson.fromJson(line, RealTimeRespirationResponse::class.java)
                            // 변환된 데이터를 상태에 저장
//                            realTimeData = parsedResponse
                            Log.d(TAG, "line: $line")
                            if(line != null) {
                                val body = response.body?.string()  // 응답 본문을 문자열로 읽기
                                Log.d(TAG, "Response Body: $body")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing JSON: ${e.message}")
                        }
                    }
                } else {
                    Log.e(TAG, "Error: $response")
                }
            }
        })
    }

    // UI 구성
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("SSE 연결 상태", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        // 실시간 데이터를 화면에 표시
        realTimeData?.let { data ->
            Text(
                text = "ID: ${data.data.id}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = "호흡수: ${data.data.breathingRate}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        } ?: run {
            Text(
                text = "서버로부터 받은 실시간 데이터를 여기에 표시",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }

    // 화면 종료 시 연결 종료
    DisposableEffect(Unit) {
        onDispose {
            client.dispatcher.executorService.shutdown()
        }
    }
}
