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
import com.aitronbiz.arron.util.ActivityAlertStore
import com.aitronbiz.arron.util.CustomUtil.TAG
import kotlinx.coroutines.*
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
                fetchOnceAndPublish(token, roomId, selectedDate)
                return@launch
            }

            while (isActive) {
                fetchOnceAndPublish(token, roomId, selectedDate)

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

    private suspend fun fetchOnceAndPublish(token: String, roomId: String, date: LocalDate) {
        val zone = ZoneId.systemDefault()
        val minuteAvg = FloatArray(1440) { 0f }

        // 1) 하루의 시작/끝 인스턴트를 UTC ISO로
        val startInstant = date.atStartOfDay(zone).toInstant()
        val endInstant = date.plusDays(1).atStartOfDay(zone).toInstant()
        val startIso = startInstant.toString() // 예: 2025-08-18T15:00:00Z (KST 기준 전일 15시)
        val endIso = endInstant.toString()

        try {
            val res = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getRespiration(
                    "Bearer $token",
                    roomId,
                    startIso,
                    endIso
                )
            }
            if (res.isSuccessful) {
                val list = res.body()?.breathing ?: emptyList<Any>()

                // 2) (선택) 서버가 기간 필터링 했어도 안전하게 한 번 더 필터링/정렬
                val start = startInstant
                val end = endInstant

                val pairs = list.mapNotNull { item ->
                    val ts = getStringField(item, "createdAt") ?: return@mapNotNull null
                    val rate = getFloatField(item, "breathingRate") ?: return@mapNotNull null
                    val t = runCatching { Instant.parse(ts) }.getOrNull() ?: return@mapNotNull null
                    if (t >= start && t < end) t to rate else null
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
                Log.e(TAG, "getRespiration failed: code=${res.code()} msg=${res.message()}")
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "fetchOnceAndPublish error", e)
        }

        // 나머지 통계/차트 업데이트 로직은 기존 그대로
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

        if (date == LocalDate.now()) {
            val bpmNow = _currentBpm.value.toInt()
            ActivityAlertStore.setLatestResp(roomId, bpmNow)
        }
    }

    private fun getStringField(o: Any, name: String): String? =
        runCatching {
            val f = o::class.java.getDeclaredField(name).apply { isAccessible = true }
            f.get(o) as? String
        }.getOrNull()

    private fun getFloatField(o: Any, name: String): Float? =
        runCatching {
            val f = o::class.java.getDeclaredField(name).apply { isAccessible = true }
            when (val v = f.get(o)) {
                is Number -> v.toFloat()
                is String -> v.toFloatOrNull()
                else -> null
            }
        }.getOrNull()

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
