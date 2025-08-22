package com.aitronbiz.arron.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Room
import com.aitronbiz.arron.model.ChartPoint
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.*

data class RespStats(val current: Int, val min: Int, val max: Int, val avg: Int)

class RespirationViewModel : ViewModel() {
    private val _chartData = MutableStateFlow<List<ChartPoint>>(emptyList())
    val chartData: StateFlow<List<ChartPoint>> = _chartData

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

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

    private var pollingJob: Job? = null

    fun updateSelectedDate(date: LocalDate) {
        if (date.isAfter(LocalDate.now())) return
        _selectedDate.value = date
        _chartData.value = emptyList()
        _currentBpm.value = 0f
        _stats.value = RespStats(0, 0, 0, 0)
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
                } else {
                    Log.e(TAG, "getPresence: ${resp.code()} ${resp.message()}")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "getPresence error", t)
            }
        }
    }

    fun fetchRespirationData(roomId: String, selectedDate: LocalDate) {
        stopPolling()

        val token = AppController.prefs.getToken().orEmpty()
        if (token.isBlank()) {
            _chartData.value = emptyList()
            _currentBpm.value = 0f
            _stats.value = RespStats(0, 0, 0, 0)
            return
        }

        pollingJob = viewModelScope.launch {
            if (selectedDate.isBefore(LocalDate.now())) {
                fetchOnceAndPublish(roomId, selectedDate)
                return@launch
            }

            while (isActive) {
                fetchOnceAndPublish(roomId, selectedDate)

                val now = System.currentTimeMillis()
                val nextMinute = ((now / 60_000) + 1) * 60_000
                delay(nextMinute - now)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun fetchOnceAndPublish(roomId: String, date: LocalDate) {
        val zone = ZoneId.systemDefault()
        val minuteAvg = FloatArray(1440) { 0f }

        val startInstant = date.atStartOfDay(zone).toInstant()
        val endInstant = date.plusDays(1).atStartOfDay(zone).toInstant()
        val startIso = startInstant.toString()
        val endIso = endInstant.toString()

        try {
            val res = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getRespiration(
                    "Bearer ${AppController.prefs.getToken()}",
                    roomId,
                    startIso,
                    endIso
                )
            }
            if (res.isSuccessful) {
                val list = res.body()?.breathing.orEmpty()

                val pairs = list.mapNotNull { b ->
                    val ts = (b.endTime.ifBlank { b.startTime })
                    val t = runCatching { Instant.parse(ts) }.getOrNull() ?: return@mapNotNull null
                    if (t >= startInstant && t < endInstant) t to b.breathingRate else null
                }.sortedBy { it.first }

                if (pairs.isNotEmpty()) {
                    val acc = Array(1440) { mutableListOf<Float>() }
                    pairs.forEach { (inst, v) ->
                        val lt = inst.atZone(zone).toLocalTime()
                        val idx = lt.hour * 60 + lt.minute
                        if (idx in 0..1439) acc[idx].add(v)
                    }
                    for (i in 0..1439) {
                        minuteAvg[i] = if (acc[i].isEmpty()) 0f else (acc[i].sum() / acc[i].size)
                    }
                }
            } else {
                Log.e(TAG, "getRespiration: $res")
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "fetchOnceAndPublish error", e)
        }

        val nowMin = if (date == LocalDate.now())
            LocalTime.now().let { (it.hour * 60 + it.minute).coerceIn(0, 1439) }
        else 1439

        _currentBpm.value = if (date == LocalDate.now()) minuteAvg[nowMin] else minuteAvg.last()

        val upto = nowMin.coerceIn(0, 1439)
        val values = (0..upto).map { minuteAvg[it] }
        val nonZero = values.filter { it > 0f }
        val minVal = if (nonZero.isEmpty()) 0 else nonZero.minOrNull()!!.toInt()
        val maxVal = values.maxOrNull()?.toInt() ?: 0
        val avgVal = if (nonZero.isEmpty()) 0 else nonZero.average().toInt()
        _stats.value = RespStats(_currentBpm.value.toInt(), minVal, maxVal, avgVal)

        _chartData.value = List(1440) { m ->
            val h = m / 60
            val mm = m % 60
            ChartPoint(String.format("%02d:%02d", h, mm), minuteAvg[m])
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
