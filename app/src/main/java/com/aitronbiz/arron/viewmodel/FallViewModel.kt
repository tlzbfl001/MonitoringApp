package com.aitronbiz.arron.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.*
import java.time.format.DateTimeFormatter

class FallViewModel(application: Application) : AndroidViewModel(application) {
    val selectedIndex = MutableStateFlow(0)
    fun selectBar(minuteOfDay: Int) { selectedIndex.value = minuteOfDay.coerceIn(0, 1439) }

    private val _chartPoints = MutableStateFlow<List<ChartPoint>>(emptyList())
    val chartPoints: StateFlow<List<ChartPoint>> = _chartPoints

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount

    var roomName = mutableStateOf("")
        private set

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val _presenceByRoomId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val presenceByRoomId: StateFlow<Map<String, Boolean>> = _presenceByRoomId

    private var selectedDate: LocalDate = LocalDate.now()
    private var pollJob: Job? = null

    fun updateSelectedDate(date: LocalDate) {
        selectedDate = date
        _chartPoints.value = emptyList()
        _totalCount.value = 0
        stopPolling()
    }

    fun fetchRoomName(token: String, roomId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.apiService.getRoom("Bearer $token", roomId)
                roomName.value = if (resp.isSuccessful) {
                    resp.body()?.room?.name.orEmpty().ifBlank { "장소" }
                } else "장소"
            } catch (t: Throwable) {
                roomName.value = "장소"
                Log.e(TAG, "getRoom error", t)
            }
        }
    }

    fun fetchRooms(token: String, homeId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
                _rooms.value = if (resp.isSuccessful) resp.body()?.rooms ?: emptyList() else emptyList()
            } catch (t: Throwable) {
                _rooms.value = emptyList()
                Log.e(TAG, "getAllRoom error", t)
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
            } catch (t: Throwable) {
                Log.e(TAG, "getPresence error", t)
            }
        }
    }

    fun fetchFallsData(token: String, roomId: String, date: LocalDate) {
        stopPolling()
        selectedDate = date

        pollJob = viewModelScope.launch {
            if (date.isBefore(LocalDate.now())) {
                val (points, count) = loadFallsOnce(token, roomId, date)
                _chartPoints.value = points
                _totalCount.value = count
                return@launch
            }

            while (isActive) {
                val (points, count) = loadFallsOnce(token, roomId, date)
                _chartPoints.value = points
                _totalCount.value = count

                val now = System.currentTimeMillis()
                val nextMinute = ((now / 60_000) + 1) * 60_000
                delay(nextMinute - now)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun loadFallsOnce(
        token: String,
        roomId: String,
        date: LocalDate
    ): Pair<List<ChartPoint>, Int> {
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"))

        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()

        return try {
            val res = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getFalls(
                    token = "Bearer $token",
                    roomId = roomId,
                    startTime = formatter.format(start),
                    endTime = formatter.format(end)
                )
            }

            if (res.isSuccessful) {
                Log.d(TAG, "res: ${res.body()}")
                val alerts = res.body()?.alerts.orEmpty()

                val total = alerts.size

                val points = alerts.mapNotNull { a ->
                    val t = runCatching { Instant.parse(a.detectedAt) }.getOrNull()
                        ?: return@mapNotNull null
                    if (t < start || t >= end) return@mapNotNull null
                    val lt = t.atZone(zone).toLocalTime()
                    ChartPoint(String.format("%02d:%02d", lt.hour, lt.minute), 1f)
                }.sortedBy { it.timeLabel }

                points to total
            } else {
                Log.e(TAG, "getFalls: $res")
                emptyList<ChartPoint>() to 0
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "loadFallsOnce error", e)
            emptyList<ChartPoint>() to 0
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}