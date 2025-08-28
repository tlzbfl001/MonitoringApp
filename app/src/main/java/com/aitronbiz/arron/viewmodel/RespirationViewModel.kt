package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class RespStats(
    val cur: Int,
    val min: Int,
    val max: Int,
    val avg: Int
)

class RespirationViewModel : ViewModel() {
    private val _chartData = MutableStateFlow<List<ChartPoint>>(emptyList())
    val chartData: StateFlow<List<ChartPoint>> = _chartData

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex

    private val _currentBpm = MutableStateFlow(0f)
    val currentBpm: StateFlow<Float> = _currentBpm

    private val _stats = MutableStateFlow(RespStats(0, 0, 0, 0))
    val stats: StateFlow<RespStats> = _stats

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val _presenceByRoomId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val presenceByRoomId: StateFlow<Map<String, Boolean>> = _presenceByRoomId

    private val _tick = MutableStateFlow(0L)
    val tick: StateFlow<Long> = _tick

    private var pollingJob: Job? = null

    fun updateSelectedDate(date: LocalDate) {
        if (date.isAfter(LocalDate.now())) return
        _selectedDate.value = date
        _chartData.value = emptyList()
        _currentBpm.value = 0f
        _stats.value = RespStats(0, 0, 0, 0)
        _selectedIndex.value = 0
        stopPolling()
    }

    fun selectBar(index: Int) {
        _selectedIndex.value = index
    }

    fun fetchRooms(token: String, homeId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.apiService.getAllRoom("Bearer $token", homeId)
                _rooms.value = if (resp.isSuccessful) resp.body()?.rooms ?: emptyList() else emptyList()
            } catch (t: Throwable) {
                _rooms.value = emptyList()
                Log.e(TAG, "getAllRoom: $t")
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
                } else {
                    Log.e(TAG, "getPresence: $resp")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "getPresence: $t")
            }
        }
    }

    fun fetchRespirationData(roomId: String, date: LocalDate) {
        stopPolling()

        val token = AppController.prefs.getToken().orEmpty()
        if (token.isBlank()) {
            clearUi()
            return
        }

        pollingJob = viewModelScope.launch {
            if (date.isBefore(LocalDate.now())) {
                fetchOnceAndPublishSingle(roomId, date)
                return@launch
            }
            while (isActive) {
                fetchOnceAndPublishSingle(roomId, date)
                val now = System.currentTimeMillis()
                val nextMinute = ((now / 60_000) + 1) * 60_000
                delay(nextMinute - now)
            }
        }
    }

    fun fetchRespirationDataAll(roomIds: List<String>, date: LocalDate) {
        stopPolling()

        val token = AppController.prefs.getToken().orEmpty()
        if (token.isBlank() || roomIds.isEmpty()) {
            clearUi()
            return
        }

        pollingJob = viewModelScope.launch {
            if (date.isBefore(LocalDate.now())) {
                fetchOnceAndPublishAll(roomIds, date)
                return@launch
            }
            while (isActive) {
                fetchOnceAndPublishAll(roomIds, date)
                val now = System.currentTimeMillis()
                val nextMinute = ((now / 60_000) + 1) * 60_000
                delay(nextMinute - now)
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

    private fun clearUi() {
        _chartData.value = emptyList()
        _currentBpm.value = 0f
        _stats.value = RespStats(0, 0, 0, 0)
        _selectedIndex.value = 0
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun fetchOnceAndPublishSingle(roomId: String, date: LocalDate) {
        val zone = ZoneId.systemDefault()
        val (startInstant, endInstant) = boundsFor(date, zone)
        val minuteAvg = minuteAvgForRoom(roomId, startInstant, endInstant, zone)
        publish(minuteAvg, date)
    }

    private suspend fun fetchOnceAndPublishAll(roomIds: List<String>, date: LocalDate) {
        val zone = ZoneId.systemDefault()
        val (startInstant, endInstant) = boundsFor(date, zone)

        val roomMinuteArrays = mutableListOf<FloatArray>()
        for (rid in roomIds) {
            val arr = minuteAvgForRoom(rid, startInstant, endInstant, zone)
            roomMinuteArrays.add(arr)
        }

        val agg = FloatArray(1440) { 0f }
        for (m in 0..1439) {
            var sum = 0f
            var cnt = 0
            for (arr in roomMinuteArrays) {
                val v = arr[m]
                if (v > 0f) { sum += v; cnt++ }
            }
            agg[m] = if (cnt > 0) sum / cnt else 0f
        }

        publish(agg, date)
    }

    private fun boundsFor(date: LocalDate, zone: ZoneId): Pair<Instant, Instant> {
        val start = date.atStartOfDay(zone).toInstant()
        val end = if (date == LocalDate.now()) Instant.now()
        else date.plusDays(1).atStartOfDay(zone).toInstant()
        return start to end
    }

    private suspend fun minuteAvgForRoom(
        roomId: String,
        start: Instant,
        end: Instant,
        zone: ZoneId
    ): FloatArray {
        val minuteAvg = FloatArray(1440) { 0f }
        try {
            val res = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getRespiration(
                    "Bearer ${AppController.prefs.getToken()}",
                    roomId,
                    start.toString(),
                    end.toString()
                )
            }
            if (res.isSuccessful) {
                val list = res.body()?.breathing.orEmpty()
                if (list.isNotEmpty()) {
                    val buckets = Array(1440) { mutableListOf<Float>() }
                    list.forEach { b ->
                        val ts = (b.endTime.ifBlank { b.startTime })
                        val t = parseInstantFlexible(ts, zone) ?: return@forEach
                        if (t >= start && t < end) {
                            val lt = t.atZone(zone).toLocalTime()
                            val idx = lt.hour * 60 + lt.minute
                            if (idx in 0..1439) buckets[idx].add(b.breathingRate)
                        }
                    }
                    for (i in 0..1439) {
                        minuteAvg[i] = if (buckets[i].isEmpty()) 0f
                        else (buckets[i].sum() / buckets[i].size)
                    }
                }
            } else {
                Log.e(TAG, "getRespiration: $res")
            }
        } catch (e: Exception) {
            Log.e(TAG, "minuteAvgForRoom error", e)
        }
        return minuteAvg
    }

    private fun publish(minuteAvg: FloatArray, date: LocalDate) {
        val nowMin = if (date == LocalDate.now()) {
            val now = LocalTime.now()
            (now.hour * 60 + now.minute).coerceIn(0, 1439)
        } else 1439

        // 현재 호흡수
        _currentBpm.value = minuteAvg[nowMin]

        // 통계
        val values = (0..nowMin).map { minuteAvg[it] }
        val nonZero = values.filter { it > 0f }

        val minVal = nonZero.minOrNull()?.roundToInt() ?: 0
        val maxVal = values.maxOrNull()?.roundToInt() ?: 0
        val avgVal = if (nonZero.isNotEmpty()) nonZero.average().roundToInt() else 0

        _stats.value = RespStats(_currentBpm.value.roundToInt(), minVal, maxVal, avgVal)

        // 차트 데이터
        _chartData.value = List(1440) { m ->
            val h = m / 60
            val mm = m % 60
            ChartPoint(String.format("%02d:%02d", h, mm), minuteAvg[m])
        }

        _selectedIndex.value = nowMin
        _tick.value = System.currentTimeMillis()
    }

    private fun parseInstantFlexible(ts: String, zone: ZoneId): Instant? {
        runCatching { return Instant.parse(ts) }
        return runCatching {
            val ldt = LocalDateTime.parse(ts, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ldt.atZone(zone).toInstant()
        }.getOrNull()
    }
}
