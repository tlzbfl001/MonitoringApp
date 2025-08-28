package com.aitronbiz.arron.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val _presenceByRoomId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val presenceByRoomId: StateFlow<Map<String, Boolean>> = _presenceByRoomId

    private val _chartData = MutableStateFlow<List<ChartPoint>>(emptyList())
    val chartData: StateFlow<List<ChartPoint>> = _chartData

    private val _isLoading = MutableStateFlow(false)

    private val _selectedIndex = MutableStateFlow(0)
    fun selectBar(idx: Int) { _selectedIndex.value = idx }

    private var selectedDate: LocalDate = LocalDate.now()

    fun updateSelectedDate(date: LocalDate) {
        selectedDate = date
        _chartData.value = emptyList()
    }

    fun fetchRooms(token: String, homeId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
                _rooms.value = if (resp.isSuccessful) resp.body()?.rooms ?: emptyList() else emptyList()
            } catch (t: Throwable) {
                Log.e(TAG, "getAllRoom error", t)
                _rooms.value = emptyList()
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

    // 단일 방 활동 데이터
    fun fetchActivityData(token: String, roomId: String, date: LocalDate) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = loadActivityRaw(token, roomId, date)
                _chartData.value = list
            } catch (t: Throwable) {
                if (t !is CancellationException) Log.e(TAG, "fetchActivityData", t)
                _chartData.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChart() { _chartData.value = emptyList() }

    fun fetchActivityDataAll(token: String, roomIds: List<String>, date: LocalDate) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val lists = roomIds.map { id ->
                    async { loadActivityRaw(token, id, date) }
                }.awaitAll()

                // 시간 라벨("HH:mm") 기준 합산
                val map = linkedMapOf<String, Float>()
                for (l in lists) {
                    for (p in l) {
                        map[p.timeLabel] = (map[p.timeLabel] ?: 0f) + p.value
                    }
                }
                val merged = map.entries
                    .sortedBy { it.key }
                    .map { ChartPoint(it.key, it.value) }

                _chartData.value = merged
            } catch (t: Throwable) {
                if (t !is CancellationException) Log.e(TAG, "fetchActivityDataAll", t)
                _chartData.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkNotifications(onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!AppController.prefs.getToken().isNullOrEmpty()) {
                    val response = RetrofitClient.apiService.getNotification("Bearer ${AppController.prefs.getToken()}", 1, 40)
                    if (response.isSuccessful) {
                        val notifications = response.body()?.notifications ?: emptyList()
                        val hasUnread = notifications.any { it.isRead == false }
                        withContext(Dispatchers.Main) { onResult(hasUnread) }
                    } else {
                        withContext(Dispatchers.Main) { onResult(false) }
                    }
                } else {
                    withContext(Dispatchers.Main) { onResult(false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    private suspend fun loadActivityRaw(
        token: String,
        roomId: String,
        date: LocalDate
    ): List<ChartPoint> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"))

        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()

        fun parseInstantOrNull(s: String?): Instant? =
            if (s.isNullOrBlank()) null else runCatching { Instant.parse(s) }.getOrNull()

        try {
            val res = RetrofitClient.apiService.getActivity(
                token = "Bearer $token",
                roomId = roomId,
                startTime = formatter.format(start),
                endTime = formatter.format(end)
            )
            if (!res.isSuccessful) return@withContext emptyList()

            val items = res.body()?.activityScores.orEmpty()

            val minuteMap = linkedMapOf<String, Float>()

            for (a in items) {
                val inst = parseInstantOrNull(a.endTime) ?: parseInstantOrNull(a.startTime) ?: continue
                if (inst < start || inst >= end) continue

                val lt = inst.atZone(zone).toLocalTime()
                val label = String.format("%02d:%02d", lt.hour, lt.minute)
                val value = a.activityScore.toFloat().coerceAtLeast(0f)

                minuteMap[label] = (minuteMap[label] ?: 0f) + value
            }

            minuteMap.entries
                .sortedBy { it.key }
                .map { ChartPoint(it.key, it.value) }
        } catch (e: Exception) {
            Log.e(TAG, "loadActivityRaw error", e)
            emptyList()
        }
    }
}