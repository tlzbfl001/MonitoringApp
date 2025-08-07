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
import com.aitronbiz.arron.entity.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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

    private var respirationJob: Job? = null

    val roomMap = mutableMapOf<String, Float>()

    val roomPresenceMap = mutableStateMapOf<String, PresenceResponse>()

    private val _autoScrollEnabled = MutableStateFlow(true)
    val autoScrollEnabled: StateFlow<Boolean> = _autoScrollEnabled

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    fun setAutoScrollEnabled(enabled: Boolean) {
        _autoScrollEnabled.value = enabled
    }

    fun resetState() {
        _rooms.value = emptyList()
        _selectedRoomId.value = ""
        _chartData.value = emptyList()
        roomMap.clear()
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
                Log.d(TAG, "homeId: $homeId")
                val res = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
                if (res.isSuccessful) {
                    val roomList = res.body()?.rooms ?: emptyList()
                    _rooms.value = roomList
                    Log.d(TAG, "roomList: $roomList")

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

    fun fetchRespirationData(token: String, roomId: String, selectedDate: LocalDate) {
        if (selectedDate != LocalDate.now() || !_rooms.value.any { it.id == roomId }) return

        respirationJob?.cancel()
        respirationJob = viewModelScope.launch {
            val formatterHHmm = DateTimeFormatter.ofPattern("HH:mm")
            val zoneId = ZoneId.systemDefault()
            val startOfDay = selectedDate.atStartOfDay(zoneId).toInstant()

            while (isActive) {
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
                        }.distinctBy { it.timeLabel }
                            .sortedBy { it.timeLabel }

                        val now = LocalTime.now()
                        val nowIndex = now.hour * 60 + now.minute
                        val slots = MutableList(nowIndex + 1) { index ->
                            val h = index / 60
                            val m = index % 60
                            val label = "%02d:%02d".format(h, m)
                            ChartPoint(label, 0f)
                        }

                        val map = chartPoints.associateBy { it.timeLabel }
                        val filled = slots.map { map[it.timeLabel] ?: it }

                        _chartData.value = filled
                    } else {
                        Log.e(TAG, "getRespiration: ${res.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "getRespiration", e)
                }
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