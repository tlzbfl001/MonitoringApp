package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.util.ActivityAlertStore
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex

    private var activityJob: Job? = null

    // 경고 임계값
    private val THRESHOLD = 0.0

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        _chartData.value = emptyList()
        // 날짜 바뀌면 기존 알림 상태 리셋
        ActivityAlertStore.clearAll()
    }

    fun resetState() {
        _chartData.value = emptyList()
        _selectedIndex.value = -1
        _selectedDate.value = LocalDate.now()
        ActivityAlertStore.clearAll()
    }

    fun selectBar(index: Int) {
        _selectedIndex.value = index
    }

    fun fetchActivityData(token: String, roomId: String, selectedDate: LocalDate) {
        activityJob?.cancel()

        activityJob = viewModelScope.launch {
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC"))

            while (isActive) {
                if (this@ActivityViewModel.selectedDate.value != selectedDate) break

                val start: Instant
                val end: Instant

                if (selectedDate == today) {
                    val existing = _chartData.value
                    start = if (existing.isEmpty()) {
                        selectedDate.atStartOfDay(zoneId).toInstant()
                    } else {
                        val lastLabel = existing.last().timeLabel
                        val h = lastLabel.substringBefore(":").toInt()
                        val m = lastLabel.substringAfter(":").toInt()
                        LocalDateTime.of(selectedDate, LocalTime.of(h, m))
                            .atZone(zoneId).toInstant()
                    }
                    end = Instant.now()
                } else {
                    start = selectedDate.atStartOfDay(zoneId).toInstant()
                    end   = selectedDate.atTime(23, 59, 59).atZone(zoneId).toInstant()
                }

                val response = try {
                    RetrofitClient.apiService.getActivity(
                        token    = "Bearer $token",
                        roomId   = roomId,
                        startTime= formatter.format(start),
                        endTime  = formatter.format(end)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "getActivity: ", e)
                    null
                }

                if (response != null && response.isSuccessful) {
                    val list = response.body()?.activityScores ?: emptyList()

                    val newPoints = list.sortedBy { Instant.parse(it.startTime) }
                        .map {
                            val localTime = Instant.parse(it.startTime)
                                .atZone(zoneId)
                                .toLocalTime()
                                .truncatedTo(ChronoUnit.MINUTES)
                            ChartPoint(
                                String.format("%02d:%02d", localTime.hour, localTime.minute),
                                it.activityScore.toFloat().coerceAtLeast(0f)
                            )
                        }

                    if (this@ActivityViewModel.selectedDate.value == selectedDate) {
                        // 차트 데이터 반영
                        _chartData.value = if (selectedDate == today) {
                            (_chartData.value + newPoints)
                                .groupBy { it.timeLabel }
                                .map { it.value.last() }
                                .sortedBy { it.timeLabel }
                        } else {
                            newPoints
                        }

                        // 마지막 포인트 기준으로만 경고 상태 갱신
                        val lastVal = _chartData.value.lastOrNull()?.value ?: 0f
                        val isDangerNow = (selectedDate == today) && (lastVal >= THRESHOLD)
                        ActivityAlertStore.set(roomId, isDangerNow)
                    }
                } else if (response != null) {
                    Log.e(TAG, "getActivity: ${response.code()}")
                }

                // 과거 날짜는 1회 조회 후 종료 + 경고 OFF
                if (selectedDate != today) {
                    ActivityAlertStore.set(roomId, false)
                    break
                }

                // 오늘은 폴링
                delay(60_000L)
            }
        }
    }
}