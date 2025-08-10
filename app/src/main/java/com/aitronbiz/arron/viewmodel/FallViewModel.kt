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
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class FallViewModel : ViewModel() {
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

    val roomMap = mutableMapOf<String, Float>()

    val roomPresenceMap = mutableStateMapOf<String, PresenceResponse>()

    fun resetState() {
        _rooms.value = emptyList()
        _selectedRoomId.value = ""
        _chartData.value = emptyList()
        roomMap.clear()
        _selectedIndex.value = -1
        _selectedDate.value = LocalDate.now()
    }

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

    fun fetchFallData(token: String, roomId: String, selectedDate: LocalDate) {

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