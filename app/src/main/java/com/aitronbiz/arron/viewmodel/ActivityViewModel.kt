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

    // 선택된 날짜
    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    // 선택된 인덱스(슬롯)
    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex

    // 홈의 룸 목록
    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    // 룸별 재실 맵
    private val _presenceByRoomId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val presenceByRoomId: StateFlow<Map<String, Boolean>> = _presenceByRoomId

    private val _showMonthlyCalendar = mutableStateOf(false)

    private var activityJob: Job? = null

    private val _avgActivity = MutableStateFlow(0)
    val avgActivity: StateFlow<Int> = _avgActivity

    private val _minActivity = MutableStateFlow(0)
    val minActivity: StateFlow<Int> = _minActivity

    // 최대 활동량
    private val _maxActivity = MutableStateFlow(0)
    val maxActivity: StateFlow<Int> = _maxActivity

    private val _endIndex = MutableStateFlow(0)
    val endIndex: StateFlow<Int> = _endIndex

    // 경고 임계값
    private val THRESHOLD = 80.0

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

        // 내부 날짜 동기화 및 초기화
        _selectedDate.value = selectedDate
        _chartData.value = emptyList()
        _selectedIndex.value = 0
        ActivityAlertStore.setLatestActivity(roomId, 0)
        ActivityAlertStore.set(roomId, false)

        activityJob = viewModelScope.launch {
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC"))

            suspend fun loadOnce(start: Instant, end: Instant) {
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

                    _chartData.value = newPoints

                    val lastVal = _chartData.value.lastOrNull()?.value ?: 0f
                    ActivityAlertStore.setLatestActivity(roomId, lastVal.toInt())

                    ActivityAlertStore.set(roomId, false)
                } else {
                    Log.e(TAG, "getActivity: $response")
                }
            }

            if (selectedDate != today) {
                val start = selectedDate.atStartOfDay(zoneId).toInstant()
                val end = selectedDate.atTime(23, 59, 59).atZone(zoneId).toInstant()
                loadOnce(start, end)
                return@launch
            }

            while (isActive) {
                if (this@ActivityViewModel.selectedDate.value != selectedDate) break

                val existing = _chartData.value
                val start = if (existing.isEmpty()) {
                    selectedDate.atStartOfDay(zoneId).toInstant()
                } else {
                    val last = existing.last().timeLabel
                    val h = last.substringBefore(":").toIntOrNull() ?: 0
                    val m = last.substringAfter(":").toIntOrNull() ?: 0
                    LocalDateTime.of(selectedDate, LocalTime.of(h, m))
                        .atZone(zoneId).toInstant()
                }
                val end = Instant.now()

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

                    _chartData.value = (_chartData.value + newPoints)
                        .groupBy { it.timeLabel }
                        .map { it.value.last() }
                        .sortedBy { it.timeLabel }

                    val lastVal = _chartData.value.lastOrNull()?.value ?: 0f
                    ActivityAlertStore.setLatestActivity(roomId, lastVal.toInt())

                    val isDangerNow = lastVal >= THRESHOLD
                    ActivityAlertStore.set(roomId, isDangerNow)
                } else {
                    Log.e(TAG, "getActivity: $response")
                }

                // 1분 간격 폴링
                delay(60_000L)
            }
        }
    }

    fun fetchPresence(token: String, roomId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.apiService.getPresence("Bearer $token", roomId)
                if (resp.isSuccessful) {
                    val isPresent = resp.body()?.isPresent == true
                    _presenceByRoomId.value = _presenceByRoomId.value.toMutableMap().apply {
                        this[roomId] = isPresent
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    fun fetchRooms(token: String, homeId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
                if (resp.isSuccessful) {
                    _rooms.value = resp.body()?.rooms ?: emptyList()
                } else {
                    _rooms.value = emptyList()
                }
            } catch (_: Throwable) {
                _rooms.value = emptyList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activityJob?.cancel()
        activityJob = null
    }
}
