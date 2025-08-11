package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.PresenceResponse
import com.aitronbiz.arron.api.response.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.util.ActivityAlertStore
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val _selectedRoomId = MutableStateFlow("")
    val selectedRoomId: StateFlow<String> = _selectedRoomId

    private var activityJob: Job? = null

    val roomMap = mutableMapOf<String, Float>()

    val roomPresenceMap = mutableStateMapOf<String, PresenceResponse>()

    private val THRESHOLD = 50f

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        _chartData.value = emptyList()
        // 오늘이 아니면 경고 끔
        if (date != LocalDate.now()) {
            val rid = _selectedRoomId.value
            if (rid.isNotBlank()) ActivityAlertStore.set(rid, false)
        }
    }

    fun resetState() {
        _rooms.value = emptyList()
        _selectedRoomId.value = ""
        _chartData.value = emptyList()
        roomMap.clear()
        _selectedIndex.value = -1
        _selectedDate.value = LocalDate.now()
    }

    fun selectBar(index: Int) {
        _selectedIndex.value = index
    }

    fun selectRoom(roomId: String) {
        val old = _selectedRoomId.value
        if (old.isNotBlank()) ActivityAlertStore.set(old, false)
        _selectedRoomId.value = roomId
        _chartData.value = emptyList()
    }

    fun fetchRooms(token: String, homeId: String) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
                if (res.isSuccessful) {
                    val roomList = res.body()?.rooms ?: emptyList()
                    _rooms.value = roomList

                    if (roomList.isEmpty()) {
                        _selectedRoomId.value = ""
                        _chartData.value = emptyList()
                        roomMap.clear()
                    } else if (_selectedRoomId.value.isBlank() || roomList.none { it.id == _selectedRoomId.value }) {
                        _selectedRoomId.value = roomList.first().id
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchActivityData(token: String, roomId: String, selectedDate: LocalDate) {
        activityJob?.cancel()
        activityJob = viewModelScope.launch {
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC"))

            while (isActive && _selectedRoomId.value == roomId) {
                val start: Instant
                val end: Instant

                if (selectedDate == today) {
                    val existing = _chartData.value
                    start = if (existing.isEmpty()) {
                        // 오늘 00:00 ~ now
                        selectedDate.atStartOfDay(zoneId).toInstant()
                    } else {
                        // 차트 마지막 라벨 이후 ~ now
                        val lastLabel = existing.last().timeLabel
                        val h = lastLabel.substringBefore(":").toInt()
                        val m = lastLabel.substringAfter(":").toInt()
                        LocalDateTime.of(selectedDate, LocalTime.of(h, m))
                            .atZone(zoneId).toInstant()
                    }
                    end = Instant.now()
                } else {
                    // 과거 날짜: 하루 전체
                    start = selectedDate.atStartOfDay(zoneId).toInstant()
                    end = selectedDate.atTime(23, 59, 59).atZone(zoneId).toInstant()
                }

                val response = try {
                    RetrofitClient.apiService.getActivity(
                        token = "Bearer $token",
                        roomId = roomId,
                        startTime = formatter.format(start),
                        endTime = formatter.format(end)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "getActivity", e)
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

                    if (_selectedRoomId.value == roomId) {
                        _chartData.value = if (selectedDate == today) {
                            (_chartData.value + newPoints)
                                .groupBy { it.timeLabel }
                                .map { it.value.last() }
                                .sortedBy { it.timeLabel }
                        } else {
                            newPoints
                        }

                        val alert = (selectedDate == today) && _chartData.value.any { it.value >= THRESHOLD }
                        ActivityAlertStore.set(roomId, alert)
                    }
                } else if (response != null) {
                    Log.e(TAG, "getActivity: ${response.code()}")
                }

                // 과거 날짜는 한 번만 조회하고 경고 끔
                if (selectedDate != today) {
                    ActivityAlertStore.set(roomId, false)
                    break
                }

                delay(60_000L)
            }
        }
    }

    private fun fetchPresence(token: String, roomId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getPresence("Bearer $token", roomId)
                if (response.isSuccessful) {
                    response.body()?.let { presence ->
                        roomPresenceMap[roomId] = presence
                    }
                } else {
                    Log.e(TAG, "fetchPresence: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchPresence", e)
            }
        }
    }

    fun fetchAllPresence(token: String) {
        val currentRooms = _rooms.value
        currentRooms.forEach { room ->
            fetchPresence(token, room.id)
        }
    }
}