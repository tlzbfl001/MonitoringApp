package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.api.response.Room
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

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val _selectedRoomId = MutableStateFlow("")
    val selectedRoomId: StateFlow<String> = _selectedRoomId

    val roomActivityMap = mutableMapOf<String, Float>()

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        _chartData.value = emptyList()
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
                    res.body()?.let {
                        _rooms.value = it.rooms
                        if (_selectedRoomId.value.isBlank() && it.rooms.isNotEmpty()) {
                            _selectedRoomId.value = it.rooms[0].id
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchActivityData(token: String, roomId: String, selectedDate: LocalDate) {
        viewModelScope.launch {
            val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC")) // 서버로 보낼 때는 UTC 기준 포맷

            val seoulZone = ZoneId.of("Asia/Seoul")
            val start: Instant
            val end: Instant

            if (selectedDate == today) {
                // 실시간 업데이트 시(오늘)
                val existingData = _chartData.value
                start = if (existingData.isEmpty()) {
                    selectedDate.atStartOfDay(seoulZone).toInstant()
                } else {
                    val lastLabel = existingData.last().timeLabel
                    val hour = lastLabel.substringBefore(":").toInt()
                    val minute = lastLabel.substringAfter(":").toInt()
                    val lastTime = LocalDateTime.of(selectedDate, LocalTime.of(hour, minute)).plusMinutes(10)
                    lastTime.atZone(seoulZone).toInstant()
                }
                end = Instant.now() // 현재 UTC 기준 시간
            } else {
                // 하루 전체 범위 요청
                start = selectedDate.atStartOfDay(seoulZone).toInstant()
                end = selectedDate.atTime(23, 59, 59).atZone(seoulZone).toInstant()
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
                    Log.d(TAG, "body: $body")

                    val updatedPoints = list.sortedBy {
                        Instant.parse(it.startTime)
                    }.map {
                        val localTime = Instant.parse(it.startTime)
                            .atZone(seoulZone) // UTC → KST 변환
                            .toLocalTime()
                            .truncatedTo(ChronoUnit.MINUTES)
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                        ChartPoint(localTime, it.activityScore.toFloat())
                    }

                    if (_selectedRoomId.value == roomId) {
                        if (selectedDate == today) {
                            _chartData.value = (_chartData.value + updatedPoints).distinctBy { it.timeLabel }
                        } else {
                            _chartData.value = updatedPoints.distinctBy { it.timeLabel }
                        }
                    }

                    val lastValue = updatedPoints.lastOrNull()?.value
                    if (lastValue != null) {
                        roomActivityMap[roomId] = lastValue
                    }
                } else {
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