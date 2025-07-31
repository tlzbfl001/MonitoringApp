package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.api.response.PresenceResponse
import com.aitronbiz.arron.api.response.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.aitronbiz.arron.entity.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.google.gson.Gson
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

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        _chartData.value = emptyList()
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
        _selectedRoomId.value = roomId
        _chartData.value = emptyList() // 다른 룸 선택 시 이전 데이터 제거
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
        activityJob?.cancel() // 이전 Job 중지
        activityJob = viewModelScope.launch {
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC"))

            while (isActive && _selectedRoomId.value == roomId) {
                val start: Instant
                val end: Instant

                if (selectedDate == today) {
                    val existingData = _chartData.value
                    start = if (existingData.isEmpty()) {
                        // 첫 진입: 오늘 00:00부터 지금까지
                        selectedDate.atStartOfDay(zoneId).toInstant()
                    } else {
                        // 이후: 차트의 마지막 시간부터 지금까지
                        val lastLabel = existingData.last().timeLabel
                        val hour = lastLabel.substringBefore(":").toInt()
                        val minute = lastLabel.substringAfter(":").toInt()
                        val lastTime = LocalDateTime.of(selectedDate, LocalTime.of(hour, minute))
                        lastTime.atZone(zoneId).toInstant()
                    }
                    end = Instant.now()
                } else {
                    // 오늘이 아닌 경우: 하루 전체
                    start = selectedDate.atStartOfDay(zoneId).toInstant()
                    end = selectedDate.atTime(23, 59, 59).atZone(zoneId).toInstant()
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

                        val newPoints = list.sortedBy {
                            Instant.parse(it.startTime)
                        }.map {
                            val localTime = Instant.parse(it.startTime)
                                .atZone(zoneId)
                                .toLocalTime()
                                .truncatedTo(ChronoUnit.MINUTES)
                            ChartPoint(
                                String.format("%02d:%02d", localTime.hour, localTime.minute),
                                it.activityScore.toFloat()
                            )
                        }

                        if (_selectedRoomId.value == roomId) {
                            if (selectedDate == today) {
                                // 중복 제거 후 추가
                                _chartData.value = (_chartData.value + newPoints)
                                    .groupBy { it.timeLabel }
                                    .map { it.value.last() }
                                    .sortedBy { it.timeLabel }
                            } else {
                                _chartData.value = newPoints
                            }
                        }

                        Log.d(TAG, "getActivity: ${_chartData.value.size} points loaded")
                    } else {
                        Log.e(TAG, "getActivity 실패: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "getActivity 예외 발생", e)
                }

                // 오늘일 경우 주기적으로 반복, 과거일 경우 한 번만 실행
                if (selectedDate != today) break
                delay(60_000)
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
                    Log.e(TAG, "fetchPresence 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchPresence 예외 발생", e)
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