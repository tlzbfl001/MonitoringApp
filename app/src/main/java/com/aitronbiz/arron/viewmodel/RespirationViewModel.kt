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

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    companion object {
        private const val TAG = "RespirationViewModel"
    }

    fun resetState() {
        _rooms.value = emptyList()
        _selectedRoomId.value = ""
        _chartData.value = emptyList()
        roomRespirationMap.clear()
        _selectedIndex.value = -1
        _toastMessage.value = null
        _selectedDate.value = LocalDate.now()
    }

    fun consumeToast() {
        _toastMessage.value = null
    }

    fun updateSelectedDate(date: LocalDate) {
        if (date != LocalDate.now()) {
            _toastMessage.value = "서버에 데이터가 없습니다"
            return
        }
        _selectedDate.value = date
        _chartData.value = emptyList()
    }

    fun selectBar(index: Int) {
        _selectedIndex.value = index
    }

    fun selectRoom(roomId: String) {
        _selectedRoomId.value = roomId
        _chartData.value = emptyList() // 룸 변경 시 이전 차트 초기화
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
                        roomRespirationMap.clear()
                    } else if (_selectedRoomId.value.isBlank() || roomList.none { it.id == _selectedRoomId.value }) {
                        _selectedRoomId.value = roomList.first().id
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchRespirationData(token: String, roomId: String, selectedDate: LocalDate) {
        if (selectedDate != LocalDate.now() || !_rooms.value.any { it.id == roomId }) return

        viewModelScope.launch {
            val formatterHHmm = DateTimeFormatter.ofPattern("HH:mm")
            val zoneId = ZoneId.systemDefault()
            val startOfDay = selectedDate.atStartOfDay(zoneId).toInstant()

            while (_selectedRoomId.value == roomId && selectedDate == LocalDate.now()) {
                try {
                    val res = RetrofitClient.apiService.getRespiration("Bearer $token", roomId)
                    if (res.isSuccessful) {
                        val list = res.body()?.breathing ?: emptyList()

                        val filtered = list.filter {
                            val created = Instant.parse(it.createdAt)
                            created >= startOfDay
                        }

                        val chartPoints = filtered.map {
                            val timeLabel = Instant.parse(it.createdAt)
                                .atZone(zoneId)
                                .toLocalTime()
                                .truncatedTo(ChronoUnit.MINUTES)
                                .format(formatterHHmm)
                            ChartPoint(timeLabel, it.breathingRate.toFloat())
                        }.distinctBy { it.timeLabel }.sortedBy { it.timeLabel }

                        _chartData.value = chartPoints

                        if (chartPoints.isNotEmpty()) {
                            roomRespirationMap[roomId] = chartPoints.last().value
                        }

                    } else {
                        Log.e(TAG, "API Error: ${res.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during respiration fetch", e)
                }

                delay(60_000)
            }
        }
    }
}