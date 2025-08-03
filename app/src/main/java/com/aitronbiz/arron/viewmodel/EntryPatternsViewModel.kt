package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.HourlyPattern
import com.aitronbiz.arron.api.response.PresenceResponse
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.api.response.WeeklyPattern
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EntryPatternsViewModel : ViewModel() {
    private val _entryPatterns = MutableStateFlow<LifePatternsData?>(null)
    val entryPatterns: StateFlow<LifePatternsData?> = _entryPatterns

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val _selectedRoomId = MutableStateFlow("")
    val selectedRoomId: StateFlow<String> = _selectedRoomId

    val roomPresenceMap = mutableStateMapOf<String, PresenceResponse>()

    fun resetState(token: String, homeId: String) {
        _entryPatterns.value = null
        _rooms.value = emptyList()
        _selectedRoomId.value = ""
        fetchRooms(token, homeId)
    }

    fun selectRoom(roomId: String, token: String) {
        _selectedRoomId.value = roomId
        fetchEntryPatternsData(token, roomId)
    }

    private fun fetchRooms(token: String, homeId: String) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
                if (res.isSuccessful) {
                    val roomList = res.body()?.rooms ?: emptyList()
                    _rooms.value = roomList

                    if (roomList.isNotEmpty()) {
                        val firstRoom = roomList.first().id
                        _selectedRoomId.value = firstRoom
                        fetchEntryPatternsData(token, firstRoom)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchEntryPatternsData(token: String, roomId: String) {
        viewModelScope.launch {
            try {
                val hourlyRes = RetrofitClient.apiService.getHourlyEntryPatterns("Bearer $token", roomId)
                val weeklyRes = RetrofitClient.apiService.getWeeklyEntryPatterns("Bearer $token", roomId)

                if (hourlyRes.isSuccessful && weeklyRes.isSuccessful) {
                    _entryPatterns.value = LifePatternsData(
                        hourlyPatterns = hourlyRes.body()?.patterns ?: emptyList(),
                        weeklyPatterns = weeklyRes.body()?.patterns ?: emptyList()
                    )
                } else {
                    _entryPatterns.value = null
                    Log.e(TAG, "getHourlyEntryPatterns: ${hourlyRes.code()}")
                    Log.e(TAG, "getWeeklyEntryPatterns: ${weeklyRes.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _entryPatterns.value = null
                Log.e(TAG, "getEntryPatterns: $e")
            }
        }
    }
}

data class LifePatternsData(
    val hourlyPatterns: List<HourlyPattern> = emptyList(),
    val weeklyPatterns: List<WeeklyPattern> = emptyList()
)