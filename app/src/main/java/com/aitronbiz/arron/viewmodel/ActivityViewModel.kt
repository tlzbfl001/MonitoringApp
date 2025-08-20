package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
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

    // 홈의 전체 룸 목록
    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val _showMonthlyCalendar = mutableStateOf(false)

    private var activityJob: Job? = null

    // 경고 임계값
    private val THRESHOLD = 0.0

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        _chartData.value = emptyList()
        _selectedIndex.value = 0
        ActivityAlertStore.clearAll()
        _showMonthlyCalendar.value = false
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

            ActivityAlertStore.setLatestActivity(roomId, 0)
            ActivityAlertStore.set(roomId, false)

            while (isActive) {
                if (this@ActivityViewModel.selectedDate.value != selectedDate) break

                val (start, end) = if (selectedDate == today) {
                    val existing = _chartData.value
                    val s = if (existing.isEmpty()) {
                        selectedDate.atStartOfDay(zoneId).toInstant()
                    } else {
                        val last = existing.last().timeLabel
                        val h = last.substringBefore(":").toInt()
                        val m = last.substringAfter(":").toInt()
                        LocalDateTime.of(selectedDate, LocalTime.of(h, m)).atZone(zoneId).toInstant()
                    }
                    s to Instant.now()
                } else {
                    selectedDate.atStartOfDay(zoneId).toInstant() to
                            selectedDate.atTime(23, 59, 59).atZone(zoneId).toInstant()
                }

                val response = try {
                    RetrofitClient.apiService.getActivity(
                        token = "Bearer $token",
                        roomId = roomId,
                        startTime = formatter.format(start),
                        endTime = formatter.format(end)
                    )
                } catch (e: Exception) {
                    ActivityAlertStore.setLatestActivity(roomId, 0)
                    ActivityAlertStore.set(roomId, false)
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
                        _chartData.value = if (selectedDate == today) {
                            (_chartData.value + newPoints)
                                .groupBy { it.timeLabel }
                                .map { it.value.last() }
                                .sortedBy { it.timeLabel }
                        } else {
                            newPoints
                        }

                        // 새 데이터가 없으면 0을 반영
                        val lastVal = _chartData.value.lastOrNull()?.value ?: 0f
                        ActivityAlertStore.setLatestActivity(roomId, lastVal.toInt())

                        // 임계치(>=80) 경고
                        val isDangerNow = (selectedDate == today) && (lastVal >= THRESHOLD)
                        ActivityAlertStore.set(roomId, isDangerNow)
                    }
                } else if (response != null) {
                    ActivityAlertStore.setLatestActivity(roomId, 0)
                    ActivityAlertStore.set(roomId, false)
                }

                if (selectedDate != today) {
                    ActivityAlertStore.set(roomId, false)
                    break
                }

                // 오늘은 1분마다
                delay(60_000L)
            }
        }
    }

    fun fetchRooms(token: String, homeId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.apiService.getAllRoom(
                    token = "Bearer $token",
                    homeId = homeId
                )
                if (resp.isSuccessful) {
                    _rooms.value = resp.body()?.rooms ?: emptyList()
                } else {
                    _rooms.value = emptyList()
                    Log.e(TAG, "getAllRoom: ${resp.code()} ${resp.message()}")
                }
            } catch (t: Throwable) {
                _rooms.value = emptyList()
                Log.e(TAG, "getAllRoom error", t)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activityJob?.cancel()
        activityJob = null
    }
}
