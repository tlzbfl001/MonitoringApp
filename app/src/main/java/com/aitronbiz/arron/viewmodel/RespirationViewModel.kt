package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.aitronbiz.arron.entity.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class RespirationViewModel : ViewModel() {
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

    val roomRespirationMap = mutableMapOf<String, Float>()

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

    fun fetchRespirationData(token: String, roomId: String, selectedDate: LocalDate) {
        if (selectedDate != LocalDate.now()) return

        viewModelScope.launch {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val seenLabels = _chartData.value.map { it.timeLabel }.toMutableSet()

            while (selectedRoomId.value == roomId && selectedDate == LocalDate.now()) {
                try {
                    val response = RetrofitClient.apiService.getRespiration("Bearer $token", roomId)
                    if (response.isSuccessful) {
                        val body = response.body()
                        val rawList = body?.breathing ?: emptyList()
                        Log.d(TAG, "rawList: $rawList")

                        val newPoints = rawList.mapNotNull {
                            val time = Instant.parse(it.createdAt)
                                .atZone(ZoneId.systemDefault())
                                .toLocalTime()
                                .truncatedTo(ChronoUnit.MINUTES)
                                .format(formatter)

                            if (time in seenLabels) null
                            else {
                                seenLabels += time
                                ChartPoint(time, it.breathingRate.toFloat())
                            }
                        }.sortedBy { it.timeLabel }

                        if (newPoints.isNotEmpty()) {
                            _chartData.value = _chartData.value + newPoints

                            val lastValue = newPoints.lastOrNull()?.value
                            if (lastValue != null) {
                                roomRespirationMap[roomId] = lastValue
                            }
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("Respiration", "API Error: $errorBody")
                    }
                } catch (e: Exception) {
                    Log.e("Respiration", "Exception", e)
                }

                delay(60_000) // 1분마다 polling
            }
        }
    }
}