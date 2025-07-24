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
    }

    fun selectBar(index: Int) {
        _selectedIndex.value = index
    }

    fun selectRoom(roomId: String) {
        _selectedRoomId.value = roomId
        _chartData.value = emptyList() // 다른 룸 선택 시 이전 데이터 제거
    }

    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"))

    fun fetchRooms(token: String, homeId: String) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
                if (res.isSuccessful) {
                    res.body()?.let {
                        _rooms.value = it.rooms
                        // 디폴트 선택
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

                    // 만약 선택된 roomId에 대한 fetch라면 UI에 반영
                    if (_selectedRoomId.value == roomId) {
                        _chartData.value = (currentData + updatedPoints).distinctBy { it.timeLabel }
                    }

                    // room별 마지막 점수 저장
                    val lastValue = updatedPoints.lastOrNull()?.value
                    if (lastValue != null) {
                        roomActivityMap[roomId] = lastValue
                    }
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