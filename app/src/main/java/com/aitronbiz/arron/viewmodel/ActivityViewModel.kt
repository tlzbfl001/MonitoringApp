package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.ErrorResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.aitronbiz.arron.entity.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ActivityViewModel : ViewModel() {
    private val _chartData = MutableStateFlow<List<ChartPoint>>(emptyList())
    val chartData: StateFlow<List<ChartPoint>> = _chartData

    private val _selectedIndex = MutableStateFlow(-1)
    val selectedIndex: StateFlow<Int> = _selectedIndex

    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"))

    fun selectBar(index: Int) {
        _selectedIndex.value = index
    }

    fun fetchActivityData(token: String, roomId: String) {
        viewModelScope.launch {
            val currentData = _chartData.value

            val end = Instant.now()

            val start = if (currentData.isEmpty()) {
                // 오늘 자정부터
                LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            } else {
                // 마지막 데이터의 startTime + 10분
                val lastLabel = currentData.last().timeLabel // "HH:mm"
                val today = LocalDate.now()
                val hour = lastLabel.substringBefore(":").toInt()
                val minute = lastLabel.substringAfter(":").toInt()
                val lastTime = LocalDateTime.of(today, LocalTime.of(hour, minute)).plusMinutes(10)
                lastTime.atZone(ZoneId.systemDefault()).toInstant()
            }

            val formattedStart = formatter.format(start)
            val formattedEnd = formatter.format(end)

            try {
                val response = RetrofitClient.apiService.getActivity(
                    token = "Bearer $token",
                    roomId = roomId,
                    startTime = formattedStart,
                    endTime = formattedEnd
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val list = body?.activityScores ?: emptyList()
                    Log.d(TAG, "getActivity: $body")

                    val updatedPoints = list.sortedBy {
                        Instant.parse(it.startTime)
                    }.map {
                        val time = Instant.parse(it.startTime)
                            .atZone(ZoneId.systemDefault())
                            .toLocalTime()
                            .truncatedTo(ChronoUnit.MINUTES)
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                        ChartPoint(time, it.activityScore.toFloat())
                    }

                    _chartData.value = (_chartData.value + updatedPoints).distinctBy { it.timeLabel }
                }else {
                    val errorBody = response.errorBody()?.string()
                    val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                    Log.e(TAG, "getActivity: $errorResponse")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}